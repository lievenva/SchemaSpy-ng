package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.Dimension;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;

/**
 * @author John Currier
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JPanel dbConfigPanel = null;
    private JPanel buttonBar = null;
    private JButton launchButton = null;

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
        this.setSize(new Dimension(500, 281));
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * This method initializes dbConfigPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getDbConfigPanel() {
        if (dbConfigPanel == null) {
            dbConfigPanel = new DbConfigPanel();
        }
        return dbConfigPanel;
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getDbConfigPanel(), BorderLayout.NORTH);
            jContentPane.add(getButtonBar(), BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes buttonBar	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getButtonBar() {
        if (buttonBar == null) {
            buttonBar = new JPanel();
            buttonBar.setLayout(new FlowLayout(FlowLayout.TRAILING));
            buttonBar.add(getLaunchButton(), null);
        }
        return buttonBar;
    }

    /**
     * This method initializes launchButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getLaunchButton() {
        if (launchButton == null) {
            launchButton = new JButton();
            launchButton.setText("Launch");
        }
        return launchButton;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
