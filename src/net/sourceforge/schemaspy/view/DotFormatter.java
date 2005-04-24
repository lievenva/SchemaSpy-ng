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
     * @return int
     */
    public void writeRelationships(Table table, LineWriter out) throws IOException {
        Set tablesWritten = new HashSet();

        DotTableFormatter formatter = new DotTableFormatter();

        writeDotHeader(out);

        Set relatedTables = getImmediateRelatives(table);

        formatter.writeNode(table, "", true, true, out);
        Set relationships = formatter.getRelationships(table);
        tablesWritten.add(table);

        // write immediate relatives first
        Iterator iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            formatter.writeNode(relatedTable, "", true, false, out);
            relationships.addAll(formatter.getRelationships(relatedTable, table));
        }

        // next write 'cousins' (2nd degree of separation)
        iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            Set cousins = getImmediateRelatives(relatedTable);

            Iterator cousinsIter = cousins.iterator();
            while (cousinsIter.hasNext()) {
                Table cousin = (Table)cousinsIter.next();
                if (!tablesWritten.add(cousin))
                    continue; // already written
                relationships.addAll(formatter.getRelationships(cousin, relatedTable));
                formatter.writeNode(cousin, "", false, false, out);
            }
        }

        iter = new TreeSet(relationships).iterator();
        while (iter.hasNext())
            out.writeln(iter.next().toString());

        out.writeln("}");
    }

    private Set getImmediateRelatives(Table table) {
        Set relatedColumns = new HashSet();
        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            relatedColumns.addAll(column.getChildren());
            relatedColumns.addAll(column.getParents());
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


    private void writeDotHeader(LineWriter out) throws IOException {
        out.writeln("digraph tables {");
        out.writeln("  graph [");
        out.writeln("    rankdir=\"RL\"");
        out.writeln("    bgcolor=\"" + new CssFormatter().getBodyBackground() + "\"");
        out.writeln("    concentrate=\"true\"");
        out.writeln("  ];");
        out.writeln("  node [");
        out.writeln("    fontsize=\"11\"");
        out.writeln("    shape=\"plaintext\"");
        out.writeln("  ];");
    }

    public int writeRelationships(Collection tables, LineWriter out) throws IOException {
        return write(tables, false, out);
    }

    public int writeOrphans(Collection tables, LineWriter out) throws IOException {
        return write(tables, true, out);
    }

    private int write(Collection tables, boolean onlyOrphans, LineWriter out) throws IOException {
        DotTableFormatter formatter = new DotTableFormatter();
        int numWritten = 0;
        writeDotHeader(out);

        Iterator iter = tables.iterator();

        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            boolean isOrphan = table.getMaxParents() == 0 && table.getMaxChildren() == 0;
            if (onlyOrphans && isOrphan || !onlyOrphans && !isOrphan) {
                formatter.writeNode(table, "tables/", true, false, out);
                ++numWritten;
            }
        }

        if (!onlyOrphans) { // orphans don't have relationships so don't even try
            Set relationships = new TreeSet();
            iter = tables.iterator();

            while (iter.hasNext())
                relationships.addAll(formatter.getRelationships((Table)iter.next()));

            iter = relationships.iterator();
            while (iter.hasNext())
                out.writeln(iter.next().toString());
        }

        out.writeln("}");

        return numWritten;
    }
}
