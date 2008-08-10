package net.sourceforge.schemaspy.model.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author John Currier
 */
public class ForeignKeyMeta {
    private final String tableName;
    private final String columnName;
    
    ForeignKeyMeta(Node foreignKeyNode) {
        NamedNodeMap attribs = foreignKeyNode.getAttributes();
        
        tableName = attribs.getNamedItem("table").getNodeValue();
        columnName = attribs.getNamedItem("column").getNodeValue();
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    @Override
    public String toString() {
        return tableName + '.' + columnName;
    }
}