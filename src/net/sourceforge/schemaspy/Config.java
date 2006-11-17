package net.sourceforge.schemaspy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Configuration of an SchemaSpy run
 * 
 * @author John Currier
 */
public class Config
{
    private List options;
    private PrintWriter output;
    private Pattern classPattern;
    private String xpathQuery;
    private boolean helpRequired;
    private boolean dbHelpRequired;
    private Boolean generateHtml;
    private Boolean includeImpliedConstraints;
    private File outputDir;
    private String dbType;
    private String user;
    private String password;
    private String schema;
    private Integer maxDetailedTables;
    private Properties userConnectionProperties;
    private String classpath;
    private String css;
    private String description;
    private Integer maxDbThreads;
    private Boolean logoEnabled;
    private Boolean rankDirBugEnabled;
    private Boolean encodeCommentsEnabled;
    private Boolean displayCommentsInitiallyEnabled;
    private Boolean tableCommentsEnabled;
    private Boolean numRowsEnabled;
    private Boolean meterEnabled;

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
        this(Arrays.asList(argv));
    }

    /**
     * Construct a configuration from a list of options (e.g. from a command
     * line interface).
     * 
     * @param options
     */
    public Config(List options)
    {
        this.options = fixupArgs(options);
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
                System.err.println("Failed to create directory '" + outputDir + "'");
                System.exit(2);
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
    
    public void setDatabaseType(String dbType) {
        this.dbType = dbType;
    }
    
    public String getDatabaseType() {
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
    
    public void setConnectionPropertiesFile(String propertiesFilename) throws FileNotFoundException, IOException {
        if (userConnectionProperties == null)
            userConnectionProperties = new Properties();
        userConnectionProperties.load(new FileInputStream(propertiesFilename));
    }
    
    public Properties getConnectionProperties() throws FileNotFoundException, IOException {
        if (userConnectionProperties == null) {
            userConnectionProperties = new Properties();
            String propertiesFilename = pullParam("-connprops");
            if (propertiesFilename != null)
                setConnectionPropertiesFile(propertiesFilename);
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
    
    public int getMaxDbThreads() {
        if (maxDbThreads == null) {
            Properties properties = getDbProperties(getDatabaseType());
            
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
        return logoEnabled.booleanValue();
    }
    
    public void setRankDirBugEnabled(boolean enabled) {
        this.rankDirBugEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isRankDirBugEnabled() {
        if (rankDirBugEnabled == null)
            rankDirBugEnabled = Boolean.valueOf(options.remove("-rankdirbug"));
        return rankDirBugEnabled.booleanValue();
    }
    
    public void setEncodeCommentsEnabled(boolean enabled) {
        this.encodeCommentsEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isEncodeCommentsEnabled() {
        if (encodeCommentsEnabled == null)
            encodeCommentsEnabled = Boolean.valueOf(!options.remove("-ahic"));
        return encodeCommentsEnabled.booleanValue();
    }

    public void setDisplayCommentsIntiallyEnabled(boolean enabled) {
        this.displayCommentsInitiallyEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isDisplayCommentsIntiallyEnabled() {
        if (displayCommentsInitiallyEnabled == null)
            displayCommentsInitiallyEnabled = Boolean.valueOf(options.remove("-cid"));
        return displayCommentsInitiallyEnabled.booleanValue();
    }

    public void setTableCommentsEnabled(boolean enabled) {
        this.tableCommentsEnabled = Boolean.valueOf(enabled);
    }
    
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

    public void setMeterEnabled(boolean enabled) {
        this.meterEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isMeterEnabled() {
        if (meterEnabled == null)
            meterEnabled = Boolean.valueOf(options.remove("-meter"));
        return meterEnabled.booleanValue();
    }
////////////////STOPPED HERE at metered///////////////////////////////
//TODO

    /**
     * Set what LRIDs to include in the transformed output as a regular
     * expression. Defaults to all: <code>Pattern.compile(".*")</code>
     * 
     * @param classPattern
     * @see #setClassPattern(Pattern)
     */
    public void setClassPattern(Pattern classPattern)
    {
        this.classPattern = classPattern;
    }

    /**
     * @see #setClassPattern(Pattern)
     */
    public void setClassPattern(String classPattern)
    {
        this.classPattern = Pattern.compile(classPattern);
    }

    /**
     * Returns the <code>Pattern</code> that specifies which LRIDs to
     * transform. Is subsequenty filtered by
     * {@link #getXPathQuery() XPath query}.
     * 
     * @return
     * @see #setClassPattern(Pattern)
     */
    public Pattern getClassPattern()
    {
        if (classPattern == null)
        {
            String lridSpec = pullParam("-lrid");
            if (lridSpec == null)
                lridSpec = ".*";

            classPattern = Pattern.compile(lridSpec);
        }

        return classPattern;
    }

    /**
     * Set the <a href="http://www.w3.org/TR/xpath">XPath query</a> to apply
     * after filtering by {@link #getClassPattern()() class pattern}.
     * 
     * @param xpathQuery
     * @see #getXPathQuery()
     */
    public void setXPathQuery(String xpathQuery)
    {
        this.xpathQuery = xpathQuery;
    }

    /**
     * Returns the <a href="http://www.w3.org/TR/xpath">XPath query</a> to
     * apply after filtering by {@link #getClassPattern()() class pattern}.
     * 
     * @return
     * @see #setXPathQuery(String)
     */
    public String getXPathQuery()
    {
        if (xpathQuery == null)
        {
            xpathQuery = pullParam("-query");
            if (xpathQuery == null)
                xpathQuery = "/LRIDs/LRID/*";
        }

        return xpathQuery;
    }

    /**
     * Returns <code>true</code> if the options indicate that the user wants
     * to see some help information.
     * 
     * @return
     */
    public boolean isHelpRequired()
    {
        return helpRequired;
    }
    
    
    public static String getLoadedFromJar() {
        String classpath = System.getProperty("java.class.path");
        return new StringTokenizer(classpath, File.pathSeparator).nextToken();
    }

    public Properties getDbProperties(StringBuffer loadedFrom) throws IOException {
        ResourceBundle bundle = null;
        String dbType = getDatabaseType();

        try {
            File propertiesFile = new File(dbType);
            bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
            loadedFrom.append(propertiesFile.getAbsolutePath());
        } catch (FileNotFoundException notFoundOnFilesystemWithoutExtension) {
            try {
                File propertiesFile = new File(dbType + ".properties");
                bundle = new PropertyResourceBundle(new FileInputStream(propertiesFile));
                loadedFrom.append(propertiesFile.getAbsolutePath());
            } catch (FileNotFoundException notFoundOnFilesystemWithExtensionTackedOn) {
                try {
                    bundle = ResourceBundle.getBundle(dbType);
                    loadedFrom.append("[" + getLoadedFromJar() + "]" + File.separator + dbType + ".properties");
                } catch (Exception notInJarWithoutPath) {
                    try {
                        String path = SchemaSpy.class.getPackage().getName() + ".dbTypes." + dbType;
                        path = path.replace('.', '/');
                        bundle = ResourceBundle.getBundle(path);
                        loadedFrom.append("[" + getLoadedFromJar() + "]/" + path + ".properties");
                    } catch (Exception notInJar) {
                        notInJar.printStackTrace();
                        notFoundOnFilesystemWithExtensionTackedOn.printStackTrace();
                        throw notFoundOnFilesystemWithoutExtension;
                    }
                }
            }
        }

        Properties properties;

        try {
            String baseDbType = bundle.getString("extends").trim();
            properties = getDbProperties(baseDbType, new StringBuffer());
        } catch (MissingResourceException doesntExtend) {
            properties = new Properties();
        }

        return add(properties, bundle);
    }

    /**
     * Add the contents of <code>bundle</code> to the specified <code>properties</code>.
     *
     * @param properties Properties
     * @param bundle ResourceBundle
     * @return Properties
     */
    private static Properties add(Properties properties, ResourceBundle bundle) {
        Enumeration iter = bundle.getKeys();
        while (iter.hasMoreElements()) {
            Object key = iter.nextElement();
            properties.put(key, bundle.getObject(key.toString()));
        }

        return properties;
    }

    public List getRemainingParameters()
    {
        return options;
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
            if (required) {
                dumpUsage("Parameter '" + paramId + "' missing." + (dbTypeSpecific ? "  It is required for this database type." : ""), !dbTypeSpecific, dbTypeSpecific);
                System.exit(1);
            } else {
                return null;
            }
        }
        options.remove(paramIndex);
        String param = options.get(paramIndex).toString();
        options.remove(paramIndex);
        return param;
    }
    

    /**
     * Allow an equal sign in args...like "-o=foo.bar". Useful for things like
     * Ant and Maven.
     * 
     * @param args
     *            List
     * @return List
     */
    private static List fixupArgs(List args)
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
     * 
     */
    public static void dumpUsage(PrintStream stream, String indent)
    {
        stream.println(indent + "-f inputLrids - what LRID file to read");
        stream.println(indent + "                defaults to standard input");
        stream.println(indent + "-o outputFile - where to sent the output");
        stream.println(indent + "                defaults to standard output");
        stream.println(indent + "-from begin   - beginning time");
        stream.println(indent + "                defaults to \"start of epoch\" (1970-01-01 00:00:00.000000000)");
        stream.println(indent + "-to end       - ending time");
        stream.println(indent + "                defaults to \"end of epoch\" (2262-04-11 23:47:16.854775807)");
        stream.println(indent + "-lrid regex   - which LRID classes to include");
        stream.println(indent + "                regular expression - defaults to all (\".*\")");
        stream.println(indent + "-query xpath  - after filtered by -lrid, XPath expression");
        stream.println(indent + "                 of which LRIDs to transform");
        stream.println(indent + "                defaults to \"/LRIDs/LRID/*\"");
        stream.println(indent + "-skip         skip bad records");
    }
}
