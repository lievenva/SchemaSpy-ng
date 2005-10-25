package net.sourceforge.schemaspy.view;

import java.io.*;
import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 */
public class DotConnectorFinder {
    private static DotConnectorFinder instance = new DotConnectorFinder();

    /**
     * Singleton - prevent creation
     */
    private DotConnectorFinder() {
    }

    public static DotConnectorFinder getInstance() {
        return instance;
    }

    /**
     *
     * @param table Table
     * @param includeImplied boolean
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    public Set getRelatedConnectors(Table table, boolean includeImplied) throws IOException {
        Set relationships = new HashSet();

        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), null, includeImplied));
        }

        return relationships;
    }

    /**
     * Get all the relationships that exist between these two tables.
     *
     * @param table1 Table
     * @param table2 Table
     * @param includeImplied boolean
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    public Set getRelatedConnectors(Table table1, Table table2, boolean includeImplied) throws IOException {
        Set relationships = new HashSet();

        Iterator iter = table1.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), table2, includeImplied));
        }

        iter = table2.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), table1, includeImplied));
        }

        return relationships;
    }

    /**
     * @param column TableColumn
     * @param targetTable Table
     * @param includeImplied boolean
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    private Set getRelatedConnectors(TableColumn column, Table targetTable, boolean includeImplied) throws IOException {
        Set relationships = new HashSet();

        for (Iterator iter = column.getParents().iterator(); iter.hasNext(); ) {
            TableColumn parentColumn = (TableColumn)iter.next();
            Table parentTable = parentColumn.getTable();
            if (targetTable != null && parentTable != targetTable)
                continue;
            boolean implied = column.getParentConstraint(parentColumn).isImplied();
            if (includeImplied || !implied) {
                relationships.add(new DotConnector(parentColumn, column, implied));
            }
        }

        for (Iterator iter = column.getChildren().iterator(); iter.hasNext(); ) {
            TableColumn childColumn = (TableColumn)iter.next();
            Table childTable = childColumn.getTable();
            if (targetTable != null && childTable != targetTable)
                continue;
            boolean implied = column.getChildConstraint(childColumn).isImplied();
            if (includeImplied || !implied) {
                relationships.add(new DotConnector(column, childColumn, implied));
            }
        }

        return relationships;
    }
}
