package cloud.apposs.socketio.distributed.pubsub.message;

import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;

import java.util.Set;
import java.util.UUID;

public class BulkJoinLeaveMessage extends PubSubMessage {
    private static final long serialVersionUID = 7506016762607624388L;

    private final UUID sessionId;
    private final String namespace;
    private final Set<String> rooms;

    public BulkJoinLeaveMessage(UUID id, Set<String> rooms, String namespace) {
        this.sessionId = id;
        this.rooms = rooms;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Set<String> getRooms() {
        return rooms;
    }
}
