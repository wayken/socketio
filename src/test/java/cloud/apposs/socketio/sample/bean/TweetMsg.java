package cloud.apposs.socketio.sample.bean;

/**
 * 客户端发送的消息示例为：
 * <pre>
 *     {"content":"hello"}
 * </pre>
 */
public class TweetMsg {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
