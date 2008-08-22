package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;
import net.sourceforge.schemaspy.view.DotNode.DotNodeConfig;

/**
 * Format table data into .dot format to feed to Graphvis' dot program.
 *
 * @author John Currier
 */
public class DotFormatter {
    private static DotFormatter instance = new DotFormatter();
    private final int fontSize = Config.getInstance().getFontSize();

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
    public void writeRealRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, LineWriter dot) throws IOException {
        writeRelationships(table, twoDegreesOfSeparation, stats, false, dot);
    }

    /**
     * Write implied relationships associated with the given table
     */
    public void writeAllRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, LineWriter dot) throws IOException {
        writeRelationships(table, twoDegreesOfSeparation, stats, true, dot);
    }

    /**
     * Write relationships associated with the given table
     */
    private void writeRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, boolean includeImplied, LineWriter dot) throws IOException {
        Set<Table> tablesWritten = new HashSet<Table>();

        DotConnectorFinder finder = DotConnectorFinder.getInstance();

        String diagramName = includeImplied ? "impliedTwoDegreesRelationshipsDiagram" : (twoDegreesOfSeparation ? "twoDegreesRelationshipsDiagram" : "oneDegreeRelationshipsDiagram");
        writeHeader(diagramName, true, dot);

        Set<Table> relatedTables = getImmediateRelatives(table, stats, true, includeImplied);

        Set<DotConnector> connectors = new TreeSet<DotConnector>(finder.getRelatedConnectors(table));
        tablesWritten.add(table);

        Map<Table, DotNode> nodes = new TreeMap<Table, DotNode>();

        // write immediate relatives first
        for (Table relatedTable : relatedTables) {
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            nodes.put(relatedTable, new DotNode(relatedTable, true, ""));
            connectors.addAll(finder.getRelatedConnectors(relatedTable, table, true));
        }

        // connect the edges that go directly to the target table
        // so they go to the target table's type column instead
        for (DotConnector connector : connectors) {
            if (connector.pointsTo(table))
                connector.connectToParentDetails();
        }

        Set<Table> allCousins = new HashSet<Table>();
        Set<DotConnector> allCousinConnectors = new TreeSet<DotConnector>();

        // next write 'cousins' (2nd degree of separation)
        if (twoDegreesOfSeparation) {
            for (Table relatedTable : relatedTables) {
                Set<Table> cousins = getImmediateRelatives(relatedTable, stats, false, includeImplied);

                for (Table cousin : cousins) {
                    if (!tablesWritten.add(cousin))
                        continue; // already written

                    allCousinConnectors.addAll(finder.getRelatedConnectors(cousin, relatedTable, false));
                    nodes.put(cousin, new DotNode(cousin, false, ""));
                }

                allCousins.addAll(cousins);
            }
        }

        markExcludedColumns(nodes, stats.getExcludedColumns());

        // now directly connect the loose ends to the title of the
        // 2nd degree of separation tables
        for (DotConnector connector : allCousinConnectors) {
            if (allCousins.contains(connector.getParentTable()) && !relatedTables.contains(connector.getParentTable()))
                connector.connectToParentTitle();
            if (allCousins.contains(connector.getChildTable()) && !relatedTables.contains(connector.getChildTable()))
                connector.connectToChildTitle();
        }

        // include the table itself
        nodes.put(table, new DotNode(table, ""));

        connectors.addAll(allCousinConnectors);
        for (DotConnector connector : connectors) {
            if (connector.isImplied()) {
                DotNode node = nodes.get(connector.getParentTable());
                if (node != null)
                    node.setShowImplied(true);
                node = nodes.get(connector.getChildTable());
                if (node != null)
                    node.setShowImplied(true);
            }
            dot.writeln(connector.toString());
        }

        for (DotNode node : nodes.values()) {
            dot.writeln(node.toString());
            stats.wroteTable(node.getTable());
        }

        dot.writeln("}");
    }

    private Set<Table> getImmediateRelatives(Table table, WriteStats stats, boolean includeExcluded, boolean includeImplied) {
        Set<TableColumn> relatedColumns = new HashSet<TableColumn>();
        boolean foundImplied = false;
        
        for (TableColumn column : table.getColumns()) {
            if (!includeExcluded && column.isExcluded()) {
                continue;
            }
            
            for (TableColumn childColumn : column.getChildren()) {
                if (!includeExcluded && childColumn.isExcluded()) {
                    continue;
                }
                boolean implied = column.getChildConstraint(childColumn).isImplied();
                foundImplied |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(childColumn);
            }
            
            for (TableColumn parentColumn : column.getParents()) {
                if (!includeExcluded && parentColumn.isExcluded()) {
                    continue;
                }
                boolean implied = column.getParentConstraint(parentColumn).isImplied();
                foundImplied |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(parentColumn);
            }
        }

        Set<Table> relatedTables = new HashSet<Table>();
        for (TableColumn column : relatedColumns)
            relatedTables.add(column.getTable());

        relatedTables.remove(table);
        stats.setWroteImplied(foundImplied);
        return relatedTables;
    }

    private void writeHeader(String diagramName, boolean showLabel, LineWriter dot) throws IOException {
        dot.writeln("// dot " + Dot.getInstance().getVersion() + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        dot.writeln("digraph \"" + diagramName + "\" {");
        dot.writeln("  graph [");
        boolean rankdirbug = Config.getInstance().isRankDirBugEnabled();
        if (!rankdirbug)
            dot.writeln("    rankdir=\"RL\"");
        dot.writeln("    bgcolor=\"" + StyleSheet.getInstance().getBodyBackground() + "\"");
        if (showLabel) {
            if (rankdirbug)
                dot.writeln("    label=\"\\nLayout is significantly better without '-rankdirbug' option\"");
            else
                dot.writeln("    label=\"\\nGenerated by SchemaSpy\"");
            dot.writeln("    labeljust=\"l\"");
        }
        dot.writeln("    nodesep=\"0.18\"");
        dot.writeln("    ranksep=\"0.46\"");
        dot.writeln("    fontname=\"" + Config.getInstance().getFont() + "\"");
        dot.writeln("    fontsize=\"" + fontSize + "\"");
        dot.writeln("  ];");
        dot.writeln("  node [");
        dot.writeln("    fontname=\"" + Config.getInstance().getFont() + "\"");
        dot.writeln("    fontsize=\"" + fontSize + "\"");
        dot.writeln("    shape=\"plaintext\"");
        dot.writeln("  ];");
        dot.writeln("  edge [");
        dot.writeln("    arrowsize=\"0.8\"");
        dot.writeln("  ];");
}

    public void writeRealRelationships(Database db, Collection<Table> tables, boolean compact, boolean showColumns, WriteStats stats, LineWriter dot) throws IOException {
        writeRelationships(db, tables, compact, showColumns, false, stats, dot);
    }

    public void writeAllRelationships(Database db, Collection<Table> tables, boolean compact, boolean showColumns, WriteStats stats, LineWriter dot) throws IOException {
        writeRelationships(db, tables, compact, showColumns, true, stats, dot);
    }

    private void writeRelationships(Database db, Collection<Table> tables, boolean compact, boolean showColumns, boolean includeImplied, WriteStats stats, LineWriter dot) throws IOException {
        DotConnectorFinder finder = DotConnectorFinder.getInstance();
        DotNodeConfig nodeConfig = showColumns ? new DotNodeConfig(!compact, false) : new DotNodeConfig();
        
        String diagramName;
        if (includeImplied) {
            if (compact)
                diagramName = "compactImpliedRelationshipsDiagram";
            else
                diagramName = "largeImpliedRelationshipsDiagram";
        } else {
            if (compact)
                diagramName = "compactRelationshipsDiagram";
            else
                diagramName = "largeRelationshipsDiagram";
        }
        writeHeader(diagramName, true, dot);

        Map<Table, DotNode> nodes = new TreeMap<Table, DotNode>();

        for (Table table : tables) {
            if (!table.isOrphan(includeImplied)) {
                nodes.put(table, new DotNode(table, "tables/", nodeConfig));
            }
        }

        for (Table table : db.getRemoteTables()) {
            nodes.put(table, new DotNode(table, "tables/", nodeConfig));
        }

        Set<DotConnector> connectors = new TreeSet<DotConnector>();

        for (DotNode node : nodes.values()) {
            connectors.addAll(finder.getRelatedConnectors(node.getTable()));
        }

        markExcludedColumns(nodes, stats.getExcludedColumns());

        for (DotNode node : nodes.values()) {
            Table table = node.getTable();

            dot.writeln(node.toString());
            stats.wroteTable(table);
            if (includeImplied && table.isOrphan(!includeImplied)) {
                stats.setWroteImplied(true);
            }
        }

        for (DotConnector connector : connectors) {
            dot.writeln(connector.toString());
        }

        dot.writeln("}");
    }

    private void markExcludedColumns(Map<Table, DotNode> nodes, Set<TableColumn> excludedColumns) {
        for (TableColumn column : excludedColumns) {
            DotNode node = nodes.get(column.getTable());
            if (node != null) {
                node.excludeColumn(column);
            }
        }
    }

    public void writeOrphan(Table table, LineWriter dot) throws IOException {
        writeHeader(table.getName(), false, dot);
        dot.writeln(new DotNode(table, true, "tables/").toString());
        dot.writeln("}");
    }
}
