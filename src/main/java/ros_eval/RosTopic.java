package ros_eval;

import org.reactive_ros.internal.notifications.Notification;
import org.reactive_ros.io.AbstractTopic;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.ros.node.ConnectedNode;
import std_msgs.ByteMultiArray;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * ROS implementation of {@link AbstractTopic}.
 * @author Orestis Melkonian
 */
public class RosTopic<T> extends AbstractTopic<T, ByteMultiArray> {

    static final boolean DEBUG = false;
    static final String type = ByteMultiArray._TYPE;

    org.ros.node.topic.Publisher<ByteMultiArray> rosPublisher;
    org.ros.node.topic.Subscriber<ByteMultiArray> rosSubscriber;

    /**
     * Constructor
     * @param name the name of this RosTopic
     */
    public RosTopic(String name) {
        super(name, new RosSerializer());
    }

    public void setClient(Object client) {
        ConnectedNode connectedNode = (ConnectedNode) client;
        rosPublisher = connectedNode.newPublisher(name, type);
        rosSubscriber = connectedNode.newSubscriber(name, type);
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

    private class BlockQueue {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        long delay = 500;

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

    @Override
    public RosTopic clone() {
        return new RosTopic(name);
    }
}
