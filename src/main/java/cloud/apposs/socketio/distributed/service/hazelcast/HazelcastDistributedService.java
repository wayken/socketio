package cloud.apposs.socketio.distributed.service.hazelcast;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.AbstractDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * 基于Hazelcast实现的分布式服务，使用Hazelcast作为底层存储和通信机制（PUB/SUB）
 */
public class HazelcastDistributedService extends AbstractDistributedService {
    private final HazelcastInstance hazelcastInstance;

    private final IPubSubService pubSubService;

    private final IRepositoryService repositoryService;

    public HazelcastDistributedService(SocketIOConfig configuration) {
        this(handleHazelcastInstanceInit(configuration), configuration);
    }

    public HazelcastDistributedService(HazelcastInstance instance, SocketIOConfig configuration) {
        super(configuration);
        this.hazelcastInstance = instance;
        this.pubSubService = new HazelcastPubSubService(hazelcastInstance, configuration);
        this.repositoryService = new HazelcastRepositoryService(hazelcastInstance);
    }

    private static HazelcastInstance handleHazelcastInstanceInit(SocketIOConfig configuration) {
        Config config = new Config();
        config.setClusterName(configuration.getDistributedServiceName());
        return Hazelcast.newHazelcastInstance(config);
    }

    @Override
    public IPubSubService getPubSubService() {
        return pubSubService;
    }

    @Override
    public IRepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public void shutdown() {
        hazelcastInstance.shutdown();
    }
}
