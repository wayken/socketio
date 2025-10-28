package cloud.apposs.socketio.interceptor;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.commandar.Commandar;
import cloud.apposs.socketio.protocol.HandshakeData;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link CommandarInterceptor}管理器
 */
public class CommandarInterceptorSupport {
    /**
     * 创建一个拦截器列表（用于存放拦截器实例）
     */
    private List<CommandarInterceptor> interceptorList;

    public CommandarInterceptorSupport() {
        this.interceptorList = new ArrayList<CommandarInterceptor>();
    }

    public List<CommandarInterceptor> getInterceptorList() {
        return interceptorList;
    }

    public void addInterceptor(CommandarInterceptor interceptor) {
        interceptorList.add(interceptor);
    }

    public void removeInterceptor(CommandarInterceptor interceptor) {
        interceptorList.remove(interceptor);
    }

    /**
     * 该方法在建立WebSocket之前进行调用，可实现登录/权限/限流/自定义注解等拦截操作
     *
     * @param data 握手数据
     * @return true/false 通过拦截验证验证返回true
     */
    public boolean isAuthorized(HandshakeData data) throws Exception {
        for (int i = interceptorList.size() - 1; i >= 0; i--) {
            CommandarInterceptor interceptor = interceptorList.get(i);
            boolean isAuth = interceptor.isAuthorized(data);
            if (!isAuth) {
                return false;
            }
        }
        return true;
    }

    /**
     * 该方法在建立WebSocket之后客户端开始发送消息事件时调用，可实现消息XSS安全验证、日志统计等拦截操作
     *
     * @param commandar 指令体
     * @param session   会话封装，可由业务自定义返回数据或者关闭当前会话
     * @param arguments 指令参数
     * @return true/false 通过拦截验证验证返回true
     */
    public boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments) {
        for (int i = interceptorList.size() - 1; i >= 0; i--) {
            CommandarInterceptor interceptor = interceptorList.get(i);
            boolean isPass = interceptor.onEvent(commandar, session, arguments);
            if (!isPass) {
                return false;
            }
        }
        return true;
    }

    /**
     * 整个SocketIO消息事件处理完毕回调方法，无论事件消息处理有没有成功，
     * 请求结束时，无论有业务方逻辑处理有没有成功，从最后一个拦截器开始拦截
     * 一般用于性能监控中在此记录结束时间并输出消耗时间，还可以进行一些资源清理
     */
    public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
        for (int i = interceptorList.size() - 1; i >= 0; i--) {
            CommandarInterceptor interceptor = interceptorList.get(i);
            interceptor.afterCompletion(commandar, session, throwable);
        }
    }
}
