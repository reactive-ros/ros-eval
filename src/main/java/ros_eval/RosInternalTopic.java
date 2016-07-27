package ros_eval;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.rhea_core.Stream;
import org.rhea_core.annotations.PlacementConstraint;
import org.rhea_core.internal.output.Output;
import org.rhea_core.io.ExternalTopic;
import org.rhea_core.io.InternalTopic;
import org.ros.node.ConnectedNode;

import java.util.ArrayList;
import java.util.List;

/**
 * ROS implementation of {@link ExternalTopic}.
 * @author Orestis Melkonian
 */
@PlacementConstraint(constraint = "ros")
public class RosInternalTopic<T> extends InternalTopic<T, ConnectedNode> {

    String type;
    org.ros.node.topic.Publisher<T> rosPublisher;
    org.ros.node.topic.Subscriber<T> rosSubscriber;

    public RosInternalTopic(String name, String type) {
        super(name, null); // TODO add serialization strategy
        this.type = type;
    }

    public void setClient(ConnectedNode client) {
        rosPublisher = client.newPublisher(name, type);
        rosSubscriber = client.newSubscriber(name, type);
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

    private void publish(T msg) {
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
        rosSubscriber.addMessageListener(s::onNext);
    }


    @Override
    public RosInternalTopic clone() {
        return new RosInternalTopic(name, type);
    }

    public static List<RosInternalTopic> extract(Stream stream, Output output) {
        List<RosInternalTopic> topics = new ArrayList<>();

        for (ExternalTopic topic : ExternalTopic.extractAll(stream, output))
            if (topic instanceof RosInternalTopic)
                topics.add(((RosInternalTopic) topic));

        return topics;
    }
}
