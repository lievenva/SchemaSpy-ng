package net.sourceforge.schemaspy;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import net.sourceforge.schemaspy.util.*;

/**
 * Configuration of a SchemaSpy run
 * 
 * @author John Currier
 */
public class Config
{
    private static final long serialVersionUID = 1L;
    private List options;
    private Map dbSpecificOptions;
    private HashMap originalDbSpecificOptions;
    private boolean helpRequired;
    private boolean dbHelpRequired;
    private File outputDir;
    private Pattern inclusions;
    private Pattern exclusions;
    private String dbType;
    private String schema;
    private String user;
    private String password;
    private String userConnectionPropertiesFile;
    private Properties userConnectionProperties;
    private Integer maxDbThreads;
    private Integer maxDetailedTables;
    private String classpath;
    private String css;
    private String description;
    private String dbPropertiesLoadedFrom;
    private Boolean generateHtml;
    private Boolean includeImpliedConstraints;
    private Boolean logoEnabled;
    private Boolean rankDirBugEnabled;
    private Boolean encodeCommentsEnabled;
    private Boolean displayCommentsInitiallyEnabled;
    private Boolean tableCommentsEnabled;
    private Boolean numRowsEnabled;
    private Boolean meterEnabled;
    private Boolean evaluteAll;

    /**
     * Default constructor. Intended for when you want to inject properties
     * independently (i.e. not from a command line interface).
     */
    public Config()
    {
        options = new ArrayList();
    }

    /**
     * Construct a configuration from an array of options (e.g. from a command
     * line interface).
     * 
     * @param options
     */
    public Config(String[] argv)
    {
        options = fixupArgs(Arrays.asList(argv));
        
        helpRequired =  options.remove("-?") || 
                        options.remove("/?") || 
                        options.remove("?") || 
                        options.remove("-h") || 
                        options.remove("-help") || 
                        options.remove("--help");
        dbHelpRequired =  options.remove("-dbHelp") || options.remove("-dbhelp");
    }

    public void setHtmlGenerationEnabled(boolean generateHtml) {
        this.generateHtml = Boolean.valueOf(generateHtml);
    }
    
    public boolean isHtmlGenerationEnabled() {
        if (generateHtml == null)
            generateHtml = Boolean.valueOf(!options.remove("-nohtml"));

        return generateHtml.booleanValue();
    }
    
    public void setImpliedConstraintsEnabled(boolean includeImpliedConstraints) {
        this.includeImpliedConstraints = Boolean.valueOf(includeImpliedConstraints);
    }
    
    public boolean isImpliedConstraintsEnabled() {
        if (includeImpliedConstraints == null)
            includeImpliedConstraints = Boolean.valueOf(!options.remove("-noimplied"));

        return includeImpliedConstraints.booleanValue();
    }
    
    public void setOutputDir(String outputDirName) throws IOException
    {
        if (outputDirName.endsWith("\""))
            outputDirName = outputDirName.substring(0, outputDirName.length() - 1);
        
        outputDir = new File(outputDirName).getCanonicalFile();
        if (!outputDir.isDirectory()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create directory '" + outputDir + "'");
            }
        }
    }
    
    public File getOutputDir() throws IOException
    {
        if (outputDir == null) {
            setOutputDir(pullRequiredParam("-o"));
        }
        
        return outputDir;
    }
    
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
    
    public String getDbType() {
        if (dbType == null) {
            dbType = pullParam("-t");
            if (dbType == null)
                dbType = "ora";
        }
        
        return dbType;
    }
    
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    public String getSchema() {
        if (schema == null) {
            schema = pullParam("-s", false, true);
        }
        
        return schema;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getUser() {
        if (user == null)
            user = pullRequiredParam("-u");
        return user;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPassword() {
        if (password == null)
            password = pullParam("-p");
        return password;
    }
    
    public void setMaxDetailedTabled(int maxDetailedTables) {
        this.maxDetailedTables = Integer.valueOf(maxDetailedTables);
    }
    
    public int getMaxDetailedTables() {
        if (maxDetailedTables == null) {
            int max = 300; // default
            try {
                max = Integer.parseInt(pullParam("-maxdet"));
            } catch (Exception notSpecified) {}
            
            maxDetailedTables = Integer.valueOf(max);
        }
        
        return maxDetailedTables.intValue();
    }
    
    public String getConnectionPropertiesFile() {
        return userConnectionPropertiesFile;
    }
    
    public void setConnectionPropertiesFile(String propertiesFilename) throws FileNotFoundException, IOException {
        if (userConnectionProperties == null)
            userConnectionProperties = new Properties();
        userConnectionProperties.load(new FileInputStream(propertiesFilename));
        userConnectionPropertiesFile = propertiesFilename;
    }
    
    public Properties getConnectionProperties() throws FileNotFoundException, IOException {
        if (userConnectionProperties == null) {
            userConnectionProperties = new Properties();
            userConnectionPropertiesFile = pullParam("-connprops");
            if (userConnectionPropertiesFile != null)
                setConnectionPropertiesFile(userConnectionPropertiesFile);
        }
        
        return userConnectionProperties;
    }
    
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }
    
    public String getClasspath() {
        if (classpath == null)
            classpath = pullParam("-cp");
        return classpath;
    }
    
    public void setCss(String css) {
        this.css = css;
    }
    
    public String getCss() {
        if (css == null) {
            css = pullParam("-css");
            if (css == null)
                css = "schemaSpy.css";
        }
        return css;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        if (description == null)
            description = pullParam("-desc");
        return description;
    }
    
    public void setMaxDbThreads(int maxDbThreads) {
        this.maxDbThreads = Integer.valueOf(maxDbThreads);
    }
    
    public int getMaxDbThreads() throws IOException {
        if (maxDbThreads == null) {
            Properties properties = getDbProperties(getDbType());
            
            int max = Integer.MAX_VALUE;
            String threads = properties.getProperty("dbThreads");
            if (threads == null)
                threads = properties.getProperty("dbthreads");
            if (threads != null)
                max = Integer.parseInt(threads);
            threads = pullParam("-dbThreads");
            if (threads == null)
                threads = pullParam("-dbthreads");
            if (threads != null)
                max = Integer.parseInt(threads);
            if (max < 0)
                max = Integer.MAX_VALUE;
            else if (max == 0)
                max = 1;
    
            maxDbThreads = Integer.valueOf(max);
        }
        
        return maxDbThreads.intValue();
    }
    
    public void setLogoEnabled(boolean enabled) {
        this.logoEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isLogoEnabled() {
        if (logoEnabled == null)
            logoEnabled = Boolean.valueOf(!options.remove("-nologo"));
        
        // nasty hack, but passing this info everywhere churns my stomach
        System.setProperty("sourceforgelogo", String.valueOf(logoEnabled));
        
        return logoEnabled.booleanValue();
    }
    
    public void setRankDirBugEnabled(boolean enabled) {
        this.rankDirBugEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isRankDirBugEnabled() {
        if (rankDirBugEnabled == null)
            rankDirBugEnabled = Boolean.valueOf(options.remove("-rankdirbug"));
        
        // another nasty hack with the same justification as the one above
        System.setProperty("rankdirbug", String.valueOf(rankDirBugEnabled));
        
        return rankDirBugEnabled.booleanValue();
    }
    
    public void setEncodeCommentsEnabled(boolean enabled) {
        this.encodeCommentsEnabled = Boolean.valueOf(enabled);
    }
    
    /**
     * Allow Html In Comments - encode them unless otherwise specified
     * @return
     */
    public boolean isEncodeCommentsEnabled() {
        if (encodeCommentsEnabled == null)
            encodeCommentsEnabled = Boolean.valueOf(!options.remove("-ahic"));
        
        System.setProperty("encodeComments", String.valueOf(encodeCommentsEnabled));
        
        return encodeCommentsEnabled.booleanValue();
    }

    public void setDisplayCommentsIntiallyEnabled(boolean enabled) {
        this.displayCommentsInitiallyEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isDisplayCommentsIntiallyEnabled() {
        if (displayCommentsInitiallyEnabled == null)
            displayCommentsInitiallyEnabled = Boolean.valueOf(options.remove("-cid"));
        
        System.setProperty("commentsInitiallyDisplayed", String.valueOf(displayCommentsInitiallyEnabled));
        
        return displayCommentsInitiallyEnabled.booleanValue();
    }

    public void setTableCommentsEnabled(boolean enabled) {
        this.tableCommentsEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isTableCommentsEnabled() {
        if (tableCommentsEnabled == null)
            tableCommentsEnabled = Boolean.valueOf(!options.remove("-notablecomments"));
        
        System.setProperty("displayTableComments", String.valueOf(tableCommentsEnabled));
        
        return tableCommentsEnabled.booleanValue();
    }

    public void setNumRowsEnabled(boolean enabled) {
        this.numRowsEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isNumRowsEnabled() {
        if (numRowsEnabled == null)
            numRowsEnabled = Boolean.valueOf(!options.remove("-norows"));
        
        System.setProperty("displayNumRows", String.valueOf(numRowsEnabled));
        
        return numRowsEnabled.booleanValue();
    }

    public void setMeterEnabled(boolean enabled) {
        this.meterEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isMeterEnabled() {
        if (meterEnabled == null)
            meterEnabled = Boolean.valueOf(options.remove("-meter"));
        
        System.setProperty("isMetered", String.valueOf(meterEnabled));
        
        return meterEnabled.booleanValue();
    }

    public void setExclusions(Pattern exclusions) {
        this.exclusions = exclusions;
    }

    public void setExclusions(String exclusions) {
        this.exclusions = Pattern.compile(exclusions);
    }

    public Pattern getExclusions() {
        if (exclusions == null) {
            String strExclusions = pullParam("-x");
            if (strExclusions == null)
                strExclusions = "[^.]";   // match nothing

            exclusions = Pattern.compile(strExclusions);
        }

        return exclusions;
    }

    public void setInclusions(Pattern exclusions) {
        this.inclusions = exclusions;
    }

    public void setInclusions(String exclusions) {
        this.inclusions = Pattern.compile(exclusions);
    }

    public Pattern getInclusions() {
        if (inclusions == null) {
            String strInclusions = pullParam("-i");
            if (strInclusions == null)
                strInclusions = ".*";     // match anything

            inclusions = Pattern.compile(strInclusions);
        }

        return inclusions;
    }
    
    public void setEvaluateAllEnabled(boolean enabled) {
        this.evaluteAll = Boolean.valueOf(enabled);
    }
    
    public boolean isEvaluateAllEnabled() {
        if (evaluteAll == null)
            evaluteAll = Boolean.valueOf(options.remove("-all"));
        return evaluteAll.booleanValue();
    }
    
    /**
     * Returns <code>true</code> if the options indicate that the user wants
     * to see some help information.
     * 
     * @return
     */
    public boolean isHelpRequired() {
        return helpRequired;
    }
    
    public boolean isDbHelpRequired() {
        return dbHelpRequired;
    }
    
    
    public String getLoadedFromJar() {
        String classpath = System.getProperty("java.class.path");
        return new StringTokenizer(classpath, File.pathSeparator).nextToken();
    }

    public Properties getDbProperties(String dbType) throws IOException {
        ResourceBundle bundle = null;

        try {
            File propertiesFile = new File(dbType);
            bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
            dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
        } catch (FileNotFoundException notFoundOnFilesystemWithoutExtension) {
            try {
                File propertiesFile = new File(dbType + ".properties");
                bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
                dbPropertiesLoadedFrom = propertiesFile.getAbsolutePath();
            } catch (FileNotFoundException notFoundOnFilesystemWithExtensionTackedOn) {
                try {
                    bundle = ResourceBundle.getBundle(dbType);
                    dbPropertiesLoadedFrom = "[" + getLoadedFromJar() + "]" + File.separator + dbType + ".properties";
                } catch (Exception notInJarWithoutPath) {
                    try {
                        String path = SchemaSpy.class.getPackage().getName() + ".dbTypes." + dbType;
                        path = path.replace('.', '/');
                        bundle = ResourceBundle.getBundle(path);
                        dbPropertiesLoadedFrom = "[" + getLoadedFromJar() + "]/" + path + ".properties";
                    } catch (Exception notInJar) {
                        notInJar.printStackTrace();
                        notFoundOnFilesystemWithExtensionTackedOn.printStackTrace();
                        throw notFoundOnFilesystemWithoutExtension;
                    }
                }
            }
        }

        Properties properties;
        String save = dbPropertiesLoadedFrom;
        
        try {
            String baseDbType = bundle.getString("extends").trim();
            properties = getDbProperties(baseDbType);
        } catch (MissingResourceException doesntExtend) {
            properties = new Properties();
        } finally {
            dbPropertiesLoadedFrom = save;
        }

        return add(properties, bundle);
    }
    
    protected String getDbPropertiesLoadedFrom() throws IOException {
        if (dbPropertiesLoadedFrom == null)
            getDbProperties(getDbType());
        return dbPropertiesLoadedFrom;
    }

    public List getRemainingParameters()
    {
        return options;
    }
    
    public void setDbSpecificOptions(Map dbSpecificOptions) {
        this.dbSpecificOptions = dbSpecificOptions;
        this.originalDbSpecificOptions = new HashMap(dbSpecificOptions);
    }
    
    public Map getDbSpecificOptions() {
        if (dbSpecificOptions ==  null)
            dbSpecificOptions = new HashMap();
        return dbSpecificOptions;
    }

    /**
     * Add the contents of <code>bundle</code> to the specified <code>properties</code>.
     *
     * @param properties Properties
     * @param bundle ResourceBundle
     * @return Properties
     */
    public static Properties add(Properties properties, ResourceBundle bundle) {
        Enumeration iter = bundle.getKeys();
        while (iter.hasMoreElements()) {
            Object key = iter.nextElement();
            properties.put(key, bundle.getObject(key.toString()));
        }

        return properties;
    }

    /**
     * 'Pull' the specified parameter from the collection of options. Returns
     * null if the parameter isn't in the list and removes it if it is.
     * 
     * @param paramId
     * @return
     */
    private String pullParam(String paramId) {
        return pullParam(paramId, false, false);
    }
    
    private String pullRequiredParam(String paramId) {
        return pullParam(paramId, true, false);
    }
    
    private String pullParam(String paramId, boolean required, boolean dbTypeSpecific) {
        int paramIndex = options.indexOf(paramId);
        if (paramIndex < 0) {
            if (required)
                throw new MissingRequiredParameterException(paramId, dbTypeSpecific);
            return null;
        }
        options.remove(paramIndex);
        String param = options.get(paramIndex).toString();
        options.remove(paramIndex);
        return param;
    }
    
    static public class MissingRequiredParameterException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private boolean dbTypeSpecific;

        public MissingRequiredParameterException(String paramId, boolean dbTypeSpecific) {
            this(paramId, null, dbTypeSpecific);
        }
        
        public MissingRequiredParameterException(String paramId, String description, boolean dbTypeSpecific) {
            super("Parameter '" + paramId + "' " + (description == null ? "" : "(" + description + ") ") + "missing." + (dbTypeSpecific ? "  It is required for this database type." : ""));
            this.dbTypeSpecific = dbTypeSpecific;
        }
        
        public boolean isDbTypeSpecific() {
            return dbTypeSpecific;
        }
    }

    /**
     * Allow an equal sign in args...like "-o=foo.bar". Useful for things like
     * Ant and Maven.
     * 
     * @param args
     *            List
     * @return List
     */
    protected List fixupArgs(List args)
    {
        List expandedArgs = new ArrayList();

        Iterator iter = args.iterator();
        while (iter.hasNext())
        {
            String arg = iter.next().toString();
            int indexOfEquals = arg.indexOf('=');
            if (indexOfEquals != -1)
            {
                expandedArgs.add(arg.substring(0, indexOfEquals));
                expandedArgs.add(arg.substring(indexOfEquals + 1));
            }
            else
            {
                expandedArgs.add(arg);
            }
        }

        return expandedArgs;
    }
    
    /**
     * Call all the getters to populate all the lazy initialized stuff.
     * After this call the remaining options can be evaluated.
     * 
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void populate() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method[] methods = getClass().getMethods();
        for (int i = 0; i < methods.length; ++i) {
            Method method = methods[i];
            if (method.getParameterTypes().length == 0 &&
                (method.getName().startsWith("is") || method.getName().startsWith("get"))) {
                method.invoke(this, null);
            }
        }
    }

    public static Set getBuiltInDatabaseTypes(String loadedFromJar) {
        Set databaseTypes = new TreeSet();
        JarInputStream jar = null;

        try {
            jar = new JarInputStream(new FileInputStream(loadedFromJar));
            JarEntry entry;

            while ((entry = jar.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                int dotPropsIndex = entryName.indexOf(".properties");
                if (dotPropsIndex != -1)
                    databaseTypes.add(entryName.substring(0, dotPropsIndex));
            }
        } catch (IOException exc) {
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignore) {}
            }
        }

        return databaseTypes;
    }
    
    protected void dumpUsage(String errorMessage, boolean detailedDb) {
        if (errorMessage != null) {
            System.err.println("*** " + errorMessage + " ***");
        }

        System.out.println("SchemaSpy generates an HTML representation of a database schema's relationships.");
        System.out.println();

        if (!detailedDb) {
            System.out.println("Usage:");
            System.out.println(" java -jar " + getLoadedFromJar() + " [options]");
            System.out.println("   -t databaseType       type of database - defaults to ora");
            System.out.println("                           use -dbhelp for a list of built-in types");
            System.out.println("   -u user               connect to the database with this user id");
            System.out.println("   -s schema             defaults to the specified user");
            System.out.println("   -p password           defaults to no password");
            System.out.println("   -o outputDirectory    directory to place the generated output in");
            System.out.println("   -cp pathToDrivers     optional - looks for JDBC drivers here before looking");
            System.out.println("                           in driverPath in [databaseType].properties.");
            System.out.println("                           must be specified after " + getLoadedFromJar());
            System.out.println("Go to http://schemaspy.sourceforge.net for a complete list/description"); 
            System.out.println(" of additional parameters.");
            System.out.println();
        }

        if (detailedDb) {
            System.out.println("Built-in database types and their required connection parameters:");
            Set datatypes = getBuiltInDatabaseTypes(getLoadedFromJar());
            for (Iterator iter = datatypes.iterator(); iter.hasNext(); ) {
                String dbType = iter.next().toString();
                new ConnectionURLBuilder(dbType).dumpUsage();
            }
            System.out.println();
        }

        if (detailedDb) {
            System.out.println("You can use your own database types by specifying the filespec of a .properties file with -t.");
            System.out.println("Grab one out of " + getLoadedFromJar() + " and modify it to suit your needs.");
            System.out.println();
        }

        System.out.println("Sample usage using the default database type (implied -t ora):");
        System.out.println(" java -jar schemaSpy.jar -db mydb -s myschema -u devuser -p password -o output");
        System.out.println();
        System.out.flush();
    }
    
    public List asList() throws IOException {
        List list = new ArrayList();
        
        if (originalDbSpecificOptions != null) {
            Iterator iter = originalDbSpecificOptions.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next().toString();
                String value = originalDbSpecificOptions.get(key).toString();
                if (!key.startsWith("-"))
                    key = "-" + key;
                list.add(key);
                list.add(value);
            }
        }
        if (isDisplayCommentsIntiallyEnabled())
            list.add("-cid");
        if (isEncodeCommentsEnabled())
            list.add("-ahic");
        if (isEvaluateAllEnabled())
            list.add("-all");
        if (!isHtmlGenerationEnabled())
            list.add("-nohtml");
        if (!isImpliedConstraintsEnabled())
            list.add("-noimplied");
        if (!isLogoEnabled())
            list.add("-nologo");
        if (isMeterEnabled())
            list.add("-meter");
        if (!isNumRowsEnabled())
            list.add("-norows");
        if (isRankDirBugEnabled())
            list.add("-rankdirbug");
        if (!isTableCommentsEnabled())
            list.add("-notablecomments");
        
        String value = getClasspath();
        if (value != null) {
            list.add("-cp");
            list.add(value);
        }
        value = getCss();
        if (value != null) {
            list.add("-css");
            list.add(value);
        }
        value = getDbType();
        if (value != null) {
            list.add("-t");
            list.add(value);
        }
        value = getDescription();
        if (value != null) {
            list.add("-desc");
            list.add(value);
        }
        value = getPassword();
        if (value != null) {
            list.add("-p");
            list.add(value);
        }
        value = getSchema();
        if (value != null) {
            list.add("-s");
            list.add(value);
        }
        list.add("-u");
        list.add(getUser());
        value = getConnectionPropertiesFile();
        if (value != null) {
            list.add("-connprops");
            list.add(value);
        }
        list.add("-i");
        list.add(getInclusions().toString());
        list.add("-x");
        list.add(getExclusions().toString());
        list.add("-dbthreads");
        list.add(String.valueOf(getMaxDbThreads()));
        list.add("-maxdet");
        list.add(String.valueOf(getMaxDetailedTables()));
        list.add("-o");
        list.add(getOutputDir());
        
        return list;
    }
}
