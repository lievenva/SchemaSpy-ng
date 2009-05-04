package test.net.sourceforge.schemaspy.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import net.sourceforge.schemaspy.util.Version;

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

    public void testExtract2203() {
        compareEquals("2.20.3", "dot - Graphviz version 2.20.3 (Wed Oct  8 06:02:12 UTC 2008");
    }

    public void testExtractMac2222() {
        compareEquals("2.22.2", "dot - graphviz version 2.22.2 (20090313.1817)");
    }
    
    private void compareEquals(String digits, String versionLine)
    {
        Version expected = new Version(digits);
        String actual = getVersionDigits(versionLine);
        assertEquals(expected, new Version(actual));
    }

    private String getVersionDigits(String versionLine) {
        Matcher matcher = Pattern.compile("[0-9][0-9.]+").matcher(versionLine);
        if (matcher.find())
            return matcher.group();
        fail("Failed to extract version digits from " + versionLine);
        return null;
    }
}
