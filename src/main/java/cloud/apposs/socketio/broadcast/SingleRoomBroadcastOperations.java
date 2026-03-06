package cloud.apposs.socketio.broadcast;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.distributed.IDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import cloud.apposs.socketio.distributed.pubsub.message.BroadcastMessage;
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

    private final Collection<SocketIOSession> sessions;

    private final IDistributedService distributedService;

    public SingleRoomBroadcastOperations(String namespace, String room, Collection<SocketIOSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.room = room;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        for (SocketIOSession session : sessions) {
            packet.setEngineIOVersion(session.getVersion());
            session.send(packet);
        }
        // 再发布分布式事件，通知其他节点指定房间内所有连接会话发送消息
        IPubSubService pubsubService = distributedService.getPubSubService();
        pubsubService.publish(PubSubType.BROADCAST, new BroadcastMessage(namespace, room, packet));
        return true;
    }

    @Override
    public boolean sendEvent(String name, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.UNKNOWN);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        return send(packet);
    }

    @Override
    public void disconnect() {
        for (SocketIOSession session : sessions) {
            session.disconnect();
        }
    }
}
