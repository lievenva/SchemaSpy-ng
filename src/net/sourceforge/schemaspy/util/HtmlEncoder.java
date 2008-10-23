package net.sourceforge.schemaspy.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple (i.e. 'stupid') class that does a simple mapping between
 * HTML characters and their 'encoded' equivalents.
 * 
 * @author John Currier
 */
public class HtmlEncoder {
    private static final Map<String, String> map = new HashMap<String, String>();
    
    static {
        map.put("<", "&lt;");
        map.put(">", "&gt;");
        map.put("\n", "<br>" + System.getProperty("line.separator"));
        map.put("\r", "");
    }
   
    private HtmlEncoder() {}
    
    /**
     * Returns an HTML-encoded equivalent of the specified character.
     * 
     * @param ch
     * @return
     */
    public static String encodeToken(char ch) {
        return encodeToken(String.valueOf(ch));
    }
    
    /**
     * Returns an HTML-encoded equivalent of the specified tokenized string,
     * where tokens such as '<', '>', '\n' and '\r' have been isolated from
     * other tokens.
     * 
     * @param str
     * @return
     */
    public static String encodeToken(String str) {
        String result = map.get(str);
        return (result == null) ? str : result;
    }
    
    /**
     * Returns an HTML-encoded version of the specified string
     * 
     * @param str
     * @return
     */
    public static String encodeString(String str) {
        int len = str.length();
    	StringBuilder buf = new StringBuilder(len * 2); // x2 should limit # of reallocs
    	for (int i = 0; i < len; i++) {
			buf.append(encodeToken(str.charAt(i)));
		}
    	return buf.toString();
    }
}
