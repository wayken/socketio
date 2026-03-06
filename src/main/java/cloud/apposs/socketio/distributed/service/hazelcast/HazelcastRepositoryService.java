package cloud.apposs.socketio.distributed.service.hazelcast;

import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import com.hazelcast.core.HazelcastInstance;

import java.util.Map;

public class HazelcastRepositoryService implements IRepositoryService {
    private final HazelcastInstance hazelcastInstance;

    public HazelcastRepositoryService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public Map<String, Object> getMap(String name) {
        return hazelcastInstance.getMap(name);
    }

    @Override
    public boolean removeMap(String name) {
        hazelcastInstance.getMap(name).destroy();
        return true;
    }
}
