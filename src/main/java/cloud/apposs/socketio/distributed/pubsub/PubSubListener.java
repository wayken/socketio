package cloud.apposs.socketio.distributed.pubsub;

/**
 * Pub/Sub监听器接口，定义了在分布式环境中接收和处理消息的回调方法，所有分布式环境下的消息处理逻辑都应该实现该接口，以确保能够正确接收和处理来自其他节点的消息
 *
 * @param <T> 消息数据类型
 */
public interface PubSubListener<T> {
    void onMessage(T data);
}

