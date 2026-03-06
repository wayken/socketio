package cloud.apposs.socketio.distributed;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOConstants;
import cloud.apposs.socketio.distributed.service.hazelcast.HazelcastDistributedService;
import cloud.apposs.socketio.distributed.service.memory.MemoryDistributedService;
import cloud.apposs.socketio.distributed.service.redission.RedissionDistributedService;

/**
 * 分布式工厂接口，负责创建和管理分布式环境中的组件和资源
 */
public class DistributedServiceFactory {
    /**
     * 创建一个新的分布式服务实例，用于处理分布式环境下的消息分发、会话管理等功能
     *
     * @return 分布式服务实例
     */
    public static IDistributedService newDistributedService(String type, SocketIOConfig configuration) {
        type = type.toLowerCase();
        IDistributedService service;
        switch (type) {
            case SocketIOConstants.DISTRIBUTED_SERVICE_MEMORY:
                service = new MemoryDistributedService(configuration);
                break;
            case SocketIOConstants.DISTRIBUTED_SERVICE_REDISSION:
                service = new RedissionDistributedService(configuration);
                break;
            case SocketIOConstants.DISTRIBUTED_SERVICE_HAZELCAST:
                service = new HazelcastDistributedService(configuration);
                break;
            default:
                throw new IllegalArgumentException("Unsupported distributed service type: " + type);
        }
        Logger.info("Created distributed service of type: %s with node id: %s", type, service.getPubSubService().getNodeId());
        return service;
    }
}
