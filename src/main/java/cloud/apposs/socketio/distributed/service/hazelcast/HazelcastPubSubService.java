package cloud.apposs.socketio.distributed.service.hazelcast;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.pubsub.AbstractPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubListener;
import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于Hazelcast实现的分布式服务，使用Hazelcast作为底层存储和通信机制（pub/sub）
 */
public class HazelcastPubSubService extends AbstractPubSubService {
    private static final String KEY_PREFIX = ":sessions:";

    private final HazelcastInstance hazelcastInstance;

    public HazelcastPubSubService(HazelcastInstance instance, SocketIOConfig configuration) {
        super(configuration);
        this.hazelcastInstance = instance;
    }

    @Override
    public String getClientNodeId(String namespace, UUID sessionId) {
        IMap<UUID, String> sessionMapping = hazelcastInstance.getMap(getKey(namespace));
        return sessionMapping.get(sessionId);
    }

    @Override
    public void registerSession(String namespace, UUID sessionId) {
        IMap<UUID, String> sessionMapping = hazelcastInstance.getMap(getKey(namespace));
        sessionMapping.put(sessionId, nodeId);
    }

    @Override
    public boolean isClientRegistered(String namespace, UUID sessionId) {
        IMap<UUID, String> sessionMapping = hazelcastInstance.getMap(getKey(namespace));
        return sessionMapping.containsKey(sessionId);
    }

    @Override
    public void unregisterSession(String namespace, UUID sessionId) {
        IMap<UUID, String> sessionMapping = hazelcastInstance.getMap(getKey(namespace));
        sessionMapping.remove(sessionId);
    }

    @Override
    public Map<UUID, String> getAllSessions(String namespace) {
        IMap<UUID, String> sessionMapping = hazelcastInstance.getMap(getKey(namespace));
        return sessionMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void publish(PubSubType type, PubSubMessage message) {
        message.setNodeId(nodeId);
        hazelcastInstance.getTopic(type.name()).publish(message);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
        hazelcastInstance.getTopic(type.name()).addMessageListener(msg -> {
            Object messageObj = msg.getMessageObject();
            if (clazz.isInstance(messageObj)) {
                T message = clazz.cast(messageObj);
                if (!nodeId.equals(message.getNodeId())) {
                    listener.onMessage(message);
                }
            }
        });
    }

    @Override
    public void unsubscribe(PubSubType type) {
        hazelcastInstance.getTopic(type.name()).destroy();
    }

    private String getKey(String namespace) {
        return configuration.getDistributedServiceName() + KEY_PREFIX + namespace;
    }
}
