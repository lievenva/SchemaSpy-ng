package net.sourceforge.schemaspy.model;

import java.sql.SQLException;

public class ImpliedForeignKeyConstraint extends ForeignKeyConstraint {
    /**
     * @param parentColumn
     * @param childColumn
     * @throws java.sql.SQLException
     */
    public ImpliedForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) throws SQLException {
        super(childColumn.getTable(), null);

        addChildColumn(childColumn);
        addParentColumn(parentColumn);

        childColumn.addParent(parentColumn, this);
        parentColumn.addChild(childColumn, this);
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return "Implied Constraint";
    }

    /**
     * @return
     */
    @Override
    public boolean isImplied() {
        return true;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append(getChildTable());
        buf.append(".");
        buf.append(toString(getChildColumns()));
        buf.append("'s name implies that it's a child of ");
        buf.append(getParentTable());
        buf.append(".");
        buf.append(toString(getParentColumns()));
        buf.append(", but it doesn't reference that column.");
        return buf.toString();
    }
}
