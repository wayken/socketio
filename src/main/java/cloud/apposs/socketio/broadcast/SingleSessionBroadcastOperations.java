package cloud.apposs.socketio.broadcast;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.distributed.IDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import cloud.apposs.socketio.distributed.pubsub.message.DispatchMessage;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class SingleSessionBroadcastOperations implements BroadcastOperations {
    private final String namespace;

    private final UUID sessionId;

    private final Map<UUID, SocketIOSession> sessions;

    private final IDistributedService distributedService;

    public SingleSessionBroadcastOperations(String namespace, UUID sessionId, Map<UUID, SocketIOSession> sessions, IDistributedService distributedService) {
        this.namespace = namespace;
        this.sessionId = sessionId;
        this.sessions = sessions;
        this.distributedService = distributedService;
    }

    @Override
    public boolean send(Packet packet) {
        // 先从当前分布式节点会话管理中获取到该连接，获取到则说明会话是连接到了当前节点
        if (sessions.containsKey(sessionId)) {
            return sessions.get(sessionId).send(packet) != null;
        }
        // 如果客户端连接没有在当前分布式节点会话管理中，说明当前连接还没有完成连接建立，或者已经断开连接了，需要通过分布式服务来获取到该连接所在的分布式节点
        IPubSubService pubsubService = distributedService.getPubSubService();
        String sourceNodeId = pubsubService.getNodeId();
        String remoteNodeId = pubsubService.getClientNodeId(namespace, sessionId);
        // 如果客户端连接没有在当前分布式节点会话管理中，说明当前连接还没有完成连接建立，或者已经断开连接了
        if (remoteNodeId == null) {
            Logger.warn("Session %s is not connected to any remote node, current node is %s", sessionId, sourceNodeId);
            return false;
        }
        // 正常情况下，不可能出现客户端连接在当前分布式节点会话管理中没有找到，但是在其他分布式节点上找到
        if (sourceNodeId.equals(remoteNodeId)) {
            Logger.warn("Session %s is connected to remote node %s, but current node is also %s, this should not happen", sessionId, remoteNodeId, sourceNodeId);
            return false;
        }
        // 如果客户端连接不在当前分布式节点会话管理中，说明当前连接已经完成连接建立了，但是在其他分布式节点上了，需要通过分布式服务来发布/订阅
        if (Logger.isDebugEnabled()) {
            Logger.debug("Session %s is connected to remote node %s, current node is %s", sessionId, remoteNodeId, sourceNodeId);
        }
        PubSubMessage message = new DispatchMessage(namespace, sessionId, packet);
        distributedService.getPubSubService().publish(PubSubType.DISPATCH, message);
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
