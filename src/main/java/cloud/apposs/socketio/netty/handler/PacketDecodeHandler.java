package cloud.apposs.socketio.netty.handler;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.messages.PacketsMessage;
import cloud.apposs.socketio.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

@Sharable
public class PacketDecodeHandler extends SimpleChannelInboundHandler<PacketsMessage> {
    private final PacketDecoder decoder;

    private final SocketIOContextHolder context;

    private final PacketProcessor processor;

    public PacketDecodeHandler(PacketDecoder decoder, SocketIOContextHolder context, PacketProcessor processor) {
        super();
        this.decoder = decoder;
        this.context = context;
        this.processor = processor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, PacketsMessage message) throws Exception {
        ByteBuf content = message.getContent();
        SocketIOSession session = message.getSession();

        if (Logger.isTraceEnabled()) {
            Logger.trace("In message: %s sessionId: %s", content.toString(CharsetUtil.UTF_8), session.getSessionId());
        }
        while (content.isReadable()) {
            try {
                Packet packet = decoder.decodePackets(content, session);
                if (packet.getSubType() == PacketType.CONNECT) {
                    // https://socket.io/docs/v4/socket-io-protocol/#connection-to-a-namespace
                    if (EngineIOVersion.V4.equals(session.getVersion())) {
                        Packet p = new Packet(PacketType.MESSAGE, session.getVersion());
                        p.setSubType(PacketType.CONNECT);
                        p.setNsp(packet.getNsp());
                        p.setData(new ConnPacket(session.getSessionId()));
                        session.send(p);
                    }
                }

                if (packet.hasAttachments() && !packet.isAttachmentsLoaded()) {
                    return;
                }

                processor.onPacket(session, packet, message.getTransport());
            } catch (Exception ex) {
                content.readerIndex(0);
                String data = content.toString(CharsetUtil.UTF_8);
                List<Map.Entry<String, String>> headers = session.getHandshakeData().getHttpHeaders().entries();
                Logger.error("Error during data processing. Client sessionId: %s, data: %s, headers: %s, message: %s",
                        session.getSessionId(), data, headers, ex.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String namespace = ctx.channel().attr(ChannelAttributeKey.NAMESPACE).get();
        if (!context.onError(namespace, cause)) {
            ctx.fireExceptionCaught(cause);
        }
    }
}
