package net.sourceforge.schemaspy.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Treat views as tables that have no rows and are represented by the SQL that
 * defined them.
 */
public class View extends Table {
    private String viewSql;

    /**
     * @param db
     * @param schema
     * @param name
     * @param remarks
     * @param viewSql
     * @param properties
     * @param excludeIndirectColumns
     * @param excludeColumns
     * @throws SQLException
     */
    public View(Database db, String schema, String name, String remarks, String viewSql,
                Properties properties,
                Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        super(db, schema, name, remarks, properties, excludeIndirectColumns, excludeColumns);

        if (viewSql == null)
            viewSql = fetchViewSql();

        if (viewSql != null && viewSql.trim().length() > 0)
            this.viewSql = viewSql;
    }

    /**
     * @return
     */
    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public String getViewSql() {
        return viewSql;
    }

    @Override
    protected int fetchNumRows() {
        return 0;
    }

    /**
     * Extract the SQL that describes this view from the database
     *
     * @return
     * @throws SQLException
     */
    private String fetchViewSql() throws SQLException {
        String selectViewSql = properties.getProperty("selectViewSql");
        if (selectViewSql == null)
            return null;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.prepareStatement(selectViewSql, getName());
            rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    return rs.getString("view_definition");
                } catch (SQLException tryOldName) {
                    return rs.getString("text");
                }
            }
            return null;
        } catch (SQLException sqlException) {
            System.err.println(selectViewSql);
            throw sqlException;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }
}
