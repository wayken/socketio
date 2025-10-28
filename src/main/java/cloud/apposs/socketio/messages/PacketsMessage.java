package cloud.apposs.socketio.messages;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.transport.Transport;
import io.netty.buffer.ByteBuf;

public class PacketsMessage {
    private final SocketIOSession session;
    private final ByteBuf content;
    private final Transport transport;

    public PacketsMessage(SocketIOSession session, ByteBuf content, Transport transport) {
        this.session = session;
        this.content = content;
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    public SocketIOSession getSession() {
        return session;
    }

    public ByteBuf getContent() {
        return content;
    }
}
