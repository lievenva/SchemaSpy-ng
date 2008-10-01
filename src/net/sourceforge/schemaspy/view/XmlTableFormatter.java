package net.sourceforge.schemaspy.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;
import net.sourceforge.schemaspy.util.DOMUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlTableFormatter {
    private static final XmlTableFormatter instance = new XmlTableFormatter();

    /**
     * Singleton...don't allow instantiation
     */
    private XmlTableFormatter() {}

    public static XmlTableFormatter getInstance() {
        return instance;
    }

    /**
     * Append the specified tables to the XML node
     * 
     * @param schemaNode
     * @param tables
     */
    public void appendTables(Element schemaNode, Collection<Table> tables) {
        Set<Table> byName = new TreeSet<Table>(new Comparator<Table>() {
            public int compare(Table table1, Table table2) {
                return table1.getName().compareToIgnoreCase(table2.getName());
            }
        });
        byName.addAll(tables);

        Document document = schemaNode.getOwnerDocument();
        Element tablesNode = document.createElement("tables");
        schemaNode.appendChild(tablesNode);
        for (Table table : byName)
            appendTable(tablesNode, table);
    }
    
    private void appendTable(Element tablesNode, Table table) {
        Document document = tablesNode.getOwnerDocument();
        Element tableNode = document.createElement("table");
        tablesNode.appendChild(tableNode);
        if (table.getId() != null)
            DOMUtil.appendAttribute(tableNode, "id", String.valueOf(table.getId()));
        if (table.getSchema() != null)
            DOMUtil.appendAttribute(tableNode, "schema", table.getSchema());
        DOMUtil.appendAttribute(tableNode, "name", table.getName());
        if (table.getNumRows() != -1)
            DOMUtil.appendAttribute(tableNode, "numRows", String.valueOf(table.getNumRows()));
        DOMUtil.appendAttribute(tableNode, "type", table.isView() ? "VIEW" : "TABLE");
        DOMUtil.appendAttribute(tableNode, "remarks", table.getComments() == null ? "" : table.getComments());
        appendColumns(tableNode, table);
        appendPrimaryKeys(tableNode, table);
        appendIndexes(tableNode, table);
        appendCheckConstraints(tableNode, table);
        appendView(tableNode, table);
    }

    private void appendColumns(Element tableNode, Table table) {
        for (TableColumn column : table.getColumns()) {
            appendColumn(tableNode, column);
        }
    }

    private Node appendColumn(Node tableNode, TableColumn column) {
        Document document = tableNode.getOwnerDocument();
        Node columnNode = document.createElement("column");
        tableNode.appendChild(columnNode);

        DOMUtil.appendAttribute(columnNode, "id", String.valueOf(column.getId()));
        DOMUtil.appendAttribute(columnNode, "name", column.getName());
        DOMUtil.appendAttribute(columnNode, "type", column.getType());
        DOMUtil.appendAttribute(columnNode, "size", String.valueOf(column.getLength()));
        DOMUtil.appendAttribute(columnNode, "digits", String.valueOf(column.getDecimalDigits()));
        DOMUtil.appendAttribute(columnNode, "nullable", String.valueOf(column.isNullable()));
        DOMUtil.appendAttribute(columnNode, "autoUpdated", String.valueOf(column.isAutoUpdated()));
        if (column.getDefaultValue() != null)
            DOMUtil.appendAttribute(columnNode, "defaultValue", String.valueOf(column.getDefaultValue()));
        DOMUtil.appendAttribute(columnNode, "remarks", column.getComments() == null ? "" : column.getComments());

        for (TableColumn childColumn : column.getChildren()) {
            Node childNode = document.createElement("child");
            columnNode.appendChild(childNode);
            ForeignKeyConstraint constraint = column.getChildConstraint(childColumn);
            DOMUtil.appendAttribute(childNode, "foreignKey", constraint.getName());
            DOMUtil.appendAttribute(childNode, "table", childColumn.getTable().getName());
            DOMUtil.appendAttribute(childNode, "column", childColumn.getName());
            DOMUtil.appendAttribute(childNode, "implied", String.valueOf(constraint.isImplied()));
            DOMUtil.appendAttribute(childNode, "onDeleteCascade", String.valueOf(constraint.isOnDeleteCascade()));
        }

        for (TableColumn parentColumn : column.getParents()) {
            Node parentNode = document.createElement("parent");
            columnNode.appendChild(parentNode);
            ForeignKeyConstraint constraint = column.getParentConstraint(parentColumn);
            DOMUtil.appendAttribute(parentNode, "foreignKey", constraint.getName());
            DOMUtil.appendAttribute(parentNode, "table", parentColumn.getTable().getName());
            DOMUtil.appendAttribute(parentNode, "column", parentColumn.getName());
            DOMUtil.appendAttribute(parentNode, "implied", String.valueOf(constraint.isImplied()));
            DOMUtil.appendAttribute(parentNode, "onDeleteCascade", String.valueOf(constraint.isOnDeleteCascade()));
        }

        return columnNode;
    }

    private void appendPrimaryKeys(Element tableNode, Table table) {
        Document document = tableNode.getOwnerDocument();
        int index = 1;

        for (TableColumn primaryKeyColumn : table.getPrimaryColumns()) {
            Node primaryKeyNode = document.createElement("primaryKey");
            tableNode.appendChild(primaryKeyNode);
            
            DOMUtil.appendAttribute(primaryKeyNode, "column", primaryKeyColumn.getName());
            DOMUtil.appendAttribute(primaryKeyNode, "sequenceNumberInPK", String.valueOf(index++));
        }
    }
    
    private void appendCheckConstraints(Element tableNode, Table table) {
        Document document = tableNode.getOwnerDocument();
        Map<String, String> constraints = table.getCheckConstraints();
        if (constraints != null && !constraints.isEmpty()) {
            for (String name : constraints.keySet()) {
                Node constraintNode = document.createElement("checkConstraint");
                tableNode.appendChild(constraintNode);
                DOMUtil.appendAttribute(tableNode, "name", name);
                DOMUtil.appendAttribute(tableNode, "constraint", constraints.get(name).toString());
            }
        }
    }

    private void appendIndexes(Node tableNode, Table table) {
        boolean showId = table.getId() != null;
        Set<TableIndex> indexes = table.getIndexes();
        if (indexes != null && !indexes.isEmpty()) {
            indexes = new TreeSet<TableIndex>(indexes); // sort primary keys first
            Document document = tableNode.getOwnerDocument();

            for (TableIndex index : indexes) {
                Node indexNode = document.createElement("index");
                if (showId)
                    DOMUtil.appendAttribute(indexNode, "id", String.valueOf(index.getId()));
                DOMUtil.appendAttribute(indexNode, "name", index.getName());
                DOMUtil.appendAttribute(indexNode, "unique", String.valueOf(index.isUnique()));
                
                for (TableColumn column : index.getColumns()) {
                    Node columnNode = document.createElement("column");
                    DOMUtil.appendAttribute(columnNode, "name", column.getName());
                    DOMUtil.appendAttribute(columnNode, "ascending", String.valueOf(index.isAscending(column)));
                    indexNode.appendChild(columnNode);
                }
                tableNode.appendChild(indexNode);
            }
        }
    }

    private void appendView(Element tableNode, Table table) {
        String sql;
        if (table.isView() && (sql = table.getViewSql()) != null) {
            DOMUtil.appendAttribute(tableNode, "viewSql", sql);
        }
    }
}