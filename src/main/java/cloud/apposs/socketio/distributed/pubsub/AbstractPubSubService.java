package cloud.apposs.socketio.distributed.pubsub;

import cloud.apposs.socketio.SocketIOConfig;

import java.util.UUID;

public abstract class AbstractPubSubService implements IPubSubService {
    protected final String nodeId = "distributed-pubsub-service:" + UUID.randomUUID().toString();

    protected final SocketIOConfig configuration;

    protected AbstractPubSubService(SocketIOConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }
}
