package cloud.apposs.socketio.distributed.pubsub.message;

import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.protocol.Packet;

import java.util.Set;
import java.util.UUID;

public class BulkDispatchMessage extends PubSubMessage {
    private static final long serialVersionUID = 6692047718303934349L;

    private final String namespace;
    private final Packet packet;

    private final Set<UUID> sessionIds;

    public BulkDispatchMessage(String namespace, Set<UUID> sessionIds, Packet packet) {
        this.namespace = namespace;
        this.sessionIds = sessionIds;
        this.packet = packet;
    }

    public String getNamespace() {
        return namespace;
    }

    public Set<UUID> getSessionIds() {
        return sessionIds;
    }

    public Packet getPacket() {
        return packet;
    }
}
