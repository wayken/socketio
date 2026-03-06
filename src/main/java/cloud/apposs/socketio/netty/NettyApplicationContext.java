package cloud.apposs.socketio.netty;

import cloud.apposs.socketio.ApplicationContext;
import cloud.apposs.socketio.SocketIOConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class NettyApplicationContext extends ApplicationContext {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ApplicationHandler application;

    public NettyApplicationContext() {
        super(new SocketIOConfig());
    }

    public NettyApplicationContext(SocketIOConfig configuration) {
        super(configuration);
    }

    @Override
    protected void start(SocketIOConfig configuration) throws Exception {
        Class<? extends ServerChannel> channelClass = null;
        if (Epoll.isAvailable()) {
            this.bossGroup = new EpollEventLoopGroup(configuration.getNumOfGroup());
            this.workerGroup = new EpollEventLoopGroup(configuration.getWorkerCount());
            channelClass = EpollServerSocketChannel.class;
        } else {
            this.bossGroup = new NioEventLoopGroup(configuration.getNumOfGroup());
            this.workerGroup = new NioEventLoopGroup(configuration.getWorkerCount());
            channelClass = NioServerSocketChannel.class;
        }
        application = new ApplicationHandler(configuration);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(channelClass)
                .option(ChannelOption.SO_BACKLOG, configuration.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, configuration.isReuseAddress())
                .childOption(ChannelOption.TCP_NODELAY, configuration.isTcpNoDelay())
                .childOption(ChannelOption.SO_KEEPALIVE,false)
                .childOption(ChannelOption.SO_REUSEADDR,true)
                .childHandler(application.getPipeline());
        InetSocketAddress address = new InetSocketAddress(configuration.getHost(), configuration.getPort());
        bootstrap.bind(address).addListener((FutureListener<Void>) future -> {
            if (!future.isSuccess()) {
                throw new IOException(future.cause());
            }
        }).sync();
    }

    @Override
    protected void handleCloseWebSocketServer() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (application != null) {
            application.shutdown();
        }
    }
}
