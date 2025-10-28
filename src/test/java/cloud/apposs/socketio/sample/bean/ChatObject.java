package cloud.apposs.socketio.sample.bean;

/**
 * 客户端发送的消息示例
 * 服务路径：user
 * 参数内容：
 * <pre>
 *     {"username":"zhangsan","message":"hello"}
 * </pre>
 */
public class ChatObject {
    private String username;
    private String message;

    public ChatObject() {
    }

    public ChatObject(String username, String message) {
        super();
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
