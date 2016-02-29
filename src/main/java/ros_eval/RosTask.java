package ros_eval;

import org.rhea_core.Stream;
import org.rhea_core.internal.graph.FlowGraph;
import org.rhea_core.internal.output.Output;
import org.rhea_core.io.AbstractTopic;
import org.rhea_core.util.functions.Action1;
import org.rhea_core.util.functions.Func0;
import org.rhea_core.evaluation.EvaluationStrategy;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import remote_execution.Broker;
import remote_execution.StreamTask;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * A ROS node that executes a given {@link FlowGraph} and redirects the resulting stream to given {@link Output}.
 * @author Orestis Melkoniang
 */
public class RosTask extends StreamTask {
    Broker broker;
    String name;

    public RosTask(Func0<EvaluationStrategy> strategyGen, Stream stream, Output output, List<String> attr, Broker broker, String name) {
        super(strategyGen, stream, output, attr);
        this.broker = broker;
        this.name = name;
    }

    public RosTask(StreamTask task, Broker broker, String name) {
        this(task.getStrategyGenerator(), task.getStream(), task.getOutput(), task.getRequiredAttributes(), broker, name);
    }

    @Override
    public void run() {

        // Set ROS_MASTER_URI
        try {
            Process p = Runtime.getRuntime().exec("export ROS_MASTER_URI=http://" + broker.getIp() + ":" + broker.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get ConnectedNode
        NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
        NodeConfiguration config = NodeConfiguration.newPublic(broker.getIp());
        final ConnectedNode[] connectedNode = new ConnectedNode[1];
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(new Initiator(c -> {
            connectedNode[0] = c;
        }, latch, name), config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Set client on Topics
        List<RosTopic> topics = AbstractTopic.extract(stream, output).stream().map(t -> ((RosTopic) t)).collect(Collectors.toList());
        for (RosTopic topic : topics)
            topic.setClient(connectedNode[0]);

        super.run();
    }

    private class Initiator extends AbstractNodeMain {
        Action1<ConnectedNode> initAction;
        CountDownLatch latch;
        String name;

        public Initiator(Action1<ConnectedNode> initAction, CountDownLatch latch, String name) {
            this.initAction = initAction;
            this.latch = latch;
            this.name = name;
        }

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of(name);
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            initAction.call(connectedNode);
            latch.countDown();
        }
    }
}
