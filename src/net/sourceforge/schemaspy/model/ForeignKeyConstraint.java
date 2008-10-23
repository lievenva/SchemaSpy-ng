package net.sourceforge.schemaspy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForeignKeyConstraint {
    private final String name;
    private Table parentTable;
    private final List<TableColumn> parentColumns = new ArrayList<TableColumn>();
    private final Table childTable;
    private final List<TableColumn> childColumns = new ArrayList<TableColumn>();
    private final char deleteRule;
    private final char updateRule;

    ForeignKeyConstraint(Table child, String name) {
        this.name = name; // implied constraints will have a null name and override getName()
        childTable = child;
        deleteRule = 'D';
        updateRule = 'U';
    }
    
    /**
     * This constructor is intended for use <b>after</b> all of the tables have been
     * found in the system.  One impact of using this constructor is that it will
     * "glue" the two tables together through their columns.
     * 
     * @param parentColumn
     * @param childColumn
     */
    public ForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) {
        this(childColumn.getTable(), null);

        addChildColumn(childColumn);
        addParentColumn(parentColumn);

        childColumn.addParent(parentColumn, this);
        parentColumn.addChild(childColumn, this);
    }

    void addParentColumn(TableColumn column) {
        if (column != null) {
            parentColumns.add(column);
            parentTable = column.getTable();
        }
    }

    void addChildColumn(TableColumn column) {
        if (column != null)
            childColumns.add(column);
    }

    public String getName() {
        return name;
    }

    public Table getParentTable() {
        return parentTable;
    }

    public List<TableColumn> getParentColumns() {
        return Collections.unmodifiableList(parentColumns);
    }

    public Table getChildTable() {
        return childTable;
    }

    public List<TableColumn> getChildColumns() {
        return Collections.unmodifiableList(childColumns);
    }

    public char getDeleteRule() {
        return deleteRule;
    }

    /**
     * @return
     */
    public boolean isOnDeleteCascade() {
        return deleteRule == 'C';
    }

    public char getUpdateRule() {
        return updateRule;
    }

    /**
     * @return
     */
    public boolean isImplied() {
        return false;
    }
    
    /**
     * We have several types of constraints.  
     * This returns <code>true</code> if the constraint came from the database
     * metadata and not inferred by something else.  
     * This is different than {@link #isImplied()} in that implied relationships
     * are a specific type of non-real relationships.
     *  
     * @return
     */
    public boolean isReal() {
        return getClass() == ForeignKeyConstraint.class;
    }

    /**
     * @param columns
     * @return
     */
    public static String toString(List<TableColumn> columns) {
        if (columns.size() == 1)
            return columns.iterator().next().toString();
        return columns.toString();
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(childTable.getName());
        buf.append('.');
        buf.append(toString(childColumns));
        buf.append(" refs ");
        buf.append(parentTable.getName());
        buf.append('.');
        buf.append(toString(parentColumns));
        if (parentTable.isRemote()) {
            buf.append(" in ");
            buf.append(parentTable.getSchema());
        }
        if (name != null) {
            buf.append(" via ");
            buf.append(name);
        }

        return buf.toString();
    }
}
