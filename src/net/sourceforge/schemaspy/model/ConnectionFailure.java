package net.sourceforge.schemaspy.model;

/**
 * Indicates that we couldn't connect to the database
 *
 * @author John Currier
 */
public class ConnectionFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * When a message is sufficient
     *
     * @param msg
     */
    public ConnectionFailure(String msg) {
        super(msg);
    }

    /**
     * When there's an associated root cause.
     * The resultant msg will be a combination of <code>msg</code> and cause's <code>msg</code>.
     *
     * @param msg
     * @param cause
     */
    public ConnectionFailure(String msg, Throwable cause) {
        super(msg + " " + cause.getMessage(), cause);
    }

    /**
     * When there are no details other than the root cause
     *
     * @param cause
     */
    public ConnectionFailure(Throwable cause) {
        super(cause);
    }
}
