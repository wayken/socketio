package cloud.apposs.socketio.distributed;

import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import cloud.apposs.socketio.namespace.NamespacesHub;

/**
 * 分布式服务接口，负责处理分布式环境下以下功能：
 * <pre>
 *  1. 消息分发：将消息分发到正确的节点或服务实例
 *  2. 会话管理：在分布式环境中进行用户会话注册和管理，确保会话数据的一致性和可用性
 *  3. 负载均衡：根据节点或服务实例的负载情况进行消息分发，确保系统的高可用性和性能
 *  4. 集群通信：提供节点之间的通信机制（即pub/sub），以实现消息的广播和订阅功能，确保消息能够在整个集群中正确传递和处理
 * </pre>
 */
public interface IDistributedService {
    /**
     * 初始化分布式服务，建立与分布式注册中心的连接，并准备好处理分布式环境下的消息分发、会话管理、负载均衡和集群通信等功能，
     * 该方法会在服务启动时调用，确保分布式服务能够正确初始化并准备好处理分布式环境下的各种功能
     *
     * @param configuration SocketIO配置对象
     * @param namespacesHub 命名空间管理服务
     */
    void initialize(SocketIOConfig configuration, NamespacesHub namespacesHub);

    /**
     * 获取分布式服务的消息发布和订阅功能接口，提供节点之间的通信机制（即pub/sub），以实现消息的广播和订阅功能，确保消息能够在整个集群中正确传递和处理，
     * 该方法会返回一个IPubSubService接口实例，供其他组件或服务使用，以便进行消息的发布和订阅操作
     *
     * @return IPubSubService接口实例
     */
    IPubSubService getPubSubService();

    /**
     * 获取分布式存储服务，提供分布式环境下的会话数据存储和管理功能
     *
     * @return IRepositoryService接口实例
     */
    IRepositoryService getRepositoryService();

    /**
     * 关闭分布式服务，释放资源并断开与分布式注册中心的连接，
     * 该方法会在服务关闭时调用，确保分布式服务能够正确释放资源并断开与分布式注册中心的连接
     */
    void shutdown();
}
