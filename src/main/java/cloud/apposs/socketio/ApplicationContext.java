package cloud.apposs.socketio;

import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SystemInfo;

import java.util.List;
import java.util.Properties;

public abstract class ApplicationContext {
    /** 全局配置 */
    private SocketIOConfig config;

    /** 服务启动开始时间 */
    protected long appStartTime;

    public ApplicationContext() {
        this(new SocketIOConfig());
    }

    public ApplicationContext(SocketIOConfig config) {
        this.config = config;
    }

    /**
     * 启动WebSocket服务
     */
    public ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        appStartTime = System.currentTimeMillis();
        try {
            handleRunApplication();
            Logger.info("%s SocketIO Server %s:%s Startup In %d MilliSeconds", primarySource.getSimpleName(),
                    config.getHost(), config.getPort(), (System.currentTimeMillis() - appStartTime));
        } catch (Exception cause) {
            Logger.error(cause, "%s SocketIO %s:%s Server Startup Fail", primarySource.getSimpleName(),
                    config.getHost(), config.getPort());
            shutdown();
        }
        return this;
    }

    private void handleRunApplication() throws Exception {
        // 初始化日志
        handleInitLogger(config);

        // 输出BANNER信息和系统信息
        if (config.isShowSysInfo()) {
            Banner banner = new Banner();
            banner.printBanner(System.out);
            handlePrintSysInfomation();
        }

        // 开始启动WebSocket服务
        start(config);

        // 注册服务被kill时的回调
        handleShutdownHookRegister();
    }

    /**
     * 初始化日志
     */
    private void handleInitLogger(SocketIOConfig config) {
        Properties properties = new Properties();
        properties.put(Configuration.Prefix.APPENDER, config.getLogAppender());
        properties.put(Configuration.Prefix.LEVEL, config.getLogLevel());
        properties.put(Configuration.Prefix.FILE, config.getLogPath());
        properties.put(Configuration.Prefix.FORMAT, config.getLogFormat());
        Logger.config(properties);
    }

    /**
     * 输出系统信息
     */
    private void handlePrintSysInfomation() {
        SystemInfo OS = SystemInfo.getInstance();
        Logger.info("OS Name: %s", OS.getOsName());
        Logger.info("OS Arch: %s", OS.getOsArch());
        Logger.info("Java Home: %s", OS.getJavaHome());
        Logger.info("Java Version: %s", OS.getJavaVersion());
        Logger.info("Java Vendor: %s", OS.getJavaVendor());
        List<String> jvmArguments = OS.getJvmArguments();
        for (String argument : jvmArguments) {
            Logger.info("Jvm Argument: [%s]", argument);
        }
    }

    /**
     * 注册服务被kill时的回调，只能捕获kill -15的信号量 kill -9 没办法
     */
    private void handleShutdownHookRegister() {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void shutdown() {
        handleCloseWebSocketServer();
        Logger.info("SocketIO Server Has Been Shutdown. Running %s", StrUtil.formatTimeOutput(System.currentTimeMillis() - appStartTime));
        Logger.close(true);
    }

    /**
     * 启动服务，由网络内核服务（如Netty/Undertow）根据自身服务特点启动
     */
    protected abstract void start(SocketIOConfig config) throws Exception;

    /**
     * 关闭服务，释放资源
     */
    protected abstract void handleCloseWebSocketServer();
}
