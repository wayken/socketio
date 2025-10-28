package cloud.apposs.socketio;

import cloud.apposs.configure.ConfigurationFactory;
import cloud.apposs.configure.ConfigurationParser;
import cloud.apposs.socketio.netty.NettyApplicationContext;
import cloud.apposs.util.GetOpt;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.ResourceUtil;
import cloud.apposs.util.StrUtil;

import java.io.InputStream;

/**
 * SocketIO服务启动程序
 */
public class SocketIOApplication {
    private final Class<?> primarySource;

    public SocketIOApplication(Class<?> primarySource) {
        this.primarySource = primarySource;
    }

    /**
     * 启动运行SocketIO服务
     */
    public static ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        return run(primarySource, generateConfiguration(primarySource, args), args);
    }

    public static ApplicationContext run(Class<?> primarySource, Object options, String... args) throws Exception {
        SocketIOConfig config = new SocketIOConfig();
        config.setOptions(options);
        return run(primarySource, SocketIOApplication.generateConfiguration(primarySource, config, args), args);
    }

    public static ApplicationContext run(Class<?> primarySource, SocketIOConfig config, String... args) throws Exception {
        return new SocketIOApplication(primarySource).run(config, args);
    }

    public ApplicationContext run(SocketIOConfig config, String... args) throws Exception {
        return new NettyApplicationContext(config).run(primarySource, args);
    }

    public static SocketIOConfig generateConfiguration(Class<?> primarySource, String... args) throws Exception {
        return generateConfiguration(primarySource, SocketIOConstants.DEFAULT_HOST, SocketIOConstants.DEFAULT_PORT, args);
    }

    public static SocketIOConfig generateConfiguration(Class<?> primarySource, int bindPort, String... args) throws Exception {
        return generateConfiguration(primarySource, SocketIOConstants.DEFAULT_HOST, bindPort, args);
    }

    public static SocketIOConfig generateConfiguration(Class<?> primarySource, SocketIOConfig config , String... args) throws Exception {
        return generateConfiguration(primarySource, config, SocketIOConstants.DEFAULT_HOST, SocketIOConstants.DEFAULT_PORT, args);
    }

    public static SocketIOConfig generateConfiguration(Class<?> primarySource,
                                                 String bindHost, int bindPort, String... args) throws Exception {
        return generateConfiguration(primarySource, new SocketIOConfig(), bindHost, bindPort, args);
    }

    public static SocketIOConfig generateConfiguration(Class<?> primarySource, SocketIOConfig config,
                                                 String bindHost, int bindPort, String... args) throws Exception {
        String configFile = SocketIOConstants.DEFAULT_CONFIG_FILE;
        // 判断是否从命令行中传递配置文件路径
        GetOpt option = new GetOpt(args);
        if (option.containsKey("c")) {
            configFile = option.get("c");
        }
        // 加载配置文件配置
        InputStream filestream = ResourceUtil.getResource(configFile, primarySource);
        ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
        cp.parse(config, filestream);

        if (config.getPort() == -1) {
            config.setHost(bindHost);
            config.setPort(bindPort);
        }
        if (StrUtil.isEmpty(config.getBasePackage())) {
            String basePackage = ReflectUtil.getPackage(primarySource);
            config.setBasePackage(basePackage);
        }

        return config;
    }

    /**
     * 关闭HTTP服务
     */
    public static void shutdown(ApplicationContext context) {
        if (context != null) {
            context.shutdown();
        }
    }
}
