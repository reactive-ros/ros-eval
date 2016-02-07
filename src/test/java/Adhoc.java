import org.junit.Test;
import org.reactive_ros.Stream;
import ros_eval.RosEvaluationStrategy;
import rx_eval.RxjavaEvaluationStrategy;
import test_data.utilities.Threads;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
        Stream.setEvaluationStrategy(new RosEvaluationStrategy(() -> new RxjavaEvaluationStrategy()));

//        Stream.concat(Stream.just(0), Stream.just(1)).printAll();
//        Stream.just(0).concatWith(Stream.just(1)).printAll();


//        Stream.concat(Stream.from(Stream.just(0), Stream.just(1))).printAll();

        /*Stream.nat()
                .takeWhile(i -> i < 20)
                .id().id().id().id().id().id().id().id().id().id().id().id().id().id().id()
                .printAll();*/

        /*Stream<Integer> s1 = Stream.just(1, 2, 3, 4, 5);
        Stream<String> s2 = s1.map(Object::toString);
//        Stream.zip(s1, s2, (i, str) -> i + " : " + str).printAll();
//        Stream.merge(s1, s2.map(Integer::valueOf)).printAll();
        Stream.concat(s1, s2.map(Integer::valueOf)).printAll();*/


//        Stream.nat().take(20).printAll();

        /*Stream.merge(
                Stream.just(1, 2, 3),
                Stream.just(4, 5 ,6)
        ).printAll();*/

        Stream.merge(
                Stream.just(
                    Stream.just(1, 2, 3),
                    Stream.just(4, 5, 6)
                )
        ).printAll();


        /*Stream.range(1,20)
                .id().id().id().id().id().id().id().id().id().id()
                .map(i -> i + 1).map(i -> i - 1)
                .map(i -> i + 1).map(i -> i - 1)
                .map(i -> i + 1).map(i -> i - 1)
                .printAll();*/

        Threads.sleep();
    }
}
