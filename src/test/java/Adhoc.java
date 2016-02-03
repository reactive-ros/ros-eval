import org.reactive_ros.Stream;
import ros_eval.RosEvaluationStrategy;
import rx_eval.RxjavaEvaluationStrategy;
import org.junit.Test;
import java.util.Arrays;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
        Stream.setEvaluationStrategy(new RosEvaluationStrategy(new RxjavaEvaluationStrategy(true)));
//        Stream.setEvaluationStrategy(new RxjavaEvaluationStrategy());

        /*System.out.println(
                Arrays.toString(Stream.concat(Stream.just(0, 1, 2), Stream.just(3, 4, 5)).toBlocking().toList().toArray())
        );*/

//        Stream.concat(Stream.just(0, 1, 2), Stream.just(3, 4, 5))
//        Stream.merge(Stream.range(0, 10), Stream.range(10, 10))
//              .subscribe(System.out::println, System.out::println, () -> System.out.println("Complete"));

        /*Stream<Integer> ints = Stream.just(0,10,20,30,40,50);
        System.out.println(ints.toBlocking().toList());
        System.out.println(ints.map(i -> i + 1).toBlocking().toList());
        System.out.println(ints.map(i -> i + 2).toBlocking().toList());
        System.out.println(ints.map(i -> i + 3).toBlocking().toList());*/

        /*Stream.range(0, 10)
                .id().id().id().id().id().id().id().id().id()
                .map(i -> i + 1).map(i -> i + 1).map(i -> i + 1)
                .map(i -> i - 1).map(i -> i - 1).map(i -> i - 1)
                .map(i -> i + 1).map(i -> i + 1).map(i -> i + 1)
                .map(i -> i - 1).map(i -> i - 1).map(i -> i - 1)
                .map(i -> i + 1).map(i -> i + 1).map(i -> i + 1)
                .map(i -> i - 1).map(i -> i - 1).map(i -> i - 1)
                .map(i -> i + 1).map(i -> i + 1).map(i -> i + 1)
                .map(i -> i - 1).map(i -> i - 1).map(i -> i - 1)
                .print();*/

        /*Stream<Integer> s1 = Stream.just(1, 2, 3, 4, 5);
        Stream<String> s2 = s1.map(Object::toString);
//        Stream.zip(s1, s2, (i, str) -> i + " : " + str).print();
        Stream.merge(s1, s2.map(Integer::valueOf)).print();*/

        Stream.concat(Stream.just(0), Stream.just(1)).print();

        sleep();
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
