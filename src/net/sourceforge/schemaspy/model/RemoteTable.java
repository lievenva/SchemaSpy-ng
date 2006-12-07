package net.sourceforge.schemaspy.model;

import java.sql.*;

/**
 * A table that's outside of the default schema but is referenced
 * by or references a table in the default schema.
 * 
 * @author John Currier
 */
public class RemoteTable extends Table {
    public RemoteTable(Database db, String schema, String name) throws SQLException {
        super(db, schema, name, null, null);
    }
    
    public boolean isRemote() {
        return true;
    }
}
