package cloud.apposs.socketio.ack;

import cloud.apposs.socketio.scheduler.SchedulerKey;

import java.util.UUID;

public class AckSchedulerKey extends SchedulerKey {
    private final long index;

    public AckSchedulerKey(Type type, UUID sessionId, long index) {
        super(type, sessionId);
        this.index = index;
    }

    public long getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int) (index ^ (index >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AckSchedulerKey other = (AckSchedulerKey) obj;
        if (index != other.index) {
            return false;
        }
        return true;
    }
}
