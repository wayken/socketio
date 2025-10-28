package cloud.apposs.socketio;

public class SocketIOException extends RuntimeException {
    private static final long serialVersionUID = -1218908839842557188L;

    public SocketIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocketIOException(String message) {
        super(message);
    }

    public SocketIOException(Throwable cause) {
        super(cause);
    }
}
