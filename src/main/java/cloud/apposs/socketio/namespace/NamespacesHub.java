package cloud.apposs.socketio.namespace;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import io.netty.util.internal.PlatformDependent;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * Namespace命名空间管理服务，负责管理SocketIO服务所有的命名空间
 */
public final class NamespacesHub {
    private final SocketIOConfig configuration;

    private final ConcurrentMap<String, Namespace> namespaces = PlatformDependent.newConcurrentHashMap();

    public NamespacesHub(SocketIOConfig configuration) {
        this.configuration = configuration;
    }

    /**
     * 创建命名空间
     *
     * @param name 空间名，即SocketIO请求中的Path
     */
    public Namespace create(String name) {
        Namespace namespace = namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(name, configuration);
            Namespace oldNamespace = namespaces.putIfAbsent(name, namespace);
            if (oldNamespace != null) {
                namespace = oldNamespace;
            }
        }
        Logger.info("Create Namespace %s success", name);
        return namespace;
    }

    /**
     * 根据请求Path获取指定命名空间
     */
    public Namespace get(String name) {
        return namespaces.get(name);
    }

    /**
     * 根据请求Path判断是否存在指定命名空间
     */
    public boolean contains(String name) {
        return namespaces.containsKey(name);
    }

    /**
     * 获取所有命名空间
     */
    public Collection<Namespace> getAllNamespaces() {
        return namespaces.values();
    }
}
