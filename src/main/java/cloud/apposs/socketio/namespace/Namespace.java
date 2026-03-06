package cloud.apposs.socketio.namespace;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.SocketIOConfig;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.broadcast.*;
import cloud.apposs.socketio.distributed.IDistributedService;
import cloud.apposs.socketio.distributed.pubsub.IPubSubService;
import cloud.apposs.socketio.distributed.pubsub.PubSubType;
import cloud.apposs.socketio.distributed.pubsub.message.BulkJoinLeaveMessage;
import cloud.apposs.socketio.distributed.pubsub.message.JoinLeaveMessage;
import cloud.apposs.socketio.distributed.repository.IRepositoryService;
import cloud.apposs.socketio.protocol.Packet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SocketIO命名空间服务，
 * 建立WebSocket连接的时候，使用路径名来指定命名空间，在没有指定命名空间下，默认会使用 / 作为命名空间
 */
public final class Namespace {
    private final String name;

    private final SocketIOConfig configuration;

    private final IDistributedService distributedService;

    // 该命名空间下的所有客户端连接
    private final Map<UUID, SocketIOSession> sessionList = new ConcurrentHashMap<>();

    // 当前房间加入的所有客户端，方便通过房间名查找客户端连接
    private final ConcurrentMap<String, Set<UUID>> roomClients = new ConcurrentHashMap<>();
    // 当前客户端加入的所有房间，方便通过客户端连接查找房间集合
    private final ConcurrentMap<UUID, Set<String>> clientRooms = new ConcurrentHashMap<>();

    public Namespace(String name, SocketIOConfig configuration, IDistributedService distributedService) {
        this.name = name;
        this.configuration = configuration;
        this.distributedService = distributedService;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取当前实例客户端会话连接
     *
     * @param  sessionId 客户端连接ID
     * @return 客户端会话连接对象
     */
    public SocketIOSession getSession(UUID sessionId) {
        return sessionList.get(sessionId);
    }

    /**
     * 获取当前实例下的所有客户端会话连接
     *
     * @return 客户端会话连接集合
     */
    public Collection<SocketIOSession> getSessions() {
        return Collections.unmodifiableCollection(sessionList.values());
    }

    /**
     * 获取分布式服务中的发布订阅服务，主要用于分布式环境下的消息广播和事件通知
     *
     * @return 发布订阅服务实例
     */
    public IPubSubService getPubSubService() {
        return distributedService.getPubSubService();
    }

    /**
     * 获取分布式服务中的数据存储服务，主要用于分布式环境下的自定义数据存储和查询
     *
     * @return 数据存储服务实例
     */
    public IRepositoryService getRepositoryService() {
        return distributedService.getRepositoryService();
    }

    /**
     * 处理客户端连接事件
     *
     * @param session 客户端连接
     */
    public void onConnect(SocketIOSession session) {
        sessionList.put(session.getSessionId(), session);
    }

    /**
     * 获取分布式环境下当前命名空间下所有客户端连接的会话ID和所在实例ID映射关系
     *
     * @return 会话ID和所在实例ID映射关系
     */
    public Map<UUID, String> getDistributedSessions() {
        return distributedService.getPubSubService().getAllSessions(name);
    }

    /**
     * 获取指定房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public Collection<SocketIOSession> getRoomSessions(String room) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<SocketIOSession> result = new ArrayList<SocketIOSession>(sessionIds.size());
        for (UUID sessionId : sessionIds) {
            SocketIOSession session = sessionList.get(sessionId);
            if(session != null) {
                result.add(session);
            }
        }
        return result;
    }

    /**
     * 获取分布式环境下当前命名空间下所有客户端连接加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getAllDistributedRooms() {
        return roomClients.keySet();
    }

    /**
     * 获取分布式环境下指定客户端连接加入的所有房间
     *
     * @param  sessionId 客户端连接ID
     * @return 房间名集合
     */
    public Set<String> getSessionDistributedRooms(UUID sessionId) {
        Set<String> rooms = clientRooms.get(sessionId);
        if (rooms == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(rooms);
    }

    /**
     * 获取分布式环境下指定会话ID的客户端连接
     *
     * @param  sessionId 客户端连接ID
     * @return 客户端连接对象，如果当前实例没有该连接，则返回null
     */
    public BroadcastOperations getSessionOperations(UUID sessionId) {
        if (!distributedService.getPubSubService().isClientRegistered(name, sessionId)) {
            return null;
        }
        return new SingleSessionBroadcastOperations(name, sessionId, sessionList, distributedService);
    }

    /**
     * 获取分布式环境下指定会话ID集合的客户端连接
     *
     * @param  sessionIds 客户端连接ID集合
     * @return 客户端连接对象集合
     */
    public BroadcastOperations getMultiSessionOperations(Collection<UUID> sessionIds) {
        Set<UUID> operationsList = new HashSet<>(sessionIds.size());
        IPubSubService pubsubService = distributedService.getPubSubService();
        for (UUID sessionId : sessionIds) {
            if (!pubsubService.isClientRegistered(name, sessionId)) {
                continue;
            }
            operationsList.add(sessionId);
        }
        return new MultiSessionBroadcastOperations(name, operationsList, sessionList, distributedService);
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接
     */
    public BroadcastOperations getBroadcastOperations() {
        return new SingleRoomBroadcastOperations(getName(), getName(), sessionList.values(), distributedService);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String room) {
        return new SingleRoomBroadcastOperations(getName(), room, getRoomSessions(room), distributedService);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(Collection<String> rooms) {
        List<BroadcastOperations> roomList = new ArrayList<>();
        for (String room : rooms) {
            roomList.add(new SingleRoomBroadcastOperations(getName(), room, getRoomSessions(room), distributedService));
        }
        return new MultiRoomBroadcastOperations(roomList);
    }

    /**
     * 广播消息到指定房间的所有客户端连接
     *
     * @param room 房间名
     * @param packet 消息数据包
     */
    public void broadcast(String room, Packet packet) {
        Iterable<SocketIOSession> sessions = getRoomSessions(room);
        for (SocketIOSession session : sessions) {
            session.send(packet);
        }
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void joinRoom(String room, UUID sessionId) {
        join(room, sessionId);
        distributedService.getPubSubService().publish(PubSubType.JOIN, new JoinLeaveMessage(sessionId, room, getName()));
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
        distributedService.getPubSubService().publish(PubSubType.BULK_JOIN, new BulkJoinLeaveMessage(sessionId, rooms, getName()));
    }

    /**
     * 加入指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void join(String room, UUID sessionId) {
        handleJoinRoom(roomClients, room, sessionId);
        handleJoinRoom(clientRooms, sessionId, room);
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leaveRoom(String room, UUID sessionId) {
        leave(room, sessionId);
        distributedService.getPubSubService().publish(PubSubType.LEAVE, new JoinLeaveMessage(sessionId, room, getName()));
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
        distributedService.getPubSubService().publish(PubSubType.BULK_LEAVE, new BulkJoinLeaveMessage(sessionId, rooms, getName()));
    }

    /**
     * 离开指定房间
     *
     * @param room 房间名
     * @param sessionId 客户端连接ID
     */
    public void leave(String room, UUID sessionId) {
        handleLeaveRoom(roomClients, room, sessionId);
        handleLeaveRoom(clientRooms, sessionId, room);
    }

    private <K, V> void handleJoinRoom(ConcurrentMap<K, Set<V>> map, K key, V value) {
        Set<V> clients = map.get(key);
        if (clients == null) {
            clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Set<V> oldClients = map.putIfAbsent(key, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(value);
        // object may be changed due to other concurrent call
        if (clients != map.get(key)) {
            // re-join if queue has been replaced
            handleJoinRoom(map, key, value);
        }
    }

    private <K, V> void handleLeaveRoom(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
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
        // 1. 移除客户端会话连接
        sessionList.remove(session.getSessionId());
        // 2. 移除客户端连接与房间的关联关系
        Set<String> joinedRooms = session.getAllRooms();
        for (String joinedRoom : joinedRooms) {
            handleLeaveRoom(roomClients, joinedRoom, session.getSessionId());
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
