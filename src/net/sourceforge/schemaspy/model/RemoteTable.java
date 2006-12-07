package net.sourceforge.schemaspy.model;

import java.sql.*;

/**
 * @author John Currier
 */
public class RemoteTable extends Table {
    public RemoteTable(Database db, String schema, String name) throws SQLException {
        super(db, schema, name, null, null);
    }
}
