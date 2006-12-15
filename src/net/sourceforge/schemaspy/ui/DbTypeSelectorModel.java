package net.sourceforge.schemaspy.ui;

import java.util.*;
import javax.swing.*;
import net.sourceforge.schemaspy.*;
import net.sourceforge.schemaspy.util.*;

/**
 * @author John Currier
 */
public class DbTypeSelectorModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;
    private List dbConfigs = new ArrayList();
    private Object selected;
    
    public DbTypeSelectorModel() {
        Set dbTypes = new TreeSet(Config.getBuiltInDatabaseTypes(Config.getLoadedFromJar()));
        for (Iterator iter = dbTypes.iterator(); iter.hasNext(); ) {
            String dbType = iter.next().toString();
            dbConfigs.add(new DbSpecificConfig(dbType));
        }
        
        selected = dbConfigs.size() > 0 ? dbConfigs.get(0) : null;
    }

    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#getSelectedItem()
     */
    public Object getSelectedItem() {
        return selected;
    }

    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
     */
    public void setSelectedItem(Object anItem) {
        selected = anItem;
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int index) {
        return dbConfigs.get(index);
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getSize()
     */
    public int getSize() {
        return dbConfigs.size();
    }
}

