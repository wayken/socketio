package cloud.apposs.socketio.distributed.service.memory;

import cloud.apposs.socketio.distributed.repository.IRepositoryService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版本的分布式存储服务（仅用于单实例部署的环境下或测试）
 */
public class MemoryRepositoryService implements IRepositoryService {
    private final Map<String, Map<String, Object>> mapping = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getMap(String name) {
        return mapping.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
    }

    @Override
    public boolean removeMap(String name) {
        return mapping.remove(name) != null;
    }
}
