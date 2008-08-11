package net.sourceforge.schemaspy.model.xml;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author John Currier
 */
public class TableColumnMeta {
    private final String name;
    private final String comments;
    private final boolean isPrimary;
    private final List<ForeignKeyMeta> foreignKeys = new ArrayList<ForeignKeyMeta>();
    
    TableColumnMeta(Node colNode) {
        NamedNodeMap attribs = colNode.getAttributes();
        String tmp;
        
        name = attribs.getNamedItem("name").getNodeValue();
        Node commentNode = attribs.getNamedItem("comments");
        if (commentNode != null) {
            tmp = commentNode.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }
        
        Node primaryKeyNode = attribs.getNamedItem("primaryKey");
        if (primaryKeyNode != null) {
            tmp = primaryKeyNode.getNodeValue().trim().toLowerCase();
            isPrimary = tmp.equals("true") || tmp.equals("yes") || tmp.equals("1");
        } else {
            isPrimary = false;
        }
        
        NodeList fkNodes = ((Element)colNode.getChildNodes()).getElementsByTagName("foreignKey");
        
        for (int i = 0; i < fkNodes.getLength(); ++i) {
            Node fkNode = fkNodes.item(i);
            foreignKeys.add(new ForeignKeyMeta(fkNode));
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getComments() {
        return comments;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public List<ForeignKeyMeta> getForeignKeys() {
        return foreignKeys;
    }
}