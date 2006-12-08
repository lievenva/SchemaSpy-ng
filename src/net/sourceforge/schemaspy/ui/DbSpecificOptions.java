package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import net.sourceforge.schemaspy.util.*;
import net.sourceforge.schemaspy.util.ConnectionURLBuilder.*;

/**
 * @author John Currier
 */
public class DbSpecificOptions extends JPanel {
    private static final long serialVersionUID = 1L;
    private final ConnectionURLBuilder dbType;

    /**
     * This is the default constructor
     */
    public DbSpecificOptions() {
        super();
        dbType = null;
        initialize();
    }
    
    public DbSpecificOptions(ConnectionURLBuilder dbType) {
        super();
        this.dbType = dbType;
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        setSize(300, 200);
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        
        for (Iterator iter = dbType.getOptions().iterator(); iter.hasNext(); ) {
            DbOption option = (DbOption)iter.next();
            constraints.gridy++;
            constraints.gridx = 0;
            add(new JLabel(option.name), constraints);
            constraints.gridx = 1;
            add(new JLabel(option.description), constraints);
        }
    }
}