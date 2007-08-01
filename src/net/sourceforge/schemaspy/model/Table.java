package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.util.*;
import net.sourceforge.schemaspy.*;

public class Table implements Comparable {
    private final String schema;
    private final String name;
    protected final Map columns = new HashMap();
    private final List primaryKeys = new ArrayList();
    private final Map foreignKeys = new HashMap();
    private final Map indexes = new HashMap();
    private       Object id;
    private final Map checkConstraints = new TreeMap(new ByCheckConstraintStringsComparator());
    private final int numRows;
    private       String comments;
    private int maxChildren;
    private int maxParents;

    public Table(Database db, String schema, String name, String comments, Properties properties) throws SQLException {
        this.schema = schema;
        this.name = name;
        setComments(comments);
        initColumns(db);
        initIndexes(db, properties);
        initPrimaryKeys(db.getMetaData());
        numRows = Config.getInstance().isNumRowsEnabled() ? fetchNumRows(db) : -1;
    }
    
    public void connectForeignKeys(Map tables, Database db) throws SQLException {
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getImportedKeys(null, getSchema(), getName());

            while (rs.next())
                addForeignKey(rs, tables, db);
        } finally {
            if (rs != null)
                rs.close();
        }
        
        // if we're one of multples then also find all of the 'remote' tables in other
        // schemas that point to our primary keys (not necessary in the normal case
        // as we infer this from the opposite direction)
        if (getSchema() != null && Config.getInstance().isOneOfMultipleSchemas()) {
            try {
                rs = db.getMetaData().getExportedKeys(null, getSchema(), getName());

                while (rs.next()) {
                    String otherSchema = rs.getString("FKTABLE_SCHEM");
                    if (!getSchema().equals(otherSchema))
                        db.addRemoteTable(otherSchema, rs.getString("FKTABLE_NAME"), getSchema());
                }
            } finally {
                if (rs != null)
                    rs.close();
            }
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
     * @param rs ResultSet from {@link DatabaseMetaData#getImportedKeys(String, String, String)}
     * @param tables Map
     * @param db 
     * @throws SQLException
     */
    protected void addForeignKey(ResultSet rs, Map tables, Database db) throws SQLException {
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
        if (parentTable == null) {
            String otherSchema = rs.getString("PKTABLE_SCHEM");
            if (otherSchema != null && !otherSchema.equals(getSchema()) && Config.getInstance().isOneOfMultipleSchemas()) {
                parentTable = db.addRemoteTable(otherSchema, rs.getString("PKTABLE_NAME"), getSchema());
            }
        }
        
        if (parentTable != null) {
            TableColumn parentColumn = parentTable.getColumn(rs.getString("PKCOLUMN_NAME"));
            if (parentColumn != null) {
                foreignKey.addParentColumn(parentColumn);
    
                childColumn.addParent(parentColumn, foreignKey);
                parentColumn.addChild(childColumn, foreignKey);
            } else {
                System.err.println("Couldn't add FK to " + this + " - Unknown Parent Column '" + rs.getString("PKCOLUMN_NAME") + "'");
            }
        } else {
            System.err.println("Couldn't add FK to " + this + " - Unknown Parent Table '" + rs.getString("PKTABLE_NAME") + "'");
        }
    }

    private void initPrimaryKeys(DatabaseMetaData meta) throws SQLException {
        ResultSet rs = null;

        try {
            rs = meta.getPrimaryKeys(null, getSchema(), getName());

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

    private void initColumns(Database db) throws SQLException {
        ResultSet rs = null;

        synchronized (Table.class) {
            try {
                rs = db.getMetaData().getColumns(null, getSchema(), getName(), "%");

                while (rs.next())
                    addColumn(rs);
            } catch (SQLException exc) {
                class ColumnInitializationFailure extends SQLException {
                    private static final long serialVersionUID = 1L;

                    public ColumnInitializationFailure(SQLException failure) {
                        super("Failed to collect column details for " + (isView() ? "view" : "table") + " '" + getName() + "' in schema '" + getSchema() + "'");
                        initCause(failure);
                    }
                }
                
                throw new ColumnInitializationFailure(exc);
            } finally {
                if (rs != null)
                    rs.close();
            }
        }

        if (!isView() && !isRemote())
            initColumnAutoUpdate(db, false);
    }

    private void initColumnAutoUpdate(Database db, boolean forceQuotes) throws SQLException {
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
        
        if (forceQuotes) {
            String quote = db.getMetaData().getIdentifierQuoteString().trim();
            sql.append(quote + getName() + quote);
        } else
            sql.append(db.getQuotedIdentifier(getName()));
        
        sql.append(" where 0 = 1");

        try {
            stmt = db.getMetaData().getConnection().prepareStatement(sql.toString());
            rs = stmt.executeQuery();

            ResultSetMetaData rsMeta = rs.getMetaData();
            for (int i = rsMeta.getColumnCount(); i > 0; --i) {
                TableColumn column = getColumn(rsMeta.getColumnName(i));
                column.setIsAutoUpdated(rsMeta.isAutoIncrement(i));
            }
        } catch (SQLException exc) {
            if (forceQuotes) {
                // don't completely choke just because we couldn't do this....
                System.err.println("Failed to determine auto increment status: " + exc);
                System.err.println("SQL: " + sql.toString());
            } else {
                initColumnAutoUpdate(db, true);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    /**
     * @param rs - from {@link DatabaseMetaData#getColumns(String, String, String, String)}
     * @throws SQLException
     */
    protected void addColumn(ResultSet rs) throws SQLException {
        String columnName = rs.getString("COLUMN_NAME");

        if (columnName == null)
            return;

        if (getColumn(columnName) == null) {
            TableColumn column = new TableColumn(this, rs);

            columns.put(column.getName().toUpperCase(), column);
        }
    }

    /**
     * Initialize index information
     *
     * @throws SQLException
     */
    private void initIndexes(Database db, Properties properties) throws SQLException {
        if (isView() || isRemote())
            return;

        // first try to initialize using the index query spec'd in the .properties
        // do this first because some DB's (e.g. Oracle) do 'bad' things with getIndexInfo()
        // (they try to do a DDL analyze command that has some bad side-effects)
        if (initIndexes(db, properties.getProperty("selectIndexesSql")))
            return;

        // couldn't, so try the old fashioned approach
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getIndexInfo(null, getSchema(), getName(), false, true);

            while (rs.next()) {
                if (rs.getShort("TYPE") != DatabaseMetaData.tableIndexStatistic)
                    addIndex(rs);
            }
        } catch (SQLException exc) {
            System.err.println("Unable to extract index info for table '" + getName() + "' in schema '" + getSchema() + "': " + exc);
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    /**
     * Try to initialize index information based on the specified SQL
     *
     * @return boolean <code>true</code> if it worked, otherwise <code>false</code>
     */
    private boolean initIndexes(Database db, String selectIndexesSql) {
        if (selectIndexesSql == null)
            return false;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.prepareStatement(selectIndexesSql, getName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (rs.getShort("TYPE") != DatabaseMetaData.tableIndexStatistic)
                    addIndex(rs);
            }
        } catch (SQLException sqlException) {
            System.err.println("Failed to query index information with SQL: " + selectIndexesSql);
            System.err.println(sqlException);
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
            if (stmt != null)  {
                try {
                    stmt.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }

        return true;
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

        index.addColumn(getColumn(rs.getString("COLUMN_NAME")), rs.getString("ASC_OR_DESC"));
    }

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public void setId(Object id) {
        this.id = id;
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
    
    /**
     * @return Comments associated with this table, or <code>null</code> if none.
     */
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = (comments == null || comments.trim().length() == 0) ? null : comments.trim();
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
            if (column.isForeignKey()) {
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
            // more drastic removal solution by Remke Rutgers:
            for (int i = 0; i < recursiveConstraint.getChildColumns().size(); i++) {
                TableColumn childColumn = (TableColumn)recursiveConstraint.getChildColumns().get(i);
                TableColumn parentColumn = (TableColumn)recursiveConstraint.getParentColumns().get(i);
                childColumn.removeParent(parentColumn);
                parentColumn.removeChild(childColumn);
            }
            return recursiveConstraint;
        }

        return null;
    }

    private ForeignKeyConstraint getSelfReferencingConstraint() {
        for (Iterator columnIter = getColumns().iterator(); columnIter.hasNext(); ) {
            TableColumn column = (TableColumn)columnIter.next();
            for (Iterator parentColumnIter = column.getParents().iterator(); parentColumnIter.hasNext(); ) {
                TableColumn parentColumn = (TableColumn)parentColumnIter.next();
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
    
    public boolean isRemote() {
        return false;
    }
    
    public String getViewSql() {
        return null;
    }

    public int getNumRows() {
        return numRows;
    }

    /**
     * fetch the number of rows contained in this table.
     *
     * returns -1 if unable to successfully fetch the row count
     *
     * @param db Database
     * @throws SQLException
     * @return int
     */
    protected int fetchNumRows(Database db) {
        try {
            // '*' should work best for the majority of cases
            return fetchNumRows(db, "count(*)", false);
        } catch (SQLException exc) {
            try {
                // except nested tables...try using '1' instead
                return fetchNumRows(db, "count(1)", false);
            } catch (SQLException try2Exception) {
                System.err.println(try2Exception);
                System.err.println("Unable to extract the number of rows for table " + getName() + ", using '-1'");
                return -1;
            }
        }
    }

    protected int fetchNumRows(Database db, String clause, boolean forceQuotes) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuffer sql = new StringBuffer("select ");
        sql.append(clause);
        sql.append(" from ");
        if (getSchema() != null) {
            sql.append(getSchema());
            sql.append('.');
        }

        if (forceQuotes) {
            String quote = db.getMetaData().getIdentifierQuoteString().trim();
            sql.append(quote + getName() + quote);
        } else
            sql.append(db.getQuotedIdentifier(getName()));

        try {
            stmt = db.getConnection().prepareStatement(sql.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException exc) {
            if (forceQuotes) // we tried with and w/o quotes...fail this attempt
                throw exc;
            
            return fetchNumRows(db, clause, true);
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

    public int compareTo(Object o) {
        Table table = (Table)o;
        return getName().compareTo(table.getName());
    }

    private static class ByIndexColumnComparator implements Comparator {
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

    private static class ByCheckConstraintStringsComparator implements Comparator {
        public int compare(Object object1, Object object2) {
            return object1.toString().compareTo(object2.toString());
        }
    }
}
