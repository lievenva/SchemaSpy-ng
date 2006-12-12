package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author John Currier
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JPanel dbConfigPanel = null;

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
        }
        return jContentPane;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
