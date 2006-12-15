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
        
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                TableColumn paramColumn = table.getColumnModel().getColumn(0);
                paramColumn.setPreferredWidth(UiUtils.getPreferredColumnWidth(table, paramColumn) + 4);
                paramColumn.setMaxWidth(paramColumn.getPreferredWidth());
                table.sizeColumnsToFit(0);
            }
        });
        
        setLayout(new BorderLayout());
        JScrollPane scroller = new JScrollPane(table);
        scroller.setViewportBorder(null);
        add(scroller, BorderLayout.CENTER);
        
        add(getDatabaseTypeSelector(), BorderLayout.NORTH);
    }

    /**
     * This method initializes databaseTypeSelector 
     *  
     * @return javax.swing.JComboBox
     */
    private JComboBox getDatabaseTypeSelector() {
        if (databaseTypeSelector == null) {
            DbTypeSelectorModel selectorModel = new DbTypeSelectorModel("ora");
            databaseTypeSelector = new JComboBox(selectorModel);
            databaseTypeSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt) {
                    if (evt.getStateChange() == ItemEvent.SELECTED)
                        model.setDbSpecificConfig((DbSpecificConfig)evt.getItem());
                }
            });
            
            DbSpecificConfig selected = (DbSpecificConfig)selectorModel.getSelectedItem();
            if (selected != null)
                model.setDbSpecificConfig(selected);
        }
        return databaseTypeSelector;
    }
}