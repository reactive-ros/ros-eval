package ros_eval;

import org.reactive_ros.Stream;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.output.Output;
import org.reactive_ros.io.AbstractTopic;
import org.reactive_ros.util.functions.Func0;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.reactive_ros.evaluation.EvaluationStrategy;
import remote_execution.StreamTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A ROS node that executes a given {@link FlowGraph} and redirects the resulting stream to given {@link Output}.
 * @author Orestis Melkonian
 */
public class RosTask extends StreamTask {
    private String broker;
    private String name;

    public RosTask(Func0<EvaluationStrategy> strategyGen, Stream stream, Output output, List<String> attr, String broker, String name) {
        super(strategyGen, stream, output, attr);
        this.broker = broker;
        this.name = name;
    }

    public RosTask(StreamTask task, String broker, String name) {
        this(task.getStrategyGenerator(), task.getStream(), task.getOutput(), task.getRequiredAttributes, broker, name);
    }

    @Override
    public void run() {
//        display();
        // TODO get ConnectedNode

        List<RosTopic> topics = AbstractTopic.extract(stream, output).stream().map(t -> ((RosTopic) t)).collect(Collectors.toList());

        for (RosTopic topic : topics)
            topic.setClient(connectedNode);

        super.run();
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
