package net.sourceforge.schemaspy.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.sql.PreparedStatement;

public class Database {
    private final String name;
    private final String schema;
    private final Map tables = new HashMap();
    private final Map views = new HashMap();
    private final DatabaseMetaData meta;
    private final Connection connection;
    private final String connectTime = new SimpleDateFormat("EEE MMM dd HH:mm z yyyy").format(new Date());

    public Database(Connection connection, DatabaseMetaData meta, String name, String schema, Properties properties) throws SQLException, MissingResourceException {
        this.connection = connection;
        this.meta = meta;
        this.name = name;
        this.schema = schema;
        initTables(schema, this.meta, properties);
        initViews(schema, this.meta, connection, properties);
        connectTables(this.meta);
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public Collection getTables() {
        return tables.values();
    }

    public Collection getViews() {
        return views.values();
    }

    public Connection getConnection() {
        return connection;
    }

    public String getConnectTime() {
        return connectTime;
    }

    public String getDatabaseProduct() {
        try {
            return meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion();
        } catch (SQLException exc) {
            return "";
        }
    }

    private void initTables(String schema, DatabaseMetaData metadata, Properties properties) throws SQLException {
        String[] types = {"TABLE"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                System.out.print('.');
                Table table = new Table(this, rs, metadata, properties);
                tables.put(table.getName().toUpperCase(), table);
            }
        } finally {
            if (rs != null)
                rs.close();
        }

        String selectCheckConstraintsSql = properties.getProperty("selectCheckConstraintsSql");
        if (selectCheckConstraintsSql != null) {
            PreparedStatement stmt = null;

            try {
                stmt = getConnection().prepareStatement(selectCheckConstraintsSql);
                boolean schemaRequired = selectCheckConstraintsSql.indexOf('?') != -1;
                if (schemaRequired)
                    stmt.setString(1, getSchema());
                rs = stmt.executeQuery();

                while (rs.next()) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        Table table = (Table)tables.get(tableName.toUpperCase());
                        table.addCheckConstraint(rs.getString("constname"), rs.getString("text"));
                    }
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(selectCheckConstraintsSql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initViews(String schema, DatabaseMetaData metadata, Connection connection, Properties properties) throws SQLException {
        String[] types = {"VIEW"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                System.out.print('.');
                Table view = new View(this, rs, metadata, properties.getProperty("selectViewSql"));
                views.put(view.getName().toUpperCase(), view);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    private void connectTables(DatabaseMetaData metadata) throws SQLException {
        Iterator iter = tables.values().iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            table.connectForeignKeys(tables, metadata);
        }
    }
}
