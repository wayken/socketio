package cloud.apposs.socketio.ack;

import java.util.Iterator;
import java.util.List;

public class MultiTypeArgs implements Iterable<Object> {

    private final List<Object> args;

    public MultiTypeArgs(List<Object> args) {
        super();
        this.args = args;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return args.size();
    }

    public List<Object> getArgs() {
        return args;
    }

    public <T> T first() {
        return get(0);
    }

    public <T> T second() {
        return get(1);
    }

    /**
     * "index out of bounds"-safe method for getting elements
     *
     * @param <T> type of argument
     * @param index to get
     * @return argument
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        if (size() <= index) {
            return null;
        }
        return (T) args.get(index);
    }

    @Override
    public Iterator<Object> iterator() {
        return args.iterator();
    }
}
