package cloud.apposs.socketio.protocol;

import java.util.List;

public class Event {
    private String name;
    private List<Object> args;

    public Event() {
    }

    public Event(String name, List<Object> args) {
        super();
        this.name = name;
        this.args = args;
    }

    public List<Object> getArgs() {
        return args;
    }

    public String getName() {
        return name;
    }
}
