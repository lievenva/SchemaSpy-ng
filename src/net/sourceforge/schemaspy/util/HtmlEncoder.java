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
    
    public static String encode(char ch) {
        return encode(String.valueOf(ch));
    }
    
    public static String encode(String str) {
        String result = map.get(str);
        return (result == null) ? str : result;
    }
}
