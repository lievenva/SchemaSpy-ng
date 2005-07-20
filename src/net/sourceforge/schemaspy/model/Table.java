package net.sourceforge.schemaspy.model;

import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class Table implements Serializable {
    private final String schema;
    private final String name;
    private final Map columns = new HashMap();
    private final List primaryKeys = new ArrayList();
    private final Map foreignKeys = new HashMap();
    private final Map indexes = new HashMap();
    private final Object tablespace;
    private final Object id;
    private final Map checkConstraints = new TreeMap(new ByCheckConstraintStringsComparator());
    private final int numRows;
    private int maxChildren;
    private int maxParents;
    private boolean oracleSelectIndexesBug = false;

    public Table(Database db, String schema, String name, DatabaseMetaData meta, Properties properties) throws SQLException {
        this.schema = schema;
        this.name = name;
        initColumns(meta);
        initIndexes(db, meta, properties);
        initPrimaryKeys(meta);

        tablespace = null;
        id = null;
        numRows = fetchNumRows(db);
    }

    public void connectForeignKeys(Map tables, DatabaseMetaData meta) throws SQLException {
        ResultSet rs = null;

        try {
            rs = meta.getImportedKeys(null, schema, name);

            while (rs.next())
                addForeignKey(rs, tables, meta);
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    public ForeignKeyConstraint getForeignKey(String keyName) {
        return (ForeignKeyConstraint)foreignKeys.get(keyName.toUpperCase());
    }

    public Collection getForeignKeys() {
        return Collections.unmodifiableCollection(foreignKeys.values());
    }

    public void addCheckConstraint(String name, String text) {
        checkConstraints.put(name, text);
    }

    /**
     *
     * @param rs ResultSet from DatabaseMetaData.getImportedKeys()
     * @param tables Map
     * @param meta DatabaseMetaData
     * @throws SQLException
     */
    private void addForeignKey(ResultSet rs, Map tables, DatabaseMetaData meta) throws SQLException {
        String name = rs.getString("FK_NAME");

        if (name == null)
            return;

        ForeignKeyConstraint foreignKey = getForeignKey(name);

        if (foreignKey == null) {
            foreignKey = new ForeignKeyConstraint(this, rs);

            foreignKeys.put(foreignKey.getName().toUpperCase(), foreignKey);
        }

        TableColumn childColumn = getColumn(rs.getString("FKCOLUMN_NAME"));
        foreignKey.addChildColumn(childColumn);

        Table parentTable = (Table)tables.get(rs.getString("PKTABLE_NAME").toUpperCase());
        TableColumn parentColumn = parentTable.getColumn(rs.getString("PKCOLUMN_NAME"));
        foreignKey.addParentColumn(parentColumn);

        childColumn.addParent(parentColumn, foreignKey);
        parentColumn.addChild(childColumn, foreignKey);
    }

    private void initPrimaryKeys(DatabaseMetaData meta) throws SQLException {
        ResultSet rs = null;

        try {
            rs = meta.getPrimaryKeys(null, schema, name);

            while (rs.next())
                addPrimaryKey(rs);
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    private void addPrimaryKey(ResultSet rs) throws SQLException {
        String name = rs.getString("PK_NAME");
        if (name == null)
            return;

        TableIndex index = getIndex(name);
        if (index != null) {
            index.setIsPrimaryKey(true);
        }

        String columnName = rs.getString("COLUMN_NAME");

        primaryKeys.add(getColumn(columnName));
    }

    private void initColumns(DatabaseMetaData meta) throws SQLException {
        ResultSet rs = null;

        synchronized (Table.class) {
            try {
                rs = meta.getColumns(null, getSchema(), getName(), "%");

                while (rs.next())
                    addColumn(rs);
            } finally {
                if (rs != null)
                    rs.close();
            }
        }

        if (!isView())
            initColumnAutoUpdate(meta);
    }

    private void initColumnAutoUpdate(DatabaseMetaData meta) throws SQLException {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        // we've got to get a result set with all the columns in it
        // so we can ask if the columns are auto updated
        // Ugh!!!  Should have been in DatabaseMetaData instead!!!
        StringBuffer sql = new StringBuffer("select * from ");
        if (getSchema() != null) {
            sql.append(getSchema());
            sql.append('.');
        }
        sql.append(getName());
        sql.append(" where 0 = 1");

        try {
            stmt = meta.getConnection().prepareStatement(sql.toString());
            rs = stmt.executeQuery();

            ResultSetMetaData rsMeta = rs.getMetaData();
            for (int i = rsMeta.getColumnCount(); i > 0; --i) {
                TableColumn column = getColumn(rsMeta.getColumnName(i));
                column.setIsAutoUpdated(rsMeta.isAutoIncrement(i));
            }
        } catch (SQLException exc) {
            // don't completely choke just because we couldn't do this....
            System.err.println("Failed to determine auto increment status: " + exc);
            System.err.println("SQL: " + sql.toString());
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    private void addColumn(ResultSet rs) throws SQLException {
        String columnName = rs.getString("COLUMN_NAME");

        if (columnName == null)
            return;

        if (getColumn(columnName) == null) {
            TableColumn column = new TableColumn(this, rs);

            columns.put(column.getName().toUpperCase(), column);
        }
    }

    private void initIndexes(Database db, DatabaseMetaData meta, Properties properties) throws SQLException {
        if (!isView()) {
            ResultSet rs = null;

            try {
                try {
                    if (oracleSelectIndexesBug)
                        rs = getOracleIndexInfo(db, properties);
                    else
                        rs = meta.getIndexInfo(null, schema, name, false, true);
                } catch (SQLException exc) {
                    if (exc.getMessage().indexOf("ORA-01031") != -1) {
                        rs = getOracleIndexInfo(db, properties);
                        oracleSelectIndexesBug = true;
                    } else {
                        throw exc;
                    }
                }

                while (rs.next()) {
                    if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic)
                        continue;

                    addIndex(rs);
                }
            } finally {
                if (rs != null)
                    rs.close();
            }
        }
    }

    /**
     * Oracle's oracle.jdbc.OracleDatabaseMetaData.getIndexInfo() does an executeUpdate() and
     * therefore requires write access to the database...which is inappropriate in most cases.
     * If we run into that problem (ORA-01031: insufficient privileges) then we'll use this method.
     */
    private ResultSet getOracleIndexInfo(Database db, Properties properties) throws SQLException {
        String selectIndexesSql = properties.getProperty("selectIndexesSql");
        if (selectIndexesSql == null)
            throw new IllegalStateException("selectIndexesSql not specified in .properties file and is required for this type of database");

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.getConnection().prepareStatement(selectIndexesSql);
            stmt.setString(1, getName());
            boolean schemaRequired = selectIndexesSql.indexOf('?') != selectIndexesSql.lastIndexOf('?');
            if (schemaRequired)
                stmt.setString(2, getSchema());
            rs = stmt.executeQuery();
        } catch (SQLException sqlException) {
            System.err.println(selectIndexesSql);
            throw sqlException;
        } finally {
            if (rs != null) {
                try {
                    Method closeStmtOnClose = rs.getClass().getMethod("closeStatementOnClose", new Class[] {});
                    closeStmtOnClose.invoke(rs, null);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }

        return rs;
    }

    public TableIndex getIndex(String indexName) {
        return (TableIndex)indexes.get(indexName.toUpperCase());
    }

    private void addIndex(ResultSet rs) throws SQLException {
        String indexName = rs.getString("INDEX_NAME");

        if (indexName == null)
            return;

        TableIndex index = getIndex(indexName);

        if (index == null) {
            index = new TableIndex(rs);

            indexes.put(index.getName().toUpperCase(), index);
        }

        index.addColumn(getColumn(rs.getString("COLUMN_NAME")));
    }

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public Object getTablespace() {
        return tablespace;
    }

    public Object getId() {
        return id;
    }

    public Map getCheckConstraints() {
        return checkConstraints;
    }

    public Set getIndexes() {
        return new HashSet(indexes.values());
    }

    public List getPrimaryColumns() {
        return Collections.unmodifiableList(primaryKeys);
    }

    public TableColumn getColumn(String columnName) {
        return (TableColumn)columns.get(columnName.toUpperCase());
    }

    /**
     * Returns <code>List</code> of <code>TableColumn</code>s in ascending column number order.
     * @return
     */
    public List getColumns() {
        Set sorted = new TreeSet(new ByIndexColumnComparator());
        sorted.addAll(columns.values());
        return new ArrayList(sorted);
    }

    public int getMaxParents() {
        return maxParents;
    }

    public void addedParent() {
        maxParents++;
    }

    public void unlinkParents() {
        for (Iterator iter = columns.values().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            column.unlinkParents();
        }
    }

    public boolean isRoot() {
        for (Iterator iter = columns.values().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            if (!column.getParents().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public int getMaxChildren() {
        return maxChildren;
    }

    public void addedChild() {
        maxChildren++;
    }

    public void unlinkChildren() {
        for (Iterator iter = columns.values().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            column.unlinkChildren();
        }
    }

    public boolean isLeaf() {
        for (Iterator iter = columns.values().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            if (!column.getChildren().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ForeignKeyConstraint removeSelfReferencingConstraint() {
        ForeignKeyConstraint recursiveConstraint = getSelfReferencingConstraint();
        if (recursiveConstraint != null) {
            TableColumn childColumn = (TableColumn)recursiveConstraint.getChildColumns().get(0);
            TableColumn parentColumn = (TableColumn)recursiveConstraint.getParentColumns().get(0);
            childColumn.removeParent(parentColumn);
            parentColumn.removeChild(childColumn);
            return recursiveConstraint;
        }

        return null;
    }

    private ForeignKeyConstraint getSelfReferencingConstraint() {
        for (Iterator columnIter = getColumns().iterator(); columnIter.hasNext(); ) {
            TableColumn column = (TableColumn)columnIter.next();
            for (Iterator parentColumnIter = column.getParents().iterator(); parentColumnIter.hasNext(); ) {
                TableColumn parentColumn = (TableColumn)parentColumnIter.next();
                ForeignKeyConstraint recursiveConstraint = column.getParentConstraint(parentColumn);
                if (parentColumn.getTable().getName().equals(getName())) {
                    return column.getParentConstraint(parentColumn);
                }
            }
        }
        return null;
    }

    public int getNumChildren() {
        int numChildren = 0;

        for (Iterator iter = getColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            numChildren += column.getChildren().size();
        }

        return numChildren;
    }

    public int getNumRealChildren() {
        int numChildren = 0;

        for (Iterator iter = getColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                if (!column.getChildConstraint(childColumn).isImplied())
                    ++numChildren;
            }
        }

        return numChildren;
    }

    public int getNumParents() {
        int numParents = 0;

        for (Iterator iter = getColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            numParents += column.getParents().size();
        }

        return numParents;
    }

    public int getNumRealParents() {
        int numParents = 0;

        for (Iterator iter = getColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                if (!column.getParentConstraint(parentColumn).isImplied())
                    ++numParents;
            }
        }

        return numParents;
    }

    public ForeignKeyConstraint removeAForeignKeyConstraint() {
        final List columns = getColumns();
        int numParents = 0;
        int numChildren = 0;
        // remove either a child or parent, chosing which based on which has the
        // least number of foreign key associations (when either gets to zero then
        // the table can be pruned)
        for (Iterator iter = columns.iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            numParents += column.getParents().size();
            numChildren += column.getChildren().size();
        }

        for (Iterator iter = columns.iterator(); iter.hasNext(); ) {
            TableColumn column = (TableColumn)iter.next();
            ForeignKeyConstraint constraint;
            if (numParents <= numChildren)
                constraint = column.removeAParentFKConstraint();
            else
                constraint = column.removeAChildFKConstraint();
            if (constraint != null)
                return constraint;
        }

        return null;
    }

    public boolean isView() {
        return false;
    }

    public String getViewSql() {
        return null;
    }

    public int getNumRows() {
        return numRows;
    }

    protected int fetchNumRows(Database db) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuffer sql = new StringBuffer("select count(*) from ");
        if (getSchema() != null) {
            sql.append(getSchema());
            sql.append('.');
        }
        sql.append(getName());

        try {
            stmt = db.getConnection().prepareStatement(sql.toString());
            rs = stmt.executeQuery();

            while (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException sqlException) {
            System.err.println(sql);
            throw sqlException;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    public String toString() {
        return getName();
    }

    /**
     * isOrphan
     *
     * @param withImpliedRelationships boolean
     * @return boolean
     */
    public boolean isOrphan(boolean withImpliedRelationships) {
        if (withImpliedRelationships)
            return getMaxParents() == 0 && getMaxChildren() == 0;

        Iterator iter = getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                if (!column.getParentConstraint(parentColumn).isImplied())
                    return false;
            }
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                if (!column.getChildConstraint(childColumn).isImplied())
                    return false;
            }
        }
        return true;
    }

    private static class ByIndexColumnComparator implements Comparator, Serializable {
        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            if (column1.getId() == null || column2.getId() == null)
                return column1.getName().compareTo(column2.getName());
            if (column1.getId() instanceof Number)
                return ((Number)column1.getId()).intValue() - ((Number)column2.getId()).intValue();
            return column1.getId().toString().compareTo(column2.getId().toString());
        }
    }

    private static class ByCheckConstraintStringsComparator implements Comparator, Serializable {
        public int compare(Object object1, Object object2) {
            return object1.toString().compareTo(object2.toString());
        }
    }
}
