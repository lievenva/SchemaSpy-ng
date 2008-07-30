package net.sourceforge.schemaspy.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.util.DbSpecificConfig;

/**
 * @author John Currier
 */
public class DbTypeSelectorModel extends AbstractListModel implements ComboBoxModel {
    private static final long serialVersionUID = 1L;
    private List<DbSpecificConfig> dbConfigs = new ArrayList<DbSpecificConfig>();
    private Object selected;
    
    public DbTypeSelectorModel(String defaultType) {
        Pattern pattern = Pattern.compile(".*/" + defaultType);
        Set<String> dbTypes = new TreeSet<String>(Config.getBuiltInDatabaseTypes(Config.getLoadedFromJar()));
        for (String dbType : dbTypes) {
            DbSpecificConfig config = new DbSpecificConfig(dbType);
            dbConfigs.add(config);
            
            if (pattern.matcher(dbType).matches()) {
                setSelectedItem(config);
            }
        }

        if (getSelectedItem() == null && dbConfigs.size() > 0)
            setSelectedItem(dbConfigs.get(0));
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

