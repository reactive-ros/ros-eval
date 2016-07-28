package ros_eval;

import org.jboss.netty.buffer.ChannelBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rhea_core.Stream;
import org.rhea_core.annotations.PlacementConstraint;
import org.rhea_core.internal.Notification;
import org.rhea_core.internal.output.Output;
import org.rhea_core.io.ExternalTopic;
import org.rhea_core.io.InternalTopic;
import org.ros.message.MessageFactory;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;

import java.util.ArrayList;
import java.util.List;

import std_msgs.ByteMultiArray;

/**
 * ROS implementation of {@link ExternalTopic}.
 * @author Orestis Melkonian
 */
@PlacementConstraint(constraint = "ros")
public class RosInternalTopic<T> extends InternalTopic<T, ConnectedNode, ChannelBuffer> {

    org.ros.node.topic.Publisher<ByteMultiArray> rosPublisher;
    org.ros.node.topic.Subscriber<ByteMultiArray> rosSubscriber;

    MessageFactory factory = NodeConfiguration.newPrivate().getTopicMessageFactory();

    public RosInternalTopic(String name) {
        super(name, new RosSerializationStrategy());
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
        publish(t);
    }

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onComplete() {}

    private void publish(T value) {
        // Create ROS message
        ByteMultiArray msg = factory.newFromType(ByteMultiArray._TYPE);

        // TODO generalize
        Notification<T> notification = Notification.createOnNext(value);

        ChannelBuffer buffer = serializationStrategy.serialize(notification);
        msg.setData(buffer);

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
            Notification<T> notification = serializationStrategy.deserialize(msg.getData());
            switch (notification.getKind()) {
                case OnCompleted:
                    s.onComplete();
                    break;
                case OnError:
                    s.onError(notification.getThrowable());
                    break;
                case OnNext:
                    s.onNext(notification.getValue());
            }
        });
    }


    @Override
    public RosInternalTopic clone() {
        return new RosInternalTopic(name);
    }

    public static List<RosInternalTopic> extract(Stream stream, Output output) {
        List<RosInternalTopic> topics = new ArrayList<>();

        for (ExternalTopic topic : ExternalTopic.extractAll(stream, output))
            if (topic instanceof RosInternalTopic)
                topics.add(((RosInternalTopic) topic));

        return topics;
    }
}
