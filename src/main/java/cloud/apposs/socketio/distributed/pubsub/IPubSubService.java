package cloud.apposs.socketio.distributed.pubsub;

import java.util.Map;
import java.util.UUID;

/**
 * 分布式服务接口，负责处理分布式环境下的消息发布和订阅功能，确保消息能够在整个集群中正确传递和处理
 */
public interface IPubSubService {
    /**
     * 获取当前服务实例唯一的节点标识，用于区分不同的节点或服务实例，
     * 必须是一个全局唯一的标识，可以使用UUID、IP地址加端口号等方式生成
     *
     * @return 唯一的节点标识
     */
    String getNodeId();

    /**
     * 获取客户端所在的节点标识，根据命名空间和会话ID获取客户端所在的节点标识，
     * 该方法会根据命名空间和会话ID查询分布式注册中心，返回对应的节点标识，以便进行消息分发和路由
     *
     * @param namespace 命名空间
     * @param sessionId 会话ID
     * @return 客户端所在的节点标识，如果没有找到对应的节点，则返回null或抛出异常
     */
    String getClientNodeId(String namespace, UUID sessionId);

    /**
     * 注册会话信息到分布式注册中心，确保在分布式环境中能够正确管理和路由会话，
     * 该方法会将会话信息注册到分布式注册中心，以便其他节点或服务实例能够识别和处理该会话
     *
     * @param namespace 命名空间
     * @param sessionId 会话ID
     */
    void registerSession(String namespace, UUID sessionId);

    /**
     * 检查客户端是否已注册，根据命名空间和会话ID检查客户端是否已注册到分布式注册中心，
     * 该方法会查询分布式注册中心，判断是否存在对应的会话信息，以便进行后续的处理和路由
     *
     * @param namespace 命名空间
     * @param sessionId 会话ID
     * @return 如果客户端已注册，则返回true；否则返回false
     */
    boolean isClientRegistered(String namespace, UUID sessionId);

    /**
     * 注销会话信息，从分布式注册中心中移除会话信息，确保在分布式环境中能够正确管理和路由会话，
     * 该方法会将会话信息从分布式注册中心中移除，以便其他节点或服务实例不再识别和处理该会话
     *
     * @param namespace 命名空间
     * @param sessionId 会话ID
     */
    void unregisterSession(String namespace, UUID sessionId);

    /**
     * 获取所有在线客户端及其节点映射，
     * 该方法会从分布式注册中心中获取所有在线客户端的会话信息，并返回一个映射关系，其中键是客户端会话ID，值是对应的节点ID，
     *
     * @param namespace 命名空间
     * @return 客户端会话ID到节点ID的映射
     */
    Map<UUID, String> getAllSessions(String namespace);

    /**
     * 发布消息到分布式环境中，确保消息能够在整个集群中正确传递和处理，
     * 该方法会将消息发布到分布式环境中，以便其他节点或服务实例能够接收到该消息并进行处理
     *
     * @param type 消息类型
     * @param message  消息内容
     */
    void publish(PubSubType type, PubSubMessage message);

    /**
     * 订阅消息，确保能够接收到分布式环境中发布的消息并进行处理，
     * 该方法会订阅指定类型的消息，以便在分布式环境中接收到该类型的消息时能够正确处理
     *
     * @param type     消息类型
     * @param listener 消息监听器，用于处理接收到的消息
     * @param clazz    消息类，用于反序列化消息内容
     * @param <T>      消息类型，必须继承自PubSubMessage
     */
    <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz);

    /**
     * 取消订阅消息，确保不再接收分布式环境中发布的消息，
     * 该方法会取消订阅指定类型的消息，以便在分布式环境中不再接收到该类型的消息
     *
     * @param type 消息类型
     */
    void unsubscribe(PubSubType type);
}
