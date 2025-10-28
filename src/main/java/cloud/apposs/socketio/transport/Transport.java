package cloud.apposs.socketio.transport;

public enum Transport {
    WEBSOCKET(WebSocketTransport.NAME),
    POLLING(PollingTransport.NAME);

    private final String value;

    Transport(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
