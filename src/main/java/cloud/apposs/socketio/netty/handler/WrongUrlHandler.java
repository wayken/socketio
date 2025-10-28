package cloud.apposs.socketio.netty.handler;

import cloud.apposs.logger.Logger;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class WrongUrlHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) message;
            Channel channel = context.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());

            HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            ChannelFuture f = channel.writeAndFlush(res);
            f.addListener(ChannelFutureListener.CLOSE);
            request.release();
            Logger.warn("Blocked wrong socket.io-context request! url: %s, params: %s, ip: %s",
                    queryDecoder.path(), queryDecoder.parameters(), channel.remoteAddress());
            return;
        }
        super.channelRead(context, message);
    }
}
