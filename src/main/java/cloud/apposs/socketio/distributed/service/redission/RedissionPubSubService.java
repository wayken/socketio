package cloud.apposs.socketio.distributed.service.redission;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.pubsub.AbstractPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubListener;
import cloud.apposs.socketio.distributed.pubsub.PubSubMessage;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;

import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于Redisson实现的分布式发布订阅服务，使用Redis作为底层存储和通信机制（PUB/SUB）
 */
public class RedissionPubSubService extends AbstractPubSubService {
    private final RedissonClient redissonClient;

    private static final String KEY_PREFIX = ":sessions:";
    private static final long EXPIRATION_TIME = 300;

    private final ConcurrentMap<String, Queue<Integer>> topicRegMapping = new ConcurrentHashMap<>();

    public RedissionPubSubService(RedissonClient redissonClient, SocketIOConfig configuration) {
        super(configuration);
        this.redissonClient = redissonClient;
    }

    @Override
    public String getClientNodeId(String namespace, UUID sessionId) {
        RBucket<String> bucket = redissonClient.getBucket(getKey(namespace, sessionId));
        return bucket.get();
    }

    @Override
    public void registerSession(String namespace, UUID sessionId) {
        RBucket<String> bucket = redissonClient.getBucket(getKey(namespace, sessionId));
        bucket.set(nodeId, EXPIRATION_TIME < 0 ? null : Duration.ofSeconds(EXPIRATION_TIME));
    }

    @Override
    public boolean isClientRegistered(String namespace, UUID sessionId) {
        return redissonClient.getBucket(getKey(namespace, sessionId)).isExists();
    }

    @Override
    public void unregisterSession(String namespace, UUID sessionId) {
        redissonClient.getBucket(getKey(namespace, sessionId)).delete();
    }

    @Override
    public Map<UUID, String> getAllSessions(String namespace) {
        KeysScanOptions scanOptions = KeysScanOptions.defaults().pattern(getKey(namespace) + "*").chunkSize(100);
        Iterable<String> scannedKeys = redissonClient.getKeys().getKeys(scanOptions);
        Map<UUID, String> sessionNodeMap = new ConcurrentHashMap<>();
        for (String key : scannedKeys) {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String nodeId = bucket.get();
            String sessionIdStr = key.substring((getKey(namespace)).length());
            UUID sessionId = UUID.fromString(sessionIdStr);
            sessionNodeMap.put(sessionId, nodeId);
        }
        return sessionNodeMap;
    }

    @Override
    public void publish(PubSubType type, PubSubMessage message) {
        message.setNodeId(nodeId);
        redissonClient.getTopic(type.toString()).publish(message);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
        String name = type.toString();
        RTopic topic = redissonClient.getTopic(name);
        int regId = topic.addListener(PubSubMessage.class, (channel, message) -> {
            if (!nodeId.equals(message.getNodeId())) {
                listener.onMessage((T) message);
            }
        });

        Queue<Integer> topicRegQueue = topicRegMapping.get(name);
        if (topicRegQueue == null) {
            topicRegQueue = new ConcurrentLinkedQueue<Integer>();
            Queue<Integer> oldList = topicRegMapping.putIfAbsent(name, topicRegQueue);
            if (oldList != null) {
                topicRegQueue = oldList;
            }
        }
        topicRegQueue.add(regId);
    }

    @Override
    public void unsubscribe(PubSubType type) {
        String name = type.toString();
        Queue<Integer> regIds = topicRegMapping.remove(name);
        RTopic topic = redissonClient.getTopic(name);
        for (Integer id : regIds) {
            topic.removeListener(id);
        }
    }

    private String getKey(String namespace) {
        return getKey(namespace, null);
    }

    private String getKey(String namespace, UUID sessionId) {
        return configuration.getDistributedServiceName() + KEY_PREFIX + namespace + ":" + (sessionId == null ? "" : sessionId.toString());
    }
}
