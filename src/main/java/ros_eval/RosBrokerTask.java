package ros_eval;

import org.rhea_core.distribution.Broker;
import org.rhea_core.distribution.BrokerTask;
import org.ros.RosCore;

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
