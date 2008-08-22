package net.sourceforge.schemaspy.model.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author John Currier
 */
public class ForeignKeyMeta {
    private final String tableName;
    private final String columnName;
    private final String remoteSchema;
    
    ForeignKeyMeta(Node foreignKeyNode) {
        NamedNodeMap attribs = foreignKeyNode.getAttributes();
        Node node = attribs.getNamedItem("table");
        if (node == null)
            throw new IllegalStateException("XML foreignKey definition requires 'table' attribute");
        tableName = node.getNodeValue();
        attribs.removeNamedItem("table");
        node = attribs.getNamedItem("column");
        if (node == null)
            throw new IllegalStateException("XML foreignKey definition requires 'column' attribute");
        columnName = node.getNodeValue();
        attribs.removeNamedItem("column");
        node = attribs.getNamedItem("remoteSchema");
        if (node != null) {
            remoteSchema = node.getNodeValue();
            attribs.removeNamedItem("remoteSchema");
        } else {
            remoteSchema = null;
        }
        
        for (int i = 0; i < attribs.getLength(); ++i) {
            System.err.println("Unrecognized attribute '" + attribs.item(i).getNodeName() + "' in XML definition of foreignKey");
        }
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public String getRemoteSchema() {
        return remoteSchema;
    }
    
    @Override
    public String toString() {
        return tableName + '.' + columnName;
    }
}