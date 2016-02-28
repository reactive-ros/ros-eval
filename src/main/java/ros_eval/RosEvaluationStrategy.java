package ros_eval;

import org.jgrapht.traverse.BreadthFirstIterator;
import org.reactive_ros.Stream;
import org.reactive_ros.evaluation.EvaluationStrategy;
import org.reactive_ros.internal.expressions.MultipleInputExpr;
import org.reactive_ros.internal.expressions.NoInputExpr;
import org.reactive_ros.internal.expressions.SingleInputExpr;
import org.reactive_ros.internal.expressions.Transformer;
import org.reactive_ros.internal.expressions.creation.FromSource;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.output.*;
import org.reactive_ros.util.functions.Func0;
import remote_execution.Broker;
import remote_execution.RemoteExecution;
import remote_execution.StreamTask;
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

    final RemoteExecution executor = new RemoteExecution(); // TODO add machines
    Func0<EvaluationStrategy> evaluationStrategy;
    Broker broker;

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
