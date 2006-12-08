package net.sourceforge.schemaspy;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.xml.parsers.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;
import net.sourceforge.schemaspy.util.ConnectionURLBuilder.*;
import net.sourceforge.schemaspy.view.*;
import org.w3c.dom.*;

/**
 * @author John Currier
 */
public class SchemaAnalyzer {
	public int analyze(Config config) throws Exception {
		try {
            if (config.isHelpRequired()) {
                config.dumpUsage(null, false);
                return 1;
            }

            if (config.isDbHelpRequired()) {
                config.dumpUsage(null, true);
                return 1;
            }

            long start = System.currentTimeMillis();
            long startGraphingDetails = start;
            long startSummarizing = start;

            Properties properties = config.getDbProperties(config.getDbType());

            ConnectionURLBuilder urlBuilder = new ConnectionURLBuilder(config, properties);
            if (config.getDb() == null)
                config.setDb(urlBuilder.getConnectionURL());

            if (config.getRemainingParameters().size() != 0) {
                System.out.print("Warning: Unrecognized option(s):");
                for (Iterator iter = config.getRemainingParameters().iterator(); iter.hasNext(); )
                    System.out.print(" " + iter.next());
                System.out.println();
            }

            String driverClass = properties.getProperty("driver");
            String driverPath = properties.getProperty("driverPath");
            if (config.getClasspath() != null)
                driverPath = config.getClasspath() + File.pathSeparator + driverPath;

            Connection connection = getConnection(config, urlBuilder.getConnectionURL(), driverClass, driverPath);
            if (connection == null)
            	return 3;
            
            DatabaseMetaData meta = connection.getMetaData();
            String dbName = config.getDb();
            String schema = config.getSchema();
            File outputDir = config.getOutputDir();

            if (config.isEvaluateAllEnabled()) {
                List args = config.asList();
                Iterator iter = urlBuilder.getOptions().iterator();
                while (iter.hasNext()) {
                    DbOption option = (DbOption)iter.next();
                    if (!args.contains("-" + option.name)) {
                        args.add("-" + option.name);
                        args.add(option.value);
                    }
                }
                    
                yankParam(args, "-o");  // param will be replaced by something appropriate
                yankParam(args, "-s");  // param will be replaced by something appropriate
                args.remove("-all");    // param will be replaced by something appropriate
                
                String schemaSpec = config.getSchemaSpec();
                if (schemaSpec == null)
                    schemaSpec = properties.getProperty("schemaSpec", ".*");
                return MultipleSchemaAnalyzer.getInstance().analyze(dbName, meta, schemaSpec, args, config.getUser(), outputDir, Config.getLoadedFromJar());
            }

            if (schema == null && meta.supportsSchemasInTableDefinitions()) {
                schema = config.getUser();
                config.setSchema(schema);
            }

            if (config.isHtmlGenerationEnabled()) {
                new File(outputDir, "tables").mkdirs();
                new File(outputDir, "graphs/summary").mkdirs();
                StyleSheet.init(new BufferedReader(getStyleSheet(config.getCss())));

                System.out.println("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());
                System.out.println();
                System.out.print("Gathering schema details");
            }

            //
            // create the spy
            //
            SchemaSpy spy = new SchemaSpy(connection, meta, dbName, schema, config.getDescription(), properties, config.getTableInclusions(), config.getMaxDbThreads());
            Database db = spy.getDatabase();

            LineWriter out;
            Collection tables = new ArrayList(db.getTables());
            tables.addAll(db.getViews());

            if (tables.isEmpty()) {
                dumpNoTablesMessage(schema, config.getUser(), meta, config.getTableInclusions() != null);
                return 2;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element rootNode = document.createElement("database");
            document.appendChild(rootNode);
            DOMUtil.appendAttribute(rootNode, "name", dbName);
            if (schema != null)
                DOMUtil.appendAttribute(rootNode, "schema", schema);
            DOMUtil.appendAttribute(rootNode, "type", db.getDatabaseProduct());

            if (config.isHtmlGenerationEnabled()) {
                startSummarizing = System.currentTimeMillis();
                System.out.println("(" + (startSummarizing - start) / 1000 + "sec)");
                System.out.print("Writing/graphing summary");
                System.out.print(".");
                ImageWriter.getInstance().writeImages(outputDir);
                System.out.print(".");

                boolean showDetailedTables = tables.size() <= config.getMaxDetailedTables();
                final boolean includeImpliedConstraints = config.isImpliedConstraintsEnabled();

                File graphsDir = new File(outputDir, "graphs/summary");
                String dotBaseFilespec = "relationships";
                out = new LineWriter(new FileOutputStream(new File(graphsDir, dotBaseFilespec + ".real.compact.dot")));
                WriteStats stats = new WriteStats(config.getColumnExclusions(), includeImpliedConstraints);
                DotFormatter.getInstance().writeRealRelationships(db, tables, true, showDetailedTables, stats, out);
                boolean hasRealRelationships = stats.getNumTablesWritten() > 0 || stats.getNumViewsWritten() > 0;
                stats = new WriteStats(stats);
                out.close();

                if (hasRealRelationships) {
                    System.out.print(".");
                    out = new LineWriter(new FileOutputStream(new File(graphsDir, dotBaseFilespec + ".real.large.dot")));
                    DotFormatter.getInstance().writeRealRelationships(db, tables, false, showDetailedTables, stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                }

                // getting implied constraints has a side-effect of associating the parent/child tables, so don't do it
                // here unless they want that behavior
                List impliedConstraints = null;
                if (includeImpliedConstraints)
                    impliedConstraints = DBAnalyzer.getImpliedConstraints(tables);
                else
                    impliedConstraints = new ArrayList();

                List orphans = DBAnalyzer.getOrphans(tables);
                boolean hasOrphans = !orphans.isEmpty() && Dot.getInstance().isValid();

                System.out.print(".");

                File impliedDotFile = new File(graphsDir, dotBaseFilespec + ".implied.compact.dot");
                out = new LineWriter(new FileOutputStream(impliedDotFile));
                stats = new WriteStats(config.getColumnExclusions(), includeImpliedConstraints);
                DotFormatter.getInstance().writeAllRelationships(db, tables, true, showDetailedTables, stats, out);
                boolean hasImplied = stats.wroteImplied();
                Set excludedColumns = stats.getExcludedColumns();
                stats = new WriteStats(stats);
                out.close();
                if (hasImplied) {
                    impliedDotFile = new File(graphsDir, dotBaseFilespec + ".implied.large.dot");
                    out = new LineWriter(new FileOutputStream(impliedDotFile));
                    DotFormatter.getInstance().writeAllRelationships(db, tables, false, showDetailedTables, stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                } else {
                    impliedDotFile.delete();
                }

                out = new LineWriter(new FileWriter(new File(outputDir, dotBaseFilespec + ".html")));
                HtmlRelationshipsPage.getInstance().write(db, graphsDir, dotBaseFilespec, hasOrphans, hasRealRelationships, hasImplied, excludedColumns, out);
                out.close();

                System.out.print(".");
                dotBaseFilespec = "utilities";
                out = new LineWriter(new FileWriter(new File(outputDir, dotBaseFilespec + ".html")));
                HtmlOrphansPage.getInstance().write(db, orphans, graphsDir, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                out = new LineWriter(new FileWriter(new File(outputDir, "index.html")), 64 * 1024);
                HtmlMainIndexPage.getInstance().write(db, tables, hasOrphans, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                List constraints = DBAnalyzer.getForeignKeyConstraints(tables);
                out = new LineWriter(new FileWriter(new File(outputDir, "constraints.html")), 256 * 1024);
                HtmlConstraintsPage constraintIndexFormatter = HtmlConstraintsPage.getInstance();
                constraintIndexFormatter.write(db, constraints, tables, hasOrphans, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                out = new LineWriter(new FileWriter(new File(outputDir, "anomalies.html")), 16 * 1024);
                HtmlAnomaliesPage.getInstance().write(db, tables, impliedConstraints, hasOrphans, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                Iterator iter = HtmlColumnsPage.getInstance().getColumnInfos().iterator();
                while (iter.hasNext()) {
                    HtmlColumnsPage.ColumnInfo columnInfo = (HtmlColumnsPage.ColumnInfo)iter.next();
                    out = new LineWriter(new FileWriter(new File(outputDir, columnInfo.getLocation())), 16 * 1024);
                    HtmlColumnsPage.getInstance().write(db, tables, columnInfo, hasOrphans, out);
                    stats = new WriteStats(stats);
                    out.close();
                }


                startGraphingDetails = System.currentTimeMillis();
                System.out.println("(" + (startGraphingDetails - startSummarizing) / 1000 + "sec)");
                System.out.print("Writing/graphing results");

                HtmlTablePage tableFormatter = HtmlTablePage.getInstance();
                for (iter = tables.iterator(); iter.hasNext(); ) {
                    System.out.print('.');
                    Table table = (Table)iter.next();
                    out = new LineWriter(new FileWriter(new File(outputDir, "tables/" + table.getName() + ".html")), 24 * 1024);
                    tableFormatter.write(db, table, hasOrphans, outputDir, stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                }

                out = new LineWriter(new FileWriter(new File(outputDir, "schemaSpy.css")));
                StyleSheet.getInstance().write(out);
                out.close();
                out = new LineWriter(new FileWriter(new File(outputDir, "schemaSpy.js")));
                JavaScriptFormatter.getInstance().write(out);
                out.close();
            }


            XmlTableFormatter.getInstance().appendTables(rootNode, tables);
            
            String xmlName = dbName;
            if (schema != null)
                xmlName += '.' + schema;
            // use OutputStream constructor to force it to use UTF8 (per Bernard D'Havé)
            out = new LineWriter(new FileOutputStream(new File(outputDir, xmlName + ".xml")));
            document.getDocumentElement().normalize();
            DOMUtil.printDOM(document, out);
            out.close();

            // 'try' to make some memory available for the sorting process
            // (some people have run out of memory while RI sorting tables)
            builder = null;
            connection = null;
            db = null;
            document = null;
            factory = null;
            meta = null;
            properties = null;
            rootNode = null;
            urlBuilder = null;
            
            List recursiveConstraints = new ArrayList();

            // side effect is that the RI relationships get trashed
            // also populates the recursiveConstraints collection
            List orderedTables = spy.sortTablesByRI(recursiveConstraints);

            out = new LineWriter(new FileWriter(new File(outputDir, "insertionOrder.txt")), 16 * 1024);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            out = new LineWriter(new FileWriter(new File(outputDir, "deletionOrder.txt")), 16 * 1024);
            Collections.reverse(orderedTables);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            /* we'll eventually want to put this functionality back in with a
             * database independent implementation
            File constraintsFile = new File(outputDir, "removeRecursiveConstraints.sql");
            constraintsFile.delete();
            if (!recursiveConstraints.isEmpty()) {
                out = new LineWriter(new FileWriter(constraintsFile), 4 * 1024);
                writeRemoveRecursiveConstraintsSql(recursiveConstraints, schema, out);
                out.close();
            }

            constraintsFile = new File(outputDir, "restoreRecursiveConstraints.sql");
            constraintsFile.delete();

            if (!recursiveConstraints.isEmpty()) {
                out = new LineWriter(new FileWriter(constraintsFile), 4 * 1024);
                writeRestoreRecursiveConstraintsSql(recursiveConstraints, schema, out);
                out.close();
            }
            */

            if (config.isHtmlGenerationEnabled()) {
                long end = System.currentTimeMillis();
                System.out.println("(" + (end - startGraphingDetails) / 1000 + "sec)");
                System.out.println("Wrote relationship details of " + tables.size() + " tables/views to directory '" + config.getOutputDir() + "' in " + (end - start) / 1000 + " seconds.");
                System.out.println("View the results by opening " + new File(config.getOutputDir(), "index.html"));
            }
            
            return 0;
        } catch (Config.MissingRequiredParameterException missingParam) {
            config.dumpUsage(missingParam.getMessage(), missingParam.isDbTypeSpecific());
            return 1;
        }
	}

    /**
     * dumpNoDataMessage
     *
     * @param schema String
     * @param user String
     * @param meta DatabaseMetaData
     */
    private static void dumpNoTablesMessage(String schema, String user, DatabaseMetaData meta, boolean specifiedInclusions) throws SQLException {
        System.out.println();
        System.out.println();
        System.out.println("No tables or views were found in schema '" + schema + "'.");
        List schemas = DBAnalyzer.getSchemas(meta);
        if (schema == null || schemas.contains(schema)) {
            System.out.println("The schema exists in the database, but the user you specified (" + user + ')');
            System.out.println("  might not have rights to read its contents.");
            if (specifiedInclusions) {
                System.out.println("Another possibility is that the regular expression that you specified");
                System.out.println("  for what to include (via -i) didn't match any tables.");
            }
        } else {
            System.out.println("The schema does not exist in the database.");
            System.out.println("Make sure that you specify a valid schema with the -s option and that");
            System.out.println("  the user specified (" + user + ") can read from the schema.");
            System.out.println("Note that schema names are usually case sensitive.");
        }
        System.out.println();
        boolean plural = schemas.size() != 1;
        System.out.println(schemas.size() + " schema" + (plural ? "s" : "") + " exist" + (plural ? "" : "s") + " in this database.");
        System.out.println("Some of these \"schemas\" may be users or system schemas.");
        System.out.println();
        Iterator iter = schemas.iterator();
        while (iter.hasNext()) {
            System.out.print(iter.next() + " ");
        }

        System.out.println();
        System.out.println("These schemas contain tables/views that user '" + user + "' can see:");
        System.out.println();
        iter = DBAnalyzer.getPopulatedSchemas(meta).iterator();
        while (iter.hasNext()) {
            System.out.print(iter.next() + " ");
        }
    }

    private static Connection getConnection(Config config, String connectionURL,
                      String driverClass, String driverPath) throws FileNotFoundException, IOException {
        System.out.println("Using database properties:");
        System.out.println("    " + config.getDbPropertiesLoadedFrom());

        List classpath = new ArrayList();
		List invalidClasspathEntries = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(driverPath, File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            File pathElement = new File(tokenizer.nextToken());
            if (pathElement.exists())
                classpath.add(pathElement.toURL());
            else
				invalidClasspathEntries.add(pathElement);
        }

        URLClassLoader loader = new URLClassLoader((URL[])classpath.toArray(new URL[0]));
        Driver driver = null;
        try {
            driver = (Driver)Class.forName(driverClass, true, loader).newInstance();

            // have to use deprecated method or we won't see messages generated by older drivers
            //java.sql.DriverManager.setLogStream(System.err);
        } catch (Exception exc) {
            System.err.println(exc); // people don't want to see a stack trace...
            System.err.println();
            System.err.println("Failed to load driver '" + driverClass + "' from: " + classpath);
			if (!invalidClasspathEntries.isEmpty()) {
				if (invalidClasspathEntries.size() == 1)
					System.err.print("This entry doesn't point to a valid file/directory: ");
				else
					System.err.print("These entries don't point to valid files/directories: ");
				System.err.println(invalidClasspathEntries);
			}
            System.err.println();
            System.err.println("Use -t [databaseType] to specify what drivers to use or modify");
            System.err.println("one of the .properties from the jar, put it on your file");
            System.err.println("system and point to it with -t [databasePropertiesFile].");
            System.err.println();
            System.err.println("For many people it's easiest to use the -cp option to directly specify");
            System.err.println("where the database drivers exist (usually in a .jar or .zip/.Z).");
			System.err.println("Note that the -cp option must be specified AFTER " + Config.getLoadedFromJar());
            System.err.println();
            return null;
        }

        Properties connectionProperties = new Properties();
        connectionProperties.put("user", config.getUser());
        if (config.getPassword() != null)
            connectionProperties.put("password", config.getPassword());
        connectionProperties.putAll(config.getConnectionProperties());

        Connection connection = null;
        try {
            connection = driver.connect(connectionURL, connectionProperties);
            if (connection == null) {
                System.err.println();
                System.err.println("Cannot connect to this database URL:");
                System.err.println("  " + connectionURL);
                System.err.println("with this driver:");
                System.err.println("  " + driverClass);
                System.err.println();
                System.err.println("Additional connection information may be available in ");
                System.err.println("  " + config.getDbPropertiesLoadedFrom());
            }
        } catch (UnsatisfiedLinkError badPath) {
            System.err.println();
            System.err.println("Failed to load driver [" + driverClass + "] from classpath " + classpath);
            System.err.println();
            System.err.println("Make sure the reported library (.dll/.lib/.so) from the following line can be");
            System.err.println("found by your PATH (or LIB*PATH) environment variable");
            System.err.println();
            badPath.printStackTrace();
        } catch (Exception exc) {
            System.err.println();
            System.err.println("Failed to connect to database URL [" + connectionURL + "]");
            System.err.println();
            exc.printStackTrace();
        }

        return connection;
    }

    /**
     * Currently very DB2-specific
     * @param recursiveConstraints List
     * @param schema String
     * @param out LineWriter
     * @throws IOException
     */
    /* we'll eventually want to put this functionality back in with a
     * database independent implementation
    private static void writeRemoveRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.writeln("ALTER TABLE " + schema + "." + constraint.getChildTable() + " DROP CONSTRAINT " + constraint.getName() + ";");
        }
    }
    */

    /**
     * Currently very DB2-specific
     * @param recursiveConstraints List
     * @param schema String
     * @param out LineWriter
     * @throws IOException
     */
    /* we'll eventually want to put this functionality back in with a
     * database independent implementation
    private static void writeRestoreRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        Map ruleTextMapping = new HashMap();
        ruleTextMapping.put(new Character('C'), "CASCADE");
        ruleTextMapping.put(new Character('A'), "NO ACTION");
        ruleTextMapping.put(new Character('N'), "NO ACTION"); // Oracle
        ruleTextMapping.put(new Character('R'), "RESTRICT");
        ruleTextMapping.put(new Character('S'), "SET NULL");  // Oracle

        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.write("ALTER TABLE \"" + schema + "\".\"" + constraint.getChildTable() + "\" ADD CONSTRAINT \"" + constraint.getName() + "\"");
            StringBuffer buf = new StringBuffer();
            for (Iterator columnIter = constraint.getChildColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" FOREIGN KEY (" + buf.toString() + ")");
            out.write(" REFERENCES \"" + schema + "\".\"" + constraint.getParentTable() + "\"");
            buf = new StringBuffer();
            for (Iterator columnIter = constraint.getParentColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" (" + buf.toString() + ")");
            out.write(" ON DELETE ");
            out.write(ruleTextMapping.get(new Character(constraint.getDeleteRule())).toString());
            out.write(" ON UPDATE ");
            out.write(ruleTextMapping.get(new Character(constraint.getUpdateRule())).toString());
            out.writeln(";");
        }
    }
    */

    private static void yankParam(List args, String paramId) {
        int paramIndex = args.indexOf(paramId);
        if (paramIndex >= 0) {
            args.remove(paramIndex);
            args.remove(paramIndex);
        }
    }

    private static Reader getStyleSheet(String cssName) throws IOException {
        File cssFile = new File(cssName);
        if (cssFile.exists())
            return new FileReader(cssFile);
        cssFile = new File(System.getProperty("user.dir"), cssName);
        if (cssFile.exists())
            return new FileReader(cssFile);
        
        InputStream cssStream = StyleSheet.class.getClassLoader().getResourceAsStream(cssName);
        if (cssStream == null)
            throw new IllegalStateException("Unable to find requested style sheet: " + cssName);
        return new InputStreamReader(cssStream);
    }
}
