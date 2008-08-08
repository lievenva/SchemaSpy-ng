package net.sourceforge.schemaspy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * A table that's outside of the default schema but is referenced
 * by or references a table in the default schema.
 * 
 * @author John Currier
 */
public class RemoteTable extends Table {
    private final String baseSchema;

    public RemoteTable(Database db, String schema, String name, String baseSchema, Properties properties) throws SQLException {
        super(db, schema, name, null, properties);
        this.baseSchema = baseSchema;
    }
    
    /**
     * Connect to the PK's referenced by this table that live in the original schema
     * @param db
     * @param tables
     */
    @Override
    public void connectForeignKeys(Map<String, Table> tables, Database db, Properties properties) throws SQLException {
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getImportedKeys(null, getSchema(), getName());

            while (rs.next()) {
                String otherSchema = rs.getString("PKTABLE_SCHEM");
                if (otherSchema != null && otherSchema.equals(baseSchema))
                    addForeignKey(rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"), 
                            rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"),
                            rs.getString("PKCOLUMN_NAME"), tables, db, properties);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    @Override
    public boolean isRemote() {
        return true;
    }
}
