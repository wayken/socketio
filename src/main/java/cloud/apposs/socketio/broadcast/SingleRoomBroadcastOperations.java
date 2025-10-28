package cloud.apposs.socketio.broadcast;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.protocol.EngineIOVersion;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;

import java.util.Arrays;
import java.util.Collection;

/**
 * SocketIO 单个房间广播操作
 */
public class SingleRoomBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final String room;

    private final Collection<SocketIOSession> clients;

    public SingleRoomBroadcastOperations(String namespace, String room, Collection<SocketIOSession> clients) {
        this.namespace = namespace;
        this.room = room;
        this.clients = clients;
    }

    @Override
    public Collection<SocketIOSession> getClients() {
        return clients;
    }

    @Override
    public void send(Packet packet) {
        for (SocketIOSession client : clients) {
            packet.setEngineIOVersion(client.getVersion());
            client.send(packet);
        }
    }

    @Override
    public void sendEvent(String name, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.UNKNOWN);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    @Override
    public void disconnect() {
        for (SocketIOSession client : clients) {
            client.disconnect();
        }
    }
}
