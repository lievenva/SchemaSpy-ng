package net.sourceforge.schemaspy.model;

/**
 * Base class to indicate that there was problem with how SchemaSpy was configured / used.
 *
 * @author John Currier
 */
public class InvalidConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String paramName;

    /**
     * When a message is sufficient
     *
     * @param msg
     */
    public InvalidConfigurationException(String msg) {
        super(msg);
    }

    /**
     * When there's an associated root cause.
     * The resultant msg will be a combination of <code>msg</code> and cause's <code>msg</code>.
     *
     * @param msg
     * @param cause
     */
    public InvalidConfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * When there are no details other than the root cause
     *
     * @param cause
     */
    public InvalidConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidConfigurationException setParamName(String paramName) {
        this.paramName = paramName;
        return this;
    }

    public String getParamName() {
        return paramName;
    }
}
