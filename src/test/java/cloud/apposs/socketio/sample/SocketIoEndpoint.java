package cloud.apposs.socketio.sample;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;
import cloud.apposs.socketio.sample.bean.AttachmentObject;
import cloud.apposs.socketio.sample.bean.ChatObject;

@ServerEndpoint({"/socket.io"})
public class SocketIoEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        System.out.println("connected " + session.getNamespace().getSessions().size());
    }

    @OnEvent("chatevent")
    public void onEvent01(SocketIOSession session, ChatObject data, Integer count) {
        System.out.println(count);
        System.out.println("onEvent01");
        data.setMessage("from server0");
        session.getDistributedRoomOperations().sendEvent("chatevent", data, 101);
    }

    @OnEvent("event_send02")
    public void onEvent02(SocketIOSession session) {
        System.out.println("onEvent02");
        session.sendEvent("event_send02", "from server1");
    }

    /**
     * 二进制数据传输，客户端代码示例：
     * <pre>
     *     var socket = io.connect('http://localhost:9092');
     *     socket.on('connect', function () {
     *         socket.emit('binary_send', new Uint8Array([1, 2, 3, 4, 5]));
     *     });
     *     socket.on('binary_send', function (data) {
     *         console.log(data);
     *     });
     * </pre>
     */
    @OnEvent("binary_send")
    public void onBinaryEvent01(SocketIOSession session, byte[] data) {
        System.out.println("onBinaryEvent01, data length: " + new String(data));
        session.sendEvent("binary_send", data);
    }

    @OnEvent("attachment_send")
    public void onAttachment(SocketIOSession session, AttachmentObject attachmentObject, String message) {
        System.out.println("onAttachment, name: " + attachmentObject.getName() + ", content: " + new String(attachmentObject.getContent()));
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
