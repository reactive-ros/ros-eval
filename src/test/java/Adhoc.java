import org.junit.Test;
import org.rhea_core.Stream;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import ros_eval.RosEvaluationStrategy;
import ros_eval.RosInternalTopic;
import ros_eval.RosTopic;
import rx_eval.RxjavaEvaluationStrategy;
import std_msgs.Int32;
import test_data.utilities.Threads;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

//    @Test
    public void ros() {
        // Configuration
        Stream.evaluationStrategy = new RosEvaluationStrategy(
                new RxjavaEvaluationStrategy(),
                "localhost",
                "myrosclient"
        );

        MessageFactory msgFactory = NodeConfiguration.newPrivate().getTopicMessageFactory();

        RosTopic<Int32> testTopic = new RosTopic<>("/test_topic", Int32._TYPE);

        Stream.from(testTopic).subscribe(msg -> System.out.println(msg.getData()));

        Stream.range(0, 100).map(i -> {
            Int32 msg = msgFactory.newFromType(Int32._TYPE);
            msg.setData(i);
            return msg;
        }).subscribe(testTopic);


        Threads.sleep();
    }

    @Test
    public void ros_internal() {
        // Configuration
        Stream.evaluationStrategy = new RosEvaluationStrategy(
                new RxjavaEvaluationStrategy(),
                "localhost",
                "myrosclient"
        );

        RosInternalTopic<Integer> testTopic = new RosInternalTopic<>("/test_topic");

        Stream.from(testTopic).print();
        Stream.range(0, 100).subscribe(testTopic);

        // TODO fix this
        /*Stream.range(0, 100).subscribe(testTopic);
        Stream.from(testTopic).print();*/

        Threads.sleep();
    }

    //    @Test
    public void hazelConfig() {
        /*Config config = new Config();
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
        );*/
    }

    /*private static class MyMachine implements Machine {
        @Override
        public int cores() {
            return 4;
        }
    }*/
}
