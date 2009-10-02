package net.sourceforge.schemaspy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a <a href='http://en.wikipedia.org/wiki/Foreign_key'>
 * Foreign Key Constraint</a> that "ties" a child table to a parent table
 * via foreign and primary keys.
 */
public class ForeignKeyConstraint implements Comparable<ForeignKeyConstraint> {
    private final String name;
    private Table parentTable;
    private final List<TableColumn> parentColumns = new ArrayList<TableColumn>();
    private final Table childTable;
    private final List<TableColumn> childColumns = new ArrayList<TableColumn>();
    private final char deleteRule;
    private final char updateRule;

    /**
     * Construct a foreign key for the specified child table.
     * Relationship details will be added later.
     *
     * @param child
     * @param name
     */
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

    /**
     * Add a "parent" side to the constraint.
     *
     * @param column
     */
    void addParentColumn(TableColumn column) {
        if (column != null) {
            parentColumns.add(column);
            parentTable = column.getTable();
        }
    }

    /**
     * Add a "child" side to the constraint.
     *
     * @param column
     */
    void addChildColumn(TableColumn column) {
        if (column != null)
            childColumns.add(column);
    }

    /**
     * Returns the name of the constraint
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the parent table (the table that contains the referenced primary key
     * column).
     *
     * @return
     */
    public Table getParentTable() {
        return parentTable;
    }

    /**
     * Returns all of the primary key columns that are referenced by this constraint.
     *
     * @return
     */
    public List<TableColumn> getParentColumns() {
        return Collections.unmodifiableList(parentColumns);
    }

    /**
     * Returns the table on the "child" end of the relationship (contains the foreign
     * key that references the parent table's primary key).
     *
     * @return
     */
    public Table getChildTable() {
        return childTable;
    }

    /**
     * Returns all of the foreign key columns that are referenced by this constraint.
     *
     * @return
     */
    public List<TableColumn> getChildColumns() {
        return Collections.unmodifiableList(childColumns);
    }

    /**
     * Returns the delete rule for this constraint.
     *
     * @return
     */
    public char getDeleteRule() {
        //TODO Needs to be further clarified / implemented.
        return deleteRule;
    }

    /**
     * Returns <code>true</code> if this constraint should
     * <a href='http://en.wikipedia.org/wiki/Cascade_delete'>cascade deletions</code>.
     *
     * @return
     */
    public boolean isOnDeleteCascade() {
        return getDeleteRule() == 'C';
    }

    /**
     * Returns the update rule for this constraint.
     *
     * @return
     */
    public char getUpdateRule() {
        //TODO Needs to be further clarified / implemented.
        return updateRule;
    }

    /**
     * Returns <code>true</code> if this is an implied constraint or
     * <code>false</code> if it is "real".
     *
     * Subclasses that implement implied constraints should override this method.
     *
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
     * Custom comparison method to deal with foreign key names that aren't
     * unique across all schemas being evaluated
     *
     * @param other ForeignKeyConstraint
     *
     * @return
     */
    public int compareTo(ForeignKeyConstraint other) {
        if (other == this)
            return 0;

        int rc = getName().compareToIgnoreCase(other.getName());
        if (rc == 0) {
            // should only get here if we're dealing with cross-schema references (rare)
            String ours = getChildColumns().get(0).getTable().getSchema();
            String theirs = other.getChildColumns().get(0).getTable().getSchema();
            if (ours != null && theirs != null)
                rc = ours.compareToIgnoreCase(theirs);
            else if (ours == null)
                rc = -1;
            else
                rc = 1;
        }

        return rc;
    }

    /**
     * Static method that returns a string representation of the specified
     * list of {@link TableColumn columns}.
     *
     * @param columns
     * @return
     */
    public static String toString(List<TableColumn> columns) {
        if (columns.size() == 1)
            return columns.iterator().next().toString();
        return columns.toString();
    }

    /**
     * Returns a string representation of this foreign key constraint.
     *
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
