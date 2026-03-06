package cloud.apposs.socketio.distributed.service.memory;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.AbstractDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.repository.IRepositoryService;

/**
 * 内存版本的分布式服务实现（仅用于单实例部署的环境下或测试）
 */
public class MemoryDistributedService extends AbstractDistributedService {
    private final IPubSubService pubSubService;

    private final IRepositoryService repositoryService;

    public MemoryDistributedService(SocketIOConfig configuration) {
        super(configuration);
        this.pubSubService = new MemoryPubSubService(configuration);
        this.repositoryService = new MemoryRepositoryService();
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
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (local session distributed only)";
    }
}
