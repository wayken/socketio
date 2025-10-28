package cloud.apposs.socketio.sample;

import cloud.apposs.socketio.SocketIOSession;
import cloud.apposs.socketio.annotation.*;
import cloud.apposs.socketio.sample.bean.TweetMsg;

@ServerEndpoint("twitter.ex")
public class TweetEndpoint {
    @OnConnect
    public void onConnect(SocketIOSession session) {
        System.out.println(session.getSessionId() + " connected " + session.getNamespace().getAllSessions().size());
    }

    @OnEvent("ex_report_tweets_ts")
    public void tweetMsg(SocketIOSession session, TweetMsg data) {
        session.sendEvent("ex_report_tweets_resp", "hello ok");
    }

    @OnError
    public void onError(Throwable ex) {
        ex.printStackTrace();
        System.out.println("exp caught");
    }

    @OnDisconnect
    public void onDisconnect(SocketIOSession session) {
        System.err.println(session.getSessionId() + " disconnected");
    }
}
