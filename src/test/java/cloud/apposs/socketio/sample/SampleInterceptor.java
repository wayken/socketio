package cloud.apposs.socketio.sample;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.commandar.Commandar;
import cloud.apposs.socketio.interceptor.CommandarInterceptorAdapter;

import java.util.List;

@Component
public class SampleInterceptor extends CommandarInterceptorAdapter {
    @Override
    public boolean onEvent(Commandar commandar, SocketIOSession session, List<Object> arguments) {
        System.out.println("SampleInterceptor onEvent " + commandar.getPath());
        return true;
    }
}
