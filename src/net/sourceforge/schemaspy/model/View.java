package net.sourceforge.schemaspy.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Pattern;

public class View extends Table {
    private final String viewSql;

    /**
     * @param db
     * @param rs
     * @param selectViewSql
     * @throws java.sql.SQLException
     */
    public View(Database db, ResultSet rs, String selectViewSql, Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        super(db, rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), db.getOptionalString(rs, "REMARKS"), null, excludeIndirectColumns, excludeColumns);
        viewSql = getViewSql(db, selectViewSql);
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
    protected int fetchNumRows(Database database, Properties properties) {
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
