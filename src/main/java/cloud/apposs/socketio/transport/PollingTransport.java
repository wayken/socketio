package cloud.apposs.socketio.transport;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.SocketIOSessionBox;
import cloud.apposs.socketio.messages.PacketsMessage;
import cloud.apposs.socketio.messages.XHROptionsMessage;
import cloud.apposs.socketio.messages.XHRPostMessage;
import cloud.apposs.socketio.protocol.ChannelAttributeKey;
import cloud.apposs.socketio.protocol.PacketDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class PollingTransport extends ChannelInboundHandlerAdapter {
    public static final String NAME = "polling";

    private final PacketDecoder decoder;

    private final SocketIOContextHolder contextHolder;

    private final SocketIOSessionBox sessionBox;

    public PollingTransport(PacketDecoder decoder, SocketIOContextHolder contextHolder, SocketIOSessionBox sessionBox) {
        this.decoder = decoder;
        this.contextHolder = contextHolder;
        this.sessionBox = sessionBox;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) message;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());

            List<String> transport = queryDecoder.parameters().get("transport");

            if (transport != null && NAME.equals(transport.get(0))) {
                List<String> sid = queryDecoder.parameters().get("sid");
                List<String> j = queryDecoder.parameters().get("j");
                List<String> b64 = queryDecoder.parameters().get("b64");

                String origin = req.headers().get(HttpHeaderNames.ORIGIN);
                context.channel().attr(ChannelAttributeKey.ORIGIN).set(origin);

                String userAgent = req.headers().get(HttpHeaderNames.USER_AGENT);
                context.channel().attr(ChannelAttributeKey.USER_AGENT).set(userAgent);
                context.channel().attr(ChannelAttributeKey.NAMESPACE).set(queryDecoder.path());

                if (j != null && j.get(0) != null) {
                    Integer index = Integer.valueOf(j.get(0));
                    context.channel().attr(ChannelAttributeKey.JSONP_INDEX).set(index);
                }
                if (b64 != null && b64.get(0) != null) {
                    String flag = b64.get(0);
                    if ("true".equals(flag)) {
                        flag = "1";
                    } else if ("false".equals(flag)) {
                        flag = "0";
                    }
                    Integer enable = Integer.valueOf(flag);
                    context.channel().attr(ChannelAttributeKey.B64).set(enable == 1);
                }

                try {
                    if (sid != null && sid.get(0) != null) {
                        final UUID sessionId = UUID.fromString(sid.get(0));
                        handleMessage(req, sessionId, queryDecoder, context);
                    } else {
                        // first connection
                        SocketIOSession client = context.channel().attr(SocketIOSession.SESSION).get();
                        if (client != null) {
                            handleMessage(req, client.getSessionId(), queryDecoder, context);
                        }
                    }
                } finally {
                    req.release();
                }
                return;
            }
        }
        context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        final Channel channel = context.channel();
        SocketIOSession session = sessionBox.get(channel);
        if (session != null && session.isTransportChannel(context.channel(), Transport.POLLING)) {
            Logger.debug("channel inactive %s", session.getSessionId());
            session.releasePollingChannel(channel);
        }
        super.channelInactive(context);
    }

    private void handleMessage(FullHttpRequest request, UUID sessionId,
                               QueryStringDecoder queryDecoder, ChannelHandlerContext context) throws Exception {
        String origin = request.headers().get(HttpHeaderNames.ORIGIN);
        if (queryDecoder.parameters().containsKey("disconnect")) {
            SocketIOSession session = sessionBox.get(sessionId);
            session.onChannelDisconnect();
            context.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));
        } else if (HttpMethod.POST.equals(request.method())) {
            onPost(sessionId, context, origin, request.content());
        } else if (HttpMethod.GET.equals(request.method())) {
            onGet(sessionId, context, origin);
        } else if (HttpMethod.OPTIONS.equals(request.method())) {
            onOptions(sessionId, context, origin);
        } else {
            Logger.error("Wrong %s method invocation for %s", request.method(), sessionId);
            sendError(context);
        }
    }

    private void onOptions(UUID sessionId, ChannelHandlerContext context, String origin) {
        SocketIOSession session = sessionBox.get(sessionId);
        if (session == null) {
            Logger.error("%s is not registered. Closing connection", sessionId);
            sendError(context);
            return;
        }
        context.channel().writeAndFlush(new XHROptionsMessage(origin, sessionId));
    }

    private void onPost(UUID sessionId, ChannelHandlerContext context, String origin, ByteBuf content) throws IOException {
        SocketIOSession session = sessionBox.get(sessionId);
        if (session == null) {
            Logger.error("%s is not registered. Closing connection", sessionId);
            sendError(context);
            return;
        }

        // release POST response before message processing
        context.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));

        Boolean b64 = context.channel().attr(ChannelAttributeKey.B64).get();
        if (b64 != null && b64) {
            Integer jsonIndex = context.channel().attr(ChannelAttributeKey.JSONP_INDEX).get();
            content = decoder.preprocessJson(jsonIndex, content);
        }

        context.pipeline().fireChannelRead(new PacketsMessage(session, content, Transport.POLLING));
    }

    protected void onGet(UUID sessionId, ChannelHandlerContext context, String origin) throws Exception {
        SocketIOSession session = sessionBox.get(sessionId);
        if (session == null) {
            Logger.error("%s is not registered. Closing connection", sessionId);
            sendError(context);
            return;
        }
        session.bindChannel(context.channel(), Transport.POLLING);
        try {
            contextHolder.onConnect(session);
        } catch (Throwable cause) {
            if (!contextHolder.onError(session.getPath(), cause)) {
                throw cause;
            }
        }
    }

    private void sendError(ChannelHandlerContext context) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        context.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }
}
