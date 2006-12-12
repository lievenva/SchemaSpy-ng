package net.sourceforge.schemaspy.util;

import java.util.*;
import net.sourceforge.schemaspy.*;

/**
 * @author John Currier
 */
public class ConnectionURLBuilder {
    private final String connectionURL;
    private List options;

    /**
     * @param config
     * @param properties
     */
    public ConnectionURLBuilder(Config config, Properties properties) {
        List opts = new ArrayList();
        Iterator iter = config.getDbSpecificOptions().keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            opts.add((key.startsWith("-") ? "" : "-") + key);
            opts.add(config.getDbSpecificOptions().get(key));
        }
        opts.addAll(config.getRemainingParameters());
        
        DbSpecificConfig dbConfig = new DbSpecificConfig(config.getDbType());
        options = dbConfig.getOptions();
        connectionURL = buildUrl(opts, properties, config);
        
        List remaining = config.getRemainingParameters();
        iter = options.iterator();
        while (iter.hasNext()) {
            DbSpecificOption option = (DbSpecificOption)iter.next();
            int idx = remaining.indexOf("-" + option.getName());
            if (idx >= 0) {
                remaining.remove(idx);  // -paramKey
                remaining.remove(idx);  // paramValue
            }
        }
    }

    private String buildUrl(List args, Properties properties, Config config) {
        String connectionSpec = properties.getProperty("connectionSpec");
        
        Iterator iter = options.iterator();
        while (iter.hasNext()) {
            DbSpecificOption option = (DbSpecificOption)iter.next();
            option.setValue(getParam(args, option, config));
            
            // replace e.g. <host> with <myDbHost>
            connectionSpec = connectionSpec.replaceAll("\\<" + option.getName() + "\\>", option.getValue()); 
        }
        
        return connectionSpec;
    }
    
    public String getConnectionURL() {
        return connectionURL;
    }

    /**
     * Returns a {@link List} of populated {@link DbSpecificOption}s that are applicable to 
     * the specified database type.
     * 
     * @return
     */
    public List getOptions() {
        return options;
    }

    private String getParam(List args, DbSpecificOption option, Config config) {
        String param = null;
        int paramIndex = args.indexOf("-" + option.getName());

        if (paramIndex < 0) {
            if (config != null)
                param = config.getParam(option.getName());  // not in args...might be one of
                                                            // the common db params
            if (param == null)
                throw new Config.MissingRequiredParameterException(option.getName(), option.getDescription(), true);
        } else {
            args.remove(paramIndex);
            param = args.get(paramIndex).toString();
            args.remove(paramIndex);
        }
        
        return param;
    }
}
