package cloud.apposs.socketio.distributed.pubsub;

import java.io.Serializable;

/**
 * Pub/Sub消息基类，所有分布式环境下的消息都应该继承自该类，以确保消息能够正确序列化和传输，
 * 该类实现了Serializable接口，确保消息对象能够被序列化和反序列化，以便在分布式环境中进行传输和处理
 */
public abstract class PubSubMessage implements Serializable {
    private static final long serialVersionUID = -8789343104393884987L;

    private String nodeId;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
