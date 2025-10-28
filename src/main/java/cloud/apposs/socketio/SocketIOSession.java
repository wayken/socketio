package cloud.apposs.socketio;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.broadcast.BroadcastOperations;
import cloud.apposs.socketio.messages.OutPacketMessage;
import cloud.apposs.socketio.namespace.Namespace;
import cloud.apposs.socketio.protocol.*;
import cloud.apposs.socketio.scheduler.CancelableScheduler;
import cloud.apposs.socketio.scheduler.SchedulerKey;
import cloud.apposs.socketio.transport.Transport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AttributeKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SocketIO 会话，即客户端连接，负责管理客户端连接的所有信息
 */
public class SocketIOSession {
    public static final AttributeKey<SocketIOSession> SESSION = AttributeKey.<SocketIOSession>valueOf("session");

    // 会话ID，每个客户端连接都会有一个唯一的会话ID
    private final UUID sessionId;

    // 会话路径/命名空间，即客户端连接的路径，如 /socket.io/
    private final String path;

    private final AtomicBoolean disconnected = new AtomicBoolean();

    // 会话握手数据
    private final HandshakeData handshakeData;

    // 会话连接的通道，即客户端连接的通道
    private final Map<Transport, TransportState> channels = new HashMap<Transport, TransportState>(2);

    // 当前会话请求存储的一些状态值
    private final Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(1);

    // SocketIO 客户端版本
    private final EngineIOVersion version;

    private volatile Transport transport;

    // 当前会话所属的命名空间
    private Namespace namespace;

    // 会话定时器
    private final CancelableScheduler scheduler;

    // 会话集，用于管理所有会话
    private final SocketIOSessionBox sessionBox;

    private final SocketIOContextHolder contextHolder;

    private final SocketIOConfig configuration;

    private final Disconnectable disconnectable;

    private Packet lastBinaryPacket;

    public SocketIOSession(UUID sessionId, String path, HandshakeData handshakeData, Transport transport,
                           CancelableScheduler scheduler, SocketIOSessionBox sessionBox, SocketIOContextHolder contextHolder,
                           SocketIOConfig configuration, Map<String, List<String>> params, Disconnectable disconnectable) {
        this.sessionId = sessionId;
        this.path = path;
        this.handshakeData = handshakeData;
        this.transport = transport;
        this.scheduler = scheduler;
        this.sessionBox = sessionBox;
        this.contextHolder = contextHolder;
        this.configuration = configuration;
        this.disconnectable = disconnectable;
        this.channels.put(Transport.POLLING, new TransportState());
        this.channels.put(Transport.WEBSOCKET, new TransportState());
        List<String> versions = params.getOrDefault(EngineIOVersion.EIO, new ArrayList<String>());
        if (versions.isEmpty()) {
            this.version = EngineIOVersion.UNKNOWN;
        } else {
            this.version = EngineIOVersion.fromValue(versions.get(0));
        }
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getPath() {
        return path;
    }

    public EngineIOVersion getVersion() {
        return version;
    }

    public Transport getTransport() {
        return transport;
    }

    public void upgradeTransport(Transport currentTransport) {
        TransportState state = channels.get(currentTransport);
        for (Map.Entry<Transport, TransportState> entry : channels.entrySet()) {
            if (!entry.getKey().equals(currentTransport)) {
                Queue<Packet> queue = entry.getValue().getPacketsQueue();
                state.setPacketsQueue(queue);
                doSendPackets(currentTransport, state.getChannel());
                this.transport = currentTransport;
                Logger.debug("Transport upgraded to: %s for: %s", currentTransport, sessionId);
                break;
            }
        }
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
        namespace.addSession(this);
    }

    /**
     * 判断会话是否已经连接
     */
    public boolean isConnected() {
        return !disconnected.get();
    }

    /**
     * 获取握手数据
     */
    public HandshakeData getHandshakeData() {
        return handshakeData;
    }

    public Queue<Packet> getPacketsQueue(Transport transport) {
        return channels.get(transport).getPacketsQueue();
    }

    public String getOrigin() {
        return handshakeData.getHttpHeaders().get(HttpHeaderNames.ORIGIN);
    }

    public void setLastBinaryPacket(Packet lastBinaryPacket) {
        this.lastBinaryPacket = lastBinaryPacket;
    }

    public Packet getLastBinaryPacket() {
        return lastBinaryPacket;
    }

    public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }

    /**
     * 获取指定会话请求存储的状态值
     *
     * @param  key        状态键
     * @param  defaultVal 默认值
     * @return 状态值
     */
    public final Object getAttribute(Object key, Object defaultVal) {
        Object attr = attributes.get(key);
        if (attr == null && defaultVal != null) {
            attr = defaultVal;
            attributes.put(key, attr);
        }
        return attr;
    }

    /**
     * 设置指定会话请求存储的状态值
     *
     * @param  key   状态键
     * @param  value 状态值
     * @return 之前的状态值
     */
    public final Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }

    /**
     * 判断指定会话请求存储的状态值是否存在
     *
     * @param  key 状态键
     * @return 状态值是否存在
     */
    public final boolean hasAttribute(Object key) {
        return attributes.containsKey(key);
    }

    /**
     * 删除指定会话请求存储的状态值
     *
     * @param  key 状态键
     * @return 被移除的状态值
     */
    public Object removeAttribute(Object key) {
        return attributes.remove(key);
    }

    /**
     * 获取指定房间内的所有客户端连接数量
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接数量
     */
    public int getCurrentRoomSize(String room) {
        return namespace.getRoomClientsInCluster(room);
    }

    /**
     * 获取当前客户端加入的所有房间
     *
     * @return 房间名集合
     */
    public Set<String> getAllRooms() {
        return namespace.getRooms(this);
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param room 房间名
     */
    public void joinRoom(String room) {
        namespace.joinRoom(room, getSessionId());
    }

    /**
     * 当前客户端加入指定房间
     *
     * @param rooms 房间名
     */
    public void joinRooms(Set<String> rooms) {
        namespace.joinRooms(rooms, getSessionId());
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param room 房间名
     */
    public void leaveRoom(String room) {
        namespace.leaveRoom(room, getSessionId());
    }

    /**
     * 当前客户端离开指定房间
     *
     * @param rooms 房间名
     */
    public void leaveRooms(Set<String> rooms) {
        namespace.leaveRooms(rooms, getSessionId());
    }

    /**
     * 获取当前命名空间下所有默认房间内的客户端连接进行后续广播操作
     */
    public BroadcastOperations getBroadcastOperations() {
        return namespace.getBroadcastOperations();
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  room 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String room) {
        return namespace.getRoomOperations(room);
    }

    /**
     * 根据指定房间名获取房间内的所有客户端连接
     *
     * @param  rooms 房间名
     * @return 房间内的所有客户端连接
     */
    public BroadcastOperations getRoomOperations(String... rooms) {
        return namespace.getRoomOperations(rooms);
    }

    public boolean isTransportChannel(Channel channel, Transport transport) {
        TransportState state = channels.get(transport);
        if (state.getChannel() == null) {
            return false;
        }
        return state.getChannel().equals(channel);
    }

    public void bindChannel(Channel channel, Transport transport) {
        TransportState state = channels.get(transport);
        Channel prevChannel = state.update(channel);
        if (prevChannel != null) {
            sessionBox.remove(prevChannel);
        }
        sessionBox.add(channel, this);
        doSendPackets(transport, channel);
        Logger.debug("bind channel: %s to transport: %s", channel, transport);
    }

    public void releasePollingChannel(Channel channel) {
        TransportState state = channels.get(Transport.POLLING);
        if(channel.equals(state.getChannel())) {
            sessionBox.remove(channel);
            state.update(null);
        }
    }

    public void schedulePing() {
        cancelPing();
        final SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, sessionId);
        scheduler.schedule(key, () -> {
            SocketIOSession session = sessionBox.get(sessionId);
            if (session != null) {
                EngineIOVersion version = session.getVersion();
                // only send ping packet for engine.io version 4
                if (EngineIOVersion.V4.equals(version)) {
                    session.send(new Packet(PacketType.PING, version));
                }
                schedulePing();
            }
        }, configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 创建ping超时检测任务，如果超时则断开连接
     */
    public void schedulePingTimeout() {
        // 当pingTimeout小于等于0时，不进行超时检测
        if (configuration.getPingTimeout() <= 0) {
            return;
        }
        // 取消之前的ping超时检测任务
        cancelPingTimeout();
        // 创建新的ping超时检测任务
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, sessionId);
        scheduler.schedule(key, () -> {
            SocketIOSession session = sessionBox.get(sessionId);
            if (session != null) {
                session.disconnect();
                if (Logger.isDebugEnabled()) {
                    Logger.debug("%s removed due to ping timeout", sessionId);
                }
            }
        }, configuration.getPingTimeout() + configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    public void cancelPing() {
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, sessionId);
        scheduler.cancel(key);
    }

    public void cancelPingTimeout() {
        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, sessionId);
        scheduler.cancel(key);
    }

    /**
     * 发送自定义消息数据包
     *
     * @param  packet 消息数据包
     * @return 异步操作句柄
     */
    public ChannelFuture send(Packet packet) {
        return send(packet, getTransport());
    }

    /**
     * 发送自定义消息数据包
     *
     * @param  packet 消息数据包
     * @param  transport 传输方式
     * @return 异步操作句柄
     */
    public ChannelFuture send(Packet packet, Transport transport) {
        TransportState state = channels.get(transport);
        state.getPacketsQueue().add(packet);

        Channel channel = state.getChannel();
        if (channel == null
                || (transport == Transport.POLLING && channel.attr(ChannelAttributeKey.WRITE_ONCE).get() != null)) {
            return null;
        }
        return doSendPackets(transport, channel);
    }

    /**
     * 发送消息数据包
     *
     * @param name 事件名称
     * @param data 数据包，可由业务自定义JSON对象格式
     */
    public void sendEvent(String name, Object ... data) {
        Packet packet = new Packet(PacketType.MESSAGE, getVersion());
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    /**
     * 断开客户端连接
     */
    public void disconnect() {
        Packet packet = new Packet(PacketType.MESSAGE, version);
        packet.setSubType(PacketType.DISCONNECT);
        ChannelFuture future = send(packet);
        if (future != null) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        onChannelDisconnect();
    }

    public void onChannelDisconnect() {
        cancelPing();
        cancelPingTimeout();

        disconnected.set(true);
        sessionBox.removeSession(sessionId);
        // 有可能只是在握手阶段断开连接，此时namespace还未初始化
        if (namespace != null) {
            namespace.onDisconnect(this);
        }
        disconnectable.onDisconnect(this);
        try {
            contextHolder.onDisconnect(this);
        } catch (Throwable e) {
            contextHolder.onError(path, e);
        }

        for (TransportState state : channels.values()) {
            if (state.getChannel() != null) {
                sessionBox.remove(state.getChannel());
            }
        }
    }

    private ChannelFuture doSendPackets(Transport transport, Channel channel) {
        return channel.writeAndFlush(new OutPacketMessage(this, transport));
    }
}
