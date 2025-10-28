package cloud.apposs.socketio.scheduler;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeUnit;

public interface CancelableScheduler {
    void update(ChannelHandlerContext ctx);

    void cancel(SchedulerKey key);

    void scheduleCallback(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    void schedule(Runnable runnable, long delay, TimeUnit unit);

    void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    void shutdown();
}
