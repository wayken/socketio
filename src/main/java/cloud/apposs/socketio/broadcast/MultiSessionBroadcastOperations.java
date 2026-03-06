package cloud.apposs.socketio.broadcast;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.distributed.IDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import cloud.apposs.socketio.distributed.pubsub.message.BulkDispatchMessage;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;

import java.util.*;

public class MultiSessionBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final Set<UUID> sessionIds;

    private final Map<UUID, SocketIOSession> sessions;

    private final IDistributedService distributedService;

    public MultiSessionBroadcastOperations(String namespace, Set<UUID> sessionIds, Map<UUID, SocketIOSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.sessionIds = sessionIds;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        Set<UUID> remoteSessionIds = new HashSet<>();
        IPubSubService pubsubService = distributedService.getPubSubService();
        for (UUID sessionId : sessionIds) {
            SocketIOSession session = sessions.get(sessionId);
            if (session == null && pubsubService.isClientRegistered(namespace, sessionId)) {
                remoteSessionIds.add(sessionId);
                continue;
            }
            if (session != null) {
                packet.setEngineIOVersion(session.getVersion());
                session.send(packet);
            }
        }
        // 如果有远程会话，则发布分布式事件，通知其他节点发送消息
        if (!remoteSessionIds.isEmpty()) {
            pubsubService.publish(PubSubType.BULK_DISPATCH, new BulkDispatchMessage(namespace, remoteSessionIds, packet));
        }
        return true;
    }

    @Override
    public boolean sendEvent(String name, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        return send(packet);
    }

    @Override
    public void disconnect() {
    }
}
