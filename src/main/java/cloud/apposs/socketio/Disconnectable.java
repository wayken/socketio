package cloud.apposs.socketio;

/**
 * 资源释放接口，用于在连接断开时统一释放资源
 */
public interface Disconnectable {
    void onDisconnect(SocketIOSession session);
}
