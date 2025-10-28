package cloud.apposs.socketio.netty;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOContextHolder;
import cloud.apposs.socketio.SocketIOSessionBox;
import cloud.apposs.socketio.annotation.Order;
import cloud.apposs.socketio.annotation.ServerEndpoint;
import cloud.apposs.socketio.commandar.CommandarInvocation;
import cloud.apposs.socketio.commandar.CommandarRouter;
import cloud.apposs.socketio.interceptor.CommandarInterceptor;
import cloud.apposs.socketio.interceptor.CommandarInterceptorSupport;
import cloud.apposs.socketio.namespace.NamespacesHub;
import cloud.apposs.socketio.protocol.JsonSupport;
import cloud.apposs.socketio.protocol.JsonSupportWrapper;
import cloud.apposs.util.StrUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ApplicationHandler {
    /** IOC容器 */
    private final BeanFactory beanFactory;

    /**
     * 指令映射
     */
    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport = new CommandarInterceptorSupport();

    private final SocketIOContextHolder contextHolder;

    private final SocketIOSessionBox sessionBox = new SocketIOSessionBox();

    private final NamespacesHub namespacesHub;

    /**
     * Netty IO处理器初始化
     */
    private final SocketIOChannelInitializer pipeline;

    public ApplicationHandler(SocketIOConfig configuration) throws Exception {
        // 初始化配置项
        if (configuration.getJsonSupport() == null) {
            try {
                getClass().getClassLoader().loadClass("com.fasterxml.jackson.databind.ObjectMapper");
                try {
                    Class<?> jjs = getClass().getClassLoader().loadClass("cloud.apposs.socketio.protocol.JacksonJsonSupport");
                    JsonSupport js = (JsonSupport) jjs.getConstructor().newInstance();
                    configuration.setJsonSupport(js);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Can't find jackson lib in classpath", e);
            }
        }
        configuration.setJsonSupport(new JsonSupportWrapper(configuration.getJsonSupport()));
        beanFactory = new BeanFactory();
        commandarRouter = new CommandarRouter(configuration);
        commandarInvocation = new CommandarInvocation(beanFactory);
        // 初始化IOC容器，从WebX框架配置扫描包路径中扫描所有Bean实例
        String basePackages = configuration.getBasePackage();
        if (StrUtil.isEmpty(basePackages)) {
            throw new IllegalStateException("base package not setting");
        }
        // 判断是否是以cloud.apposs.xxx, com.example.*作为多包扫描
        String[] basePackageSplit = basePackages.split(",");
        String[] basePackageList = new String[basePackageSplit.length];
        for (int i = 0; i < basePackageSplit.length; i++) {
            basePackageList[i] = basePackageSplit[i].trim();
        }
        // 扫描包将各个IOC组件添加进容器中
        beanFactory.load(basePackageList);
        // 扫描basePackage包下所有的ServerEndpoint注解类，并注册到命名空间中和Commandar处理器中
        List<Class<?>> endpointClassList = beanFactory.getClassAnnotationList(ServerEndpoint.class);
        namespacesHub = new NamespacesHub(configuration);
        for (Class<?> endpointClass : endpointClassList) {
            ServerEndpoint serverEndpoint = endpointClass.getAnnotation(ServerEndpoint.class);
            // 初始化命名空间
            String[] pathList = serverEndpoint.value();
            for (int i = 0; i < pathList.length; i++) {
                String path = pathList[i];
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                namespacesHub.create(path);
            }
            // 初始化Commandar处理器，获取并遍历该ServerEndpoint类中所有的方法，建立RouterPath -> Commandar映射匹配
            Method[] methods = endpointClass.getDeclaredMethods();
            for (Method method : methods) {
                commandarRouter.addCommandar(endpointClass, method);
            }
        }
        // 初始化拦截器
        List<CommandarInterceptor> interceptorList = beanFactory.getBeanHierarchyList(CommandarInterceptor.class);
        // 对拦截器进行排序后添加
        doSortByOrderAnnotation(interceptorList);
        for (CommandarInterceptor interceptor : interceptorList) {
            commandarInterceptorSupport.addInterceptor(interceptor);
        }
        // 初始化IO处理器
        contextHolder = new SocketIOContextHolder(namespacesHub, commandarRouter, commandarInvocation, commandarInterceptorSupport);
        pipeline = new SocketIOChannelInitializer(contextHolder, sessionBox);
        pipeline.initialize(configuration);
    }

    public SocketIOChannelInitializer getPipeline() {
        return pipeline;
    }

    /**
     * 根据Order注解进行列表的排序
     */
    private <T> void doSortByOrderAnnotation(List<T> compareList) {
        Collections.sort(compareList, new Comparator<T>() {
            @Override
            public int compare(T object1, T object2) {
                Order order1 = object1.getClass().getAnnotation(Order.class);
                Order order2 = object2.getClass().getAnnotation(Order.class);
                int order1Value = order1 == null ? 0 : order1.value();
                int order2Value = order2 == null ? 0 : order2.value();
                return order1Value - order2Value;
            }
        });
    }
}
