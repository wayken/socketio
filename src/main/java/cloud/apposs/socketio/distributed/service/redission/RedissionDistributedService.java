package cloud.apposs.socketio.distributed.service.redission;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.AbstractDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * 基于Redisson实现的分布式服务，使用Redis作为底层存储和通信机制（pub/sub），注意有一个命名空间对应一个Redis连接
 */
public class RedissionDistributedService extends AbstractDistributedService {
    private final RedissonClient redissonClient;

    private final IPubSubService pubSubService;

    private final IRepositoryService repositoryService;

    public RedissionDistributedService(SocketIOConfig configuration) {
        this(handleRedissionClientInit(configuration), configuration);
    }

    public RedissionDistributedService(RedissonClient redissonClient, SocketIOConfig configuration) {
        super(configuration);
        this.redissonClient = redissonClient;
        this.pubSubService = new RedissionPubSubService(redissonClient, configuration);
        this.repositoryService = new RedissionRepositoryService(redissonClient);
    }

    @Override
    public IPubSubService getPubSubService() {
        return pubSubService;
    }

    @Override
    public IRepositoryService getRepositoryService() {
        return repositoryService;
    }

    private static RedissonClient handleRedissionClientInit(SocketIOConfig configuration) {
        Config config = new Config();
        String redisAddress = configuration.getDistributedServerAddress();
        if (!redisAddress.startsWith("redis://") && !redisAddress.startsWith("rediss://")) {
            redisAddress = "redis://" + redisAddress;
        }
        config.useSingleServer().setAddress(redisAddress);
        Logger.info("Initialized Redisson Distributed Service with address: " + redisAddress);
        return Redisson.create(config);
    }

    @Override
    public void shutdown() {
        redissonClient.shutdown();
    }
}
