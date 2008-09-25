package net.sourceforge.schemaspy.model;

import net.sourceforge.schemaspy.DbAnalyzer;

/**
 * See {@link DbAnalyzer#getRailsConstraints(java.util.Map)} for
 * details on Rails naming conventions.
 * 
 * @author John Currier
 */
public class RailsForeignKeyConstraint extends ForeignKeyConstraint {
    /**
     * @param parentColumn
     * @param childColumn
     */
    public RailsForeignKeyConstraint(TableColumn parentColumn, TableColumn childColumn) {
        super(parentColumn, childColumn);
    }

    /**
     * Normally the name of the constraint, but this one is implied by
     * Rails naming conventions.
     * 
     * @return
     */
    @Override
    public String getName() {
        return "ByRailsConventionConstraint";
    }
}