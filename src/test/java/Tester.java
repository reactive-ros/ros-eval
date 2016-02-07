import static org.junit.Assert.*;

import org.junit.Test;
import org.reactive_ros.Stream;
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
        Stream.setEvaluationStrategy(new RosEvaluationStrategy(() -> new RxjavaEvaluationStrategy()));
        
        for (TestInfo test : TestData.tests()) {
            System.out.print(test.name + ": ");
            assertTrue(test.equality());
            Colors.print(Colors.GREEN , "PASSED");
        }
    }
}
