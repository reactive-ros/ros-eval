package ros_eval;

import org.reactive_ros.Stream;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.output.Output;
import org.reactive_ros.io.AbstractTopic;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.reactive_ros.evaluation.EvaluationStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A ROS node that executes a given {@link FlowGraph} and redirects the resulting stream to given {@link Output}.
 * @author Orestis Melkonian
 */
public class RosRunnable extends AbstractNodeMain implements Runnable {
    private String name;
    private EvaluationStrategy evaluationStrategy;

    private Stream stream;
    private Output output;

    /**
     * @param name the graph name of this {@link RosRunnable}
     * @param stream the {@link Stream} to be evaluated by this {@link RosRunnable}
     * @param output the {@link Output} to redirect the evaluated stream
     * @param evaluationStrategy the {@link EvaluationStrategy} to use
     */
    public RosRunnable(String name, Stream stream, Output output, EvaluationStrategy evaluationStrategy) {
        this.name = name;
        this.evaluationStrategy = evaluationStrategy;
        this.stream = stream;
        this.output = output;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(name);
    }

    /**
     * Evaluates this {@link RosRunnable}'s {@link FlowGraph}.
     * @param connectedNode automatically passed from rosjava
     */
    @Override
    public void onStart(ConnectedNode connectedNode) {
//        display();
        List<RosTopic> topics = AbstractTopic.extract(stream, output).stream().map(t -> ((RosTopic) t)).collect(Collectors.toList());

        for (RosTopic topic : topics)
            topic.setClient(connectedNode);

        evaluationStrategy.evaluate(stream, output);
    }

    @Override
    public void run() {

    }

    private void display() {
        System.out.println(
                "\n\n======================== " + info() + " ========================"
                    + "\n" + stream.getGraph()
                    + "\n\t===>\t" + output + "\n"
                    + "\n==================================================\"\n\n");
    }

    private String info() {
        return name + " [" + Thread.currentThread().getId() + "]";
//        return ManagementFactory.getRuntimeMXBean().getName() + "@" + processInfo() + ": ";
    }
}
