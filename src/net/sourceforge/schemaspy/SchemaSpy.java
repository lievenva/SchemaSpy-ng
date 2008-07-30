package net.sourceforge.schemaspy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;

public class SchemaSpy {
    private final Database database;

    public SchemaSpy(Connection connection, DatabaseMetaData meta, String dbName, String schema, String description, Properties properties, Pattern include, int maxThreads) throws SQLException {
        database = new Database(connection, meta, dbName, schema, description, properties, include, maxThreads);
    }

    public Database getDatabase() {
        return database;
    }

    /**
     * Returns a list of <code>Table</code>s ordered such that parents are listed first
     * and child tables are listed last.
     * 
     * <code>recursiveConstraints</code> gets populated with <code>TableConstraint</code>s
     * that had to be removed to resolve the returned list.
     * @param recursiveConstraints
     * @return
     */
    public List<Table> sortTablesByRI(Collection<ForeignKeyConstraint> recursiveConstraints) {
        List<Table> heads = new ArrayList<Table>();
        List<Table> tails = new ArrayList<Table>();
        List<Table> remainingTables = new ArrayList<Table>(getDatabase().getTables());
        List<Table> unattached = new ArrayList<Table>();

        // first pass to gather the 'low hanging fruit'
        for (Iterator<Table> iter = remainingTables.iterator(); iter.hasNext(); ) {
            Table table = iter.next();
            if (table.isLeaf() && table.isRoot()) {
                unattached.add(table);
                iter.remove();
            }
        }

        unattached = sortTrimmedLevel(unattached);

        while (!remainingTables.isEmpty()) {
            int tablesLeft = remainingTables.size();
            tails.addAll(0, trimLeaves(remainingTables));
            heads.addAll(trimRoots(remainingTables));

            // if we could't trim anything then there's recursion....
            // resolve it by removing a constraint, one by one, 'till the tables are all trimmed
            if (tablesLeft == remainingTables.size()) {
                boolean foundSimpleRecursion = false;
                for (Table potentialRecursiveTable : remainingTables) {
                    ForeignKeyConstraint recursiveConstraint = potentialRecursiveTable.removeSelfReferencingConstraint();
                    if (recursiveConstraint != null) {
                        recursiveConstraints.add(recursiveConstraint);
                        foundSimpleRecursion = true;
                    }
                }

                if (!foundSimpleRecursion) {
                    // expensive comparison, but we're down to the end of the tables so it shouldn't really matter
                    Set<Table> byParentChildDelta = new TreeSet<Table>(new Comparator<Table>() {
                        // sort on the delta between number of parents and kids so we can
                        // target the tables with the biggest delta and therefore the most impact
                        // on reducing the smaller of the two
                        public int compare(Table table1, Table table2) {
                            int rc = Math.abs(table2.getNumChildren() - table2.getNumParents()) - Math.abs(table1.getNumChildren() - table1.getNumParents());
                            if (rc == 0)
                                rc = table1.getName().compareTo(table2.getName());
                            return rc;
                        }
                    });
                    byParentChildDelta.addAll(remainingTables);
                    Table recursiveTable = byParentChildDelta.iterator().next(); // this one has the largest delta
                    ForeignKeyConstraint removedConstraint = recursiveTable.removeAForeignKeyConstraint();
                    recursiveConstraints.add(removedConstraint);
                }
            }
        }

        // we've gathered all the heads and tails, so combine them here moving 'unattached' tables to the end
        List<Table> ordered = new ArrayList<Table>(heads.size() + tails.size());
        
        ordered.addAll(heads);
        heads = null; // allow gc ASAP

        ordered.addAll(tails);
        tails = null; // allow gc ASAP

        ordered.addAll(unattached);

        return ordered;
    }

    private static List<Table> trimRoots(List<Table> tables) {
        List<Table> roots = new ArrayList<Table>();

        Iterator<Table> iter = tables.iterator();
        while (iter.hasNext()) {
            Table root = iter.next();
            if (root.isRoot()) {
                roots.add(root);
                iter.remove();
            }
        }

        // now sort them so the ones with large numbers of children show up first (not required, but cool)
        roots = sortTrimmedLevel(roots);
        iter = roots.iterator();
        while (iter.hasNext()) {
            // do this after the previous loop to prevent getting roots before they're ready
            // and so we can sort them correctly
            iter.next().unlinkChildren();
        }

        return roots;
    }

    private static List<Table> trimLeaves(List<Table> tables) {
        List<Table> leaves = new ArrayList<Table>();

        Iterator<Table> iter = tables.iterator();
        while (iter.hasNext()) {
            Table leaf = iter.next();
            if (leaf.isLeaf()) {
                leaves.add(leaf);
                iter.remove();
            }
        }

        // now sort them so the ones with large numbers of children show up first (not required, but cool)
        leaves = sortTrimmedLevel(leaves);
        iter = leaves.iterator();
        while (iter.hasNext()) {
            // do this after the previous loop to prevent getting leaves before they're ready
            // and so we can sort them correctly
            iter.next().unlinkParents();
        }

        return leaves;
    }

    /**
     * this doesn't change the logical output of the program because all of these (leaves or roots) are at the same logical level
     */
    private static List<Table> sortTrimmedLevel(List<Table> tables) {
        /**
         * order by
         * <ul>
         *  <li>number of kids (descending)
         *  <li>number of parents (ascending)
         *  <li>alpha name (ascending)
         * </ul>
         */
        final class TrimComparator implements Comparator<Table> {
            public int compare(Table table1, Table table2) {
                // have to keep track of and use the 'max' versions because
                // by the time we get here we'll (probably?) have no parents or children
                int rc = table2.getMaxChildren() - table1.getMaxChildren();
                if (rc == 0)
                    rc = table1.getMaxParents() - table2.getMaxParents();
                if (rc == 0)
                    rc = table1.getName().compareTo(table2.getName());
                return rc;
            }
        }

        Set<Table> sorter = new TreeSet<Table>(new TrimComparator());
        sorter.addAll(tables);
        return new ArrayList<Table>(sorter);
    }
}