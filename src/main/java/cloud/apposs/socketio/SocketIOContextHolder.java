package cloud.apposs.socketio;

import cloud.apposs.logger.Logger;
import cloud.apposs.socketio.annotation.OnConnect;
import cloud.apposs.socketio.annotation.OnDisconnect;
import cloud.apposs.socketio.annotation.OnError;
import cloud.apposs.socketio.commandar.Commandar;
import cloud.apposs.socketio.commandar.CommandarInvocation;
import cloud.apposs.socketio.commandar.CommandarRouter;
import cloud.apposs.socketio.commandar.ParameterResolver;
import cloud.apposs.socketio.interceptor.CommandarInterceptorSupport;
import cloud.apposs.socketio.namespace.Namespace;
import cloud.apposs.socketio.namespace.NamespacesHub;
import cloud.apposs.socketio.protocol.EngineIOVersion;
import cloud.apposs.socketio.protocol.Packet;
import cloud.apposs.socketio.protocol.PacketType;

import java.util.List;
import java.util.Objects;

/**
 * SocketIO全局上下文
 */
public final class SocketIOContextHolder {
    private final NamespacesHub namespacesHub;

    private final CommandarRouter commandarRouter;

    private final CommandarInvocation commandarInvocation;

    private final CommandarInterceptorSupport commandarInterceptorSupport;

    public SocketIOContextHolder(NamespacesHub namespacesHub, CommandarRouter commandarRouter,
                                 CommandarInvocation commandarInvocation, CommandarInterceptorSupport commandarInterceptorSupport) {
        this.namespacesHub = namespacesHub;
        this.commandarRouter = commandarRouter;
        this.commandarInvocation = commandarInvocation;
        this.commandarInterceptorSupport = commandarInterceptorSupport;
    }

    public NamespacesHub getNamespacesHub() {
        return namespacesHub;
    }

    public CommandarInterceptorSupport getCommandarInterceptorSupport() {
        return commandarInterceptorSupport;
    }

    public CommandarRouter getCommandarRouter() {
        return commandarRouter;
    }

    public CommandarInvocation getCommandarInvocation() {
        return commandarInvocation;
    }

    public void onConnect(SocketIOSession session) throws Exception {
        // 第一次连接时需要设置命名空间并触发连接成功回调事件
        // 如果是通过 Polling Http 请求来触发的，则是连续请求多次，只有第一次请求才会触发连接成功回调事件
        if (Objects.nonNull(session.getNamespace())) {
            return;
        }
        Packet packet = new Packet(PacketType.MESSAGE, session.getVersion());
        packet.setSubType(PacketType.CONNECT);
        if (!EngineIOVersion.V4.equals(session.getVersion())) {
            session.send(packet);
        }
        Namespace namespace = namespacesHub.get(session.getPath());
        session.setNamespace(namespace);
        // 获取注解接口的 OnConnect 方法并执行连接成功回调
        List<Commandar> onConnectCommandList = commandarRouter.getCommandar(session.getPath(), OnConnect.class.getSimpleName());
        if (onConnectCommandList != null) {
            for (Commandar commandar : onConnectCommandList) {
                commandarInvocation.invoke(commandar, session);
            }
        }
    }

    public boolean onError(String path, Throwable cause) {
        // 获取注解接口的 OnError 方法并执行方法回调
        List<Commandar> onErrorCommandList = commandarRouter.getCommandar(path, OnError.class.getSimpleName());
        if (onErrorCommandList != null) {
            for (Commandar commandar : onErrorCommandList) {
                try {
                    commandarInvocation.invoke(commandar, cause);
                } catch (Throwable ex) {
                    Logger.warn(ex, "Error during cause processing by commandar %s", commandar);
                }
            }
        }
        return onErrorCommandList != null && !onErrorCommandList.isEmpty();
    }

    public void onEvent(SocketIOSession session, Packet packet, List<Object> arguments) throws Exception {
        // 获取命名空间下的事件处理器
        List<Commandar> onEventCommandList = commandarRouter.getCommandar(session.getPath(), packet.getName());
        if (onEventCommandList == null) {
            return;
        }
        for (Commandar commandar : onEventCommandList) {
            // 进行消息事件拦截器拦截，如果返回false则不再进行后续的指令匹配处理
            if (!commandarInterceptorSupport.onEvent(commandar, session, arguments)) {
                return;
            }
            Throwable cause = null;
            try {
                Object[] args = ParameterResolver.resolveParameterArguments(commandar, session, arguments);
                commandarInvocation.invoke(commandar, args);
            } catch (Throwable ex) {
                cause = ex;
                throw ex;
            } finally {
                commandarInterceptorSupport.afterCompletion(commandar, session, cause);
            }
        }
    }

    public void onDisconnect(SocketIOSession session) throws Exception {
        List<Commandar> onDisconnectCommandList = commandarRouter.getCommandar(session.getPath(), OnDisconnect.class.getSimpleName());
        if (onDisconnectCommandList != null) {
            for (Commandar commandar : onDisconnectCommandList) {
                commandarInvocation.invoke(commandar, session);
            }
        }
    }
}
