package ros_eval;


import org.rhea_core.Stream;
import org.rhea_core.annotations.StrategyInfo;
import org.rhea_core.evaluation.EvaluationStrategy;
import org.rhea_core.internal.output.Output;
import org.rhea_core.util.functions.Action1;
import org.ros.namespace.GraphName;
import org.ros.node.*;

import java.util.concurrent.CountDownLatch;


/**
 * @author Orestis Melkonian
 */
@StrategyInfo(name = "ros", requiredSkills = {"Ros"}, priority = 1)
public class RosEvaluationStrategy implements EvaluationStrategy {

    EvaluationStrategy innerStrategy;
    String broker;
    String clientName;

    ConnectedNode[] connectedNode = new ConnectedNode[1];

    public RosEvaluationStrategy(EvaluationStrategy innerStrategy, String broker, String clientName) {
        this.innerStrategy = innerStrategy;
        this.broker = broker;
        this.clientName = clientName;

        // Set client
        NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
        NodeConfiguration config = NodeConfiguration.newPublic(broker);
        connectedNode = new ConnectedNode[1];
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(new Initiator(c -> { connectedNode[0] = c; }, latch, clientName), config);

        try { latch.await(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    @Override
    public <T> void evaluate(Stream<T> stream, Output output) {

        for (RosTopic topic : RosTopic.extract(stream, output))
            topic.setClient(connectedNode[0]);

        for (RosInternalTopic topic : RosInternalTopic.extract(stream, output))
            topic.setClient(connectedNode[0]);


        // Propagate evaluation to first-order strategy
        innerStrategy.evaluate(stream, output);
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
