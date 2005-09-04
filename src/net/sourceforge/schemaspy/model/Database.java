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
        initViews(schema, this.meta, connection, properties);
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
                    if (rs.getString("TABLE_TYPE").equals("TABLE")) {  // some databases (MySQL) return more than we wanted
                        new TableCreator().create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), properties);
                        break;
                    }
                }
            }

            while (rs.next()) {
                if (rs.getString("TABLE_TYPE").equals("TABLE")) {  // some databases (MySQL) return more than we wanted
                    creator.create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), properties);
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
                stmt = createStatement(sql);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    table.addCheckConstraint(rs.getString("constname"), rs.getString("text"));
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
                stmt = createStatement(sql);
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
                stmt = createStatement(sql);
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

    private PreparedStatement createStatement(String sql) throws SQLException {
        PreparedStatement stmt = null;
        if (sql != null) {
            try {
                stmt = getConnection().prepareStatement(sql);
                boolean schemaRequired = sql.indexOf('?') != -1;
                if (schemaRequired)
                    stmt.setString(1, getSchema());
            } catch (SQLException exc) {
                if (stmt != null)
                    stmt.close();
                throw exc;
            }
        }

        return stmt;
    }

    private void initViews(String schema, DatabaseMetaData metadata, Connection connection, Properties properties) throws SQLException {
        String[] types = {"VIEW"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                if (rs.getString("TABLE_TYPE").equals("VIEW")) {  // some databases (MySQL) return more than we wanted
                    System.out.print('.');
                    Table view = new View(this, rs, metadata, properties.getProperty("selectViewSql"));
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
        void create(String schemaName, String tableName, Properties properties) throws SQLException {
            createImpl(schemaName, tableName, properties);
        }

        protected void createImpl(String schemaName, String tableName, Properties properties) throws SQLException {
            Table table = new Table(Database.this, schemaName, tableName, meta, properties);
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
        final Set threads = Collections.synchronizedSet(new HashSet());
        final int maxThreads;

        // local alias that's thread safe while this method is running
        final Map threadSafeTables = Collections.synchronizedMap(tables);

        ThreadedTableCreator(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        void create(final String schemaName, final String tableName, final Properties properties) throws SQLException {
            // wait for enough 'room'
            while (threads.size() >= maxThreads) {
                synchronized (threads) {
                    try {
                        threads.wait();
                    } catch (InterruptedException interrupted) {
                    }
                }
            }

            Thread runner = new Thread() {
                public void run() {
                    try {
                        createImpl(schemaName, tableName, properties);
                    } catch (SQLException exc) {
                        exc.printStackTrace();

                        // wrapping exceptions weren't introduced 'til 1.4...can't require that yet
                        throw new RuntimeException(exc.toString());
                    } finally {
                        synchronized (threads) {
                            threads.remove(this);
                            threads.notify();
                        }
                    }
                }
            };

            threads.add(runner);
            runner.start();
        }

        /**
         * Wait for all the started threads to complete
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
