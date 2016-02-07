package ros_eval;

import org.reactive_ros.evaluation.Serializer;
import org.reactive_ros.internal.io.Sink;
import org.reactive_ros.internal.io.Source;
import org.reactive_ros.internal.notifications.Notification;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.ros.node.ConnectedNode;
import std_msgs.ByteMultiArray;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Information needed for a ROS topic.
 * @author Orestis Melkonian
 */
public class Topic<T> implements Source<T>, Sink<T> {

    static final boolean DEBUG = false;

    // Topic info
    public String topicName;
    public String topicType = ByteMultiArray._TYPE;

    // ROS
    final Serializer<ByteMultiArray> serializer = new RosSerializer();
    org.ros.node.topic.Publisher<ByteMultiArray> rosPublisher;
    org.ros.node.topic.Subscriber<ByteMultiArray> rosSubscriber;

    /**
     * Constructors
     * @param topicName the name of this Topic
     * @param connectedNode the ROS node to connect to
     */
    public Topic(String topicName, ConnectedNode connectedNode) {
        this.topicName = topicName;
        rosPublisher = connectedNode.newPublisher(topicName, topicType);
        rosSubscriber = connectedNode.newSubscriber(topicName, topicType);
    }

    public Topic(String topicName, String topicType, ConnectedNode connectedNode) {
        this(topicName, connectedNode);
        this.topicType = topicType;
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

    /**
     * Subscriber implementation
     */
    BlockQueue queue = new BlockQueue();

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        queue.block();

        Notification<T> notification = Notification.createOnNext(t);
        rosPublisher.publish(serializer.serialize(notification));
        if (DEBUG)
            System.out.println(name() + ": Send\t" + notification.getValue());

        queue.unblock();
    }

    @Override
    public void onError(Throwable t) {
        queue.block();

        Notification<T> notification = Notification.createOnError(t);
        rosPublisher.publish(serializer.serialize(notification));

        queue.unblock();
    }

    @Override
    public void onComplete() {
        queue.block();

        Notification<T> notification = Notification.createOnCompleted();
        rosPublisher.publish(serializer.serialize(notification));
        if (DEBUG)
            System.out.println(name() + ": Send\tComplete");

        queue.unblock();
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
                    if (DEBUG)
                        System.out.println(name() + ": Recv\t" + notification.getValue());
                    s.onNext(notification.getValue());
                    break;
                case OnError:
                    s.onError(notification.getThrowable());
                    break;
                case OnCompleted:
                    if (DEBUG)
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

    private class BlockQueue {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        long delay = 150;

        public void block() {
            if (DEBUG)
                System.out.println("[" + Thread.currentThread().getId() + "] Blocking");
            try {
                queue.put(new Object());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void unblock() {
            if (DEBUG)
                System.out.println("[" + Thread.currentThread().getId() + "] Unblocking");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
