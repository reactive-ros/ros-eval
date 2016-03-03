package ros_eval;

import org.jgrapht.traverse.BreadthFirstIterator;
import org.rhea_core.Stream;
import org.rhea_core.distribution.Broker;
import org.rhea_core.distribution.Distributor;
import org.rhea_core.distribution.StreamTask;
import org.rhea_core.evaluation.EvaluationStrategy;
import org.rhea_core.internal.expressions.MultipleInputExpr;
import org.rhea_core.internal.expressions.NoInputExpr;
import org.rhea_core.internal.expressions.SingleInputExpr;
import org.rhea_core.internal.expressions.Transformer;
import org.rhea_core.internal.expressions.creation.FromSource;
import org.rhea_core.internal.graph.FlowGraph;
import org.rhea_core.internal.output.*;
import org.rhea_core.util.functions.Func0;
import ros_eval.ros_graph.RosEdge;
import ros_eval.ros_graph.RosGraph;
import ros_eval.ros_graph.RosNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Evaluates a dataflow graph by mapping one or more stream operators to individual ROS nodes
 * to run concurrently, using the ROS message system to communicate.
 * @author Orestis Melkonian
 */
public class RosEvaluationStrategy implements EvaluationStrategy {

    Func0<EvaluationStrategy> evaluationStrategy;
    Broker broker;
    Distributor executor;

    /**
     * Constructors
     */
    public RosEvaluationStrategy(Func0<EvaluationStrategy> evaluationStrategy, Broker broker) {
        this.evaluationStrategy = evaluationStrategy;
        this.broker = broker;
    }

    public RosEvaluationStrategy(Func0<EvaluationStrategy> evaluationStrategy, Broker broker, String nodePrefix) {
        this(evaluationStrategy, broker);
        this.nodePrefix = nodePrefix;
    }

    /**
     * Generators
     */
    String nodePrefix = "~";
    int topicCounter = 0, nodeCounter = 0;
    private String newName() {
        return nodePrefix + "_" + Integer.toString(nodeCounter++);
    }
    public RosTopic newTopic() {
        return new RosTopic(nodePrefix + "/" + Integer.toString(topicCounter++));
    }


    /**
     * Evaluation
     */
    private void execute(Queue<StreamTask> tasks) {
        Queue<RosTask> wrappers = new LinkedList<>(tasks.stream().map(t -> new RosTask(t, broker, newName())).collect(Collectors.toList()));
        executor.submit(wrappers);
    }

    @Override
    public <T> void evaluate(Stream<T> stream, Output output) {
        FlowGraph flow = stream.getGraph();
        RosGraph graph = new RosGraph(flow, this::newTopic);
        Queue<StreamTask> tasks = new LinkedList<>();

        // Add task to setup broker
        executor.executeOn(new RosBrokerTask(broker), broker.getIp());

        // Run output node first
        RosTopic result = newTopic();
        tasks.add(new StreamTask(evaluationStrategy, Stream.from(result), output, new ArrayList<>()));

        // Then run each graph vertex as an individual node (reverse BFS)
        Set<RosNode> checked = new HashSet<>();
        Stack<RosNode> stack = new Stack<>();
        for (RosNode root : graph.getRoots())
            new BreadthFirstIterator<>(graph, root).forEachRemaining(stack::push);
        while (!stack.empty()) {
            RosNode toExecute = stack.pop();
            if (checked.contains(toExecute)) continue;

            Set<RosEdge> inputs = graph.incomingEdgesOf(toExecute);
            Transformer transformer = toExecute.getTransformer();

            FlowGraph innerGraph = new FlowGraph();
            if (transformer instanceof NoInputExpr) {
                assert inputs.size() == 0;
                // 0 input
                innerGraph.addConnectVertex(transformer);
            } else if (transformer instanceof SingleInputExpr) {
                assert inputs.size() == 1;
                // 1 input
                RosTopic input = inputs.iterator().next().getTopic();
                Transformer toAdd = new FromSource<>(input.clone());
                innerGraph.addConnectVertex(toAdd);
                innerGraph.attach(transformer);
            } else if (transformer instanceof MultipleInputExpr) {
                assert inputs.size() > 1;
                // N inputs
                innerGraph.setConnectNodes(inputs.stream()
                        .map(edge -> new FromSource(edge.getTopic().clone()))
                        .collect(Collectors.toList()));
                innerGraph.attachMulti(transformer);
            }

            // Set outputs according to graph connections
            Set<RosEdge> outputs = graph.outgoingEdgesOf(toExecute);
            List<Output> list = new ArrayList<>();
            if (transformer == graph.toConnect)
                list.add(new SinkOutput<>(result.clone()));
            list.addAll(outputs.stream()
                    .map(RosEdge::getTopic)
                    .map((Function<RosTopic, SinkOutput<Object>>) (sink) -> {
                        return new SinkOutput(sink);
                    })
                    .collect(Collectors.toList()));
            Output outputToExecute = (list.size() == 1) ? list.get(0) : new MultipleOutput(list);

            // Schedule for execution
            tasks.add(new StreamTask(evaluationStrategy, new Stream(innerGraph), outputToExecute, new ArrayList<>()));

            checked.add(toExecute);
        }

        // Submit the tasks for execution
        execute(tasks);
    }
}
