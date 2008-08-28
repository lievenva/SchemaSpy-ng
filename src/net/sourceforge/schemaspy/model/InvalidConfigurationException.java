package net.sourceforge.schemaspy.model;

/**
 * @author John Currier
 */
public class InvalidConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidConfigurationException() {
        super();
    }
    
    public InvalidConfigurationException(String msg) {
        super(msg);
    }
    
    public InvalidConfigurationException(Throwable cause) {
        super(cause);
    }
    
    public InvalidConfigurationException(String msg, Throwable cause) {
        super(msg + " " + cause.getMessage(), cause);
    }
}
