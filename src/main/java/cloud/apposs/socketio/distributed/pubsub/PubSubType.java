package cloud.apposs.socketio.distributed.pubsub;

public enum PubSubType {
    DISPATCH, BULK_DISPATCH, BROADCAST, JOIN, BULK_JOIN, LEAVE, BULK_LEAVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
