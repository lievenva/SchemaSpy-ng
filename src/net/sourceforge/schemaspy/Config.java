package net.sourceforge.schemaspy;

import java.beans.*;
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
    private static Config instance;
    private List<String> options;
    private Map<String, String> dbSpecificOptions;
    private Map<String, String> originalDbSpecificOptions;
    private boolean helpRequired;
    private boolean dbHelpRequired;
    private File outputDir;
    private String dbType;
    private String schema;
    private String user;
    private String password;
    private String db;
    private String host;
    private Integer port;
    private String server;
    private Pattern tableInclusions;
    private Pattern columnExclusions;
    private String userConnectionPropertiesFile;
    private Properties userConnectionProperties;
    private Integer maxDbThreads;
    private Integer maxDetailedTables;
    private String classpath;
    private String css;
    private String charset;
    private String font;
    private Integer fontSize;
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
    private Boolean highQuality;
    private String schemaSpec;  // used in conjunction with evaluateAll
    private boolean populating = false;
    public static final String DOT_CHARSET = "UTF-8"; 
    
    /**
     * Default constructor. Intended for when you want to inject properties
     * independently (i.e. not from a command line interface).
     */
    public Config()
    {
        if (instance == null)
            setInstance(this);
        options = new ArrayList<String>();
    }

    /**
     * Construct a configuration from an array of options (e.g. from a command
     * line interface).
     * 
     * @param options
     */
    public Config(String[] argv)
    {
        setInstance(this);
        options = fixupArgs(Arrays.asList(argv));
        
        helpRequired =  options.remove("-?") || 
                        options.remove("/?") || 
                        options.remove("?") || 
                        options.remove("-h") || 
                        options.remove("-help") || 
                        options.remove("--help");
        dbHelpRequired =  options.remove("-dbHelp") || options.remove("-dbhelp");
    }
    
    public static Config getInstance() {
        if (instance == null)
            instance = new Config();
        
        return instance;
    }
    
    /**
     * Sets the global instance.
     *
     * Useful for things like selecting a specific configuration in a UI.
     * 
     * @param config
     */
    public static void setInstance(Config config) {
        instance = config;
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
    
    public void setOutputDir(String outputDirName) {
        if (outputDirName.endsWith("\""))
            outputDirName = outputDirName.substring(0, outputDirName.length() - 1);

        setOutputDir(new File(outputDirName));
    }
    
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
    
    public File getOutputDir() {
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
    
    public void setDb(String db) {
        this.db = db;
    }
    
    public String getDb() {
        if (db == null)
            db = pullParam("-db");
        return db;
    }
    
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    public String getSchema() {
        if (schema == null)
            schema = pullParam("-s");
        return schema;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getHost() {
        if (host == null)
            host = pullParam("-host");
        return host;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public Integer getPort() {
        if (port == null)
            try {
                port = Integer.valueOf(pullParam("-port"));
            } catch (Exception notSpecified) {}
        return port;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public String getServer() {
        if (server == null) {
            server = pullParam("-server");
        }
        
        return server;
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
        this.maxDetailedTables = new Integer(maxDetailedTables);
    }
    
    public int getMaxDetailedTables() {
        if (maxDetailedTables == null) {
            int max = 300; // default
            try {
                max = Integer.parseInt(pullParam("-maxdet"));
            } catch (Exception notSpecified) {}
            
            maxDetailedTables = new Integer(max);
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

    /**
     * The filename of the cascading style sheet to use.  
     * Note that this file is parsed and used to determine characteristics
     * of the generated graphs, so it must contain specific settings that
     * are documented within schemaSpy.css.<p>
     * 
     * Defaults to <code>"schemaSpy.css"</code>.
     *  
     * @param css
     */
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
    
    /**
     * The font to use within graphs.  Modify the .css to specify HTML fonts.
     * 
     * @param font
     */
    public void setFont(String font) {
        this.font = font;
    }

    /**
     * @see #setFont(String)
     */
    public String getFont() {
        if (font == null) {
            font = pullParam("-font");
            if (font == null)
                font = "Helvetica";
        }
        return font;
    }

    /**
     * The font size to use within graphs.  This is the size of the font used for
     * 'large' (e.g. not 'compact') graphs.<p>
     *   
     * Modify the .css to specify HTML font sizes.<p>
     * 
     * Defaults to 11.
     * 
     * @param fontSize
     */
    public void setFontSize(int fontSize) {
        this.fontSize = new Integer(fontSize);
    }
    
    /**
     * @see #setFontSize(int)
     * @return
     */
    public int getFontSize() {
        if (fontSize == null) {
            int size = 11; // default
            try {
                size = Integer.parseInt(pullParam("-fontsize"));
            } catch (Exception notSpecified) {}
            
            fontSize = new Integer(size);
        }
        
        return fontSize.intValue();
    }

    /**
     * The character set to use within HTML pages (defaults to <code>"ISO-8859-1"</code>).
     * 
     * @param charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * @see #setCharset(String)
     */
    public String getCharset() {
        if (charset == null) {
            charset = pullParam("-charset");
            if (charset == null)
                charset = "ISO-8859-1";
        }
        return charset;
    }
    
    /**
     * Description of schema that gets display on main pages.
     * 
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * @see #setDescription(String)
     */
    public String getDescription() {
        if (description == null)
            description = pullParam("-desc");
        return description;
    }
    
    /**
     * Maximum number of threads to use when querying database metadata information.
     * 
     * @param maxDbThreads
     */
    public void setMaxDbThreads(int maxDbThreads) {
        this.maxDbThreads = new Integer(maxDbThreads);
    }
    
    /**
     * @see #setMaxDbThreads(int)
     */
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
    
            maxDbThreads = new Integer(max);
        }
        
        return maxDbThreads.intValue();
    }
    
    public boolean isLogoEnabled() {
        if (logoEnabled == null)
            logoEnabled = Boolean.valueOf(!options.remove("-nologo"));
        
        return logoEnabled.booleanValue();
    }
    
    /**
     * Don't use this unless absolutely necessary as it screws up the layout
     * 
     * @param enabled
     */
    public void setRankDirBugEnabled(boolean enabled) {
        this.rankDirBugEnabled = Boolean.valueOf(enabled);
    }

    /**
     * @see #setRankDirBugEnabled(boolean)
     */
    public boolean isRankDirBugEnabled() {
        if (rankDirBugEnabled == null)
            rankDirBugEnabled = Boolean.valueOf(options.remove("-rankdirbug"));
        
        return rankDirBugEnabled.booleanValue();
    }
    
    /**
     * Allow Html In Comments - encode them unless otherwise specified
     */
    public void setEncodeCommentsEnabled(boolean enabled) {
        this.encodeCommentsEnabled = Boolean.valueOf(enabled);
    }

    /**
     * @see #setEncodeCommentsEnabled(boolean)
     */
    public boolean isEncodeCommentsEnabled() {
        if (encodeCommentsEnabled == null)
            encodeCommentsEnabled = Boolean.valueOf(!options.remove("-ahic"));
        
        return encodeCommentsEnabled.booleanValue();
    }

    /**
     * Specifies whether comments are initially displayed or initially hidden (default).
     * @param enabled
     */
    public void setDisplayCommentsIntiallyEnabled(boolean enabled) {
        this.displayCommentsInitiallyEnabled = Boolean.valueOf(enabled);
    }
    
    /**
     * @see #setDisplayCommentsIntiallyEnabled(boolean)
     */
    public boolean isDisplayCommentsIntiallyEnabled() {
        if (displayCommentsInitiallyEnabled == null)
            displayCommentsInitiallyEnabled = Boolean.valueOf(options.remove("-cid"));
        
        return displayCommentsInitiallyEnabled.booleanValue();
    }

    /**
     * Some database types (e.g. MySQL) stuff inappropriate things in the
     * table comments.  
     * This setting allows you to disable those table comments.
     * 
     * @param enabled
     */
    public void setTableCommentsEnabled(boolean enabled) {
        this.tableCommentsEnabled = Boolean.valueOf(enabled);
    }

    /**
     * @see #setTableCommentsEnabled(boolean)
     */
    public boolean isTableCommentsEnabled() {
        if (tableCommentsEnabled == null)
            tableCommentsEnabled = Boolean.valueOf(!options.remove("-notablecomments"));
        
        return tableCommentsEnabled.booleanValue();
    }

    public void setNumRowsEnabled(boolean enabled) {
        this.numRowsEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isNumRowsEnabled() {
        if (numRowsEnabled == null)
            numRowsEnabled = Boolean.valueOf(!options.remove("-norows"));
        
        return numRowsEnabled.booleanValue();
    }

    public boolean isMeterEnabled() {
        if (meterEnabled == null)
            meterEnabled = Boolean.valueOf(options.remove("-meter"));
        
        return meterEnabled.booleanValue();
    }

    public void setColumnExclusions(String columnExclusions) {
        this.columnExclusions = Pattern.compile(columnExclusions);
    }

    public Pattern getColumnExclusions() {
        if (columnExclusions == null) {
            String strExclusions = pullParam("-x");
            if (strExclusions == null)
                strExclusions = "[^.]";   // match nothing

            columnExclusions = Pattern.compile(strExclusions);
        }

        return columnExclusions;
    }

    public void setTableInclusions(String tableInclusions) {
        this.tableInclusions = Pattern.compile(tableInclusions);
    }

    public Pattern getTableInclusions() {
        if (tableInclusions == null) {
            String strInclusions = pullParam("-i");
            if (strInclusions == null)
                strInclusions = ".*";     // match anything

            tableInclusions = Pattern.compile(strInclusions);
        }

        return tableInclusions;
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
     * Returns true if we're evaluating a bunch of schemas in one go and
     * at this point we're evaluating a specific schema.
     * 
     * @return boolean
     */
    public boolean isOneOfMultipleSchemas() {
        // set by MultipleSchemaAnalyzer
        return Boolean.getBoolean("oneofmultipleschemas");
    }
    
    /**
     * When -all (evaluateAll) is specified then this is the regular
     * expression that determines which schemas to evaluate.
     * 
     * @param schemaSpec
     */
    public void setSchemaSpec(String schemaSpec) {
        this.schemaSpec = schemaSpec;
    }
    
    public String getSchemaSpec() {
        if (schemaSpec == null)
            schemaSpec = pullParam("-schemaSpec");
        
        return schemaSpec;
    }

    // removed these as they're too low-level to expose externally...replaced with -hq
//    /**
//     * Set the renderer to use for the -Tpng[:renderer[:formatter]] dot option as specified
//     * at <a href='http://www.graphviz.org/doc/info/command.html'>
//     * http://www.graphviz.org/doc/info/command.html</a>.<p/>
//     * Note that the leading ":" is required while :formatter is optional.<p/>
//     * The default renderer is typically GD. 
//     */
//    public void setRenderer(String renderer) {
//        this.renderer = renderer;
//        Dot.getInstance().setRenderer(renderer);
//    }
//    
//    /**
//     * @see #setRenderer(String)
//     * @return
//     */
//    public String getRenderer() {
//        if (renderer == null) {
//            renderer = pullParam("-renderer");
//            if (renderer != null)
//                setRenderer(renderer);
//        }
//        
//        return renderer;
//    }
    
    /**
     * If <code>true</code> then generate graphical output of "higher quality"
     * than the default ("lower quality").  
     * Note that the default is intended to be "lower quality", 
     * but various installations of Graphviz may have have different abilities.
     * That is, some might not have the "lower quality" libraries and others might
     * not have the "higher quality" libraries.<p/>
     * Higher quality output takes longer to generate and results in significantly
     * larger image files (which take longer to download / display), but it looks better.
     */
    public void setHighQuality(boolean highQuality) {
        this.highQuality = Boolean.valueOf(highQuality);
        Dot.getInstance().setHighQuality(highQuality);
    }
    
    /**
     * @see #setHighQuality(boolean)
     */
    public boolean isHighQuality() {
        if (highQuality == null) {
            highQuality = Boolean.valueOf(options.remove("-hq"));
            Dot.getInstance().setHighQuality(highQuality.booleanValue());
        }

        return Dot.getInstance().isHighQuality();
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
    
    
    public static String getLoadedFromJar() {
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

    public List<String> getRemainingParameters()
    {
        try {
            populate();
        } catch (Exception exc) {}
        
        return options;
    }
    
    /**
     * Options that are specific to a type of database.  E.g. things like <code>host</code>,
     * <code>port</code> or <code>db</code>, but <b>don't</b> have a setter in this class.
     *  
     * @param dbSpecificOptions
     */
    public void setDbSpecificOptions(Map<String, String> dbSpecificOptions) {
        this.dbSpecificOptions = dbSpecificOptions;
        this.originalDbSpecificOptions = new HashMap<String, String>(dbSpecificOptions);
    }
    
    public Map<String, String> getDbSpecificOptions() {
        if (dbSpecificOptions ==  null)
            dbSpecificOptions = new HashMap<String, String>();
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
    
    public static class MissingRequiredParameterException extends RuntimeException {
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
    protected List<String> fixupArgs(List<String> args) {
        List<String> expandedArgs = new ArrayList<String>();

        for (String arg : args) {
            int indexOfEquals = arg.indexOf('=');
            if (indexOfEquals != -1 && indexOfEquals -1 != arg.indexOf("\\=")) {
                expandedArgs.add(arg.substring(0, indexOfEquals));
                expandedArgs.add(arg.substring(indexOfEquals + 1));
            } else {
                expandedArgs.add(arg);
            }
        }

        // some OSes/JVMs do filename expansion with runtime.exec() and some don't,
        // so MultipleSchemaAnalyzer has to surround params with double quotes...
        // strip them here for the OSes/JVMs that don't do anything with the params  
        List<String> unquotedArgs = new ArrayList<String>();
        
        for (String arg : expandedArgs) {
            if (arg.startsWith("\"") && arg.endsWith("\""))  // ".*" becomes .*
                arg = arg.substring(1, arg.length() - 1);
            unquotedArgs.add(arg);
        }

        return unquotedArgs;
    }
    
    /**
     * Call all the getters to populate all the lazy initialized stuff.
     * 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @throws IntrospectionException 
     */
    private void populate() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IntrospectionException {
        if (!populating) { // prevent recursion
            populating = true;
            
            BeanInfo beanInfo = Introspector.getBeanInfo(Config.class);
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < props.length; ++i) {
                Method readMethod = props[i].getReadMethod();
                if (readMethod != null)
                    readMethod.invoke(this, (Object[])null);
            }
            
            populating = false;
        }
    }

    public static Set<String> getBuiltInDatabaseTypes(String loadedFromJar) {
        Set<String> databaseTypes = new TreeSet<String>();
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
                new DbSpecificConfig(dbType).dumpUsage();
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
    
    /**
     * Get the value of the specified parameter.  
     * Used for properties that are common to most db's, but aren't required.
     *  
     * @param paramName
     * @return
     */
    public String getParam(String paramName) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(Config.class);
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < props.length; ++i) {
                PropertyDescriptor prop = props[i];
                if (prop.getName().equalsIgnoreCase(paramName)) {
                    Object result = prop.getReadMethod().invoke(this, (Object[])null);
                    return result == null ? null : result.toString();
                }
            }
        } catch (Exception failed) {
            failed.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Return all of the configuration options as a List of Strings, with
     * each parameter and its value as a separate element.
     * 
     * @return
     * @throws IOException
     */
    public List<String> asList() throws IOException {
        List<String> list = new ArrayList<String>();
        
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
        list.add("-css");
        list.add(getCss());
        list.add("-charset");
        list.add(getCharset());
        list.add("-font");
        list.add(getFont());
        list.add("-fontsize");
        list.add(String.valueOf(getFontSize()));
        list.add("-t");
        list.add(getDbType());
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
        value = getDb();
        if (value != null) {
            list.add("-db");
            list.add(value);
        }
        value = getHost();
        if (value != null) {
            list.add("-host");
            list.add(value);
        }
        if (getPort() != null) {
            list.add("-port");
            list.add(getPort().toString());
        }
        value = getServer();
        if (value != null) {
            list.add("-server");
            list.add(value);
        }
        list.add("-i");
        list.add(getTableInclusions().pattern());
        list.add("-x");
        list.add(getColumnExclusions().pattern());
        list.add("-dbthreads");
        list.add(String.valueOf(getMaxDbThreads()));
        list.add("-maxdet");
        list.add(String.valueOf(getMaxDetailedTables()));
        list.add("-o");
        list.add(getOutputDir().toString());
        
        return list;
    }
}