import org.junit.Test;
import org.reactive_ros.Stream;
import ros_eval.RosEvaluationStrategy;
import rx_eval.RxjavaEvaluationStrategy;
import test_data.utilities.Threads;

import java.util.ArrayList;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
        Stream.setEvaluationStrategy(new RosEvaluationStrategy(() -> new RxjavaEvaluationStrategy(true)));

//        Stream.concat(Stream.just(0), Stream.just(1)).printAll();
//        Stream.concat(Stream.from(Stream.just(0), Stream.just(1))).printAll();


        for (int i = 0; i < 100; i++)
            Stream.just(0, 1, 2).print();

        Threads.sleep();
    }
}
