package ros_eval.ros_graph;

import org.ros.RosCore;

import java.io.Serializable;

/**
 * @author Orestis Melkonian
 */
public class RosMainTask implements Runnable, Serializable {

    int port;

    public RosMainTask(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        RosCore roscore = RosCore.newPublic(port);
        roscore.start();
        try {
            roscore.getUri().awaitStart();
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
