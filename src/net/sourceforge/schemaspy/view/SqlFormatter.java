package net.sourceforge.schemaspy.view;

import java.util.Set;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;

/**
 * Implementations of this interface know how to take SQL and format it
 * into (hopefully) readable HTML.
 *
 * @author John Currier
 */
public interface SqlFormatter {
    /**
     * Return a HTML-formatted representation of the specified SQL.
     *
     * @param sql SQL to be formatted
     * @param db Database
     * @param references set of tables referenced by this SQL
     *      (populated by the formatter) or left empty if the formatter already
     *      provides references to those tables
     * @return HTML-formatted representation of the specified SQL
     */
    String format(String sql, Database db, Set<Table> references);
}
