package net.sourceforge.schemaspy.ui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * @author John Currier
 */
public class UiUtils {
    public static int getPreferredColumnWidth(JTable table, TableColumn col) {
        return Math.max(getPreferredColumnHeaderWidth(table, col), getWidestCellInColumn(table, col));
    }
    
    public static int getPreferredColumnHeaderWidth(JTable table, TableColumn col) {
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null)
            return 0;
        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        return comp.getPreferredSize().width;
    }
    
    public static int getWidestCellInColumn(JTable table, TableColumn col) {
        int column = col.getModelIndex();
        int max = 0;
        
        for (int row = 0; row < table.getRowCount(); ++row) {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
            max = Math.max(comp.getPreferredSize().width, max);
        }
        
        return max;
    }
}
