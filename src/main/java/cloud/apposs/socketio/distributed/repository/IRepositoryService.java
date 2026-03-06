package cloud.apposs.socketio.distributed.repository;

import java.util.Map;

/**
 * 分布式存储接口，负责处理分布式环境下的数据存储和管理功能，确保数据能够在整个集群中正确存储和访问
 */
public interface IRepositoryService {
    /**
     * 获取分布式环境中的Map对象，确保数据能够在整个集群中正确存储和访问，
     * 该方法会返回一个分布式Map对象，以便其他节点或服务实例能够访问和使用该数据
     *
     * @param name Map名称
     * @return 分布式Map对象
     */
    Map<String, Object> getMap(String name);

    /**
     * 移除分布式环境中的Map对象，确保数据能够在整个集群中正确存储和访问，
     * 该方法会从分布式环境中移除指定名称的Map对象，以便其他节点或服务实例不再访问和使用该数据
     *
     * @param name Map名称
     * @return 是否成功移除Map对象，如果Map对象不存在则返回false
     */
    boolean removeMap(String name);
}
