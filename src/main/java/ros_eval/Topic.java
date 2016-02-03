package ros_eval;

import org.reactive_ros.evaluation.Serializer;
import org.reactive_ros.internal.io.Listener;
import org.reactive_ros.internal.io.Sink;
import org.reactive_ros.internal.io.Source;
import org.reactive_ros.internal.notifications.Notification;
import org.reactive_ros.util.functions.Action1;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.ros.node.ConnectedNode;
import std_msgs.ByteMultiArray;

/**
 * Information needed for a ROS topic.
 * @author Orestis Melkonian
 */
public class Topic<T> implements Source<T>, Sink<T>, Listener<T> {

    // Topic info
    public String topicName;
    public String topicType = ByteMultiArray._TYPE;

    // ROS
    private ConnectedNode connectedNode;
    private final Serializer<ByteMultiArray> serializer = new RosSerializer();

    private org.ros.node.topic.Publisher<ByteMultiArray> rosPublisher;
    private org.ros.node.topic.Subscriber<ByteMultiArray> rosSubscriber;


    /**
     * Constructor.
     * @param topicName the name of this Topic
     */
    public Topic(String topicName, ConnectedNode connectedNode) {
        this.topicName = topicName;
        this.connectedNode = connectedNode;

        rosPublisher = connectedNode.newPublisher(topicName, topicType);
        rosSubscriber = connectedNode.newSubscriber(topicName, topicType);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Topic
                && topicName.equals(((Topic) obj).topicName)
                && topicType.equals(((Topic) obj).topicType);
    }

    @Override
    public String toString() {
        return topicName + " [" + topicType + "]";
    }

    @Override
    public void register(Action1<T> action) {
        connectedNode.<T>newSubscriber(topicName, topicType).addMessageListener(action::call);
    }


    /**
     * Subscriber implementation
     */
    // Timing issues
    long last = 0;
    long cycle = 1000;

    synchronized private void throttle() {
        /*long diff = (last == 0) ? cycle : System.currentTimeMillis() - last;
        if (diff <= cycle)
            try { Thread.sleep(cycle - diff); } catch (InterruptedException ignored) {}
        last = System.currentTimeMillis();*/
        try {
            Thread.sleep(cycle);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        throttle();
        Notification<T> notification = Notification.createOnNext(t);
        rosPublisher.publish(serializer.serialize(notification));
        System.out.println(name() + ": Send\t" + notification.getValue());
    }

    @Override
    public void onError(Throwable t) {
        throttle();
        Notification<T> notification = Notification.createOnError(t);
        rosPublisher.publish(serializer.serialize(notification));
    }

    @Override
    public void onComplete() {
        throttle();
        Notification<T> notification = Notification.createOnCompleted();
        rosPublisher.publish(serializer.serialize(notification));
        System.out.println(name() + ": Send\tComplete");
    }

    /**
     * Publisher implementation
     */
    @Override
    public void subscribe(Subscriber<? super T> s) {
        rosSubscriber.addMessageListener(msg -> {
            Notification<T> notification = serializer.deserialize(msg);
            switch (notification.getKind()) {
                case OnNext:
                    System.out.println(name() + ": Recv\t" + notification.getValue());
                    s.onNext(notification.getValue());
                    break;
                case OnError:
                    s.onError(notification.getThrowable());
                    break;
                case OnCompleted:
                    System.out.println(name() + ": Recv\tComplete");
                    s.onComplete();
                    break;
                default:
            }
        });
    }

    private String name() {
        return topicName + "[" + Thread.currentThread().getId() + "]";
    }
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
}
