import org.junit.Test;
import org.rhea_core.util.functions.Action1;
import org.ros.RosCore;
import org.ros.namespace.GraphName;
import org.ros.node.*;

import java.lang.String;
import java.util.concurrent.CountDownLatch;

/**
 * @author Orestis Melkonian
 */
public class RosRun {

    @Test
    public void rosRun() {
        RosCore roscore = RosCore.newPublic();
        NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
        NodeConfiguration config;
        ConnectedNode[] connectedNode = new ConnectedNode[1];
        CountDownLatch latch = new CountDownLatch(1);

        roscore.start();
        try {
            roscore.awaitStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config = NodeConfiguration.newPrivate(roscore.getUri());

        executor.execute(new Initiator(c -> {
            connectedNode[0] = c;
        }, latch), config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Got connected node");

        String name = "testing_topic";
        String type = std_msgs.String._TYPE;

        ConnectedNode con = connectedNode[0];
        org.ros.node.topic.Publisher<std_msgs.String> rosPublisher = con.newPublisher(name, type);
        org.ros.node.topic.Subscriber<std_msgs.String> rosSubscriber = con.newSubscriber(name, type);
        org.ros.node.topic.Subscriber<std_msgs.String> rosSubscriber2 = con.newSubscriber(name, type);

        rosSubscriber.addMessageListener(msg -> System.out.println(msg.getData()));

        rosSubscriber2.addMessageListener(msg -> System.out.println(msg.getData().length()));

        for (int i=0; i<100; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            std_msgs.String toSend = rosPublisher.newMessage();
            toSend.setData("Msg No." + i);
            rosPublisher.publish(toSend);
        }


        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
