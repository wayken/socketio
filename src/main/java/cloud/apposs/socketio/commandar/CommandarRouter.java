package cloud.apposs.socketio.commandar;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.RouterPath;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;
import cloud.apposs.socketio.protocol.JsonSupport;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指令路由映射，负责管理ServerEndpoint中的各个指向方法
 */
public final class CommandarRouter {
    private final JsonSupport jsonSupport;

    /**
     * Commandar Map，WebSocket请求与OnOpen方法的映射，
     * 数据结构为：
     * RouterPath->List<Commandar>，利用此数据结构可以实现一个SocketIO指令的多种匹配
     */
    private final Map<RouterPath, List<Commandar>> commandars = new ConcurrentHashMap<RouterPath, List<Commandar>>();

    public CommandarRouter(SocketIOConfig configuration) {
        this.jsonSupport = configuration.getJsonSupport();
    }

    /**
     * 反射添加指令映射
     */
    public boolean addCommandar(Class<?> clazz, Method method) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz");
        }
        if (method == null) {
            throw new IllegalArgumentException("method");
        }
        ServerEndpoint serverEndpoint = clazz.getAnnotation(ServerEndpoint.class);
        if (serverEndpoint == null) {
            throw new IllegalArgumentException("ServerEndpoint not found");
        }
        String command = doGetCommandar(clazz, method);
        if (command == null) {
            return false;
        }
        String[] pathList = serverEndpoint.value();
        for (int i = 0; i < pathList.length; i++) {
            String path = pathList[i];
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            RouterPath routerPath = new RouterPath(path, command);
            Commandar commandar = new Commandar(routerPath, clazz, method);
            List<Commandar> commandarList = commandars.computeIfAbsent(routerPath, k -> new LinkedList<Commandar>());
            commandarList.add(commandar);
            List<Class<?>> matchedParameterList = ParameterResolver.resolveParameterTypes(commandar);
            for (Class<?> parameter : matchedParameterList) {
                jsonSupport.addEventMapping(path, command, parameter);
            }
            doSortByOrderAnnotation(commandarList);
            Logger.info("Mapped %s on %s", commandar, doOutputMethod(commandar.getMethod()));
        }
        return true;
    }

    /**
     * 获取SocketIO指令对应的{@link Commandar}处理器，
     * 因为框架支持一个指令对应多个处理器，所以返回的是一个List
     */
    public List<Commandar> getCommandar(String path, String command) {
        RouterPath routerPath = new RouterPath(path, command);
        return commandars.get(routerPath);
    }

    // 获取指令名称，同时判断指令对应的方法参数是否合法
    private String doGetCommandar(Class<?> clazz, Method method) {
        if (method.isAnnotationPresent(OnConnect.class)) {
            boolean isParameterMatched = method.getParameterTypes().length == 1
                    && SocketIOSession.class.equals(method.getParameterTypes()[0]);
            if (!isParameterMatched) {
                throw new IllegalArgumentException("Wrong OnConnect signature: "
                        + clazz + "." + method.getName() + ", must have WSSession parameter");
            }
            return OnConnect.class.getSimpleName();
        } else if (method.isAnnotationPresent(OnDisconnect.class)) {
            boolean isParameterMatched = method.getParameterTypes().length == 1
                    && SocketIOSession.class.equals(method.getParameterTypes()[0]);
            if (!isParameterMatched) {
                throw new IllegalArgumentException("Wrong OnDisconnect signature: "
                        + clazz + "." + method.getName() + ", must have SocketIOSession parameter");
            }
            return OnDisconnect.class.getSimpleName();
        } else if (method.isAnnotationPresent(OnError.class)) {
            if (method.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("Wrong OnError signature: "
                        + clazz + "." + method.getName() + ", must have Throwable parameter");
            }
            boolean hasException = false;
            for (Class<?> eventType : method.getParameterTypes()) {
                if (Throwable.class.equals(eventType)) {
                    hasException = true;
                }
            }
            if (!hasException) {
                throw new IllegalArgumentException("Wrong OnError signature: "
                        + clazz + "." + method.getName() + ", must have Throwable parameter");
            }
            return OnError.class.getSimpleName();
        } else if (method.isAnnotationPresent(OnEvent.class)) {
            OnEvent annotation = method.getAnnotation(OnEvent.class);
            return annotation.value();
        }
        return null;
    }

    /**
     * 根据Order注解进行列表的排序
     */
    private void doSortByOrderAnnotation(List<Commandar> compareList) {
        Collections.sort(compareList, (object1, object2) -> {
            Order order1 = object1.getMethod().getAnnotation(Order.class);
            Order order2 = object2.getMethod().getAnnotation(Order.class);
            int order1Value = order1 == null ? 0 : order1.value();
            int order2Value = order2 == null ? 0 : order2.value();
            return order1Value - order2Value;
        });
    }

    /**
     * 输出Hadner Method信息，用于终端/日志输出查看框架加载了哪些Hadnler
     */
    private String doOutputMethod(Method method) {
        StringBuilder builder = new StringBuilder();
        String methodName = method.getName();
        builder.append(Modifier.toString(method.getModifiers())).append(" ");
        builder.append(method.getReturnType().getSimpleName()).append(" ");
        builder.append(methodName);
        builder.append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            builder.append(parameterType.getSimpleName());
            if (i < parameterTypes.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }
}
