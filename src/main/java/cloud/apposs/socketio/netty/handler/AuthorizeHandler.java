package cloud.apposs.socketio.netty.handler;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.SocketIOSessionBox;
import cloud.apposs.socketio.interceptor.CommandarInterceptorSupport;
import cloud.apposs.socketio.messages.HttpErrorMessage;
import cloud.apposs.socketio.protocol.*;
import cloud.apposs.socketio.scheduler.SchedulerKey;
import cloud.apposs.socketio.scheduler.SchedulerKey.Type;
import cloud.apposs.socketio.transport.Transport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class AuthorizeHandler extends ChannelInboundHandlerAdapter {
    private final SocketIOConfig configuration;
    private final SocketIOContextHolder contextHolder;
    private final SocketIOSessionBox sessionBox;

    private final List<Transport> supportTransports = Arrays.asList(Transport.WEBSOCKET, Transport.POLLING);

    public AuthorizeHandler(SocketIOConfig configuration, SocketIOContextHolder contextHolder, SocketIOSessionBox sessionBox) {
        super();
        this.configuration = configuration;
        this.contextHolder = contextHolder;
        this.sessionBox = sessionBox;
    }

    @Override
    public void channelActive(final ChannelHandlerContext context) throws Exception {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, context.channel());
        contextHolder.getScheduler().schedule(key, () -> {
            context.channel().close();
            if (Logger.isDebugEnabled()) {
                Logger.debug("Client with ip %s opened channel but doesn't send any data! Channel closed!",
                        context.channel().remoteAddress());
            }
        }, configuration.getFirstDataTimeout(), TimeUnit.MILLISECONDS);
        super.channelActive(context);
    }

    /**
     * SocketIO首次通讯连接验证入口，流程如下：
     * <pre>
     * 1. 客户端：第一次发送GET /socket.io/?EIO=3&transport=polling&t=1705917464259-0
     * 2. 服务端：响应0{"sid":"VI97HAXpY6yYWAAAC","upgrades":["websocket"],"pingInterval":25000,...}握手数据给到客户端
     * 3. 客户端：第二次发送POST /socket.io/?EIO=3&transport=polling&t=1705917471791-1&sid=VI97HAXpY6yYWAAAC
     * 4. 服务端：根据第二次请求带的SID，响应40，4为Engine.IO "message"，0为Socket.IO "CONNECT"
     * </pre>
     */
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, context.channel());
        contextHolder.getScheduler().cancel(key);

        if (message instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) message;
            Channel channel = context.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());

            // 如果没有找到对应的命名空间则表示业务没有注释@ServerEndpoint对应的命名空间，拒绝连接
            if (!contextHolder.getNamespacesHub().contains(queryDecoder.path())) {
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                request.release();
                Logger.warn("No namespace %s found for request %s", queryDecoder.path(), request.uri());
                return;
            }

            List<String> sid = queryDecoder.parameters().get("sid");
            if (sid == null) {
                if (!authorize(channel, queryDecoder, request)) {
                    request.release();
                    return;
                }
                // forward message to polling or websocket handler to bind channel
            }
        }
        context.fireChannelRead(message);
    }

    private boolean authorize(Channel channel, QueryStringDecoder decoder, FullHttpRequest request) throws Exception {
        Map<String, List<String>> headers = new HashMap<>(request.headers().names().size());
        Map<String, List<String>> parameters = decoder.parameters();
        for (String name : request.headers().names()) {
            List<String> values = request.headers().getAll(name);
            headers.put(name, values);
        }

        // 调用业务认证接口进行SocketIO拦截认证
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        HandshakeData handshakeData = new HandshakeData(request.headers(), parameters,
                (InetSocketAddress)channel.remoteAddress(),
                (InetSocketAddress)channel.localAddress(),
                request.uri(), origin != null && !origin.equalsIgnoreCase("null"));
        CommandarInterceptorSupport interceptorSupport = contextHolder.getCommandarInterceptorSupport();
        boolean isAuthSuccess = interceptorSupport.isAuthorized(handshakeData);
        if (!isAuthSuccess) {
            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            Logger.warn("Handshake unauthorized, query params: %s, headers: %s", parameters, headers);
            return false;
        }

        // 校验传输协议是否支持
        List<String> transportValue = parameters.get("transport");
        if (transportValue == null) {
            Logger.error("Got no transports for request %s", request.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }
        Transport transport = null;
        try {
            transport = Transport.valueOf(transportValue.get(0).toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.error("Unknown transport for request %s", request.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }
        if (!supportTransports.contains(transport)) {
            Logger.error("Unsupported transport for request %s", request.uri());
            writeAndFlushTransportError(channel, origin);
            return false;
        }

        UUID sessionId = UUID.randomUUID();
        SocketIOSession session = new SocketIOSession(decoder.path(), sessionId,
                handshakeData, transport, sessionBox, contextHolder, configuration, parameters);
        channel.attr(SocketIOSession.SESSION).set(session);
        sessionBox.addSession(session);

        String[] transports = {};
        if (!(EngineIOVersion.V4.equals(session.getVersion()) && Transport.WEBSOCKET.equals(session.getTransport()))) {
            transports = new String[]{"websocket"};
        }
        AuthPacket authPacket = new AuthPacket(sessionId, transports,
                configuration.getPingInterval(), configuration.getPingTimeout());
        Packet packet = new Packet(PacketType.OPEN, session.getVersion());
        packet.setData(authPacket);
        session.send(packet);

        session.schedulePing();
        session.schedulePingTimeout();
        session.scheduleRenewal();
        if (Logger.isDebugEnabled()) {
            Logger.debug("Handshake authorized for sessionId: %s, query parameters: %s headers: %s", sessionId, parameters, headers);
        }
        return true;
    }

    private void writeAndFlushTransportError(Channel channel, String origin) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", 0);
        errorData.put("message", "Transport unknown");

        channel.attr(ChannelAttributeKey.ORIGIN).set(origin);
        channel.writeAndFlush(new HttpErrorMessage(errorData));
    }

    public void connect(UUID sessionId) {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        contextHolder.getScheduler().cancel(key);
    }
}
