package net.sourceforge.schemaspy.view;

import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;

public class DotNode {
    private final Table table;
    private final boolean detailed;
    private final boolean hasColumns;
    private final String refDirectory;
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
     * @param hasColumns boolean
     * @param refDirectory String
     */
    public DotNode(Table table, boolean hasColumns, String refDirectory) {
        this(table, false, hasColumns, refDirectory);
    }

    private DotNode(Table table, boolean detailed, boolean hasColumns, String refDirectory) {
        this.table = table;
        this.detailed = detailed;
        this.hasColumns = hasColumns;
        this.refDirectory = refDirectory;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public boolean hasColumns() {
        return hasColumns;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        String tableName = table.getName();
        String colspan = detailed ? "COLSPAN=\"2\" " : "";
        StyleSheet css = StyleSheet.getInstance();

        buf.append("  \"" + tableName + "\" [" + lineSeparator);
        buf.append("    label=<" + lineSeparator);
        buf.append("    <TABLE BORDER=\"" + (detailed ? "2" : "0") + "\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"" + css.getTableBackground() + "\">" + lineSeparator);
        buf.append("      <TR><TD PORT=\"" + tableName + ".heading\" " + colspan + "BGCOLOR=\"" + css.getTableHeadBackground() + "\" ALIGN=\"CENTER\">" + tableName + "</TD></TR>" + lineSeparator);

        if (hasColumns) {
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
                if (primaryColumns.contains(column))
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
