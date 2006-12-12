package test.net.sourceforge.schemaspy.util;

import junit.framework.*;
import net.sourceforge.schemaspy.util.*;

/**
 * @author John Currier
 */
public class VersionTest extends TestCase {
    private Version twoNineOne = new Version("2.9.1");
    private Version twoTen = new Version("2.10");
    private Version twoTenOne = new Version("2.10.1");
    
    public void testCompareTo() {
        assertTrue(twoNineOne.compareTo(twoTen) < 0);
        assertTrue(twoTen.compareTo(twoTen) == 0);
        assertTrue(twoTenOne.compareTo(twoTen) > 0);
        
        assertTrue(twoNineOne.compareTo(twoTenOne) < 0);
        assertTrue(twoTen.compareTo(twoTenOne) < 0);
        assertTrue(twoTenOne.compareTo(twoTenOne) == 0);

        assertTrue(twoNineOne.compareTo(twoNineOne) == 0);
        assertTrue(twoTen.compareTo(twoNineOne) > 0);
        assertTrue(twoTenOne.compareTo(twoNineOne) > 0);
    }

    public void testEquals() {
        assertTrue(twoTen.equals(twoTen));
    }
    
    public void testNotEquals() {
        assertFalse(twoTenOne.equals(twoTen));
    }

}
