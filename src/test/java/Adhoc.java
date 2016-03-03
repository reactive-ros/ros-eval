import org.junit.Test;
import org.rhea_core.Stream;
import test_data.utilities.Threads;

/**
 * @author Orestis Melkonian
 */
public class Adhoc {

    @Test
    public void ros() {
//        Stream.concat(Stream.just(0), Stream.just(1)).printAll();
//        Stream.concat(Stream.from(Stream.just(0), Stream.just(1))).printAll();

        /*for (int i = 0; i < 100; i++)
            System.out.println(
                Stream.just(1, 2, 3, 4)
                      .scan(0, (i1, i2) -> i1 + i2)
                      .toBlocking().toQueue()
            );*/

        Stream.just(0).print();

        Threads.sleep();
    }
}
