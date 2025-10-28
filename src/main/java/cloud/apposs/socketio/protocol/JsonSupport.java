package cloud.apposs.socketio.protocol;

import cloud.apposs.socketio.ack.AckCallback;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.util.List;

/**
 * JSON infrastructure interface.
 * Allows to implement custom realizations
 * to JSON support operations.
 */
public interface JsonSupport {
    AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws Exception;

    <T> T readValue(String namespaceName, ByteBufInputStream src, Class<T> valueType) throws Exception;

    void writeValue(ByteBufOutputStream out, Object value) throws Exception;

    void addEventMapping(String namespaceName, String eventName, Class<?> ... eventClass);

    void removeEventMapping(String namespaceName, String eventName);

    List<byte[]> getArrays();
}
