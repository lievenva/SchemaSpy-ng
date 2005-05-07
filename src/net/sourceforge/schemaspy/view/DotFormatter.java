package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.LineWriter;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 * @version 1.0
 */
public class DotFormatter {
    /**
     * Write relationships associated with the given table
     *
     * @param table Table
     * @param out LineWriter
     * @throws IOException
     * @return boolean <code>true</code> if implied relationships were written
     */
    public boolean writeRelationships(Table table, boolean includeImplied, LineWriter out) throws IOException {
        Set tablesWritten = new HashSet();
        boolean[] wroteImplied = new boolean[1];

        DotTableFormatter formatter = new DotTableFormatter();

        writeDotHeader(includeImplied ? "allRelationshipsGraph" : "realRelationshipsGraph", out);

        Set relatedTables = getImmediateRelatives(table, includeImplied, wroteImplied);

        formatter.writeNode(table, "", true, true, true, out);
        Set relationships = formatter.getRelationships(table, includeImplied);
        tablesWritten.add(table);

        // write immediate relatives first
        Iterator iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            formatter.writeNode(relatedTable, "", true, false, false, out);
            relationships.addAll(formatter.getRelationships(relatedTable, table, includeImplied));
        }

        // next write 'cousins' (2nd degree of separation)
        iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            Set cousins = getImmediateRelatives(relatedTable, includeImplied, wroteImplied);

            Iterator cousinsIter = cousins.iterator();
            while (cousinsIter.hasNext()) {
                Table cousin = (Table)cousinsIter.next();
                if (!tablesWritten.add(cousin))
                    continue; // already written
                relationships.addAll(formatter.getRelationships(cousin, relatedTable, includeImplied));
                formatter.writeNode(cousin, "", false, false, false, out);
            }
        }

        iter = new TreeSet(relationships).iterator();
        while (iter.hasNext())
            out.writeln(iter.next().toString());

        out.writeln("}");
        return wroteImplied[0];
    }

    /**
     * I have having to use an array of one boolean to return another value...ugh
     */
    private Set getImmediateRelatives(Table table, boolean includeImplied, boolean[] foundImplied) {
        Set relatedColumns = new HashSet();
        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                boolean implied = column.getChildConstraint(childColumn).isImplied();
                foundImplied[0] |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(childColumn);
            }
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                boolean implied = column.getParentConstraint(parentColumn).isImplied();
                foundImplied[0] |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(parentColumn);
            }
        }

        Set relatedTables = new HashSet();
        iter = relatedColumns.iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            relatedTables.add(column.getTable());
        }

        relatedTables.remove(table);

        return relatedTables;
    }


    private void writeDotHeader(String name, LineWriter out) throws IOException {
        out.writeln("digraph " + name + " {");
        out.writeln("  graph [");
        out.writeln("    rankdir=\"RL\"");
        out.writeln("    bgcolor=\"" + StyleSheet.getBodyBackground() + "\"");
        out.writeln("    concentrate=\"true\"");
        out.writeln("  ];");
        out.writeln("  node [");
        out.writeln("    fontsize=\"11\"");
        out.writeln("    shape=\"plaintext\"");
        out.writeln("  ];");
    }

    public int writeRelationships(Collection tables, boolean includeImplied, LineWriter out) throws IOException {
        return write(tables, false, includeImplied, out);
    }

    public int writeOrphans(Collection tables, LineWriter out) throws IOException {
        return write(tables, true, false, out);
    }

    private int write(Collection tables, boolean onlyOrphans, boolean includeImplied, LineWriter out) throws IOException {
        DotTableFormatter formatter = new DotTableFormatter();
        int numWritten = 0;
        writeDotHeader(includeImplied ? "allRelationshipsGraph" : "realRelationshipsGraph", out);

        Iterator iter = tables.iterator();

        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            boolean isOrphan = table.isOrphan(includeImplied);
            if (onlyOrphans && isOrphan || !onlyOrphans && !isOrphan) {
                formatter.writeNode(table, "tables/", true, false, onlyOrphans && isOrphan, out);
                ++numWritten;
            }
        }

        if (!onlyOrphans) { // orphans don't have relationships so don't even try
            Set relationships = new TreeSet();
            iter = tables.iterator();

            while (iter.hasNext())
                relationships.addAll(formatter.getRelationships((Table)iter.next(), includeImplied));

            iter = relationships.iterator();
            while (iter.hasNext())
                out.writeln(iter.next().toString());
        }

        out.writeln("}");

        return numWritten;
    }
}
