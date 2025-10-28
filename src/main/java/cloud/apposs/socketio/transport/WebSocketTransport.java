package cloud.apposs.socketio.transport;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.SocketIOSessionBox;
import cloud.apposs.socketio.messages.PacketsMessage;
import cloud.apposs.socketio.netty.SocketIOChannelInitializer;
import cloud.apposs.socketio.protocol.ChannelAttributeKey;
import cloud.apposs.socketio.protocol.EngineIOVersion;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;
import cloud.apposs.socketio.scheduler.CancelableScheduler;
import cloud.apposs.socketio.scheduler.SchedulerKey;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Sharable
public class WebSocketTransport extends ChannelInboundHandlerAdapter {
    public static final String NAME = "websocket";

    private final SocketIOConfig configuration;
    private final SocketIOContextHolder contextHolder;
    private final CancelableScheduler scheduler;
    private final SocketIOSessionBox sessionBox;

    private final boolean isSsl;

    public WebSocketTransport(boolean isSsl, SocketIOConfig configuration, SocketIOContextHolder contextHolder,
                              CancelableScheduler scheduler, SocketIOSessionBox sessionBox) {
        this.isSsl = isSsl;
        this.configuration = configuration;
        this.contextHolder = contextHolder;
        this.scheduler = scheduler;
        this.sessionBox = sessionBox;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof CloseWebSocketFrame) {
            context.channel().writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
        } else if (message instanceof BinaryWebSocketFrame || message instanceof TextWebSocketFrame) {
            ByteBufHolder frame = (ByteBufHolder) message;
            SocketIOSession session = sessionBox.get(context.channel());
            if (session == null) {
                Logger.debug("Client with was already disconnected. Channel closed!");
                context.channel().close();
                frame.release();
                return;
            }
            context.pipeline().fireChannelRead(new PacketsMessage(session, frame.content(), Transport.WEBSOCKET));
            frame.release();
        } else if (message instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) message;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
            String path = queryDecoder.path();
            List<String> transport = queryDecoder.parameters().get("transport");
            List<String> sid = queryDecoder.parameters().get("sid");

            if (transport != null && NAME.equals(transport.get(0))) {
                try {
                    if (sid != null && sid.get(0) != null) {
                        final UUID sessionId = UUID.fromString(sid.get(0));
                        handshake(context, sessionId, path, request);
                    } else {
                        SocketIOSession session = context.channel().attr(SocketIOSession.SESSION).get();
                        // first connection
                        if (session != null) {
                            handshake(context, session.getSessionId(), path, request);
                        }
                    }
                } finally {
                    request.release();
                }
            } else {
                context.fireChannelRead(message);
            }
        } else {
            context.fireChannelRead(message);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        SocketIOSession session = sessionBox.get(context.channel());
        if (session != null && session.isTransportChannel(context.channel(), Transport.WEBSOCKET)) {
            context.flush();
        } else {
            super.channelReadComplete(context);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        final Channel channel = context.channel();
        SocketIOSession session = sessionBox.get(context.channel());
        Packet packet = new Packet(PacketType.MESSAGE, session != null ? session.getVersion() : EngineIOVersion.UNKNOWN);
        packet.setSubType(PacketType.DISCONNECT);
        if (session != null && session.isTransportChannel(context.channel(), Transport.WEBSOCKET)) {
            Logger.debug("channel inactive %s", session.getSessionId());
            session.onChannelDisconnect();
        }
        super.channelInactive(context);
        if (session != null) {
            session.send(packet);
        }
        channel.close();
        context.close();
    }

    private void handshake(ChannelHandlerContext context, final UUID sessionId, String path, FullHttpRequest request) {
        final Channel channel = context.channel();
        channel.attr(ChannelAttributeKey.NAMESPACE).set(path);
        WebSocketServerHandshakerFactory factory =
                new WebSocketServerHandshakerFactory(getWebSocketLocation(request), null, true, configuration.getMaxFramePayloadLength());
        WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
        if (handshaker != null) {
            ChannelFuture f = handshaker.handshake(channel, request);
            f.addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    Logger.error("Can't handshake " + sessionId, future.cause());
                    return;
                }
                channel.pipeline().addBefore(SocketIOChannelInitializer.WEB_SOCKET_TRANSPORT, SocketIOChannelInitializer.WEB_SOCKET_AGGREGATOR,
                        new WebSocketFrameAggregator(configuration.getMaxFramePayloadLength()));
                connectClient(channel, sessionId);
            });
        } else {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
        }
    }

    private void connectClient(final Channel channel, final UUID sessionId) throws Exception {
        SocketIOSession session = sessionBox.get(sessionId);
        if (session == null) {
            Logger.warn("Unauthorized client with sessionId: %s with ip: %s. Channel closed!",
                    sessionId, channel.remoteAddress());
            channel.close();
            return;
        }
        session.bindChannel(channel, Transport.WEBSOCKET);
        if (session.getTransport() == Transport.POLLING) {
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.UPGRADE_TIMEOUT, sessionId);
            scheduler.schedule(key, () -> {
                SocketIOSession session1 = sessionBox.get(sessionId);
                if (session1 != null) {
                    if (Logger.isDebugEnabled()) {
                        Logger.debug("client did not complete upgrade - closing transport");
                    }
                    session1.onChannelDisconnect();
                }
            }, configuration.getUpgradeTimeout(), TimeUnit.MILLISECONDS);
        }
        if (Logger.isDebugEnabled()) {
            Logger.debug("сlient %s handshake completed", sessionId);
        }
        try {
            contextHolder.onConnect(session);
        } catch (Throwable cause) {
            // 如果是方法调用中有异常，需要获取的是真正的业务异常
            if (cause instanceof InvocationTargetException) {
                cause = ((InvocationTargetException) cause).getTargetException();
            }
            if (!contextHolder.onError(session.getPath(), cause)) {
                throw (Exception) cause;
            }
        }
    }

    private String getWebSocketLocation(HttpRequest request) {
        String protocol = "ws://";
        if (isSsl) {
            protocol = "wss://";
        }
        return protocol + request.headers().get(HttpHeaderNames.HOST) + request.uri();
    }
}
