package cloud.apposs.socketio.distributed;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import cloud.apposs.socketio.distributed.pubsub.message.*;
import cloud.apposs.socketio.namespace.Namespace;
import cloud.apposs.socketio.namespace.NamespacesHub;

import java.util.Set;
import java.util.UUID;

public abstract class AbstractDistributedService implements IDistributedService {
    protected final SocketIOConfig configuration;

    public AbstractDistributedService(SocketIOConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void initialize(final SocketIOConfig configuration, final NamespacesHub namespacesHub) {
        IPubSubService pubsubService = getPubSubService();

        // 接收到远端分发消息，转发到本地对应的session进行处理
        pubsubService.subscribe(PubSubType.DISPATCH, message -> {
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                SocketIOSession session = namespace.getSession(message.getSessionId());
                if (session != null) {
                    session.send(message.getPacket());
                }
            }
        }, DispatchMessage.class);

        // 接收到远端批量分发消息，转发到本地对应的session进行处理
        pubsubService.subscribe(PubSubType.BULK_DISPATCH, message -> {
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                Set<UUID> sessionIds = message.getSessionIds();
                for (UUID sessionId : sessionIds) {
                    SocketIOSession session = namespace.getSession(sessionId);
                    if (session != null) {
                        session.send(message.getPacket());
                    }
                }
            }
        }, BulkDispatchMessage.class);

        // 接收到远端指定房间广播消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BROADCAST, message -> {
            String room = message.getRoom();
            namespacesHub.get(message.getNamespace()).broadcast(room, message.getPacket());
        }, BroadcastMessage.class);

        // 接收到远端加入房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.JOIN, message -> {
            String name = message.getRoom();
            Namespace namespace = namespacesHub.get(message.getNamespace());
            if (namespace != null) {
                namespace.join(name, message.getSessionId());
            }
        }, JoinLeaveMessage.class);

        // 接收到远端批量加入房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BULK_JOIN, message -> {
            Set<String> rooms = message.getRooms();
            for (String room : rooms) {
                Namespace namespace = namespacesHub.get(message.getNamespace());
                if (namespace != null) {
                    namespace.join(room, message.getSessionId());
                }
            }
        }, BulkJoinLeaveMessage.class);

        // 接收到远端离开房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.LEAVE, message -> {
            String name = message.getRoom();
            Namespace n = namespacesHub.get(message.getNamespace());
            if (n != null) {
                n.leave(name, message.getSessionId());
            }
        }, JoinLeaveMessage.class);

        // 接收到远端批量离开房间消息，转发到本地对应的namespace进行处理
        pubsubService.subscribe(PubSubType.BULK_LEAVE, message -> {
            Set<String> rooms = message.getRooms();
            for (String room : rooms) {
                Namespace namespace = namespacesHub.get(message.getNamespace());
                if (namespace != null) {
                    namespace.leave(room, message.getSessionId());
                }
            }
        }, BulkJoinLeaveMessage.class);
    }
}
