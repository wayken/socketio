package cloud.apposs.socketio.messages;

import java.util.UUID;

public abstract class HttpMessage {
    private final String origin;
    private final UUID sessionId;

    public HttpMessage(String origin, UUID sessionId) {
        this.origin = origin;
        this.sessionId = sessionId;
    }

    public String getOrigin() {
        return origin;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
