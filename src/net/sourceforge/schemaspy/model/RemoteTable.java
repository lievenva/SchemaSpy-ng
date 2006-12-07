package net.sourceforge.schemaspy.model;

import java.sql.*;

/**
 * @author John Currier
 */
public class RemoteTable extends Table {
    private final String baseSchema; // the original (non-remote) schema
    
    public RemoteTable(Database db, String baseSchema, String schema, String name) throws SQLException {
        super(db, schema, name, null, null);
        this.baseSchema = baseSchema;
    }
}
