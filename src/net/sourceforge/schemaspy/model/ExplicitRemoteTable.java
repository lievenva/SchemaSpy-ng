package net.sourceforge.schemaspy.model;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * A remote table (exists in another schema) that was explicitly created via XML metadata.
 * 
 * @author John Currier
 */
public class ExplicitRemoteTable extends RemoteTable {
    private static final Pattern excludeNone = Pattern.compile("[^.]");

    public ExplicitRemoteTable(Database db, String schema, String name, String baseSchema) throws SQLException {
        super(db, schema, name, baseSchema, null, excludeNone, excludeNone);
    }
    
    @Override
    public void connectForeignKeys(Map<String, Table> tables, Database db, Properties properties, Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        // this probably won't work, so ignore any failures...but try anyways just in case
        try {
            super.connectForeignKeys(tables, db, properties, excludeIndirectColumns, excludeColumns);
        } catch (SQLException ignore) {}
    }
}