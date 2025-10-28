package cloud.apposs.socketio.namespace;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.broadcast.BroadcastOperations;
import cloud.apposs.socketio.broadcast.MultiRoomBroadcastOperations;
import cloud.apposs.socketio.broadcast.SingleRoomBroadcastOperations;
import io.netty.util.internal.PlatformDependent;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * SocketIO命名空间服务，
 * 建立WebSocket连接的时候，使用路径名来指定命名空间，在没有指定命名空间下，默认会使用 / 作为命名空间
 */
public final class Namespace {
    private final String name;

    private final SocketIOConfig configuration;

    // 该命名空间下的所有客户端连接
    private final Map<UUID, SocketIOSession> sessionList = PlatformDependent.newConcurrentHashMap();

    // 当前房间加入的所有客户端，方便通过房间名查找客户端连接
    private final ConcurrentMap<String, Set<UUID>> roomClients = PlatformDependent.newConcurrentHashMap();
    // 当前客户端加入的所有房间，方便通过客户端连接查找房间集合
    private final ConcurrentMap<UUID, Set<String>> clientRooms = PlatformDependent.newConcurrentHashMap();

    public Namespace(String name, SocketIOConfig configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }

    public SocketIOSession getSession(UUID uuid) {
        return sessionList.get(uuid);
    }

    public void addSession(SocketIOSession session) {
        sessionList.put(session.getSessionId(), session);
    }

    public Collection<SocketIOSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessionList.values());
    }

    /**
     * 获取当前命名空间下所有客户端连接加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getRooms() {
        return roomClients.keySet();
    }

    /**
     * 获取指定客户端连接加入的所有房间
     *
     * @param session 客户端连接
     * @return 房间名集合
     */
    public Set<String> getRooms(SocketIOSession session) {
        Set<String> rooms = clientRooms.get(session.getSessionId());
        if (rooms == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(rooms);
    }

    /**
     * 获取指定房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public Collection<SocketIOSession> getRoomClients(String room) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<SocketIOSession> result = new ArrayList<SocketIOSession>(sessionIds.size());
        for (UUID sessionId : sessionIds) {
            SocketIOSession client = sessionList.get(sessionId);
            if(client != null) {
                result.add(client);
            }
        }
        return result;
    }

    /**
     * 获取指定房间内的所有客户端连接数量
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接数量
     */
    public int getRoomClientsInCluster(String room) {
        Set<UUID> sessionIds = roomClients.get(room);
        return sessionIds == null ? 0 : sessionIds.size();
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接
     */
    public BroadcastOperations getBroadcastOperations() {
        return new SingleRoomBroadcastOperations(getName(), getName(), sessionList.values());
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String room) {
        return new SingleRoomBroadcastOperations(getName(), room, getRoomClients(room));
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String... rooms) {
        List<BroadcastOperations> roomList = new ArrayList<>();
        for (String room : rooms) {
            roomList.add(new SingleRoomBroadcastOperations(getName(), room, getRoomClients(room)));
        }
        return new MultiRoomBroadcastOperations(roomList);
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void joinRoom(String room, UUID sessionId) {
        join(room, sessionId);
    }

    /**
     * 加入指定房间
     *
     * @param rooms 房间名
     * @param sessionId 客户端连接ID
     */
    public void joinRooms(Set<String> rooms, final UUID sessionId) {
        for (String room : rooms) {
            join(room, sessionId);
        }
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void join(String room, UUID sessionId) {
        doJoinRoom(roomClients, room, sessionId);
        doJoinRoom(clientRooms, sessionId, room);
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leaveRoom(String room, UUID sessionId) {
        leave(room, sessionId);
    }

    /**
     * 离开指定房间
     *
     * @param rooms 房间名
     * @param sessionId 客户端连接ID
     */
    public void leaveRooms(Set<String> rooms, final UUID sessionId) {
        for (String room : rooms) {
            leave(room, sessionId);
        }
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leave(String room, UUID sessionId) {
        doLeaveRoom(roomClients, room, sessionId);
        doLeaveRoom(clientRooms, sessionId, room);
    }

    private <K, V> void doJoinRoom(ConcurrentMap<K, Set<V>> map, K key, V value) {
        Set<V> clients = map.get(key);
        if (clients == null) {
            clients = Collections.newSetFromMap(PlatformDependent.<V, Boolean>newConcurrentHashMap());
            Set<V> oldClients = map.putIfAbsent(key, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(value);
        // object may be changed due to other concurrent call
        if (clients != map.get(key)) {
            // re-join if queue has been replaced
            doJoinRoom(map, key, value);
        }
    }

    private <K, V> void doLeaveRoom(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
        Set<V> clients = map.get(room);
        if (clients == null) {
            return;
        }
        clients.remove(sessionId);

        if (clients.isEmpty()) {
            map.remove(room, Collections.emptySet());
        }
    }

    public void onDisconnect(SocketIOSession session) {
        sessionList.remove(session.getSessionId());
        Set<String> joinedRooms = session.getAllRooms();
        for (String joinedRoom : joinedRooms) {
            doLeaveRoom(roomClients, joinedRoom, session.getSessionId());
        }
        clientRooms.remove(session.getSessionId());
        Logger.debug("Client %s for namespace %s has been disconnected", session.getSessionId(), name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Namespace other = (Namespace) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
