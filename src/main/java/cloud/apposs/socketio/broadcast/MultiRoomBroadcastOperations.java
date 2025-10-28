package cloud.apposs.socketio.broadcast;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.protocol.Packet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * SocketIO 多个房间广播操作
 */
public class MultiRoomBroadcastOperations implements BroadcastOperations {
    private final Collection<BroadcastOperations> broadcastOperations;

    public MultiRoomBroadcastOperations(Collection<BroadcastOperations> broadcastOperations) {
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public void send(Packet packet) {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.send(packet);
        }
    }

    @Override
    public void sendEvent(String name, Object... data) {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.sendEvent(name, data);
        }
    }

    @Override
    public Collection<SocketIOSession> getClients() {
        Set<SocketIOSession> clients = new HashSet<SocketIOSession>();
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return clients;
        }
        for (BroadcastOperations b : broadcastOperations) {
            clients.addAll(b.getClients());
        }
        return clients;
    }

    @Override
    public void disconnect() {
        if (broadcastOperations == null || broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : broadcastOperations) {
            b.disconnect();
        }
    }
}
