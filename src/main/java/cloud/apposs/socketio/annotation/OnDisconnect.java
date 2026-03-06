package cloud.apposs.socketio.annotation;

import cloud.apposs.socketio.SocketIOSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SocketIO 连接断开事件注解，方法会进行相应的SocketIO指令匹配和回调，支持多个方法同时注解，可以通过{@link Order}调整执行顺序，
 * 被注解的方法参数必须为{@link SocketIOSession}类型，示例如下
 * <pre>
 * {@code @OnDisconnect}
 * public void onDisconnect(SocketIOSession session) {
 *     System.err.println("disconnected");
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDisconnect {
}
