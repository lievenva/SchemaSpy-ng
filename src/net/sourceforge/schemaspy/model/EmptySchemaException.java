package net.sourceforge.schemaspy.model;

/**
 * Indicates that we attempted to evaluate an empty schema
 *
 * @author John Currier
 */
public class EmptySchemaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * When a message is sufficient
     *
     * @param msg
     */
    public EmptySchemaException() {
        super();
    }
}
