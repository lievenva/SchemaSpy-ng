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
    
    public static String encodeString(String str) {
        int len = str.length();
    	StringBuilder s = new StringBuilder(len * 2);
    	for (int i = 0; i < len; i++) {
			s.append(encodeToken(str.charAt(i)));
		}
    	return s.toString();
    }
}
