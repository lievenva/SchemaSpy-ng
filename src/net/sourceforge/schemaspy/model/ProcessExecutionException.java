package net.sourceforge.schemaspy.model;

/**
 * Indicates that we had an issue launching a process
 *
 * @author John Currier
 */
public class ProcessExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * When a message is sufficient
     *
     * @param msg
     */
    public ProcessExecutionException(String msg) {
        super(msg);
    }

    /**
     * When there's an associated root cause.
     * The resultant msg will be a combination of <code>msg</code> and cause's <code>msg</code>.
     *
     * @param msg
     * @param cause
     */
    public ProcessExecutionException(String msg, Throwable cause) {
        super(msg + " " + cause.getMessage(), cause);
    }

    /**
     * When there are no details other than the root cause
     *
     * @param cause
     */
    public ProcessExecutionException(Throwable cause) {
        super(cause);
    }
}
