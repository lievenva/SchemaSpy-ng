package net.sourceforge.schemaspy.model;

public class ImpliedForeignKeyConstraint extends ForeignKeyConstraint {
    /**
     * @param parentColumn
     * @param childColumn
     */
    public ImpliedForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) {
        super(parentColumn, childColumn);
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
