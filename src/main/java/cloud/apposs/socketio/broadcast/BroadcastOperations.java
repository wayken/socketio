package cloud.apposs.socketio.broadcast;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.protocol.Packet;

import java.util.Collection;

/**
 * SocketIO 广播操作接口
 */
public interface BroadcastOperations {
    /**
     * 发送自定义消息包给所有连接的客户端
     *
     * @param packet 消息包
     * @return 是否成功发送
     */
    boolean send(Packet packet);

    /**
     * 发送消息包给所有连接的客户端
     *
     * @param name 事件名称
     * @param data 事件数据
     * @return 是否成功发送
     */
    boolean sendEvent(String name, Object ... data);

    /**
     * 断开所有连接的客户端
     */
    void disconnect();
}
