package net.sourceforge.schemaspy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.sourceforge.schemaspy.Config;

/**
 * @author John Currier
 */
public class ConnectionURLBuilder {
    private final String connectionURL;
    private List<DbSpecificOption> options;

    /**
     * @param config
     * @param properties
     */
    public ConnectionURLBuilder(Config config, Properties properties) {
        List<String> opts = new ArrayList<String>();
        
        for (String key : config.getDbSpecificOptions().keySet()) {
            opts.add((key.startsWith("-") ? "" : "-") + key);
            opts.add(config.getDbSpecificOptions().get(key));
        }
        opts.addAll(config.getRemainingParameters());
        
        DbSpecificConfig dbConfig = new DbSpecificConfig(config.getDbType());
        options = dbConfig.getOptions();
        connectionURL = buildUrl(opts, properties, config);
        
        List<String> remaining = config.getRemainingParameters();
        
        for (DbSpecificOption option : options) {
            int idx = remaining.indexOf("-" + option.getName());
            if (idx >= 0) {
                remaining.remove(idx);  // -paramKey
                remaining.remove(idx);  // paramValue
            }
        }
    }

    private String buildUrl(List<String> args, Properties properties, Config config) {
        String connectionSpec = properties.getProperty("connectionSpec");
        
        for (DbSpecificOption option : options) {
            option.setValue(getParam(args, option, config));
            
            // replace e.g. <host> with <myDbHost>
            connectionSpec = connectionSpec.replaceAll("\\<" + option.getName() + "\\>", option.getValue().toString()); 
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
    public List<DbSpecificOption> getOptions() {
        return options;
    }

    private String getParam(List<String> args, DbSpecificOption option, Config config) {
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
