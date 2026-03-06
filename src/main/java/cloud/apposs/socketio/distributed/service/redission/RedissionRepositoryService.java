package cloud.apposs.socketio.distributed.service.redission;

import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import org.redisson.api.RedissonClient;

import java.util.Map;

public class RedissionRepositoryService implements IRepositoryService {
    private final RedissonClient redissonClient;

    public RedissionRepositoryService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Map<String, Object> getMap(String name) {
        return redissonClient.getMap(name);
    }

    @Override
    public boolean removeMap(String name) {
        return redissonClient.getMap(name).delete();
    }
}
