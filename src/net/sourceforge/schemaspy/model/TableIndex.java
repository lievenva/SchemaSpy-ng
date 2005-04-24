package net.sourceforge.schemaspy.model;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TableIndex implements Comparable, Serializable {
    private final Object id;
    private final String name;
    private final boolean isUnique;
    private boolean isPrimary;
    private final List columns = new ArrayList();

    public TableIndex(ResultSet rs) throws SQLException {
        name = rs.getString("INDEX_NAME");
        isUnique = !rs.getBoolean("NON_UNIQUE");
        id = null;
    }

    public Object getId() {
	return id;
    }

    public String getName() {
	return name;
    }

    void addColumn(TableColumn column) {
        if (column != null)
            columns.add(column);
    }

    public String getType() {
        if (isPrimaryKey())
            return "Primary key";
        if (isUnique())
            return "Must be unique";
        return "Performance";
    }

    public boolean isPrimaryKey() {
	return isPrimary;
    }

    public void setIsPrimaryKey(boolean isPrimaryKey) {
        isPrimary = isPrimaryKey;
    }

    public boolean isUnique() {
	return isUnique;
    }

    public String getColumnsAsString() {
        StringBuffer buf = new StringBuffer();

        Iterator iter = columns.iterator();
        while (iter.hasNext()) {
            if (buf.length() > 0)
                buf.append(" + ");
            buf.append(iter.next());
        }
        return buf.toString();
    }

    public List getColumns() {
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
	for (Iterator iter = getColumns().iterator(); iter.hasNext() && allNullable; ) {
            TableColumn column = (TableColumn)iter.next();
            allNullable = column != null && column.isNullable();
        }

	return allNullable;
    }

    public int compareTo(Object object) {
	TableIndex other = (TableIndex)object;
        if (isPrimaryKey() && !other.isPrimaryKey())
            return -1;
        if (!isPrimaryKey() && other.isPrimaryKey())
            return 1;
	if (getId() == null)
	    return getName().compareTo(other.getName());
	if (getId() instanceof Number)
	    return ((Number)getId()).intValue() - ((Number)other.getId()).intValue();
	return getId().toString().compareTo(other.getId().toString());
    }
}
