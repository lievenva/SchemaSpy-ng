package net.sourceforge.schemaspy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.schemaspy.model.ImpliedForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;

public class DBAnalyzer {
    public static List getImpliedConstraints(Collection tables) throws SQLException {
	List columnsWithoutParents = new ArrayList();
	Map allPrimaries = new TreeMap(new Comparator() {
	    public int compare(Object object1, Object object2) {
		TableColumn column1 = (TableColumn)object1;
		TableColumn column2 = (TableColumn)object2;
                int rc = column1.getName().compareTo(column2.getName());
                if (rc == 0)
                    rc = column1.getType().compareTo(column2.getType());
                if (rc == 0)
                    rc = column1.getLength() - column2.getLength();
                return rc;
	    }
	});

        // gather all the primary key columns and columns without parents
	for (Iterator iter = tables.iterator(); iter.hasNext(); ) {
	    Table table = (Table)iter.next();
	    List tablePrimaries = table.getPrimaryColumns();
            if (tablePrimaries.size() == 1) { // can't match up multiples...yet...
                for (Iterator primariesIter = tablePrimaries.iterator(); primariesIter.hasNext(); ) {
                    allPrimaries.put(primariesIter.next(), table);
                }
            }

	    for (Iterator columnIter = table.getColumns().iterator(); columnIter.hasNext(); ) {
		TableColumn column = (TableColumn)columnIter.next();
		if (column.getParents().isEmpty() /* && !tablePrimaries.contains(column) */)
		    columnsWithoutParents.add(column);
	    }
	}

	sortColumnsByTable(columnsWithoutParents);

	List impliedConstraints = new ArrayList();
	for (Iterator iter = columnsWithoutParents.iterator(); iter.hasNext(); ) {
	    TableColumn childColumn = (TableColumn)iter.next();
	    Table primaryTable = (Table)allPrimaries.get(childColumn);
	    if (primaryTable != null && primaryTable != childColumn.getTable()) {
                TableColumn parentColumn = primaryTable.getColumn(childColumn.getName());
                // make sure the potential child->parent relationships isn't already a
                // parent->child relationship
                if (parentColumn.getParentConstraint(childColumn) == null) {
                    // ok, we've found a potential relationship with a column matches a primary
                    // key column in another table and isn't already related to that column
                    impliedConstraints.add(new ImpliedForeignKeyConstraint(parentColumn, childColumn));
                }
	    }
	}

	return impliedConstraints;
    }


    /**
     * Returns a <code>List</code> of all of the <code>ForeignKeyConstraint</code>s
     * used by the specified tables.
     *
     * @param tables Collection
     * @return List
     */
    public static List getForeignKeyConstraints(Collection tables) {
        List constraints = new ArrayList();
        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            constraints.addAll(table.getForeignKeys());
        }

        return constraints;
    }

    /**
     * Return a list of <code>TableColumn</code>s that are both nullable
     * and have an index that specifies that they must be unique (a rather strange combo).
     */
    public static List getMustBeUniqueNullableColumns(Collection tables) {
	List uniqueNullables = new ArrayList();

	for (Iterator tablesIter = tables.iterator(); tablesIter.hasNext(); ) {
	    Table table = (Table)tablesIter.next();
	    for (Iterator indexesIter = table.getIndexes().iterator(); indexesIter.hasNext(); ) {
		TableIndex index = (TableIndex)indexesIter.next();
		if (index.isUniqueNullable()) {
		    uniqueNullables.addAll(index.getColumns());
		}
	    }
	}

	return sortColumnsByTable(uniqueNullables);
    }

    /**
     * Return a list of <code>Table</code>s that have neither an index nor a primary key.
     */
    public static List getTablesWithoutIndexes(Collection tables) {
        Set withoutIndexes = new TreeSet(new Comparator() {
            public int compare(Object object1, Object object2) {
                return ((Table)object1).getName().compareTo(((Table)object2).getName());
            }
        });

        for (Iterator tablesIter = tables.iterator(); tablesIter.hasNext(); ) {
            Table table = (Table)tablesIter.next();
            if (!table.isView() && table.getIndexes().size() == 0)
                withoutIndexes.add(table);
        }

        return new ArrayList(withoutIndexes);
    }

    public static List getTablesWithIncrementingColumnNames(Collection tables) {
        List denormalizedTables = new ArrayList();

        Iterator tableIter = tables.iterator();
        while (tableIter.hasNext()) {
            Table table = (Table)tableIter.next();
            Map columnPrefixes = new HashMap();

            Iterator columnIter = table.getColumns().iterator();
            while (columnIter.hasNext()) {
                // search for columns that start with the same prefix
                // and end in an incrementing number
                TableColumn column = (TableColumn)columnIter.next();

                String columnName = column.getName();
                String numbers = null;
                for (int i = columnName.length() - 1; i > 0; --i) {
                    if (Character.isDigit(columnName.charAt(i))) {
                        numbers = String.valueOf(columnName.charAt(i)) + numbers == null ? "" : numbers;
                    } else {
                        break;
                    }
                }

                // attempt to detect where they had an existing column
                // and added a "column2" type of column (we'll call this one "1")
                if (numbers == null) {
                    numbers = "1";
                    columnName = columnName + numbers;
                }

                // see if we've already found a column with the same prefix
                // that had a numeric suffix +/- 1.
                String prefix = columnName.substring(0, columnName.length() - numbers.length());
                long numeric = Long.parseLong(numbers);
                Long existing = (Long)columnPrefixes.get(prefix);
                if (existing != null && Math.abs(existing.longValue() - numeric) == 1) {
                    // found one so add it to our list and stop evaluating this table
                    denormalizedTables.add(table);
                    break;
                }
                columnPrefixes.put(prefix, new Long(numeric));
            }
        }

        return denormalizedTables;
    }

    public static List sortColumnsByTable(List columns) {
	Collections.sort(columns, new Comparator() {
	    public int compare(Object object1, Object object2) {
		TableColumn column1 = (TableColumn)object1;
		TableColumn column2 = (TableColumn)object2;
		int rc = column1.getTable().getName().compareTo(column2.getTable().getName());
		if (rc == 0)
		    rc = column1.getName().compareTo(column2.getName());
		return rc;
	    }
	});

	return columns;
    }
}
