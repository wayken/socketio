package cloud.apposs.socketio.messages;

import java.util.UUID;

public class XHRPostMessage extends HttpMessage {
    public XHRPostMessage(String origin, UUID sessionId) {
        super(origin, sessionId);
    }
}
