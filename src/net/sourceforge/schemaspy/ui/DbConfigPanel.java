package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.sourceforge.schemaspy.*;
import net.sourceforge.schemaspy.util.*;


/**
 * @author John Currier
 */
public class DbConfigPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JComboBox databaseTypeSelector;
    private JTextField user;
    private JTextField password;
    private JTextField outputDirectory;
    private JButton outputDirectoryButton;
    private JFileChooser outputDirectoryChooser;

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
        setLayout(new GridBagLayout());
        int y = 0;
        
        GridBagConstraints constraints = getDefaultConstraints();
        constraints.gridx = 0;
        constraints.gridy = y;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("Database type:"), constraints);
        
        constraints = getDefaultConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = y;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        add(getDatabaseTypeSelector(), constraints);

        
        constraints = getDefaultConstraints();
        constraints.gridx = 0;
        constraints.gridy = ++y;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("User:"), constraints);
        
        constraints = getDefaultConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = y;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        add(getUser(), constraints);

        
        constraints = getDefaultConstraints();
        constraints.gridx = 0;
        constraints.gridy = ++y;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), constraints);
        
        constraints = getDefaultConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = y;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        add(getPassword(), constraints);

        
        constraints = getDefaultConstraints();
        constraints.gridx = 0;
        constraints.gridy = ++y;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("Output dir:"), constraints);
        
        constraints = getDefaultConstraints();
        constraints.gridx = 1;
        constraints.gridy = y;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(getDirectoryHolder(), constraints);
    }

    private JPanel getDirectoryHolder() {
        GridBagConstraints constraints;
        JPanel directoryHolder = new JPanel();
        directoryHolder.setLayout(new GridBagLayout());
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        directoryHolder.add(getOutputDirectory(), constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        directoryHolder.add(getOutputDirectoryButton(), constraints);
        return directoryHolder;
    }

    private JTextField getOutputDirectory() {
        if (outputDirectory == null) {
            outputDirectory = new JTextField();
            outputDirectory.setText("/");
        }
        return outputDirectory;
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
                    setDbSpecificConfig((DbSpecificConfig)e.getItem());
                }
            });
            Set datatypes = Config.getBuiltInDatabaseTypes(Config.getLoadedFromJar());
            for (Iterator iter = datatypes.iterator(); iter.hasNext(); ) {
                String dbType = iter.next().toString();
                databaseTypeSelector.addItem(new DbSpecificConfig(dbType));
            }
        }
        return databaseTypeSelector;
    }
    
    private GridBagConstraints getDefaultConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        return constraints;
    }
    
    private void setDbSpecificConfig(DbSpecificConfig dbConfig) {
        Component[] components = getComponents();
        GridBagLayout layout = (GridBagLayout)getLayout();

        for (int i = 0; i < components.length; ++i) {
            Component component = components[i];
            GridBagConstraints c = layout.getConstraints(component);
            if (c.gridy >= 20) {
                remove(component);
            }
        }
        
        GridBagConstraints constraints = getDefaultConstraints();;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridy = 20;
        
        for (Iterator iter = dbConfig.getOptions().iterator(); iter.hasNext(); ) {
            DbSpecificOption option = (DbSpecificOption)iter.next();
            constraints.gridy++;

            constraints.gridx = 0;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel nameLabel = new JLabel(option.getName() + ":");
            nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(nameLabel, constraints);
            
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JTextField valueField = new JTextField(option.getValue());
            if (option.getDescription() != null)
                valueField.setToolTipText(option.getDescription());
            add(valueField, constraints);
        }

        if (getParent() != null)
            getParent().validate();
    }

    /**
     * This method initializes outputDirectoryButton    
     *  
     * @return javax.swing.JButton  
     */
    private JButton getOutputDirectoryButton() {
        if (outputDirectoryButton == null) {
            outputDirectoryButton = new JButton() {
                private static final long serialVersionUID = 1L;

                public Dimension getPreferredSize() {
                    int dim = getOutputDirectory().getPreferredSize().height;
                    return new Dimension(((dim * 7 / 8 + 1) / 2) * 2, dim);
                }
            };
            outputDirectoryButton.setText("...");
            outputDirectoryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getOutputDirectoryChooser().showOpenDialog(DbConfigPanel.this.getParent()) == JFileChooser.APPROVE_OPTION) {
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

    private JTextField getUser() {
        if (user == null) {
            user = new JTextField();
        }
        return user;
    }
    
    private JTextField getPassword() {
        if (password == null) {
            password = new JPasswordField();
        }
        return password;
    }
}