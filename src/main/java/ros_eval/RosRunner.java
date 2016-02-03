package ros_eval;

import graph_viz.GraphVisualizer;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.reactive_ros.Stream;
import org.reactive_ros.evaluation.EvaluationStrategy;
import org.reactive_ros.internal.expressions.MultipleInputExpr;
import org.reactive_ros.internal.expressions.NoInputExpr;
import org.reactive_ros.internal.expressions.SingleInputExpr;
import org.reactive_ros.internal.expressions.Transformer;
import org.reactive_ros.internal.expressions.creation.FromListener;
import org.reactive_ros.internal.expressions.creation.FromSource;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.output.MultipleOutput;
import org.reactive_ros.internal.output.Output;
import org.reactive_ros.internal.output.SinkOutput;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import ros_eval.ReactiveNodeMain;
import ros_eval.Topic;
import ros_eval.ros_graph.RosEdge;
import ros_eval.ros_graph.RosGraph;
import ros_eval.ros_graph.RosNode;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Orestis Melkonian
 */
public class RosRunner extends AbstractNodeMain {
    NodeMainExecutor exec;
    URI uri;
    EvaluationStrategy evalStrategy;
    ConnectedNode connectedNode;

    public String nodePrefix = "~";
    int topicCounter = 0;
    int topicName = 0;

    Stream stream;

    Output output;
    public RosRunner(NodeMainExecutor exec, URI uri, EvaluationStrategy evalStrategy) {
        this.exec = exec;
        this.uri = uri;
        this.evalStrategy = evalStrategy;
    }

    private String newName() {
        return nodePrefix + "_" + Integer.toString(topicName++);
    }

    public Topic newTopic() {
        return new Topic(nodePrefix + "/" + Integer.toString(topicCounter++), connectedNode);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("RosRunner");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        evaluate();
    }

    public void set(Stream stream, Output output) {
        this.stream = stream;
        this.output = output;
    }

    private void execute(Stream stream, Output output) { // TODO Network layout
        String nodeName = newName();
        exec.execute(
                new ReactiveNodeMain(nodeName, stream, output, evalStrategy.newStrategy()),
                NodeConfiguration.newPrivate(uri));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void evaluate() {
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


}
