package cloud.apposs.socketio.namespace;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.distributed.IDistributedService;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Namespace命名空间管理服务，负责如下功能：
 * <pre>
 *  1. 管理SocketIO服务所有的命名空间
 *  2. 负责分布式环境下命名空间的消息分发和同步
 * </pre>
 */
public final class NamespacesHub {
    private final SocketIOConfig configuration;

    private final ConcurrentMap<String, Namespace> namespaces = new ConcurrentHashMap<>();

    private final IDistributedService distributedService;

    public NamespacesHub(SocketIOConfig configuration, IDistributedService distributedService) {
        this.configuration = configuration;
        this.distributedService = distributedService;
        distributedService.initialize(configuration, this);
    }

    /**
     * 创建命名空间
     *
     * @param name 空间名，即SocketIO请求中的Path
     */
    public Namespace create(String name) {
        Namespace namespace = namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(name, configuration, distributedService);
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
