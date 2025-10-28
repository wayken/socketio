package cloud.apposs.socketio.netty;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.*;
import cloud.apposs.socketio.ack.AckManager;
import cloud.apposs.socketio.netty.handler.*;
import cloud.apposs.socketio.protocol.JsonSupport;
import cloud.apposs.socketio.protocol.PacketDecoder;
import cloud.apposs.socketio.protocol.PacketEncoder;
import cloud.apposs.socketio.scheduler.CancelableScheduler;
import cloud.apposs.socketio.scheduler.HashedWheelTimeoutScheduler;
import cloud.apposs.socketio.transport.PollingTransport;
import cloud.apposs.socketio.transport.WebSocketTransport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.security.KeyStore;

public class SocketIOChannelInitializer extends ChannelInitializer<Channel> implements Disconnectable {
    public static final String HTTP_REQUEST_DECODER = "httpDecoder";
    public static final String HTTP_ENCODER = "httpEncoder";
    public static final String HTTP_AGGREGATOR = "httpAggregator";
    public static final String HTTP_COMPRESSION = "httpCompression";
    public static final String WEB_SOCKET_TRANSPORT_COMPRESSION = "webSocketTransportCompression";
    public static final String AUTHORIZE_HANDLER = "authorizeHandler";
    public static final String PACKET_DECODE_HANDLER = "packetDecodeHandler";
    public static final String PACKET_ENCODE_HANDLER = "packetEncodeHandler";
    public static final String SSL_HANDLER = "ssl";
    public static final String XHR_POLLING_TRANSPORT = "xhrPollingTransport";
    public static final String WEB_SOCKET_TRANSPORT = "webSocketTransport";
    public static final String WEB_SOCKET_AGGREGATOR = "webSocketAggregator";
    public static final String WRONG_URL_HANDLER = "wrongUrlBlocker";

    private AckManager ackManager;
    private AuthorizeHandler authorizeHandler;
    private PollingTransport xhrPollingTransport;
    private WebSocketTransport webSocketTransport;
    private PacketDecodeHandler packetDecodeHandler;
    private PacketEncodeHandler packetEncodeHandler;
    private WrongUrlHandler wrongUrlHandler;

    private SocketIOConfig configuration;
    private SSLContext sslContext;

    private CancelableScheduler scheduler = new HashedWheelTimeoutScheduler();

    private final SocketIOContextHolder contextHolder;

    private final SocketIOSessionBox sessionBox;

    public SocketIOChannelInitializer(SocketIOContextHolder contextHolder, SocketIOSessionBox sessionBox) {
        this.contextHolder = contextHolder;
        this.sessionBox = sessionBox;
    }

    public SocketIOChannelInitializer initialize(SocketIOConfig configuration) throws Exception {
        this.configuration = configuration;
        boolean isSsl = configuration.getKeyStore() != null;
        if (isSsl) {
            try {
                sslContext = createSSLContext(configuration);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        this.ackManager = new AckManager(scheduler);
        this.authorizeHandler = new AuthorizeHandler(configuration, scheduler, contextHolder, sessionBox, this);
        JsonSupport jsonSupport = configuration.getJsonSupport();
        PacketDecoder decoder = new PacketDecoder(jsonSupport, ackManager);
        this.xhrPollingTransport = new PollingTransport(decoder, contextHolder, sessionBox);
        this.webSocketTransport = new WebSocketTransport(isSsl, configuration, contextHolder, scheduler, sessionBox);
        PacketProcessor packetProcessor = new PacketProcessor(contextHolder, scheduler, ackManager);
        this.packetDecodeHandler = new PacketDecodeHandler(decoder, contextHolder, packetProcessor);
        PacketEncoder encoder = new PacketEncoder(configuration, jsonSupport);
        this.packetEncodeHandler = new PacketEncodeHandler(configuration, encoder);
        this.wrongUrlHandler = new WrongUrlHandler();
        return this;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        addSslHandler(pipeline);
        addSocketioHandlers(pipeline);
    }

    private SSLContext createSSLContext(SocketIOConfig configuration) throws Exception {
        TrustManager[] managers = null;
        if (configuration.getTrustStore() != null) {
            KeyStore ts = KeyStore.getInstance(configuration.getTrustStoreFormat());
            ts.load(configuration.getTrustStore(), configuration.getTrustStorePassword().toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            managers = tmf.getTrustManagers();
        }

        KeyStore ks = KeyStore.getInstance(configuration.getKeyStoreFormat());
        ks.load(configuration.getKeyStore(), configuration.getKeyStorePassword().toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, configuration.getKeyStorePassword().toCharArray());

        SSLContext serverContext = SSLContext.getInstance(configuration.getSslProtocol());
        serverContext.init(kmf.getKeyManagers(), managers, null);
        return serverContext;
    }

    private void addSslHandler(ChannelPipeline pipeline) {
        if (sslContext != null) {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            if (configuration.isNeedClientAuth() &&(configuration.getTrustStore() != null)) {
                engine.setNeedClientAuth(true);
            }
            pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
        }
    }

    protected void addSocketioHandlers(ChannelPipeline pipeline) {
        pipeline.addLast(HTTP_REQUEST_DECODER, new HttpRequestDecoder());
        pipeline.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(configuration.getMaxHttpContentLength()) {
            @Override
            protected Object newContinueResponse(HttpMessage start, int maxContentLength, ChannelPipeline pipeline) {
                return null;
            }
        });
        pipeline.addLast(HTTP_ENCODER, new HttpResponseEncoder());
        if (configuration.isHttpCompression()) {
            pipeline.addLast(HTTP_COMPRESSION, new HttpContentCompressor());
        }
        pipeline.addLast(PACKET_DECODE_HANDLER, packetDecodeHandler);
        pipeline.addLast(AUTHORIZE_HANDLER, authorizeHandler);
        pipeline.addLast(XHR_POLLING_TRANSPORT, xhrPollingTransport);
        if (configuration.isWebsocketCompression()) {
            pipeline.addLast(WEB_SOCKET_TRANSPORT_COMPRESSION, new WebSocketServerCompressionHandler());
        }
        pipeline.addLast(WEB_SOCKET_TRANSPORT, webSocketTransport);
        pipeline.addLast(PACKET_ENCODE_HANDLER, packetEncodeHandler);
        pipeline.addLast(WRONG_URL_HANDLER, wrongUrlHandler);
    }

    @Override
    public void onDisconnect(SocketIOSession session) {
        ackManager.onDisconnect(session);
        if (Logger.isDebugEnabled()) {
            Logger.debug("Client with sessionId: %s disconnected", session.getSessionId());
        }
    }
}
