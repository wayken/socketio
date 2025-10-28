package cloud.apposs.socketio.ack;

/**
 * Multi type ack callback used in case of multiple ack arguments
 */
public abstract class MultiTypeAckCallback extends AckCallback<MultiTypeArgs> {
    private Class<?>[] resultClasses;

    public MultiTypeAckCallback(Class<?> ... resultClasses) {
        super(MultiTypeArgs.class);
        this.resultClasses = resultClasses;
    }

    public Class<?>[] getResultClasses() {
        return resultClasses;
    }
}
