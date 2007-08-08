package net.sourceforge.schemaspy.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link HashMap} implementation that uses {@link String}s as its keys
 * where the keys are treated without regard to case.  That is, <code>get("MyTableName")</code>
 * will return the same object as <code>get("MYTABLENAME")</code>.
 * 
 * @author John Currier
 */
public class CaseInsensitiveMap extends HashMap
{
    private static final long serialVersionUID = 1L;

    public Object get(Object key) {
        return super.get(((String)key).toUpperCase());
    }
    
    public Object put(Object key, Object value) {
        return super.put(((String)key).toUpperCase(), value);
    }

    public void putAll(Map map) {
        Iterator iter = map.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }
    
    public Object remove(Object key) {
        return super.remove(((String)key).toUpperCase());
    }
    
    public boolean containsKey(Object key) {
        return super.containsKey(((String)key).toUpperCase());
    }
}