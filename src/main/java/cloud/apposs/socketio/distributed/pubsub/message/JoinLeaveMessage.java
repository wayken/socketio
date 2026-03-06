package cloud.apposs.socketio.distributed.pubsub.message;

import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;

import java.util.UUID;

public class JoinLeaveMessage extends PubSubMessage {
    private static final long serialVersionUID = -944515928988033174L;

    private final UUID sessionId;
    private final String namespace;
    private final String room;

    public JoinLeaveMessage(UUID id, String room, String namespace) {
        super();
        this.sessionId = id;
        this.room = room;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getRoom() {
        return room;
    }
}
