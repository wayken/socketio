package cloud.apposs.socketio.ack;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.Disconnectable;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.scheduler.CancelableScheduler;
import cloud.apposs.socketio.scheduler.SchedulerKey;
import io.netty.util.internal.PlatformDependent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AckManager implements Disconnectable {
    static class AckEntry {
        final Map<Long, AckCallback<?>> ackCallbacks = PlatformDependent.newConcurrentHashMap();
        final AtomicLong ackIndex = new AtomicLong(-1);

        public long addAckCallback(AckCallback<?> callback) {
            long index = ackIndex.incrementAndGet();
            ackCallbacks.put(index, callback);
            return index;
        }

        public Set<Long> getAckIndexes() {
            return ackCallbacks.keySet();
        }

        public AckCallback<?> getAckCallback(long index) {
            return ackCallbacks.get(index);
        }

        public AckCallback<?> removeCallback(long index) {
            return ackCallbacks.remove(index);
        }

        public void initAckIndex(long index) {
            ackIndex.compareAndSet(-1, index);
        }
    }

    private final ConcurrentMap<UUID, AckEntry> ackEntries = PlatformDependent.newConcurrentHashMap();

    private final CancelableScheduler scheduler;

    public AckManager(CancelableScheduler scheduler) {
        super();
        this.scheduler = scheduler;
    }

    public void initAckIndex(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(index);
    }

    private AckEntry getAckEntry(UUID sessionId) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        if (ackEntry == null) {
            ackEntry = new AckEntry();
            AckEntry oldAckEntry = ackEntries.putIfAbsent(sessionId, ackEntry);
            if (oldAckEntry != null) {
                ackEntry = oldAckEntry;
            }
        }
        return ackEntry;
    }

    @SuppressWarnings("unchecked")
    public void onAck(SocketIOSession session, Packet packet) {
        AckSchedulerKey key = new AckSchedulerKey(SchedulerKey.Type.ACK_TIMEOUT, session.getSessionId(), packet.getAckId());
        scheduler.cancel(key);

        AckCallback callback = removeCallback(session.getSessionId(), packet.getAckId());
        if (callback == null) {
            return;
        }
        if (callback instanceof MultiTypeAckCallback) {
            callback.onSuccess(new MultiTypeArgs(packet.<List<Object>>getData()));
        } else {
            Object param = null;
            List<Object> args = packet.getData();
            if (!args.isEmpty()) {
                param = args.get(0);
            }
            if (args.size() > 1) {
                Logger.error("Wrong ack args amount. Should be only one argument, but current amount is: %s. Ack id: %s, sessionId: %s",
                        args.size(), packet.getAckId(), session.getSessionId());
            }
            callback.onSuccess(param);
        }
    }

    private AckCallback<?> removeCallback(UUID sessionId, long index) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        // may be null if client disconnected
        // before timeout occurs
        if (ackEntry != null) {
            return ackEntry.removeCallback(index);
        }
        return null;
    }

    public AckCallback<?> getCallback(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        return ackEntry.getAckCallback(index);
    }

    public long registerAck(UUID sessionId, AckCallback<?> callback) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(0);
        long index = ackEntry.addAckCallback(callback);

        if (Logger.isDebugEnabled()) {
            Logger.debug("AckCallback registered with id: %d for client: %s", index, sessionId);
        }

        scheduleTimeout(index, sessionId, callback);

        return index;
    }

    private void scheduleTimeout(final long index, final UUID sessionId, AckCallback<?> callback) {
        if (callback.getTimeout() == -1) {
            return;
        }
        SchedulerKey key = new AckSchedulerKey(SchedulerKey.Type.ACK_TIMEOUT, sessionId, index);
        scheduler.scheduleCallback(key, new Runnable() {
            @Override
            public void run() {
                AckCallback<?> cb = removeCallback(sessionId, index);
                if (cb != null) {
                    cb.onTimeout();
                }
            }
        }, callback.getTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void onDisconnect(SocketIOSession session) {
        AckEntry e = ackEntries.remove(session.getSessionId());
        if (e == null) {
            return;
        }

        Set<Long> indexes = e.getAckIndexes();
        for (Long index : indexes) {
            AckCallback<?> callback = e.getAckCallback(index);
            if (callback != null) {
                callback.onTimeout();
            }
            SchedulerKey key = new AckSchedulerKey(SchedulerKey.Type.ACK_TIMEOUT, session.getSessionId(), index);
            scheduler.cancel(key);
        }
    }
}
