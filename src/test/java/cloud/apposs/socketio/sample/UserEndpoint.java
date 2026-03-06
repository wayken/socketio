package cloud.apposs.socketio.sample;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;
import cloud.apposs.socketio.sample.bean.ChatObject;

import java.util.Map;
import java.util.UUID;

@ServerEndpoint("user")
public class UserEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        System.out.println("connected");
    }

    @OnConnect
    public void onConnect2(SocketIOSession session) {
        System.out.println("connected2");
    }

    @OnEvent("chatevent")
    public void onEvent01(SocketIOSession session, ChatObject data, Integer count) {
        System.out.println(count);
        System.out.println("onEvent01");
        data.setMessage("from server0");
        session.sendEvent("chatevent", data, 101);
    }

    @OnEvent("event_send02")
    public void onEvent02(SocketIOSession session) {
        System.out.println("onEvent02");
        session.sendEvent("event_send02", "from server1");
    }

    @OnEvent("chatevent2")
    public void onEvent03(SocketIOSession session, ChatObject data) {
        System.out.println("onEvent03");
        data.setMessage("from server0");
        // 分发在集群中所有连接的客户端
        Map<UUID, String> sessionList = session.getNamespace().getDistributedSessions();
        for (UUID sessionId : sessionList.keySet()) {
            if (sessionId.equals(session.getSessionId())) {
                continue;
            }
            ChatObject replyData = new ChatObject();
            replyData.setUsername(data.getUsername());
            replyData.setMessage("reply to " + sessionId);
            session.getDistributedSessionOperations(sessionId).sendEvent("chatevent", replyData, 101);
        }
        System.out.println(sessionList);
    }

    @OnError
    public void onError(Throwable ex) {
        ex.printStackTrace();
        System.out.println("exp caught");
    }

    @OnDisconnect
    public void onDisconnect(SocketIOSession session) {
        System.err.println("disconnected");
    }
}
