package net.sourceforge.schemaspy.model;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class View extends Table {
    private final String viewSql;

    public View(Database db, ResultSet rs, DatabaseMetaData meta, String selectViewSql) throws SQLException {
        super(db, rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), meta, null);
        viewSql = getViewSql(db, selectViewSql);
    }

    public boolean isView() {
        return true;
    }

    public String getViewSql() {
        return viewSql;
    }

    protected int fetchNumRecords(Database database) throws SQLException {
        return 0;
    }

    private String getViewSql(Database db, String selectViewSql) throws SQLException {
        if (selectViewSql == null)
            return null;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.getConnection().prepareStatement(selectViewSql);
            stmt.setString(1, getName());
            boolean schemaRequired = selectViewSql.indexOf('?') != selectViewSql.lastIndexOf('?');
            if (schemaRequired)
                stmt.setString(2, getSchema());
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
