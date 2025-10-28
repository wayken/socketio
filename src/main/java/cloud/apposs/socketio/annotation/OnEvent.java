package cloud.apposs.socketio.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SocketIO 事件发送注解，方法会进行相应的SocketIO指令匹配和回调，支持多个方法同时注解，可以通过{@link Order}调整执行顺序，
 * 被注解的方法参数可以由业务自定义数据类型，注意<b>自定义数据类型必须有默认构造函数</b>，代码示例如下
 * <pre>
 * {@code @OnEvent("chatevent")}
 * public void onEvent(SocketIOSession session, String data) {
 *     System.out.println("event data: " + data);
 * }
 * {@code @OnEvent("chatmulevent")}
 * public void onEvent(SocketIOSession session, ChatObject data1, String data2) {
 *     System.out.println("event data: " + data1 + "-" + data2);
 * }
 * </pre>
 * 客户端代码示例如下
 * <pre>
 * socket.emit("chatevent", "hello");
 * socket.emit("chatmulevent", {name: "hello"}, "world");
 * </prev>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnEvent {
    /**
     * 事件名称
     */
    String value();
}
