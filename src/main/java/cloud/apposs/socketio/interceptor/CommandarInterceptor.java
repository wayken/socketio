package cloud.apposs.socketio.interceptor;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.commandar.Commandar;
import cloud.apposs.socketio.protocol.HandshakeData;

import java.util.List;

/**
 * SocketIO拦截器，对HTTP/WebSocket请求进行拦截操作，在全局只有一个实例，
 * 一般用于以下场景：
 * <pre>
 * 1. 在WebSocket建立连接前的登录验证、权限验证、限流等拦截操作中
 * 2. 在数据接收过程进行XSS/CSRF安全检测、日志统计等拦截操作中
 * </pre>
 * 业务通过实现此接口并结合{@link cloud.apposs.ioc.annotation.Component}注解注入到框架中即可实现拦截操作
 */
public interface CommandarInterceptor {
    /**
     * 该方法在建立WebSocket之前进行调用，可实现登录/权限/限流/自定义注解等拦截操作
     *
     * @param  data 握手数据
     * @return true/false 通过拦截验证验证返回true
     */
    boolean isAuthorized(HandshakeData data) throws Exception;

    /**
     * 该方法在建立WebSocket之后客户端开始发送消息事件时调用，可实现消息XSS安全验证、日志统计等拦截操作
     *
     * @param  commandar 指令体
     * @param  session   会话信息
     * @param  arguments 指令参数
     * @return true/false 通过拦截验证验证返回true
     */
    boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments);

    /**
     * 整个SocketIO消息事件处理完毕回调方法，无论事件消息处理有没有成功，
     * 拦截器采用同步方式执行，避免IO等高延迟操作，建议做性能监控中在此记录结束时间并输出消耗时间，还可以进行一些资源清理
     *
     * @param commandar 指令体
     * @param session   会话信息
     * @param throwable 如果业务调用产生了异常，则该值不为空
     */
    void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable);
}
