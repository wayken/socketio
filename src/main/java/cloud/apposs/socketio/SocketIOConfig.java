package cloud.apposs.socketio;

import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.protocol.JsonSupport;

import java.io.InputStream;
import java.net.InetSocketAddress;

public class SocketIOConfig {
    /**
     * 扫描基础包，必须配置，框架会自动扫描ServerEndpoint注解类并对SocketIO.On注解方法进行映射
     */
    protected String basePackage;

    /**
     * 绑定服务器地址
     */
    private String host = "0.0.0.0";
    /**
     * 绑定服务器端口
     */
    private int port = 7018;
    /**
     * 绑定的主机列表
     */
    private InetSocketAddress bindSocketAddress;

    private int backlog = 1024;

    /**
     * 是否开启HTTP压缩
     */
    private boolean httpCompression = true;

    private boolean reuseAddress = true;

    /**
     * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出 去
     * 参考：
     * http://blog.csdn.net/huang_xw/article/details/7340241
     * http://www.open-open.com/lib/view/open1412994697952.html
     */
    private boolean tcpNoDelay = true;

    /**
     * 多少个EventLoop轮询器，主要用于处理各自网络读写数据，
     * 当服务性能不足可提高此配置提升对网络IO的并发处理，但注意EventLoop业务层必须要做到异步，不能有同步阻塞请求
     */
    private int numOfGroup = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * 工作线程池数量
     */
    private int workerCount = Runtime.getRuntime().availableProcessors() << 1;

    /**
     * 是否输出系统信息
     */
    protected boolean showSysInfo = true;

    /**
     * 自定义JSON解析器
     */
    private JsonSupport jsonSupport;

    /**
     * 第一次建立TCP连接时数据传输之间超时时间，
     * 避免'silent channel'静默攻击导致的'Too many open files'问题
     */
    private int firstDataTimeout = 5000;

    /**
     * 和客户端协商心跳超时时间，为0则表示关闭
     */
    private int pingTimeout = 60000;

    /**
     * 和客户端协商心跳检测间隔时间
     */
    private int pingInterval = 25000;

    private int upgradeTimeout = 10000;

    /**
     * Maximum http content length limit
     */
    private int maxHttpContentLength = 2 * 1024 * 1024;

    /**
     * Maximum websocket frame content length limit
     */
    private int maxFramePayloadLength = 2 * 1024 * 1024;

    /**
     * 服务是否为只读
     */
    private boolean readOnly;

    /**
     * 是否为调式模式
     */
    private boolean debug;

    /**
     * 服务编码
     */
    private String charset = "utf-8";

    /**
     * 发送数据缓存默认分配内存大小
     */
    private int bufferSize = 2 * 1024;
    /**
     * 是否直接使用堆内存
     */
    private boolean preferDirectBuffer = false;

    /**
     * 是否保持服务器端长连接，不检查网络超时
     */
    private boolean keepAlive = false;

    /**
     * 是否头部响应版本信息
     */
    private boolean addVersionHeader = true;

    /**
     * Set <b>Access-Control-Allow-Origin</b> header value for http each
     * response.
     * Default is <code>null</code>
     *
     * If value is <code>null</code> then request <b>ORIGIN</b> header value used.
     */
    private String allowOrigin;

    /**
     * Set the response Access-Control-Allow-Headers
     */
    private String allowHeaders;

    private boolean websocketCompression = true;

    /**
     * SSL证书相关配置
     */
    private String sslProtocol = "TLSv1";

    private boolean needClientAuth = false;

    private String keyStoreFormat = "JKS";
    private InputStream keyStore;
    private String keyStorePassword;

    private String trustStoreFormat = "JKS";
    private InputStream trustStore;
    private String trustStorePassword;

    /**
     * 日志输出终端
     */
    private String logAppender = Appender.CONSOLE;

    /**
     * 日志输出级别，
     * FATAL（致命）、
     * ERROR（错误）、
     * WARN（警告）、
     * INFO（信息）、
     * DEBUG（调试）、
     * OFF（关闭），
     * 默认为INFO
     */
    private String logLevel = "INFO";

    /**
     * 日志的存储路径
     */
    private String logPath = "log";

    /**
     * 日志输出模板
     */
    private String logFormat = Logger.DEFAULT_LOG_FORMAT;

    /**
     * 业务自定义配置
     */
    protected Object options;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isHttpCompression() {
        return httpCompression;
    }

    public void setHttpCompression(boolean httpCompression) {
        this.httpCompression = httpCompression;
    }

    public InetSocketAddress getBindSocketAddress() {
        return bindSocketAddress;
    }

    public void setBindSocketAddress(InetSocketAddress bindSocketAddress) {
        this.bindSocketAddress = bindSocketAddress;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getNumOfGroup() {
        return numOfGroup;
    }

    public void setNumOfGroup(int numOfGroup) {
        this.numOfGroup = numOfGroup;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public boolean isShowSysInfo() {
        return showSysInfo;
    }

    public void setShowSysInfo(boolean showSysInfo) {
        this.showSysInfo = showSysInfo;
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public int getFirstDataTimeout() {
        return firstDataTimeout;
    }

    public void setFirstDataTimeout(int firstDataTimeout) {
        this.firstDataTimeout = firstDataTimeout;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getUpgradeTimeout() {
        return upgradeTimeout;
    }

    public void setUpgradeTimeout(int upgradeTimeout) {
        this.upgradeTimeout = upgradeTimeout;
    }

    public int getMaxHttpContentLength() {
        return maxHttpContentLength;
    }

    public void setMaxHttpContentLength(int maxHttpContentLength) {
        this.maxHttpContentLength = maxHttpContentLength;
    }

    public int getMaxFramePayloadLength() {
        return maxFramePayloadLength;
    }

    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isPreferDirectBuffer() {
        return preferDirectBuffer;
    }

    public void setPreferDirectBuffer(boolean preferDirectBuffer) {
        this.preferDirectBuffer = preferDirectBuffer;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isAddVersionHeader() {
        return addVersionHeader;
    }

    public void setAddVersionHeader(boolean addVersionHeader) {
        this.addVersionHeader = addVersionHeader;
    }

    public String getAllowOrigin() {
        return allowOrigin;
    }

    public void setAllowOrigin(String allowOrigin) {
        this.allowOrigin = allowOrigin;
    }

    public String getAllowHeaders() {
        return allowHeaders;
    }

    public void setAllowHeaders(String allowHeaders) {
        this.allowHeaders = allowHeaders;
    }

    public boolean isWebsocketCompression() {
        return websocketCompression;
    }

    public void setWebsocketCompression(boolean websocketCompression) {
        this.websocketCompression = websocketCompression;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public String getKeyStoreFormat() {
        return keyStoreFormat;
    }

    public void setKeyStoreFormat(String keyStoreFormat) {
        this.keyStoreFormat = keyStoreFormat;
    }

    public InputStream getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(InputStream keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStoreFormat() {
        return trustStoreFormat;
    }

    public void setTrustStoreFormat(String trustStoreFormat) {
        this.trustStoreFormat = trustStoreFormat;
    }

    public InputStream getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(InputStream trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(String logAppender) {
        this.logAppender = logAppender;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }
}
