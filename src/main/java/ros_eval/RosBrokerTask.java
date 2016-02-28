package ros_eval;

import org.ros.RosCore;
import remote_execution.Broker;
import remote_execution.BrokerTask;

/**
 * @author Orestis Melkonian
 */
public class RosBrokerTask extends BrokerTask {
    public RosBrokerTask(Broker broker) {
        super(broker);
    }

    @Override
    public void run() {
        RosCore roscore = RosCore.newPublic(broker.getPort());
        roscore.start();
        try {
            roscore.awaitStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
