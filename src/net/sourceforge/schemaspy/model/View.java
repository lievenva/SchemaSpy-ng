package net.sourceforge.schemaspy.model;

import java.sql.*;

public class View extends Table {
    private final String viewSql;

    /**
     * @param db
     * @param rs
     * @param selectViewSql
     * @throws java.sql.SQLException
     */
    public View(Database db, ResultSet rs, String selectViewSql) throws SQLException {
        super(db, rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), db.getOptionalString(rs, "REMARKS"), null);
        viewSql = getViewSql(db, selectViewSql);
    }

    /**
     * @return
     */
    public boolean isView() {
        return true;
    }

    public String getViewSql() {
        return viewSql;
    }

    protected int fetchNumRows(Database database) {
        return 0;
    }

    private String getViewSql(Database db, String selectViewSql) throws SQLException {
        if (selectViewSql == null)
            return null;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.prepareStatement(selectViewSql, getName());
            rs = stmt.executeQuery();
            while (rs.next())
                return rs.getString("text");
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
