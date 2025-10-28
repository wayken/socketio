package cloud.apposs.socketio.sample;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;
import cloud.apposs.socketio.sample.bean.ChatObject;

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
