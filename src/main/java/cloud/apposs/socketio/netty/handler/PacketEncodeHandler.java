package cloud.apposs.socketio.netty.handler;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.messages.*;
import cloud.apposs.socketio.messages.HttpMessage;
import cloud.apposs.socketio.protocol.ChannelAttributeKey;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketEncoder;
import cloud.apposs.socketio.transport.Transport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class PacketEncodeHandler extends ChannelOutboundHandlerAdapter {
    private static final byte[] OK = "ok".getBytes(CharsetUtil.UTF_8);

    private SocketIOConfig configuration;

    private String version;

    private final PacketEncoder encoder;

    public PacketEncodeHandler(SocketIOConfig configuration, PacketEncoder encoder) throws IOException {
        this.encoder = encoder;
        this.configuration = configuration;

        if (configuration.isAddVersionHeader()) {
            readVersion();
        }
    }

    private void readVersion() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes attrs = manifest.getMainAttributes();
                if (attrs == null) {
                    continue;
                }
                String name = attrs.getValue("Bundle-Name");
                if (name != null && name.equals("socketio")) {
                    version = name + "/" + attrs.getValue("Bundle-Version");
                    break;
                }
            } catch (IOException E) {
                // skip it
            }
        }
    }

    private void write(XHROptionsMessage message, ChannelHandlerContext context, ChannelPromise promise) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.headers().add(HttpHeaderNames.SET_COOKIE, "io=" + message.getSessionId())
                    .add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    .add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaderNames.CONTENT_TYPE);

        String origin = context.channel().attr(ChannelAttributeKey.ORIGIN).get();
        addOriginHeaders(origin, res);

        ByteBuf out = encoder.allocateBuffer(context.alloc());
        sendMessage(message, context.channel(), out, res, promise);
    }

    private void write(XHRPostMessage message, ChannelHandlerContext context, ChannelPromise promise) {
        ByteBuf out = encoder.allocateBuffer(context.alloc());
        out.writeBytes(OK);
        sendMessage(message, context.channel(), out, "text/html", promise, HttpResponseStatus.OK);
    }

    private void sendMessage(HttpMessage message, Channel channel, ByteBuf out, String type, ChannelPromise promise, HttpResponseStatus status) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, status);

        res.headers().add(HttpHeaderNames.CONTENT_TYPE, type)
                    .add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        if (message.getSessionId() != null) {
            res.headers().add(HttpHeaderNames.SET_COOKIE, "io=" + message.getSessionId());
        }

        String origin = channel.attr(ChannelAttributeKey.ORIGIN).get();
        addOriginHeaders(origin, res);

        HttpUtil.setContentLength(res, out.readableBytes());

        // prevent XSS warnings on IE
        // https://github.com/LearnBoost/socket.io/pull/1333
        String userAgent = channel.attr(ChannelAttributeKey.USER_AGENT).get();
        if (userAgent != null && (userAgent.contains(";MSIE") || userAgent.contains("Trident/"))) {
            res.headers().add("X-XSS-Protection", "0");
        }

        sendMessage(message, channel, out, res, promise);
    }

    private void sendMessage(HttpMessage message, Channel channel, ByteBuf out, HttpResponse res, ChannelPromise promise) {
        channel.write(res);

        if (Logger.isTraceEnabled()) {
            if (message.getSessionId() != null) {
                Logger.trace("Out message: %s - sessionId: %s", out.toString(CharsetUtil.UTF_8), message.getSessionId());
            } else {
                Logger.trace("Out message: %s", out.toString(CharsetUtil.UTF_8));
            }
        }

        if (out.isReadable()) {
            channel.write(new DefaultHttpContent(out));
        } else {
            out.release();
        }

        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendError(HttpErrorMessage errorMsg, ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final ByteBuf encBuf = encoder.allocateBuffer(ctx.alloc());
        ByteBufOutputStream out = new ByteBufOutputStream(encBuf);
        encoder.getJsonSupport().writeValue(out, errorMsg.getData());

        sendMessage(errorMsg, ctx.channel(), encBuf, "application/json", promise, HttpResponseStatus.BAD_REQUEST);
    }

    private void addOriginHeaders(String origin, HttpResponse res) {
        if (version != null) {
            res.headers().add(HttpHeaderNames.SERVER, version);
        }

        if (configuration.getAllowOrigin() != null) {
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, configuration.getAllowOrigin());
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            if (origin != null) {
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
            } else {
                res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
        }
        if (configuration.getAllowHeaders() != null) {
            res.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, configuration.getAllowHeaders());
        }
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (!(message instanceof HttpMessage)) {
            super.write(context, message, promise);
            return;
        }

        if (message instanceof OutPacketMessage) {
            OutPacketMessage m = (OutPacketMessage) message;
            if (m.getTransport() == Transport.WEBSOCKET) {
                handleWebsocket((OutPacketMessage) message, context, promise);
            }
            if (m.getTransport() == Transport.POLLING) {
                handleHTTP((OutPacketMessage) message, context, promise);
            }
        } else if (message instanceof XHROptionsMessage) {
            write((XHROptionsMessage) message, context, promise);
        } else if (message instanceof XHRPostMessage) {
            write((XHRPostMessage) message, context, promise);
        } else if (message instanceof HttpErrorMessage) {
            sendError((HttpErrorMessage) message, context, promise);
        }
    }

    private static final int FRAME_BUFFER_SIZE = 8192;

    private void handleWebsocket(final OutPacketMessage message,
                                 ChannelHandlerContext context, ChannelPromise promise) throws Exception {
        ChannelFutureList writeFutureList = new ChannelFutureList();

        while (true) {
            Queue<Packet> queue = message.getSession().getPacketsQueue(message.getTransport());
            Packet packet = queue.poll();
            if (packet == null) {
                writeFutureList.setChannelPromise(promise);
                break;
            }

            ByteBuf out = encoder.allocateBuffer(context.alloc());
            encoder.encodePacket(packet, out, context.alloc(), true);

            if (Logger.isTraceEnabled()) {
                Logger.trace("Out message: %s sessionId: %s", out.toString(CharsetUtil.UTF_8), message.getSessionId());
            }
            if (out.isReadable() && out.readableBytes() > configuration.getMaxFramePayloadLength()) {
                ByteBuf dstStart = out.readSlice(FRAME_BUFFER_SIZE);
                dstStart.retain();
                WebSocketFrame start = new TextWebSocketFrame(false, 0, dstStart);
                context.channel().write(start);
                while (out.isReadable()) {
                    int re = Math.min(out.readableBytes(), FRAME_BUFFER_SIZE);
                    ByteBuf dst = out.readSlice(re);
                    dst.retain();
                    WebSocketFrame res = new ContinuationWebSocketFrame(!out.isReadable(), 0, dst);
                    context.channel().write(res);
                }
                out.release();
                context.channel().flush();
            } else if (out.isReadable()){
                WebSocketFrame res = new TextWebSocketFrame(out);
                context.channel().writeAndFlush(res);
            } else {
                out.release();
            }

            for (ByteBuf buf : packet.getAttachments()) {
                ByteBuf outBuf = encoder.allocateBuffer(context.alloc());
                outBuf.writeByte(4);
                outBuf.writeBytes(buf);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("Out attachment: %s sessionId: %s", ByteBufUtil.hexDump(outBuf), message.getSessionId());
                }
                writeFutureList.add(context.channel().writeAndFlush(new BinaryWebSocketFrame(outBuf)));
            }
        }
    }

    private void handleHTTP(OutPacketMessage message, ChannelHandlerContext context, ChannelPromise promise) throws Exception {
        Channel channel = context.channel();
        Attribute<Boolean> attr = channel.attr(ChannelAttributeKey.WRITE_ONCE);

        Queue<Packet> queue = message.getSession().getPacketsQueue(message.getTransport());

        if (!channel.isActive() || queue.isEmpty() || !attr.compareAndSet(null, true)) {
            promise.trySuccess();
            return;
        }

        ByteBuf out = encoder.allocateBuffer(context.alloc());
        Boolean b64 = context.channel().attr(ChannelAttributeKey.B64).get();
        if (b64 != null && b64) {
            Integer jsonpIndex = context.channel().attr(ChannelAttributeKey.JSONP_INDEX).get();
            encoder.encodeJsonP(jsonpIndex, queue, out, context.alloc(), 50);
            String type = "application/javascript";
            if (jsonpIndex == null) {
                type = "text/plain";
            }
            sendMessage(message, channel, out, type, promise, HttpResponseStatus.OK);
        } else {
            encoder.encodePackets(queue, out, context.alloc(), 50);
            sendMessage(message, channel, out, "application/octet-stream", promise, HttpResponseStatus.OK);
        }
    }

    /**
     * Helper class for the handleWebsocket method, handles a list of ChannelFutures and
     * sets the status of a promise when
     * - any of the operations fail
     * - all of the operations succeed
     * The setChannelPromise method should be called after all the futures are added
     */
    private static class ChannelFutureList implements GenericFutureListener<Future<Void>> {
        private List<ChannelFuture> futureList = new ArrayList<ChannelFuture>();
        private ChannelPromise promise = null;

        private void cleanup() {
            promise = null;
            for (ChannelFuture f : futureList) {
                f.removeListener(this);
            }
        }

        private void validate() {
            boolean allSuccess = true;
            for (ChannelFuture f : futureList) {
                if (f.isDone()) {
                    if (!f.isSuccess()) {
                        promise.tryFailure(f.cause());
                        cleanup();
                        return;
                    }
                } else {
                    allSuccess = false;
                }
            }
            if (allSuccess) {
                promise.trySuccess();
                cleanup();
            }
        }

        public void add(ChannelFuture f) {
            futureList.add(f);
            f.addListener(this);
        }

        public void setChannelPromise(ChannelPromise p) {
            promise = p;
            validate();
        }

        @Override
        public void operationComplete(Future<Void> voidFuture) throws Exception {
            if (promise != null) validate();
        }
    }
}
