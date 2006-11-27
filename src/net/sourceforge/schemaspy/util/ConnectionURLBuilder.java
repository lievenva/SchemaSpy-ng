package net.sourceforge.schemaspy.util;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.*;

/**
 * @author John Currier
 */
public class ConnectionURLBuilder {
    private final String type;
    private final String description;
    private final String connectionURL;
    private String dbName = null;
    private final List options = new ArrayList();
    public class DbOption {
        public final String name;
        public final String value;
        public final String description;
        
        public DbOption(String name, String value, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }
    }

    /**
     * @param config
     * @param args
     * @param properties
     * @throws IOException
     */
    public ConnectionURLBuilder(Config config, Properties properties) throws IOException {
        this.type = config.getDbType();

        List opts = new ArrayList();
        Iterator iter = config.getDbSpecificOptions().keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            opts.add((key.startsWith("-") ? "" : "-") + key);
            opts.add(config.getDbSpecificOptions().get(key));
        }
        opts.addAll(config.getRemainingParameters());
        
        connectionURL = parseParameters(opts, properties);

        if (dbName == null) {
            dbName = connectionURL;
        }

        description = properties.getProperty("description");
        
        List remaining = config.getRemainingParameters();
        iter = options.iterator();
        while (iter.hasNext()) {
            DbOption option = (DbOption)iter.next();
            int idx = remaining.indexOf("-" + option.name);
            if (idx >= 0) {
                remaining.remove(idx);  // -paramKey
                remaining.remove(idx);  // paramValue
            }
        }
    }

    /**
     * Constructor so you can dump usage information for a specific type of database
     * @param dbType
     */
    public ConnectionURLBuilder(final String dbType) {
        type = dbType;
        class DbPropLoader {
            Properties load(String dbType) {
                ResourceBundle bundle = ResourceBundle.getBundle(type);
                Properties properties;
                try {
                    String baseDbType = bundle.getString("extends");
                    int lastSlash = dbType.lastIndexOf('/');
                    if (lastSlash != -1)
                        baseDbType = dbType.substring(0, dbType.lastIndexOf("/") + 1) + baseDbType;
                    properties = load(baseDbType);
                } catch (MissingResourceException doesntExtend) {
                    properties = new Properties();
                }

                return Config.add(properties, bundle);
            }
        }

        parseParameters(null, new DbPropLoader().load(type));
        connectionURL = description = null;
    }

    private String parseParameters(List args, Properties properties) {
        StringBuffer url = new StringBuffer();
        boolean inParam = false;

        StringTokenizer tokenizer = new StringTokenizer(properties.getProperty("connectionSpec"), "<>", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("<")) {
                inParam = true;
            } else if (token.equals(">")) {
                inParam = false;
            } else {
                if (inParam) {
                    String paramValue = getParam(args, token, properties);
                    if (token.equals("db") || token.equals("-db"))
                        dbName = paramValue;
                    url.append(paramValue);
                } else
                    url.append(token);
            }
        }
        
        return url.toString();
    }
    
    public String getConnectionURL() {
        return connectionURL;
    }

    public String getDbName() {
        return dbName;
    }

    public List getOptions() {
        return options;
    }

    private String getParam(List args, String paramName, Properties properties) {
        String param = null;
        int paramIndex = args != null ? args.indexOf("-" + paramName) : -1;
        String description = properties.getProperty(paramName);

        if (args != null) {
            if (paramIndex < 0)
                throw new Config.MissingRequiredParameterException(paramName, description, true);
            
            args.remove(paramIndex);
            param = args.get(paramIndex).toString();
            args.remove(paramIndex);
        }
        
        options.add(new DbOption(paramName, param, description));

        return param;
    }

    public void dumpUsage() {
        System.out.println(" " + new File(type).getName() + ":");
        System.out.println("   " + description);
        Iterator iter = getOptions().iterator();

        while (iter.hasNext()) {
            DbOption option = (DbOption)iter.next();
            System.out.println("   -" + option.name + " " + (option.description != null ? "  \t" + option.description : ""));
        }
    }
}
