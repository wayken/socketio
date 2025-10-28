package cloud.apposs.socketio.interceptor;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.commandar.Commandar;
import cloud.apposs.socketio.protocol.HandshakeData;

import java.util.List;

public class CommandarInterceptorAdapter implements CommandarInterceptor {
	@Override
	public boolean isAuthorized(HandshakeData data) throws Exception {
		return true;
	}

	@Override
	public boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments) {
		return true;
	}

	@Override
	public void afterCompletion(Commandar commandar, SocketIOSession session, Throwable throwable) {
	}
}
