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
import org.reactive_ros.util.functions.Action1;
import org.reactive_ros.util.functions.Func0;
import org.ros.RosCore;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import ros_eval.ros_graph.RosEdge;
import ros_eval.ros_graph.RosGraph;
import ros_eval.ros_graph.RosNode;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Evaluates a dataflow graph by mapping one or more stream operators to individual ROS nodes
 * to run concurrently, using the ROS message system to communicate.
 * @author Orestis Melkonian
 */
public class RosEvaluationStrategy implements EvaluationStrategy {

    /**
     * The {@link EvaluationStrategy} to use inside each ROS node.
     */
    Func0<EvaluationStrategy> evaluationStrategy;

    /**
     * ROS setup
     */
    final NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
    final RosCore roscore = RosCore.newPublic();
    ConnectedNode connectedNode;
    public void setConnectedNode(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
    }
    CountDownLatch latch = new CountDownLatch(1);
    NodeConfiguration config;

    String nodePrefix = "~";
    int topicCounter = 0, nodeCounter = 0;

    long delay = 250;

    /**
     * Generators
     */
    private String newName() {
        return nodePrefix + "_" + Integer.toString(nodeCounter++);
    }

    public Topic newTopic() {
        return new Topic(nodePrefix + "/" + Integer.toString(topicCounter++), connectedNode);
    }

    /**
     * Constructors
     */
    public RosEvaluationStrategy(Func0<EvaluationStrategy> evaluationStrategy) {
        roscore.start();
        try {
            roscore.awaitStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config = NodeConfiguration.newPrivate(roscore.getUri());
        this.evaluationStrategy = evaluationStrategy;

        executor.execute(new Initiator(this::setConnectedNode, latch), config);
    }

    public RosEvaluationStrategy(Func0<EvaluationStrategy> evaluationStrategy, String nodePrefix) {
        this(evaluationStrategy);
        this.nodePrefix = nodePrefix;
    }

    /**
     * Evaluation
     */
    private void execute(Stream stream, Output output) {
        // TODO Network layout
        String nodeName = newName();
        executor.execute(
                new ReactiveNodeMain(nodeName, stream, output, evaluationStrategy.call()),
                NodeConfiguration.newPrivate(roscore.getUri()));

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> void evaluate(Stream<T> stream, Output output) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FlowGraph flow = stream.getGraph();
        RosGraph graph = new RosGraph(flow, this::newTopic);

        // Run output node first
        Topic result = newTopic();
        FlowGraph resultNode = new FlowGraph();
        resultNode.addConnectVertex(Stream.from(result).getToConnect());
        execute(new Stream(resultNode), output);

        // Then run each graph vertex as an individual ROS node (reverse BFS)
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
                Topic input = inputs.iterator().next().getTopic();
                Transformer toAdd = new FromSource<>(input);
                innerGraph.addConnectVertex(toAdd);
                innerGraph.attach(transformer);
            } else if (transformer instanceof MultipleInputExpr) {
                assert inputs.size() > 1;
                // N inputs
                innerGraph.setConnectNodes(inputs.stream()
                        .map(edge -> new FromSource(edge.getTopic()))
                        .collect(Collectors.toList()));
                innerGraph.attachMulti(transformer);
            }

            // Set outputs according to graph connections
            Set<RosEdge> outputs = graph.outgoingEdgesOf(toExecute);
            List<Output> list = new ArrayList<>();
            if (transformer == graph.toConnect)
                list.add(new SinkOutput<>(result));
            list.addAll(outputs.stream()
                    .map(RosEdge::getTopic)
                    .map((Function<Topic, SinkOutput>) SinkOutput::new)
                    .collect(Collectors.toList()));
            Output outputToExecute = (list.size() == 1) ? list.get(0) : new MultipleOutput(list);

            // Execute
            execute(new Stream(innerGraph, innerGraph.getConnectNode()), outputToExecute);

            checked.add(toExecute);
        }
    }

    /**
     * Used to handle all internal topics.
     */
    private class Initiator extends AbstractNodeMain {
        Action1<ConnectedNode> initAction;
        CountDownLatch latch;

        public Initiator(Action1<ConnectedNode> initAction, CountDownLatch latch) {
            this.initAction = initAction;
            this.latch = latch;
        }

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of("init");
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            initAction.call(connectedNode);
            latch.countDown();
        }
    }
}
