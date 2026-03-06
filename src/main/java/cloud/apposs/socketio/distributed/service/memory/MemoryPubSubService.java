package cloud.apposs.socketio.distributed.service.memory;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.pubsub.AbstractPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubListener;
import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版本的客户端注册中心（仅用于单实例部署的环境下或测试）
 */
public class MemoryPubSubService extends AbstractPubSubService {
    // 命名空间 -> (会话ID -> 节点ID)
    private final Map<String, Map<UUID, String>> sessionMapping = new ConcurrentHashMap<>();

    protected MemoryPubSubService(SocketIOConfig configuration) {
        super(configuration);
    }

    @Override
    public String getClientNodeId(String namespace, UUID sessionId) {
        Map<UUID, String> namespaceMapping = sessionMapping.get(namespace);
        if (namespaceMapping != null) {
            return namespaceMapping.get(sessionId);
        }
        return null;
    }

    @Override
    public void registerSession(String namespace, UUID sessionId) {
        sessionMapping.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>())
                .put(sessionId, nodeId);
    }

    @Override
    public boolean isClientRegistered(String namespace, UUID sessionId) {
        Map<UUID, String> namespaceMapping = sessionMapping.get(namespace);
        return namespaceMapping != null && namespaceMapping.containsKey(sessionId);
    }

    @Override
    public void unregisterSession(String namespace, UUID sessionId) {
        Map<UUID, String> namespaceMapping = sessionMapping.get(namespace);
        if (namespaceMapping != null) {
            namespaceMapping.remove(sessionId);
        }
    }

    @Override
    public Map<UUID, String> getAllSessions(String namespace) {
        return sessionMapping.getOrDefault(namespace, new ConcurrentHashMap<>());
    }

    @Override
    public void publish(PubSubType type, PubSubMessage message) {
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
    }

    @Override
    public void unsubscribe(PubSubType type) {
    }
}
