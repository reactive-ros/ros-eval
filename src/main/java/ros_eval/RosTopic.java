package ros_eval;

import org.rhea_core.Stream;
import org.rhea_core.annotations.PlacementConstraint;
import org.rhea_core.internal.Notification;
import org.rhea_core.internal.output.Output;
import org.rhea_core.io.AbstractTopic;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.ros.node.ConnectedNode;
import std_msgs.ByteMultiArray;

import java.util.ArrayList;
import java.util.List;

/**
 * ROS implementation of {@link AbstractTopic}.
 * @author Orestis Melkonian
 */
@PlacementConstraint(constraint = "ros")
public class RosTopic<T> extends AbstractTopic<T, ByteMultiArray, ConnectedNode> {

    org.ros.node.topic.Publisher<ByteMultiArray> rosPublisher;
    org.ros.node.topic.Subscriber<ByteMultiArray> rosSubscriber;

    /**
     * Constructor
     * @param name the name of this RosTopic
     */
    public RosTopic(String name) {
        super(name, new RosSerializer());
    }

    public void setClient(ConnectedNode client) {
        rosPublisher = client.newPublisher(name, ByteMultiArray._TYPE);
        rosSubscriber = client.newSubscriber(name, ByteMultiArray._TYPE);
    }

    /**
     * Subscriber implementation
     */

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        Notification<T> notification = Notification.createOnNext(t);
        publish(serializer.serialize(notification));
        if (Stream.DEBUG)
            System.out.println(name() + ": Send\t" + notification.getValue());
    }

    @Override
    public void onError(Throwable t) {
        Notification<T> notification = Notification.createOnError(t);
        publish(serializer.serialize(notification));
    }

    @Override
    public void onComplete() {
        Notification<T> notification = Notification.createOnCompleted();
        publish(serializer.serialize(notification));
        if (Stream.DEBUG)
            System.out.println(name() + ": Send\tComplete");
    }

    private void publish(ByteMultiArray msg) {
        rosPublisher.publish(msg);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                    if (Stream.DEBUG)
                        System.out.println(name() + ": Recv\t" + notification.getValue());
                    s.onNext(notification.getValue());
                    break;
                case OnError:
                    s.onError(notification.getThrowable());
                    break;
                case OnCompleted:
                    if (Stream.DEBUG)
                        System.out.println(name() + ": Recv\tComplete");
                    s.onComplete();
                    break;
                default:
            }
        });
    }


    @Override
    public RosTopic clone() {
        return new RosTopic(name);
    }

    public static List<RosTopic> extract(Stream stream, Output output) {
        List<RosTopic> topics = new ArrayList<>();

        for (AbstractTopic topic : AbstractTopic.extractAll(stream, output))
            if (topic instanceof RosTopic)
                topics.add(((RosTopic) topic));

        return topics;
    }
}
