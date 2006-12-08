package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.sourceforge.schemaspy.*;
import net.sourceforge.schemaspy.util.*;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

/**
 * @author John Currier
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JLabel databaseTypeLabel = null;
    private JComboBox databaseTypeSelector = null;
    private JLabel outputDirectoryLabel = null;
    private JLabel outputDirectory = null;
    private JButton outputDirectoryButton = null;
    private JFileChooser outputDirectoryChooser = null;
    private JPanel dbOptionsHolder = null;

    /**
     * This is the default constructor
     */
    public MainFrame() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setTitle("SchemaSpy");
        this.setSize(new Dimension(500, 250));
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridwidth = 2;
            gridBagConstraints1.gridy = 2;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 2;
            gridBagConstraints6.gridy = 0;
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridx = 1;
            gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints5.insets = new Insets(0, 2, 0, 2);
            gridBagConstraints5.weighty = 0.0;
            gridBagConstraints5.gridy = 0;
            outputDirectory = new JLabel();
            outputDirectory.setText("/");
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.anchor = GridBagConstraints.WEST;
            gridBagConstraints3.insets = new Insets(0, 2, 0, 2);
            gridBagConstraints3.gridy = 0;
            outputDirectoryLabel = new JLabel();
            outputDirectoryLabel.setText("Output Directory:");
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.fill = GridBagConstraints.BOTH;
            gridBagConstraints2.gridy = 1;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.insets = new Insets(0, 2, 0, 2);
            gridBagConstraints2.gridx = 1;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.insets = new Insets(0, 2, 0, 2);
            gridBagConstraints.gridy = 1;
            databaseTypeLabel = new JLabel();
            databaseTypeLabel.setText("Database Type:");
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(databaseTypeLabel, gridBagConstraints);
            jContentPane.add(getDatabaseTypeSelector(), gridBagConstraints2);
            jContentPane.add(outputDirectoryLabel, gridBagConstraints3);
            jContentPane.add(outputDirectory, gridBagConstraints5);
            jContentPane.add(getOutputDirectoryButton(), gridBagConstraints6);
            jContentPane.add(getDbOptionsHolder(), gridBagConstraints1);
        }
        return jContentPane;
    }

    /**
     * This method initializes databaseTypeSelector	
     * 	
     * @return javax.swing.JComboBox	
     */
    private JComboBox getDatabaseTypeSelector() {
        if (databaseTypeSelector == null) {
            databaseTypeSelector = new JComboBox();
            databaseTypeSelector.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    JPanel holder = getDbOptionsHolder();
                    holder.removeAll();
                    holder.add(new DbSpecificOptions((ConnectionURLBuilder)e.getItem()));
                    MainFrame.this.validate();
                }
            });
            Set datatypes = Config.getBuiltInDatabaseTypes(Config.getLoadedFromJar());
            for (Iterator iter = datatypes.iterator(); iter.hasNext(); ) {
                String dbType = iter.next().toString();
                databaseTypeSelector.addItem(new ConnectionURLBuilder(dbType));
            }
        }
        return databaseTypeSelector;
    }

    /**
     * This method initializes outputDirectoryButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getOutputDirectoryButton() {
        if (outputDirectoryButton == null) {
            outputDirectoryButton = new JButton();
            outputDirectoryButton.setText("...");
            outputDirectoryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getOutputDirectoryChooser().showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                        outputDirectory.setText(getOutputDirectoryChooser().getSelectedFile().getPath());
                    }
                }
            });
        }
        return outputDirectoryButton;
    }
    
    private JFileChooser getOutputDirectoryChooser() {
        if (outputDirectoryChooser == null) {
            outputDirectoryChooser = new JFileChooser();
            outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        return outputDirectoryChooser;
    }

    /**
     * This method initializes dbOptionsHolder	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getDbOptionsHolder() {
        if (dbOptionsHolder == null) {
            dbOptionsHolder = new JPanel();
            dbOptionsHolder.setLayout(new BorderLayout());
        }
        return dbOptionsHolder;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
