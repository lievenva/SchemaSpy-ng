package net.sourceforge.schemaspy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class View extends Table {
    private String viewSql;

    /**
     * @param db
     * @param rs
     * @throws java.sql.SQLException
     */
    public View(Database db, ResultSet rs) throws SQLException {
        super(db, rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), db.getOptionalString(rs, "REMARKS"), null);
    }

    /**
     * @return
     */
    @Override
    public boolean isView() {
        return true;
    }

    public void setViewSql(String viewSql) {
        this.viewSql = viewSql;
    }

    @Override
    public String getViewSql() {
        return viewSql;
    }

    @Override
    protected int fetchNumRows(Database database, Properties properties) {
        return 0;
    }
}
