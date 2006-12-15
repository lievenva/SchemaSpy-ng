package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import net.sourceforge.schemaspy.util.*;

/**
 * @author John Currier
 */
public class DbConfigPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JComboBox databaseTypeSelector;
    private DbConfigTableModel model = new DbConfigTableModel();
    private JTable table;

    public DbConfigPanel() {
        super();
        initialize();
    }
    
    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        setLayout(new BorderLayout());
        add(getDatabaseTypeSelector(), BorderLayout.NORTH);
        
        table = new JTable(model) {
            private static final long serialVersionUID = 1L;
            
            {
                setDefaultRenderer(Boolean.TYPE, getDefaultRenderer(Boolean.class));
                setDefaultEditor(Boolean.TYPE, getDefaultEditor(Boolean.class));
                setDefaultRenderer(Number.class, getDefaultRenderer(String.class));
                setDefaultEditor(Number.class, getDefaultEditor(String.class));

                DirectoryCellEditor fileEditor = new DirectoryCellEditor(model, new File("/"));
                setDefaultRenderer(File.class, fileEditor);
                setDefaultEditor(File.class, fileEditor);
            }

            public TableCellRenderer getCellRenderer(int row, int column) {
                TableCellRenderer renderer;
                
                if (column == 0)
                    renderer = super.getCellRenderer(row, column);
                else 
                    renderer = getDefaultRenderer(model.getClass(row));
                if (renderer instanceof JComponent)
                    ((JComponent)renderer).setToolTipText(model.getDescription(row));
                return renderer;
            }

            public TableCellEditor getCellEditor(int row, int column) {
                return getDefaultEditor(model.getClass(row));
            }
        };
        
        JScrollPane scroller = new JScrollPane(table);
        scroller.setViewportBorder(null);
        add(scroller, BorderLayout.CENTER);
        
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                TableColumn paramColumn = table.getColumnModel().getColumn(0);
                paramColumn.setMinWidth(getPreferredWidthForColumn(paramColumn) + 4);
                paramColumn.setMaxWidth(paramColumn.getMinWidth());
                table.sizeColumnsToFit(0);
            }
            
            private int getPreferredWidthForColumn(TableColumn col) {
                return Math.max(columnHeaderWidth(col), widestCellInColumn(col));
            }
            
            private int columnHeaderWidth(TableColumn col) {
                TableCellRenderer renderer = col.getHeaderRenderer();
                if (renderer == null)
                    return 0;
                Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
                return comp.getPreferredSize().width;
            }
            
            private int widestCellInColumn(TableColumn col) {
                int c = col.getModelIndex();
                int max = 0;
                
                for (int row = 0; row < table.getRowCount(); ++row) {
                    TableCellRenderer renderer = table.getCellRenderer(row, c);
                    Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(row, c), false, false, row, c);
                    max = Math.max(comp.getPreferredSize().width, max);
                }
                
                return max;
            }
        });
        
        model.setDbSpecificConfig(new DbSpecificConfig("ora"));
    }

    /**
     * This method initializes databaseTypeSelector 
     *  
     * @return javax.swing.JComboBox
     */
    private JComboBox getDatabaseTypeSelector() {
        if (databaseTypeSelector == null) {
            databaseTypeSelector = new JComboBox(new DbTypeSelectorModel());
            databaseTypeSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    model.setDbSpecificConfig((DbSpecificConfig)e.getItem());
                }
            });
        }
        return databaseTypeSelector;
    }
}