package net.sourceforge.schemaspy.model.xml;

import java.util.logging.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Additional metadata about a foreign key relationship as expressed in XML
 * instead of from the database.
 *
 * @author John Currier
 */
public class ForeignKeyMeta {
    private final String tableName;
    private final String columnName;
    private final String remoteSchema;
    private final static Logger logger = Logger.getLogger(ForeignKeyMeta.class.getName());

    ForeignKeyMeta(Node foreignKeyNode) {
        NamedNodeMap attribs = foreignKeyNode.getAttributes();
        Node node = attribs.getNamedItem("table");
        if (node == null)
            throw new IllegalStateException("XML foreignKey definition requires 'table' attribute");
        tableName = node.getNodeValue();
        node = attribs.getNamedItem("column");
        if (node == null)
            throw new IllegalStateException("XML foreignKey definition requires 'column' attribute");
        columnName = node.getNodeValue();
        node = attribs.getNamedItem("remoteSchema");
        if (node != null) {
            remoteSchema = node.getNodeValue();
        } else {
            remoteSchema = null;
        }

        logger.finer("Found XML FK metadata for " + tableName + "." + columnName +
                " remoteSchema: " + remoteSchema);
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