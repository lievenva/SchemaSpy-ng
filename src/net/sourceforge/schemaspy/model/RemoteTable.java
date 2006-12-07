package net.sourceforge.schemaspy.model;

import java.sql.*;

/**
 * @author John Currier
 */
public class RemoteTable extends Table {
    public RemoteTable(Database db, String schema, String name) throws SQLException {
        super(db, schema, name, null, null);
    }
    
    /**
     * Differs from {@link Table#addColumn(ResultSet)} in that ResultSet is
     * from {@link DatabaseMetaData#getImportedKeys(String, String, String)} instead of
     * {@link DatabaseMetaData#getColumns(String, String, String, String)}.  
     * Note that the ResultSet is relative to the table that references this column, 
     * not this table.
     * 
     * @param rs from {@link DatabaseMetaData#getImportedKeys(String, String, String)}
     *          relative to table that references this column
     * @throws SQLException
     */
    protected void addColumn(ResultSet rs) throws SQLException {
        String columnName = rs.getString("PKCOLUMN_NAME");

        if (columnName == null)
            return;

        if (getColumn(columnName) == null) {
            TableColumn column = new TableColumn(this, rs);

            columns.put(column.getName().toUpperCase(), column);
        }
    }
}
