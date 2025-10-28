package cloud.apposs.socketio.protocol;

import java.util.List;

public class AckArgs {
    private List<Object> args;

    public AckArgs(List<Object> args) {
        super();
        this.args = args;
    }

    public List<Object> getArgs() {
        return args;
    }
}
