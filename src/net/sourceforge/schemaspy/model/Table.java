package net.sourceforge.schemaspy.model;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.xml.ForeignKeyMeta;
import net.sourceforge.schemaspy.model.xml.TableColumnMeta;
import net.sourceforge.schemaspy.model.xml.TableMeta;
import net.sourceforge.schemaspy.util.CaseInsensitiveMap;

public class Table implements Comparable<Table> {
    private final String schema;
    private final String name;
    protected final CaseInsensitiveMap<TableColumn> columns = new CaseInsensitiveMap<TableColumn>();
    private final List<TableColumn> primaryKeys = new ArrayList<TableColumn>();
    private final CaseInsensitiveMap<ForeignKeyConstraint> foreignKeys = new CaseInsensitiveMap<ForeignKeyConstraint>();
    private final CaseInsensitiveMap<TableIndex> indexes = new CaseInsensitiveMap<TableIndex>();
    private       Object id;
    private final Map<String, String> checkConstraints = new TreeMap<String, String>(new ByCheckConstraintStringsComparator());
    private final int numRows;
    private       String comments;
    private int maxChildren;
    private int maxParents;

    public Table(Database db, String schema, String name, String comments, Properties properties, Pattern excludeColumns) throws SQLException {
        this.schema = schema;
        this.name = name;
        setComments(comments);
        initColumns(db, excludeColumns);
        initIndexes(db, properties);
        initPrimaryKeys(db.getMetaData(), properties);
        numRows = Config.getInstance().isNumRowsEnabled() ? fetchNumRows(db, properties) : -1;
    }
    
    public void connectForeignKeys(Map<String, Table> tables, Database db, Properties properties, Pattern excludeColumns) throws SQLException {
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getImportedKeys(null, getSchema(), getName());

            while (rs.next()) {
                addForeignKey(rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME"), tables, db, properties, excludeColumns);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        
        // if we're one of multiples then also find all of the 'remote' tables in other
        // schemas that point to our primary keys (not necessary in the normal case
        // as we infer this from the opposite direction)
        if (getSchema() != null && Config.getInstance().isOneOfMultipleSchemas()) {
            try {
                rs = db.getMetaData().getExportedKeys(null, getSchema(), getName());

                while (rs.next()) {
                    String otherSchema = rs.getString("FKTABLE_SCHEM");
                    if (!getSchema().equals(otherSchema))
                        db.addRemoteTable(otherSchema, rs.getString("FKTABLE_NAME"), getSchema(), properties, excludeColumns);
                }
            } finally {
                if (rs != null)
                    rs.close();
            }
        }
    }

    public ForeignKeyConstraint getForeignKey(String keyName) {
        return foreignKeys.get(keyName);
    }

    public Collection<ForeignKeyConstraint> getForeignKeys() {
        return Collections.unmodifiableCollection(foreignKeys.values());
    }

    public void addCheckConstraint(String constraintName, String text) {
        checkConstraints.put(constraintName, text);
    }

    /**
     * @param rs ResultSet from {@link DatabaseMetaData#getImportedKeys(String, String, String)}
     * rs.getString("FK_NAME");
     * rs.getString("FKCOLUMN_NAME");
     * rs.getString("PKTABLE_SCHEM");
     * rs.getString("PKTABLE_NAME");
     * rs.getString("PKCOLUMN_NAME");
     * @param tables Map
     * @param db 
     * @throws SQLException
     */
    protected void addForeignKey(String fkName, String fkColName, String pkTableSchema, String pkTableName, String pkColName, Map<String, Table> tables, Database db, Properties properties, Pattern excludeColumns) throws SQLException {
        if (fkName == null)
            return;

        ForeignKeyConstraint foreignKey = getForeignKey(fkName);

        if (foreignKey == null) {
            foreignKey = new ForeignKeyConstraint(this, fkName);

            foreignKeys.put(foreignKey.getName(), foreignKey);
        }

        TableColumn childColumn = getColumn(fkColName);
        foreignKey.addChildColumn(childColumn);

        Table parentTable = tables.get(pkTableName);
        if (parentTable == null) {
            String otherSchema = pkTableSchema;
            if (otherSchema != null && !otherSchema.equals(getSchema()) && Config.getInstance().isOneOfMultipleSchemas()) {
                parentTable = db.addRemoteTable(otherSchema, pkTableName, getSchema(), properties, excludeColumns);
            }
        }
        
        if (parentTable != null) {
            TableColumn parentColumn = parentTable.getColumn(pkColName);
            if (parentColumn != null) {
                foreignKey.addParentColumn(parentColumn);
    
                childColumn.addParent(parentColumn, foreignKey);
                parentColumn.addChild(childColumn, foreignKey);
            } else {
                System.err.println("Couldn't add FK to " + this + " - Unknown Parent Column '" + pkColName + "'");
            }
        } else {
            System.err.println("Couldn't add FK to " + this + " - Unknown Parent Table '" + pkTableName + "'");
        }
    }

    private void initPrimaryKeys(DatabaseMetaData meta, Properties properties) throws SQLException {
        if (properties == null)
            return;
        
        ResultSet rs = null;

        try {
            rs = meta.getPrimaryKeys(null, getSchema(), getName());

            while (rs.next())
                setPrimaryColumn(rs);
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    private void setPrimaryColumn(ResultSet rs) throws SQLException {
        String pkName = rs.getString("PK_NAME");
        if (pkName == null)
            return;

        TableIndex index = getIndex(pkName);
        if (index != null) {
            index.setIsPrimaryKey(true);
        }

        String columnName = rs.getString("COLUMN_NAME");

        setPrimaryColumn(getColumn(columnName));
    }
    
    void setPrimaryColumn(TableColumn primaryColumn) {
        primaryKeys.add(primaryColumn);
    }

    private void initColumns(Database db, Pattern excludeColumns) throws SQLException {
        ResultSet rs = null;

        synchronized (Table.class) {
            try {
                rs = db.getMetaData().getColumns(null, getSchema(), getName(), "%");

                while (rs.next())
                    addColumn(rs, excludeColumns);
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
    protected void addColumn(ResultSet rs, Pattern excludeColumns) throws SQLException {
        String columnName = rs.getString("COLUMN_NAME");

        if (columnName == null)
            return;

        if (getColumn(columnName) == null) {
            TableColumn column = new TableColumn(this, rs, excludeColumns);

            columns.put(column.getName(), column);
        }
    }
    
    /**
     * Add a column that's defined in xml metadata.
     * Assumes that a column named colMeta.getName() doesn't already exist in <code>columns</code>.
     * @param colMeta
     * @return
     */
    protected TableColumn addColumn(TableColumnMeta colMeta) {
        TableColumn column = new TableColumn(this, colMeta);

        columns.put(column.getName(), column);
        
        return column;
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
        return indexes.get(indexName);
    }

    private void addIndex(ResultSet rs) throws SQLException {
        String indexName = rs.getString("INDEX_NAME");

        if (indexName == null)
            return;

        TableIndex index = getIndex(indexName);

        if (index == null) {
            index = new TableIndex(rs);

            indexes.put(index.getName(), index);
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

    public Map<String, String> getCheckConstraints() {
        return checkConstraints;
    }

    public Set<TableIndex> getIndexes() {
        return new HashSet<TableIndex>(indexes.values());
    }

    public List<TableColumn> getPrimaryColumns() {
        //return Collections.unmodifiableList(primaryKeys);
        return primaryKeys;
    }
    
    /**
     * @return Comments associated with this table, or <code>null</code> if none.
     */
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        String cmts = (comments == null || comments.trim().length() == 0) ? null : comments.trim();
        
        // MySQL's InnoDB engine does some insane crap of storing erroneous details in 
        // with table comments.  Here I attempt to strip the "crap" out without impacting
        // other databases.  Ideally this should happen in selectColumnCommentsSql (and
        // therefore isolate it to MySQL), but it's a bit too complex to do cleanly.
        if (cmts != null) {
            int crapIndex = cmts.indexOf("; InnoDB free: ");
            if (crapIndex == -1)
                crapIndex = cmts.startsWith("InnoDB free: ") ? 0 : -1;
            if (crapIndex != -1) {
                cmts = cmts.substring(0, crapIndex).trim();
                cmts = cmts.length() == 0 ? null : cmts;
            }
        }
        
        this.comments = cmts;
    }

    public TableColumn getColumn(String columnName) {
        return columns.get(columnName);
    }

    /**
     * Returns <code>List</code> of <code>TableColumn</code>s in ascending column number order.
     * @return
     */
    public List<TableColumn> getColumns() {
        Set<TableColumn> sorted = new TreeSet<TableColumn>(new ByIndexColumnComparator());
        sorted.addAll(columns.values());
        return new ArrayList<TableColumn>(sorted);
    }

    public int getMaxParents() {
        return maxParents;
    }

    public void addedParent() {
        maxParents++;
    }

    public void unlinkParents() {
        for (TableColumn column : columns.values()) {
            column.unlinkParents();
        }
    }

    public boolean isRoot() {
        for (TableColumn column : columns.values()) {
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
        for (TableColumn column : columns.values()) {
            column.unlinkChildren();
        }
    }

    public boolean isLeaf() {
        for (TableColumn column : columns.values()) {
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
                TableColumn childColumn = recursiveConstraint.getChildColumns().get(i);
                TableColumn parentColumn = recursiveConstraint.getParentColumns().get(i);
                childColumn.removeParent(parentColumn);
                parentColumn.removeChild(childColumn);
            }
            return recursiveConstraint;
        }

        return null;
    }

    private ForeignKeyConstraint getSelfReferencingConstraint() {
        for (TableColumn column : columns.values()) {
            for (TableColumn parentColumn : column.getParents()) {
                if (parentColumn.getTable().getName().equals(getName())) {
                    return column.getParentConstraint(parentColumn);
                }
            }
        }
        return null;
    }

    public int getNumChildren() {
        int numChildren = 0;

        for (TableColumn column : columns.values()) {
            numChildren += column.getChildren().size();
        }

        return numChildren;
    }

    public int getNumRealChildren() {
        int numChildren = 0;

        for (TableColumn column : columns.values()) {
            for (TableColumn childColumn : column.getChildren()) {
                if (!column.getChildConstraint(childColumn).isImplied())
                    ++numChildren;
            }
        }

        return numChildren;
    }

    public int getNumParents() {
        int numParents = 0;

        for (TableColumn column : columns.values()) {
            numParents += column.getParents().size();
        }

        return numParents;
    }

    public int getNumRealParents() {
        int numParents = 0;

        for (TableColumn column : columns.values()) {
            for (TableColumn parentColumn : column.getParents()) {
                if (!column.getParentConstraint(parentColumn).isImplied())
                    ++numParents;
            }
        }

        return numParents;
    }

    public ForeignKeyConstraint removeAForeignKeyConstraint() {
        @SuppressWarnings("hiding")
        final List<TableColumn> columns = getColumns();
        int numParents = 0;
        int numChildren = 0;
        // remove either a child or parent, chosing which based on which has the
        // least number of foreign key associations (when either gets to zero then
        // the table can be pruned)
        for (TableColumn column : columns) {
            numParents += column.getParents().size();
            numChildren += column.getChildren().size();
        }

        for (TableColumn column : columns) {
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
     * @return int
     * @throws SQLException 
     */
    protected int fetchNumRows(Database db, Properties properties) throws SQLException {
        if (properties == null) // some "meta" tables don't have associated properties
            return 0;
        
        String sql = properties.getProperty("selectRowCountSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = db.prepareStatement(sql, getName());
                rs = stmt.executeQuery();

                while (rs.next()) {
                    return rs.getInt("row_count");
                }
            } catch (SQLException sqlException) {
                // don't die just because this failed
                System.err.println();
                System.err.println("Unable to extract the number of rows for table " + getName() + ": " + sqlException);
                System.err.println(sql);
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }

        // if we get here then we either didn't have custom SQL or it didn't work
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

    /**
     * @param tableMeta
     */
    public void update(TableMeta tableMeta, Map<String, Table> tables, Map<String, Table> remoteTables) {
        String newComments = tableMeta.getComments();
        if (newComments != null) {
            comments = newComments;
        }
        
        for (TableColumnMeta colMeta : tableMeta.getColumns()) {
            TableColumn col = getColumn(colMeta.getName());
            if (col == null) {
                if (tableMeta.getRemoteSchema() == null) {
                    System.err.println();
                    System.err.print("Unrecognized column '" + colMeta.getName() + "' for table '" + getName() + '\'');
                    continue;
                }
                
                col = addColumn(colMeta);
            }

            // update the column with the changes
            col.update(colMeta);

            // go thru the new foreign key defs and associate them with our columns
            for (ForeignKeyMeta fk : colMeta.getForeignKeys()) {
                Table parent = fk.getRemoteSchema() == null ? tables.get(fk.getTableName())
                                                            : remoteTables.get(fk.getRemoteSchema() + '.' + fk.getTableName());
                if (parent != null) {
                    TableColumn parentColumn = parent.getColumn(fk.getColumnName());

                    if (parentColumn == null) {
                        System.err.println();
                        System.err.println(parent.getName() + '.' + fk.getColumnName() + " doesn't exist");
                    } else {
                        new ForeignKeyConstraint(parentColumn, col) {
                            @Override
                            public String getName() {
                                return "Defined in XML";
                            }
                        };
                    }
                } else {
                    System.err.println();
                    System.err.print("Undefined table '" + fk.getTableName() + "' referenced by '" + getName() + '.' + col.getName() + '\'');
                }
            }
        }
    }
    
    @Override
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

        for (TableColumn column : columns.values()) {
            for (TableColumn parentColumn : column.getParents()) {
                if (!column.getParentConstraint(parentColumn).isImplied())
                    return false;
            }
            for (TableColumn childColumn : column.getChildren()) {
                if (!column.getChildConstraint(childColumn).isImplied())
                    return false;
            }
        }
        return true;
    }

    public int compareTo(Table table) {
        return getName().compareTo(table.getName());
    }

    private static class ByIndexColumnComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            if (column1.getId() == null || column2.getId() == null)
                return column1.getName().compareTo(column2.getName());
            if (column1.getId() instanceof Number)
                return ((Number)column1.getId()).intValue() - ((Number)column2.getId()).intValue();
            return column1.getId().toString().compareTo(column2.getId().toString());
        }
    }

    private static class ByCheckConstraintStringsComparator implements Comparator<String> {
        public int compare(String string1, String string2) {
            return string1.compareTo(string2);
        }
    }
}