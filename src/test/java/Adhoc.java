import com.hazelcast.config.Config;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import hazelcast_distribution.HazelcastDistributionStrategy;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.rhea_core.Stream;
import org.rhea_core.network.Machine;
import org.ros.internal.message.Message;
import org.ros.message.MessageDeserializer;
import org.ros.message.MessageSerializer;
import org.ros.node.NodeConfiguration;
import ros_eval.RosEvaluationStrategy;
import ros_eval.RosSerializationStrategy;
import ros_eval.RosTopic;
import rx_eval.RxjavaEvaluationStrategy;
import sensor_msgs.Image;
import std_msgs.Bool;
import std_msgs.Char;
import std_msgs.Int64;
import test_data.utilities.Threads;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
        Config config = new Config();
        MemberAttributeConfig memberConfig = new MemberAttributeConfig();
        memberConfig.setIntAttribute("cores", 4);
        memberConfig.setBooleanAttribute("ros", true);
        memberConfig.setBooleanAttribute("Ros", true);
        memberConfig.setStringAttribute("hostname", "localhost");
        memberConfig.setStringAttribute("ip", "128.1.1.68");
        config.setMemberAttributeConfig(memberConfig);
        HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);

        Stream.distributionStrategy = new HazelcastDistributionStrategy(
                hazelcast,
                Collections.singletonList(new MyMachine()),
                Arrays.asList(
                        RxjavaEvaluationStrategy::new,
                        () -> new RosEvaluationStrategy(new RxjavaEvaluationStrategy(), "localhost", "myros_client")
                )
        );
        Stream.serializationStrategy = new RosSerializationStrategy();

        RosTopic<Image> CAMERA = new RosTopic<>("/camera/rgb/image_color", Image._TYPE);
        Stream.from(CAMERA).subscribe(im -> System.out.println("im"));

        Threads.sleep();
    }

    private static class MyMachine implements Machine {
        @Override
        public int cores() {
            return 4;
        }
    }
}
