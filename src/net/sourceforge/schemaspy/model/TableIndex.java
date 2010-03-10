package net.sourceforge.schemaspy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableIndex implements Comparable<TableIndex> {
    private final String name;
    private final boolean isUnique;
    private Object id;
    private boolean isPrimary;
    private final List<TableColumn> columns = new ArrayList<TableColumn>();
    private final List<Boolean> columnsAscending = new ArrayList<Boolean>(); // for whether colums are ascending order

    /**
     * @param rs
     * @throws java.sql.SQLException
     */
    public TableIndex(ResultSet rs) throws SQLException {
        name = rs.getString("INDEX_NAME");
        isUnique = !rs.getBoolean("NON_UNIQUE");
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void addColumn(TableColumn column, String sortOrder) {
        if (column != null) {
            columns.add(column);
            columnsAscending.add(Boolean.valueOf(sortOrder == null || sortOrder.equals("A")));
        }
    }

    /**
     * @return
     */
    public String getType() {
        if (isPrimaryKey())
            return "Primary key";
        if (isUnique())
            return "Must be unique";
        return "Performance";
    }

    /**
     * @return
     */
    public boolean isPrimaryKey() {
        return isPrimary;
    }

    /**
     * @param isPrimaryKey
     */
    public void setIsPrimaryKey(boolean isPrimaryKey) {
        isPrimary = isPrimaryKey;
    }

    /**
     * @return
     */
    public boolean isUnique() {
        return isUnique;
    }

    /**
     * @return
     */
    public String getColumnsAsString() {
        StringBuilder buf = new StringBuilder();

        for (TableColumn column : columns) {
            if (buf.length() > 0)
                buf.append(" + ");
            buf.append(column);
        }
        return buf.toString();
    }

    public List<TableColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Yes, we had a project that had columns defined as both 'nullable' and 'must be unique'.
     *
     * @return boolean
     */
    public boolean isUniqueNullable() {
        if (!isUnique())
            return false;

        // if all of the columns specified by the Unique Index are nullable
        // then return true, otherwise false
        boolean allNullable = true;
        for (TableColumn column : getColumns()) {
            allNullable = column != null && column.isNullable();
            if (!allNullable)
                break;
        }

        return allNullable;
    }

    /**
     * @param column
     * @return
     */
    public boolean isAscending(TableColumn column) {
        return columnsAscending.get(columns.indexOf(column)).booleanValue();
    }

    /**
     * @param object
     * @return
     */
    public int compareTo(TableIndex other) {
        if (isPrimaryKey() && !other.isPrimaryKey())
            return -1;
        if (!isPrimaryKey() && other.isPrimaryKey())
            return 1;

        Object thisId = getId();
        Object otherId = other.getId();
        if (thisId == null || otherId == null)
            return getName().compareToIgnoreCase(other.getName());
        if (thisId instanceof Number)
            return ((Number)thisId).intValue() - ((Number)otherId).intValue();
        return thisId.toString().compareToIgnoreCase(otherId.toString());
    }
}