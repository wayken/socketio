package cloud.apposs.socketio.commandar;

import cloud.apposs.socketio.RouterPath;

import java.lang.reflect.Method;

/**
 * SocketIO每个指令集封装
 */
public class Commandar {
    // 路由路径
    private final RouterPath path;

    // Class类
    private final Class<?> clazz;

    // 方法反射
    private final Method method;

    public Commandar(RouterPath path, Class<?> clazz, Method method) {
        this.path = path;
        this.clazz = clazz;
        this.method = method;
    }

    public RouterPath getPath() {
        return path;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(128);
        info.append("{");
        info.append("Bean: ").append(clazz.getSimpleName()).append(", ");
        info.append("Method: [").append(method.getName()).append("], ");
        info.append("Path: ").append(path.getPath()).append(", ");
        info.append("Command: ").append(path.getCommand());
        info.append("}");
        return info.toString();
    }
}
