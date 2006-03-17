package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

public class Database {
    private final String databaseName;
    private final String schema;
    private final Map tables = new HashMap();
    private final Map views = new HashMap();
    private final DatabaseMetaData meta;
    private final Connection connection;
    private final String connectTime = new SimpleDateFormat("EEE MMM dd HH:mm z yyyy").format(new Date());

    public Database(Connection connection, DatabaseMetaData meta, String name, String schema, Properties properties, int maxThreads) throws SQLException, MissingResourceException {
        this.connection = connection;
        this.meta = meta;
        this.databaseName = name;
        this.schema = schema;
        initTables(schema, this.meta, properties, maxThreads);
        initViews(schema, this.meta, properties);
        connectTables(this.meta);
    }

    public String getName() {
        return databaseName;
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

    public DatabaseMetaData getMetaData() {
        return meta;
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

    private void initTables(String schema, final DatabaseMetaData metadata, final Properties properties, final int maxThreads) throws SQLException {
        String[] types = {"TABLE"};
        ResultSet rs = null;

        // "macro" to validate that a table is somewhat valid
        class TableValidator {
            boolean isValid(ResultSet rs) throws SQLException {
                // some databases (MySQL) return more than we wanted
                if (!rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE"))
                    return false;
                
                // Oracle 10g introduced problematic flashback tables
                // with bizarre illegal names
                String tableName = rs.getString("TABLE_NAME");
                if (tableName.indexOf("$") != -1)
                    return false;
                
                return true;
            }
        }
        TableValidator tableValidator = new TableValidator();
        
        try {
            // creating tables takes a LONG time (based on JProbe analysis).
            // it's actually DatabaseMetaData.getIndexInfo() that's the pig.

            rs = metadata.getTables(null, schema, "%", types);

            TableCreator creator;
            if (maxThreads == 1) {
                creator = new TableCreator();
            } else {
                creator = new ThreadedTableCreator(maxThreads);

                // "prime the pump" so if there's a database problem we'll probably see it now
                // and not in a secondary thread
                while (rs.next()) {
                    if (tableValidator.isValid(rs)) {
                        new TableCreator().create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), rs.getString("REMARKS"), properties);
                        break;
                    }
                }
            }

            while (rs.next()) {
                if (tableValidator.isValid(rs)) {
                    creator.create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), rs.getString("REMARKS"), properties);
                }
            }

            creator.join();
        } finally {
            if (rs != null)
                rs.close();
        }

        initCheckConstraints(properties);
        initTableIds(properties);
        initIndexIds(properties);
    }

    private void initCheckConstraints(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectCheckConstraintsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.addCheckConstraint(rs.getString("constraint_name"), rs.getString("text"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initTableIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectTableIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.setId(rs.getObject("table_id"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initIndexIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectIndexIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null) {
                        TableIndex index = table.getIndex(rs.getString("index_name"));
                        if (index != null)
                            index.setId(rs.getObject("index_id"));
                    }
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    /**
     * Create a <code>PreparedStatement</code> from the specified SQL.
     * The SQL can contain these named parameters (but <b>not</b> question marks).
     * <ol>
     * <li>:schema - replaced with the name of the schema
     * <li>:owner - alias for :schema
     * <li>:table - replaced with the name of the table
     * </ol>
     * @param sql String - SQL without question marks
     * @param tableName String - <code>null</code> if the statement doesn't deal with <code>Table</code>-level details.
     * @throws SQLException
     * @return PreparedStatement
     */
    public PreparedStatement prepareStatement(String sql, String tableName) throws SQLException {
        PreparedStatement stmt = null;
        StringBuffer sqlBuf = new StringBuffer(sql);
        List sqlParams = getSqlParams(sqlBuf, tableName); // modifies sqlBuf

        try {
            stmt = getConnection().prepareStatement(sqlBuf.toString());

            for (int i = 0; i < sqlParams.size(); ++i) {
                stmt.setString(i + 1, sqlParams.get(i).toString());
            }
        } catch (SQLException exc) {
            if (stmt != null)
                stmt.close();
            throw exc;
        }

        return stmt;
    }

    /**
     * Replaces named parameters in <code>sql</code> with question marks and
     * returns appropriate matching values in the returned <code>List</code> of <code>String</code>s.
     *
     * @param sql StringBuffer input SQL with named parameters, output named params are replaced with ?'s.
     * @param tableName String
     * @return List of Strings
     *
     * @see #prepareStatement(String, String)
     */
    private List getSqlParams(StringBuffer sql, String tableName) {
        Map namedParams = new HashMap();
        namedParams.put(":schema", getSchema());
        namedParams.put(":owner", getSchema()); // alias for :schema
        if (tableName != null) {
            namedParams.put(":table", tableName);
            namedParams.put(":view", tableName); // alias for :table
        }

        List sqlParams = new ArrayList();
        int nextColon = sql.indexOf(":");
        while (nextColon != -1) {
            String paramName = new StringTokenizer(sql.substring(nextColon), " ,").nextToken();
            String paramValue = (String)namedParams.get(paramName);
            if (paramValue == null)
                throw new IllegalArgumentException("Unexpected named parameter '" + paramName + "' found in SQL '" + sql + "'");
            sqlParams.add(paramValue);
            sql.replace(nextColon, nextColon + paramName.length(), "?"); // replace with a ?
            nextColon = sql.indexOf(":", nextColon);
        }

        return sqlParams;
    }


    private void initViews(String schema, DatabaseMetaData metadata, Properties properties) throws SQLException {
        String[] types = {"VIEW"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                if (rs.getString("TABLE_TYPE").equals("VIEW")) {  // some databases (MySQL) return more than we wanted
                    System.out.print('.');
                    Table view = new View(this, rs, metadata, properties.getProperty("selectViewSql"));
                    if (view != null)
                        views.put(view.getName().toUpperCase(), view);
                }
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

    /**
     * Single-threaded implementation of a class that creates tables
     */
    private class TableCreator {
        /**
         * Create a table and put it into <code>tables</code>
         */
        void create(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            createImpl(schemaName, tableName, remarks, properties);
        }

        protected void createImpl(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            Table table = new Table(Database.this, schemaName, tableName, remarks, meta, properties);
            tables.put(table.getName().toUpperCase(), table);
            System.out.print('.');
        }

        /**
         * Wait for all of the tables to be created.
         * By default this does nothing since this implementation isn't threaded.
         */
        void join() {
        }
    }

    /**
     * Multi-threaded implementation of a class that creates tables
     */
    private class ThreadedTableCreator extends TableCreator {
        private final Set threads = new HashSet();
        private final int maxThreads;

        ThreadedTableCreator(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        void create(final String schemaName, final String tableName, final String remarks, final Properties properties) throws SQLException {
            Thread runner = new Thread() {
                public void run() {
                    try {
                        createImpl(schemaName, tableName, remarks, properties);
                    } catch (SQLException exc) {
                        exc.printStackTrace(); // nobody above us in call stack...dump it here
                    } finally {
                        synchronized (threads) {
                            threads.remove(this);
                            threads.notify();
                        }
                    }
                }
            };

            synchronized (threads) {
                // wait for enough 'room'
                while (threads.size() >= maxThreads) {
                    try {
                        threads.wait();
                    } catch (InterruptedException interrupted) {
                    }
                }

                threads.add(runner);
            }

            runner.start();
        }

        /**
         * Wait for all of the started threads to complete
         */
        public void join() {
            while (true) {
                Thread thread;

                synchronized (threads) {
                    Iterator iter = threads.iterator();
                    if (!iter.hasNext())
                        break;

                    thread = (Thread)iter.next();
                }

                try {
                    thread.join();
                } catch (InterruptedException exc) {
                }
            }
        }
    }
}
