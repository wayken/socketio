package cloud.apposs.socketio.ack;

public abstract class AckCallback<T> {

    protected final Class<T> resultClass;
    protected final int timeout;

    /**
     * Create AckCallback
     *
     * @param resultClass - result class
     */
    public AckCallback(Class<T> resultClass) {
        this(resultClass, -1);
    }

    /**
     * Creates AckCallback with timeout
     *
     * @param resultClass - result class
     * @param timeout - callback timeout in seconds
     */
    public AckCallback(Class<T> resultClass, int timeout) {
        this.resultClass = resultClass;
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Executes only once when acknowledgement received from client.
     *
     * @param result - object sended by client
     */
    public abstract void onSuccess(T result);

    /**
     * Invoked only once then <code>timeout</code> defined
     */
    public void onTimeout() {
    }

    /**
     * Returns class of argument in {@link #onSuccess} method
     *
     * @return - result class
     */
    public Class<T> getResultClass() {
        return resultClass;
    }
}
