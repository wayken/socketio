package cloud.apposs.socketio.messages;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.transport.Transport;

public class OutPacketMessage extends HttpMessage {
    private final SocketIOSession session;
    private final Transport transport;

    public OutPacketMessage(SocketIOSession session, Transport transport) {
        super(session.getOrigin(), session.getSessionId());

        this.session = session;
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    public SocketIOSession getSession() {
        return session;
    }
}
