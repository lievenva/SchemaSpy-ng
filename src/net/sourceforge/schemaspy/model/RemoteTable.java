package net.sourceforge.schemaspy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import net.sourceforge.schemaspy.util.*;

/**
 * A table that's outside of the default schema but is referenced
 * by or references a table in the default schema.
 * 
 * @author John Currier
 */
public class RemoteTable extends Table {
    private final String baseSchema;

    public RemoteTable(Database db, String schema, String name, String baseSchema) throws SQLException {
        super(db, schema, name, null, null);
        this.baseSchema = baseSchema;
    }
    
    /**
     * Connect to the PK's referenced by this table that live in the original schema
     * @param db
     * @param tables
     */
    public void connectForeignKeys(CaseInsensitiveMap tables, Database db) throws SQLException {
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getImportedKeys(null, getSchema(), getName());

            while (rs.next()) {
                String otherSchema = rs.getString("PKTABLE_SCHEM");
                if (otherSchema != null && otherSchema.equals(baseSchema))
                    addForeignKey(rs, tables, db);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    public boolean isRemote() {
        return true;
    }
}
