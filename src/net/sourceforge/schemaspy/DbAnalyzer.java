package net.sourceforge.schemaspy;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.ImpliedForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;

public class DbAnalyzer {
    public static List<ImpliedForeignKeyConstraint> getImpliedConstraints(Collection<Table> tables) {
        List<TableColumn> columnsWithoutParents = new ArrayList<TableColumn>();
        Map<TableColumn, Table> allPrimaries = new TreeMap<TableColumn, Table>(new Comparator<TableColumn>() {
            public int compare(TableColumn column1, TableColumn column2) {
                int rc = column1.getName().compareTo(column2.getName());
                if (rc == 0)
                    rc = column1.getType().compareTo(column2.getType());
                if (rc == 0)
                    rc = column1.getLength() - column2.getLength();
                return rc;
            }
        });

        int duplicatePrimaries = 0;

        // gather all the primary key columns and columns without parents
        for (Table table : tables) {
            List<TableColumn> tablePrimaries = table.getPrimaryColumns();
            if (tablePrimaries.size() == 1) { // can't match up multiples...yet...
                for (TableColumn primary : tablePrimaries) {
                    if (allPrimaries.put(primary, table) != null)
                        ++duplicatePrimaries;
                }
            }

            for (TableColumn column : table.getColumns()) {
                if (!column.isForeignKey())
                    columnsWithoutParents.add(column);
            }
        }

        // if more than half of the tables have the same primary key then
        // it's most likey a database where primary key names aren't unique
        // (e.g. they all have a primary key named 'ID')
        if (duplicatePrimaries > allPrimaries.size()) // bizarre logic, but it does approximately what we need
            return new ArrayList<ImpliedForeignKeyConstraint>();

        sortColumnsByTable(columnsWithoutParents);

        List<ImpliedForeignKeyConstraint> impliedConstraints = new ArrayList<ImpliedForeignKeyConstraint>();
        for (TableColumn childColumn : columnsWithoutParents) {
            Table primaryTable = allPrimaries.get(childColumn);
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
    public static List<ForeignKeyConstraint> getForeignKeyConstraints(Collection<Table> tables) {
        List<ForeignKeyConstraint> constraints = new ArrayList<ForeignKeyConstraint>();
        
        for (Table table : tables) {
            constraints.addAll(table.getForeignKeys());
        }

        return constraints;
    }

    public static List<Table> getOrphans(Collection<Table> tables) {
        List<Table> orphans = new ArrayList<Table>();

        for (Table table : tables) {
            if (table.isOrphan(false)) {
                orphans.add(table);
            }
        }

        return sortTablesByName(orphans);
    }

    /**
     * Return a list of <code>TableColumn</code>s that are both nullable
     * and have an index that specifies that they must be unique (a rather strange combo).
     */
    public static List<TableColumn> getMustBeUniqueNullableColumns(Collection<Table> tables) {
        List<TableColumn> uniqueNullables = new ArrayList<TableColumn>();

        for (Table table : tables) {
            for (TableIndex index : table.getIndexes()) {
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
    public static List<Table> getTablesWithoutIndexes(Collection<Table> tables) {
        List<Table> withoutIndexes = new ArrayList<Table>();

        for (Table table : tables) {
            if (!table.isView() && table.getIndexes().size() == 0)
                withoutIndexes.add(table);
        }

        return sortTablesByName(withoutIndexes);
    }

    public static List<Table> getTablesWithIncrementingColumnNames(Collection<Table> tables) {
        List<Table> denormalizedTables = new ArrayList<Table>();

        for (Table table : tables) {
            Map<String, Long> columnPrefixes = new HashMap<String, Long>();

            for (TableColumn column : table.getColumns()) {
                // search for columns that start with the same prefix
                // and end in an incrementing number

                String columnName = column.getName();
                String numbers = null;
                for (int i = columnName.length() - 1; i > 0; --i) {
                    if (Character.isDigit(columnName.charAt(i))) {
                        numbers = String.valueOf(columnName.charAt(i)) + (numbers == null ? "" : numbers);
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
                Long existing = columnPrefixes.get(prefix);
                if (existing != null && Math.abs(existing.longValue() - numeric) == 1) {
                    // found one so add it to our list and stop evaluating this table
                    denormalizedTables.add(table);
                    break;
                }
                columnPrefixes.put(prefix, new Long(numeric));
            }
        }

        return sortTablesByName(denormalizedTables);
    }

    public static List<Table> getTablesWithOneColumn(Collection<Table> tables) {
        List<Table> singleColumnTables = new ArrayList<Table>();

        for (Table table : tables) {
            if (table.getColumns().size() == 1)
                singleColumnTables.add(table);
        }

        return sortTablesByName(singleColumnTables);
    }

    public static List<Table> sortTablesByName(List<Table> tables) {
        Collections.sort(tables, new Comparator<Table>() {
            public int compare(Table table1, Table table2) {
                return table1.getName().compareTo(table2.getName());
            }
        });

        return tables;
    }

    public static List<TableColumn> sortColumnsByTable(List<TableColumn> columns) {
        Collections.sort(columns, new Comparator<TableColumn>() {
            public int compare(TableColumn column1, TableColumn column2) {
                int rc = column1.getTable().getName().compareTo(column2.getTable().getName());
                if (rc == 0)
                    rc = column1.getName().compareTo(column2.getName());
                return rc;
            }
        });

        return columns;
    }

    /**
     * Returns a list of columns that have the word "NULL" or "null" as their default value
     * instead of the likely candidate value null.
     *
     * @param tables Collection
     * @return List
     */
    public static List<TableColumn> getDefaultNullStringColumns(Collection<Table> tables) {
        List<TableColumn> defaultNullStringColumns = new ArrayList<TableColumn>();

        for (Table table : tables) {
            for (TableColumn column : table.getColumns()) {
                Object defaultValue = column.getDefaultValue();
                if (defaultValue != null && defaultValue instanceof String) {
                    String defaultString = defaultValue.toString();
                    if (defaultString.trim().equalsIgnoreCase("null")) {
                        defaultNullStringColumns.add(column);
                    }
                }
            }
        }

        return sortColumnsByTable(defaultNullStringColumns);
    }

    /**
     * getSchemas - returns a List of schema names (Strings)
     *
     * @param meta DatabaseMetaData
     */
    public static List<String> getSchemas(DatabaseMetaData meta) throws SQLException {
        List<String> schemas = new ArrayList<String>();

        ResultSet rs = meta.getSchemas();
        while (rs.next()) {
            schemas.add(rs.getString("TABLE_SCHEM"));
        }
        rs.close();

        return schemas;
    }

    /**
     * getSchemas - returns a List of schema names (Strings) that contain tables
     *
     * @param meta DatabaseMetaData
     */
    public static List<String> getPopulatedSchemas(DatabaseMetaData meta) throws SQLException {
        return getPopulatedSchemas(meta, ".*");
    }

    /**
     * getSchemas - returns a List of schema names (Strings) that contain tables and
     * match the <code>schemaSpec</code> regular expression
     *
     * @param meta DatabaseMetaData
     */
    public static List<String> getPopulatedSchemas(DatabaseMetaData meta, String schemaSpec) throws SQLException {
        Set<String> schemas = new TreeSet<String>(); // alpha sorted
        Pattern schemaRegex = Pattern.compile(schemaSpec);

        Iterator<String> iter = getSchemas(meta).iterator();
        while (iter.hasNext()) {
            String schema = iter.next().toString();
            if (schemaRegex.matcher(schema).matches()) {
                ResultSet rs = null;
                try {
                    rs = meta.getTables(null, schema, "%", null);
                    if (rs.next())
                        schemas.add(schema);
                } catch (SQLException ignore) {
                } finally {
                    if (rs != null)
                        rs.close();
                }
            }
        }

        return new ArrayList<String>(schemas);
    }

    /**
     * For debugging/analyzing result sets
     * @param rs ResultSet
     * @throws SQLException
     */
    public static void dumpResultSetRow(ResultSet rs, String description) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int numColumns = meta.getColumnCount();
        System.out.println(numColumns + " columns of " + description + ":");
        for (int i = 1; i <= numColumns; ++i) {
            System.out.print(meta.getColumnLabel(i));
            System.out.print(": ");
            System.out.print(String.valueOf(rs.getString(i)));
            System.out.print("\t");
        }
        System.out.println();
    }
}
