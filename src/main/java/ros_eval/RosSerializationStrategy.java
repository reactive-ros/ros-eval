package ros_eval;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.rhea_core.internal.Notification;
import org.rhea_core.internal.Notification.Kind;
import org.rhea_core.serialization.SerializationStrategy;
import org.ros.internal.message.Message;
import org.ros.message.MessageDeserializer;
import org.ros.message.MessageSerializationFactory;
import org.ros.message.MessageSerializer;
import org.ros.node.NodeConfiguration;

import java.lang.String;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * A (de)serializer for the messages transferred within the inner generated topics of {@link RosEvaluationStrategy}.
 * @author Orestis Melkonian
 */
public class RosSerializationStrategy implements SerializationStrategy<ChannelBuffer> {
    private static final String complete = "C";
    private static final String error = "E";
    private static final String next = "N";

    public static final MessageSerializationFactory serFactory = NodeConfiguration.newPrivate().getMessageSerializationFactory();

    @Override
    public <T> ChannelBuffer serialize(Notification<T> notification) {
        Kind kind = notification.getKind();
        String prefix, content;
        switch(kind) {
            case OnCompleted:
                prefix = complete;
                content = "";
                break;
            case OnError:
                prefix = error;
                content = JsonWriter.objectToJson(notification.getThrowable());
                break;
            default:
                Object obj = notification.getValue();
                if (obj instanceof Message) {
                    prefix = ((Message) obj).toRawMessage().getType();
                    MessageSerializer<Message> serializer = serFactory.newMessageSerializer(prefix);
                    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 1000);
                    serializer.serialize((Message) obj, buffer);
                    content = buffer.toString(Charset.defaultCharset());
                }
                else {
                    prefix = next;
                    content = JsonWriter.objectToJson(obj);
                }
        }
        return ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, (prefix + "$" + content).getBytes());
    }

    @Override
    public Notification deserialize(ChannelBuffer buffer) {
        String msg = buffer.toString(Charset.defaultCharset());
        int sep = msg.indexOf("$");
        String prefix = msg.substring(0, sep);
        String content = msg.substring(sep + 1);

        switch (prefix) {
            case complete: // onCompleted
                return Notification.createOnCompleted();
            case error: // onError
                return Notification.createOnError((Throwable) JsonReader.jsonToJava(content));
            case next: // onNext
                return Notification.createOnNext(JsonReader.jsonToJava(content));
            default: // onNext (ROS)
                ChannelBuffer buf = ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, content, Charset.defaultCharset());
                MessageDeserializer deserializer = serFactory.newMessageDeserializer(prefix);
                return Notification.createOnNext(deserializer.deserialize(buf));
        }
    }
}
