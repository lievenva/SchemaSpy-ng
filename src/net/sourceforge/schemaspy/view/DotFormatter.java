package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 */
public class DotFormatter {
    private static DotFormatter instance = new DotFormatter();
    private final int CompactGraphFontSize = 9;
    private final int LargeGraphFontSize = 11;
    private final String CompactNodeSeparator = "0.05";
    private final String CompactRankSeparator = "0.2";

    /**
     * Singleton - prevent creation
     */
    private DotFormatter() {
    }

    public static DotFormatter getInstance() {
        return instance;
    }

    /**
     * Write all relationships (including implied) associated with the given table
     */
    public WriteStats writeRealRelationships(Table table, boolean twoDegreesOfSeparation, LineWriter dot) throws IOException {
        return writeRelationships(table, false, twoDegreesOfSeparation, dot);
    }

    /**
     * Write implied relationships associated with the given table
     */
    public WriteStats writeImpliedRelationships(Table table, boolean twoDegreesOfSeparation, LineWriter dot) throws IOException {
        return writeRelationships(table, true, twoDegreesOfSeparation, dot);
    }

    /**
     * Write relationships associated with the given table
     */
    private WriteStats writeRelationships(Table table, boolean includeImplied, boolean twoDegreesOfSeparation, LineWriter dot) throws IOException {
        Set tablesWritten = new HashSet();
        WriteStats stats = new WriteStats();

        DotConnectorFinder formatter = DotConnectorFinder.getInstance();

        String graphName = includeImplied ? "impliedTwoDegreesRelationshipsGraph" : (twoDegreesOfSeparation ? "twoDegreesRelationshipsGraph" : "oneDegreeRelationshipsGraph");
        writeHeader(graphName, false, true, dot);

        Set relatedTables = getImmediateRelatives(table, includeImplied, stats);

        dot.writeln(new DotNode(table, "").toString());
        Set connectors = new TreeSet(formatter.getRelatedConnectors(table, includeImplied));
        tablesWritten.add(table);
        stats.wroteTable(table);

        // write immediate relatives first
        Iterator iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            dot.writeln(new DotNode(relatedTable, true, "").toString());
            stats.wroteTable(relatedTable);
            connectors.addAll(formatter.getRelatedConnectors(relatedTable, table, includeImplied));
        }

        // connect the edges that go directly to the target table
        // so they go to the target table's type column instead
        iter = connectors.iterator();
        while (iter.hasNext()) {
            DotConnector edge = (DotConnector)iter.next();
            if (edge.pointsTo(table))
                edge.connectToParentDetails();
        }

        Set allCousins = new HashSet();
        Set allCousinConnectors = new TreeSet();

        // next write 'cousins' (2nd degree of separation)
        if (twoDegreesOfSeparation) {
            iter = relatedTables.iterator();
            while (iter.hasNext()) {
                Table relatedTable = (Table)iter.next();
                Set cousins = getImmediateRelatives(relatedTable, includeImplied, stats);

                Iterator cousinsIter = cousins.iterator();
                while (cousinsIter.hasNext()) {
                    Table cousin = (Table)cousinsIter.next();
                    if (!tablesWritten.add(cousin))
                        continue; // already written

                    allCousinConnectors.addAll(formatter.getRelatedConnectors(cousin, relatedTable, includeImplied));
                    dot.writeln(new DotNode(cousin, true, "").toString());
                    stats.wroteTable(cousin);
                }

                allCousins.addAll(cousins);
            }
        }

        // now figure out what's related at the outskirts to give visual clues
        Set outskirts = new TreeSet();
        iter = tablesWritten.iterator();
        while (iter.hasNext()) {
            Table t = (Table)iter.next();
            if (t != table)
                outskirts.addAll(formatter.getRelatedConnectors(t, includeImplied));
        }
        outskirts.removeAll(connectors);
        // remove the ones that inappropriately point to main table
        iter = outskirts.iterator();
        while (iter.hasNext())  {
            DotConnector edge = (DotConnector)iter.next();
            if (edge.pointsTo(table))
                iter.remove();
            //else
            //    edge.stubMissingTables(tablesWritten);
        }

        // now directly connect the loose ends to the title of the
        // 2nd degree of separation tables
        outskirts.addAll(allCousinConnectors);
        iter = outskirts.iterator();
        while (iter.hasNext()) {
            DotConnector connector = (DotConnector)iter.next();
            if (allCousins.contains(connector.getParentTable()))
                connector.connectToParentTitle();
            if (allCousins.contains(connector.getChildTable()))
                connector.connectToChildTitle();
        }

        connectors.addAll(outskirts);

        // write the collected connectors
        iter = connectors.iterator();
        while (iter.hasNext())
            dot.writeln(iter.next().toString());

        dot.writeln("}");
        return stats;
    }

    private Set getImmediateRelatives(Table table, boolean includeImplied, WriteStats stats) {
        Set relatedColumns = new HashSet();
        boolean foundImplied = false;
        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                boolean implied = column.getChildConstraint(childColumn).isImplied();
                foundImplied |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(childColumn);
            }
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                boolean implied = column.getParentConstraint(parentColumn).isImplied();
                foundImplied |= implied;
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
        stats.setWroteImplied(foundImplied);
        return relatedTables;
    }

    private void writeHeader(String graphName, boolean compact, boolean showLabel, LineWriter dot) throws IOException {
        dot.writeln("// dot " + Dot.getInstance().getVersion() + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        dot.writeln("digraph \"" + graphName + "\" {");
        dot.writeln("  graph [");
        dot.writeln("    rankdir=\"RL\"");
        dot.writeln("    bgcolor=\"" + StyleSheet.getInstance().getBodyBackground() + "\"");
        if (showLabel) {
            dot.writeln("    label=\"\\nGenerated by SchemaSpy\"");
            dot.writeln("    labeljust=\"l\"");
        }
        if (compact) {
            dot.writeln("    nodesep=\"" + CompactNodeSeparator + "\"");
            dot.writeln("    ranksep=\"" + CompactRankSeparator + "\"");
        }
        dot.writeln("  ];");
        dot.writeln("  node [");
        dot.writeln("    fontsize=\"" + (compact ? CompactGraphFontSize : LargeGraphFontSize) + "\"");
        dot.writeln("    shape=\"plaintext\"");
        dot.writeln("  ];");
        dot.writeln("  edge [");
        dot.writeln("    arrowsize=\"0.8\"");
        dot.writeln("  ];");
}

    public WriteStats writeRealRelationships(Collection tables, boolean compact, LineWriter dot) throws IOException {
        return writeRelationships(tables, false, compact, dot);
    }

    public WriteStats writeAllRelationships(Collection tables, boolean compact, LineWriter dot) throws IOException {
        return writeRelationships(tables, true, compact, dot);
    }

    private WriteStats writeRelationships(Collection tables, boolean includeImplied, boolean compact, LineWriter dot) throws IOException {
        DotConnectorFinder formatter = DotConnectorFinder.getInstance();
        WriteStats stats = new WriteStats();
        String graphName;
        if (includeImplied) {
            if (compact)
                graphName = "compactImpliedRelationshipsGraph";
            else
                graphName = "largeImpliedRelationshipsGraph";
        } else {
            if (compact)
                graphName = "compactRelationshipsGraph";
            else
                graphName = "largeRelationshipsGraph";
        }
        writeHeader(graphName, compact, true, dot);

        Iterator iter = tables.iterator();

        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(includeImplied)) {
                dot.writeln(new DotNode(table, true, "tables/").toString());
                stats.wroteTable(table);
                if (includeImplied && table.isOrphan(!includeImplied)) {
                    stats.setWroteImplied(true);
                }
            }
        }

        Set connectors = new TreeSet();

        iter = tables.iterator();
        while (iter.hasNext())
            connectors.addAll(formatter.getRelatedConnectors((Table)iter.next(), includeImplied));

        iter = connectors.iterator();
        while (iter.hasNext())
            dot.writeln(iter.next().toString());

        dot.writeln("}");

        return stats;
    }

    public void writeOrphan(Table table, LineWriter dot) throws IOException {
        DotConnectorFinder formatter = DotConnectorFinder.getInstance();
        writeHeader(table.getName(), false, false, dot);
        dot.writeln(new DotNode(table, true, "tables/").toString());
        dot.writeln("}");
    }
}
