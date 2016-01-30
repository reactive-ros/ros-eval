package ros_eval;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.reactive_ros.evaluation.Serializer;
import org.reactive_ros.Stream;
import org.apache.commons.lang.SerializationUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.reactive_ros.internal.notifications.Notification;
import org.ros.internal.message.Message;
import org.ros.message.MessageDeserializer;
import org.ros.message.MessageFactory;
import org.ros.message.MessageSerializationFactory;
import org.ros.message.MessageSerializer;
import org.ros.node.NodeConfiguration;
import std_msgs.*;

import java.io.Serializable;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Objects;

/**
 * A (de)serializer for the messages transferred within the inner generated topics of {@link RosEvaluationStrategy}.
 * @author Orestis Melkonian
 */
public class RosSerializer implements Serializer<ByteMultiArray> {

    public static final MessageFactory factory = NodeConfiguration.newPrivate().getTopicMessageFactory();
    public static final MessageSerializationFactory serFactory = NodeConfiguration.newPrivate().getMessageSerializationFactory();

    private static final java.lang.String jsonTitle = "JSON";
    private static final java.lang.String streamTitle = "STREAM";
    private static final java.lang.String errorTitle = "ERROR";
    private static final java.lang.String completeTitle = "COMPLETE";

    /**
     * Serializes given {@link Notification}.
     * <p> Uses rosjava's {@link MessageSerializer} for {@link Notification}s containing a ROS {@link Message}. </p>
     * <p> Uses json-io's {@link JsonWriter} for {@link Notification}s containing any other Java {@link Object}. </p>
     * <p> Uses apache's {@link SerializationUtils} for {@link Notification}s containing a {@link Stream} or any {@link Serializable} object. </p>
     * @param notification the {@link Notification} to serialize
     * @return the ROS message to transfer with type {@link ByteMultiArray}
     */
    @Override
    public ByteMultiArray serialize(Notification notification) {
        ByteMultiArray msg = factory.newFromType(ByteMultiArray._TYPE);
        MultiArrayLayout layout = factory.newFromType(MultiArrayLayout._TYPE);
        MultiArrayDimension dim = factory.newFromType(MultiArrayDimension._TYPE);
        ChannelBuffer buffer;
        java.lang.String label;

        Notification.Kind kind = notification.getKind();
        // onCompleted
        if (kind == Notification.Kind.OnCompleted) {
            buffer = jsonSerialize(notification);
            label = completeTitle;
        }
        // onError
        else if (kind == Notification.Kind.OnError) {
            buffer = jsonSerialize(notification.getThrowable());
            label = errorTitle;
        }
        // onNext
        else {
            Object obj = notification.getValue();
            if (obj instanceof Message) {
                java.lang.String type = ((Message) obj).toRawMessage().getType();
                buffer = rosSerialize((Message) obj, type);
                label = type;
            }
            else if (obj instanceof Stream) {
                buffer = apacheSerialize(obj);
                label = streamTitle;
            }
            else {
                buffer = jsonSerialize(obj);
                label = jsonTitle;
            }
        }

        dim.setLabel(label);
        layout.setDim(Collections.singletonList(dim));
        msg.setLayout(layout);
        msg.setData(buffer);
        return msg;
    }

    /**
     * De-serializes given ROS message of type {@link ByteMultiArray}.
     * <p> Uses rosjava's {@link MessageDeserializer} for ROS {@link Message}s containing another ROS {@link Message}. </p>
     * <p> Uses json-io's {@link JsonReader} for ROS {@link Message}s containing any other Java {@link Object}. </p>
     * <p> Uses apache's {@link SerializationUtils} for {@link Notification}s containing a {@link Stream} or any {@link Serializable} object. </p>
     * @param msg the {@link Message} to de-serialize
     * @return the {@link Notification} containing the value transfered
     */
    @Override
    public Notification deserialize(ByteMultiArray msg) {
        java.lang.String type = msg.getLayout().getDim().get(0).getLabel();
        // onError
        if (Objects.equals(type, errorTitle))
            return Notification.createOnError((Throwable) jsonDeserialize(msg.getData()));
        // onCompleted
        else if (Objects.equals(type, completeTitle))
            return Notification.createOnCompleted();
        // Json
        else if (Objects.equals(type, jsonTitle))
            return Notification.createOnNext(jsonDeserialize(msg.getData()));
        // Stream
        else if (Objects.equals(type, streamTitle))
            return Notification.createOnNext(apacheDeserialize(msg.getData()));
        // ROS message
        else {
            MessageDeserializer deserializer = serFactory.newMessageDeserializer(type);
            return Notification.createOnNext(deserializer.deserialize(msg.getData()));
        }
    }

    public ChannelBuffer jsonSerialize(Object obj) {
        java.lang.String json = JsonWriter.objectToJson(obj);
        return ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, json.toCharArray(), Charset.defaultCharset());
    }

    public Object jsonDeserialize(ChannelBuffer buffer) {
        return JsonReader.jsonToJava(buffer.toString(Charset.defaultCharset()));
    }

    private ChannelBuffer rosSerialize(Message msg, java.lang.String type) {
        MessageSerializer<Message> serializer = serFactory.newMessageSerializer(type);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(ByteOrder.LITTLE_ENDIAN, 1000);
        serializer.serialize(msg, buffer);
        return buffer;
    }

    public ChannelBuffer apacheSerialize(Object s) {
        return ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, SerializationUtils.serialize((Serializable) s));
    }

    public Object apacheDeserialize(ChannelBuffer buffer) {
        return SerializationUtils.deserialize(ChannelBuffers.copiedBuffer(buffer).array());
    }
}
