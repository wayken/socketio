package cloud.apposs.socketio.protocol;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.ack.AckCallback;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.util.List;

public class JsonSupportWrapper implements JsonSupport {
    private final JsonSupport delegate;

    public JsonSupportWrapper(JsonSupport delegate) {
        this.delegate = delegate;
    }

    @Override
    public AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws Exception {
        try {
            return delegate.readAckArgs(src, callback);
        } catch (Exception e) {
            src.reset();
            Logger.error("Can't read ack args: " + src.readLine() + " for type: " + callback.getResultClass(), e);
            throw e;
        }
    }

    @Override
    public <T> T readValue(String namespaceName, ByteBufInputStream src, Class<T> valueType) throws Exception {
        try {
            return delegate.readValue(namespaceName, src, valueType);
        } catch (Exception e) {
            src.reset();
            Logger.error("Can't read value: " + src.readLine() + " for type: " + valueType, e);
            throw e;
        }
    }

    @Override
    public void writeValue(ByteBufOutputStream out, Object value) throws Exception {
        try {
            delegate.writeValue(out, value);
        } catch (Exception e) {
            Logger.error("Can't write value: " + value, e);
            throw e;
        }
    }

    @Override
    public void addEventMapping(String namespaceName, String eventName, Class<?> ... eventClass) {
        delegate.addEventMapping(namespaceName, eventName, eventClass);
    }

    @Override
    public void removeEventMapping(String namespaceName, String eventName) {
        delegate.removeEventMapping(namespaceName, eventName);
    }

    @Override
    public List<byte[]> getArrays() {
        return delegate.getArrays();
    }
}
