package cloud.apposs.socketio.distributed.pubsub.message;

import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.protocol.Packet;

import java.util.UUID;

public class DispatchMessage extends PubSubMessage {
    private static final long serialVersionUID = 6692047718303934349L;

    private final String namespace;
    private final Packet packet;

    private final UUID sessionId;

    public DispatchMessage(String namespace, UUID sessionId, Packet packet) {
        this.namespace = namespace;
        this.sessionId = sessionId;
        this.packet = packet;
    }

    public String getNamespace() {
        return namespace;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Packet getPacket() {
        return packet;
    }
}
