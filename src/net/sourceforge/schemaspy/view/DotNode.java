package net.sourceforge.schemaspy.view;

import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;

public class DotNode {
    private final Table table;
    private final boolean detailed;
    private final boolean showColumns;
    private final String refDirectory;
    private final Set excludedColumns = new HashSet();
    private final Set columnsWithRelationships = new HashSet();
    private final Map referenceCountsByColumn = new HashMap();
    private final String lineSeparator = System.getProperty("line.separator");

    /**
     * Create a DotNode that is a focal point of a graph
     *
     * @param table Table
     * @param refDirectory String
     */
    public DotNode(Table table, String refDirectory) {
        this(table, true, true, refDirectory);
    }

    /**
     * Create a DotNode and specify whether it displays its columns.
     *
     * @param table Table
     * @param showColumns boolean
     * @param refDirectory String
     */
    public DotNode(Table table, boolean showColumns, String refDirectory) {
        this(table, false, showColumns, refDirectory);
    }

    public DotNode(Table table) {
        this(table, false, false, null);
    }

    private DotNode(Table table, boolean detailed, boolean showColumns, String refDirectory) {
        this.table = table;
        this.detailed = detailed;
        this.showColumns = showColumns;
        this.refDirectory = refDirectory;

        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            int referenceCount = column.getChildren().size() + column.getParents().size();
            if (referenceCount > 0) {
                columnsWithRelationships.add(column);
            }
            referenceCountsByColumn.put(column, new Integer(referenceCount));
        }
    }

    public boolean isDetailed() {
        return detailed;
    }

    public Table getTable() {
        return table;
    }

    public void excludeColumn(TableColumn column) {
        excludedColumns.add(column);
        columnsWithRelationships.remove(column);
    }

    public void decrementColumnReferences(TableColumn column) {
        Integer referenceCount = (Integer)referenceCountsByColumn.get(column);
        referenceCount = new Integer(referenceCount.intValue() - 1);
        referenceCountsByColumn.put(column, referenceCount);
        if (referenceCount.intValue() < 1)
            columnsWithRelationships.remove(column);
    }

    public boolean hasRelationships() {
        return !columnsWithRelationships.isEmpty();
    }

    public String toString() {
        StyleSheet css = StyleSheet.getInstance();
        if (refDirectory == null)
            return "\"" + table.getName() + "\" [fontcolor=\"" + css.getOutlierBackgroundColor() + "\"]";

        StringBuffer buf = new StringBuffer();
        String tableName = table.getName();
        String colspan = detailed ? "COLSPAN=\"2\" " : "";

        buf.append("  \"" + tableName + "\" [" + lineSeparator);
        buf.append("    label=<" + lineSeparator);
        buf.append("    <TABLE BORDER=\"" + (detailed ? "2" : "0") + "\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"" + css.getTableBackground() + "\">" + lineSeparator);
        buf.append("      <TR><TD PORT=\"" + tableName + ".heading\" " + colspan + "BGCOLOR=\"" + css.getTableHeadBackground() + "\" ALIGN=\"CENTER\">" + tableName + "</TD></TR>" + lineSeparator);

        if (showColumns) {
            List primaryColumns = table.getPrimaryColumns();
            Set indexColumns = new HashSet();
            Iterator iter = table.getIndexes().iterator();
            while (iter.hasNext()) {
                TableIndex index = (TableIndex)iter.next();
                indexColumns.addAll(index.getColumns());
            }
            indexColumns.removeAll(primaryColumns);

            for (iter = table.getColumns().iterator(); iter.hasNext(); ) {
                TableColumn column = (TableColumn)iter.next();
                buf.append("      <TR><TD PORT=\"");
                buf.append(column.getName());
                buf.append("\"");
                if (excludedColumns.contains(column))
                    buf.append(" BGCOLOR=\"" + css.getIgnoredColumnBackgroundColor() + "\"");
                else if (primaryColumns.contains(column))
                    buf.append(" BGCOLOR=\"" + css.getPrimaryKeyBackground() + "\"");
                else if (indexColumns.contains(column))
                    buf.append(" BGCOLOR=\"" + css.getIndexedColumnBackground() + "\"");
                buf.append(" ALIGN=\"LEFT\">");
                buf.append(column.getName());
                buf.append("</TD>");
                if (detailed) {
                    buf.append("<TD PORT=\"");
                    buf.append(column.getName());
                    buf.append(".type\" ALIGN=\"LEFT\">");
                    buf.append(column.getType().toLowerCase());
                    buf.append("[");
                    buf.append(column.getDetailedSize());
                    buf.append("]</TD>");
                }
                buf.append("</TR>" + lineSeparator);
            }
        }

        buf.append("      <TR><TD " + colspan + "ALIGN=\"RIGHT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        if (table.isView()) {
            buf.append("view");
        } else {
            String numRows = NumberFormat.getInstance().format(table.getNumRows());
            // dot can't handle some European thousands separators
            for (int i = 0; i < numRows.length(); ++i) {
                char ch = numRows.charAt(i);
                if (ch >= ' ' && ch < 'z')
                    buf.append(ch);
            }
            buf.append(" rows");
        }
        buf.append("</TD></TR>" + lineSeparator);

        buf.append("    </TABLE>>" + lineSeparator);
        buf.append("    URL=\"" + refDirectory + tableName + ".html" + (refDirectory.length() == 0 && !detailed ? "#graph" : "#") + "\"" + lineSeparator);
        buf.append("    tooltip=\"" + tableName + "\"" + lineSeparator);
        buf.append("  ];" + lineSeparator);

        return buf.toString();
    }
}
