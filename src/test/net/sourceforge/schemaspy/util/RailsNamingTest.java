/*
 * $Id$
 */
package test.net.sourceforge.schemaspy.util;

import junit.framework.TestCase;
import net.sourceforge.schemaspy.util.Inflection;

/**
 * Simple tests to try out Ruby On Rails naming convention detection techniques.
 * 
 * @author John Currier
 */
public class RailsNamingTest extends TestCase {
    /**
     * Test Rails naming convention conversion for 'table_id' to 'tables'
     */
    public void testPluralize() {
        // given column name should ref expected table (based on RoR conventions)
        String columnName = "vaccine_id";
        String expectedTableName = "vaccines";
        
        String singular = columnName.substring(0, columnName.length() - 3);
        String primaryTableName = Inflection.pluralize(singular);
        
        assertEquals(expectedTableName, primaryTableName);
    }

    /**
     * Test Rails naming convention conversion for multi-word tables
     */
    public void testPluralizeMultiWordTable() {
        // given column name should ref expected table (based on RoR conventions)
        String columnName = "active_ingredient_id";
        String expectedTableName = "active_ingredients";
        
        String singular = columnName.substring(0, columnName.length() - 3);
        String primaryTableName = Inflection.pluralize(singular);
        
        assertEquals(expectedTableName, primaryTableName);
    }
}