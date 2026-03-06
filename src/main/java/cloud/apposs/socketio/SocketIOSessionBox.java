package cloud.apposs.socketio;

import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;

import java.util.Map;
import java.util.UUID;

public final class SocketIOSessionBox {
    private final Map<UUID, SocketIOSession> sessionBox = PlatformDependent.newConcurrentHashMap();

    private final Map<Channel, SocketIOSession> channelBox = PlatformDependent.newConcurrentHashMap();

    public SocketIOSession get(UUID sessionId) {
        return sessionBox.get(sessionId);
    }

    public SocketIOSession get(Channel channel) {
        return channelBox.get(channel);
    }

    public void add(Channel channel, SocketIOSession session) {
        channelBox.put(channel, session);
    }

    public void remove(Channel channel) {
        channelBox.remove(channel);
    }

    public void addSession(SocketIOSession session) {
        sessionBox.put(session.getSessionId(), session);
    }

    public void removeSession(UUID sessionId) {
        sessionBox.remove(sessionId);
    }

    public Map<UUID, SocketIOSession> getSessionBox() {
        return sessionBox;
    }
}
