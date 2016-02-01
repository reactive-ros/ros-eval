import graph_viz.GraphVisualizer;
import org.junit.Before;
import org.reactive_ros.Stream;
import org.reactive_ros.util.functions.Action1;
import ros_eval.RosEvaluationStrategy;
import ros_eval.Topic;
import ros_eval.ros_graph.RosGraph;
import rx_eval.RxjavaEvaluationStrategy;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
        /*RosEvaluationStrategy eval = new RosEvaluationStrategy(new RxjavaEvaluationStrategy());
        Stream.setEvaluationStrategy(eval);

        Stream<Integer> ints = Stream.just(0,10,20,30,40,50);
        System.out.println(ints.toBlocking().toList());
        System.out.println(ints.map(i -> i + 1).toBlocking().toList());
        System.out.println(ints.map(i -> i + 2).toBlocking().toList());
        System.out.println(ints.map(i -> i + 3).toBlocking().toList());

        sleep();*/
    }

    private void print(Stream s) {
        System.out.println(Arrays.toString(s.toBlocking().toList().toArray()));
    }

    private void sleep() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
