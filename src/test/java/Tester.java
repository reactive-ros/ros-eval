import org.junit.Test;
import org.rhea_core.Stream;
import remote_execution.Broker;
import ros_eval.RosEvaluationStrategy;
import rx_eval.RxjavaEvaluationStrategy;
import test_data.TestData;
import test_data.TestInfo;
import test_data.utilities.Colors;

/**
 * @author Orestis Melkonian
 */
public class Tester {
    @Test
    public void test() {
        Stream.setEvaluationStrategy(new RosEvaluationStrategy(RxjavaEvaluationStrategy::new, new Broker("http://orestis-B85M-HD3", 11311)));
        
        for (TestInfo test : TestData.tests()) {
            if (test.name.equals("concat")) continue;

            System.out.print(test.name + ": ");
            if (test.equality())
                Colors.print(Colors.GREEN, "Passed");
            else {
                Colors.print(Colors.RED, "Failed");
                System.out.println(test.q1 + " != " + test.q2);
            }
        }
    }
}
