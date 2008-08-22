package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;
import net.sourceforge.schemaspy.model.View;
import net.sourceforge.schemaspy.util.CaseInsensitiveMap;
import net.sourceforge.schemaspy.util.HtmlEncoder;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlTablePage extends HtmlFormatter {
    private static final HtmlTablePage instance = new HtmlTablePage();
    private Set<String> keywords = null;
    private int columnCounter = 0;

    private Map<String, String> defaultValueAliases = new HashMap<String, String>();
    {
        defaultValueAliases.put("CURRENT TIMESTAMP", "now"); // DB2
        defaultValueAliases.put("CURRENT TIME", "now");      // DB2
        defaultValueAliases.put("CURRENT DATE", "now");      // DB2
        defaultValueAliases.put("SYSDATE", "now");           // Oracle
        defaultValueAliases.put("CURRENT_DATE", "now");      // Oracle
    }

    /**
     * Singleton...don't allow instantiation
     */
    private HtmlTablePage() {
    }

    public static HtmlTablePage getInstance() {
        return instance;
    }

    public WriteStats write(Database db, Table table, boolean hasOrphans, File outputDir, WriteStats stats, LineWriter out) throws IOException {
        File diagramsDir = new File(outputDir, "diagrams");
        boolean hasImplied = generateDots(table, diagramsDir, stats);

        writeHeader(db, table, null, hasOrphans, out);
        out.writeln("<table width='100%' border='0'>");
        out.writeln("<tr valign='top'><td class='container' align='left' valign='top'>");
        writeHeader(table, hasImplied, out);
        out.writeln("</td><td class='container' rowspan='2' align='right' valign='top'>");
        writeLegend(true, out);
        out.writeln("</td><tr valign='top'><td class='container' align='left' valign='top'>");
        boolean onCascadeDelete = writeMainTable(table, out);
        writeNumRows(db, table, out);
        out.writeln("</td></tr></table>");
        writeCheckConstraints(table, out);
        writeIndexes(table, out);
        writeView(table, db, out);
        writeDiagram(table, stats, diagramsDir, out);
        writeFooter(onCascadeDelete, out);

        return stats;
    }

    private void writeHeader(Table table, boolean hasImplied, LineWriter html) throws IOException {
        html.writeln("<form name='options' action=''>");
        if (hasImplied) {
            html.write(" <label for='implied'><input type=checkbox id='implied'");
            if (table.isOrphan(false))
                html.write(" checked");
            html.writeln(">Implied relationships</label>");
        }

        // initially show comments if any of the columns contain comments
        boolean showCommentsInitially = false;
        for (TableColumn column : table.getColumns()) {
            if (column.getComments() != null) {
                showCommentsInitially = true;
                break;
            }
        }
        
        html.writeln(" <label for='showRelatedCols'><input type=checkbox id='showRelatedCols'>Related columns</label>");
        html.writeln(" <label for='showConstNames'><input type=checkbox id='showConstNames'>Constraint names</label>");
        html.writeln(" <label for='showComments'><input type=checkbox " + (showCommentsInitially  ? "checked " : "") + "id='showComments'>Comments</label>");
        html.writeln(" <label for='showLegend'><input type=checkbox checked id='showLegend'>Legend</label>");
        html.writeln("</form>");
    }

    public boolean writeMainTable(Table table, LineWriter out) throws IOException {
        boolean onCascadeDelete = false;
        
        HtmlColumnsPage.getInstance().writeMainTableHeader(table.getId() != null, null, out);

        out.writeln("<tbody valign='top'>");
        Set<TableColumn> primaries = new HashSet<TableColumn>(table.getPrimaryColumns());
        Set<TableColumn> indexedColumns = new HashSet<TableColumn>();
        for (TableIndex index : table.getIndexes()) {
            indexedColumns.addAll(index.getColumns());
        }
        
        boolean showIds = table.getId() != null;
        for (TableColumn column : table.getColumns()) {
            onCascadeDelete = writeColumn(column, null, primaries, indexedColumns, onCascadeDelete, showIds, out);
        }
        out.writeln("</table>");

        return onCascadeDelete;
    }

    public boolean writeColumn(TableColumn column, String tableName, Set<TableColumn> primaries, Set<TableColumn> indexedColumns, boolean onCascadeDelete, boolean showIds, LineWriter out) throws IOException {
        if (tableName != null) {
            if (++columnCounter % 2 == 0)
                out.writeln("<tr class='odd'>");
            else
                out.writeln("<tr class='even'>");
        } else {
            out.writeln("<tr>");
        }
        if (showIds) {
            out.write(" <td class='detail' align='right'>");
            out.write(String.valueOf(column.getId()));
            out.writeln("</td>");
        }
        if (tableName != null) {
            out.write(" <td class='detail'><a href='tables/");
            out.write(tableName);
            out.write(".html'>");
            out.write(tableName);
            out.writeln("</a></td>");
        }
        if (primaries.contains(column))
            out.write(" <td class='primaryKey' title='Primary Key'>");
        else if (indexedColumns.contains(column))
            out.write(" <td class='indexedColumn' title='Indexed'>");
        else
            out.write(" <td class='detail'>");
        out.write(column.getName());
        out.writeln("</td>");
        out.write(" <td class='detail'>");
        out.write(column.getType().toLowerCase());
        out.writeln("</td>");
        out.write(" <td class='detail' align='right'>");
        out.write(column.getDetailedSize());
        out.writeln("</td>");
        out.write(" <td class='detail' align='center'");
        if (column.isNullable())
            out.write(" title='nullable'>&nbsp;&radic;&nbsp;");
        else
            out.write(">");
        out.writeln("</td>");
        out.write(" <td class='detail' align='center'");
        if (column.isAutoUpdated()) {
            out.write(" title='Automatically updated by the database'>&nbsp;&radic;&nbsp;");
        } else {
            out.write(">");
        }
        out.writeln("</td>");

        Object defaultValue = column.getDefaultValue();
        if (defaultValue != null || column.isNullable()) {
            Object alias = defaultValueAliases.get(String.valueOf(defaultValue).trim());
            if (alias != null) {
                out.write(" <td class='detail' align='right' title='");
                out.write(String.valueOf(defaultValue));
                out.write("'><i>");
                out.write(alias.toString());
                out.writeln("</i></td>");
            } else {
                out.write(" <td class='detail' align='right'>");
                out.write(String.valueOf(defaultValue));
                out.writeln("</td>");
            }
        } else {
            out.writeln(" <td class='detail'></td>");
        }
        out.write(" <td class='detail'>");
        String path = tableName == null ? "" : "tables/";
        onCascadeDelete |= writeRelatives(column, false, path, out);
        out.writeln("</td>");
        out.write(" <td class='detail'>");
        onCascadeDelete |= writeRelatives(column, true, path, out);
        out.writeln(" </td>");
        out.write(" <td class='comment detail'>");
        String comments = column.getComments();
        if (comments != null) {
            if (encodeComments)
                for (int i = 0; i < comments.length(); ++i)
                    out.write(HtmlEncoder.encode(comments.charAt(i)));
            else
                out.write(comments);
        }
        out.writeln("</td>");
        out.writeln("</tr>");
        return onCascadeDelete;
    }

    /**
     * Write our relatives
     * @param tableName String
     * @param baseRelative TableColumn
     * @param dumpParents boolean
     * @param out LineWriter
     * @throws IOException
     * @return boolean - true if relatives are involved in an on cascade delete relationships
     */
    private boolean writeRelatives(TableColumn baseRelative, boolean dumpParents, String path, LineWriter out) throws IOException {
        boolean onCascadeDelete = false;
        Set<TableColumn> columns = dumpParents ? baseRelative.getParents() : baseRelative.getChildren();
        final int numColumns = columns.size();

        if (numColumns > 0) {
            out.newLine();
            out.writeln("  <table border='0' width='100%' cellspacing='0' cellpadding='0'>");
        }

        for (TableColumn column : columns) {
            String columnTableName = column.getTable().getName();
            ForeignKeyConstraint constraint = dumpParents ? column.getChildConstraint(baseRelative) : column.getParentConstraint(baseRelative);
            if (constraint.isImplied())
                out.writeln("   <tr class='impliedRelationship relative' valign='top'>");
            else
                out.writeln("   <tr class='relative' valign='top'>");
            out.write("    <td class='relatedTable' title=\"");
            out.write(constraint.toString());
            out.write("\">");
            out.write("<a href='");
            out.write(path);
            if (column.getTable().isRemote()) {
                out.write("../../" + column.getTable().getSchema() + "/tables/");
            }
            out.write(columnTableName);
            out.write(".html'>");
            out.write(columnTableName);
            out.write("</a>");
            out.write("<span class='relatedKey'>.");
            out.write(column.getName());
            out.writeln("</span>");
            if (constraint.isOnDeleteCascade()) {
                out.write("<span title='On Delete Cascade\n Automatically deletes child tables when their parent is deleted'>*</span>");
                onCascadeDelete = true;
            }
            out.writeln("    </td>");

            out.write("    <td class='constraint'>");
            out.write(constraint.getName());
            out.writeln("</td>");

            out.writeln("   </tr>");
        }
        if (numColumns > 0) {
            out.writeln("  </table>");
        }

        return onCascadeDelete;
    }

    private void writeNumRows(Database db, Table table, LineWriter out) throws IOException {
        if (displayNumRows && !table.isView())
            out.write("<p>Table contained " + NumberFormat.getIntegerInstance().format(table.getNumRows()) + " rows at ");
        else
            out.write("<p>Analyzed at ");
        out.writeln(db.getConnectTime());
    }

    private void writeCheckConstraints(Table table, LineWriter out) throws IOException {
        Map<String, String> constraints = table.getCheckConstraints();
        if (constraints != null && !constraints.isEmpty()) {
            out.writeln("<div class='indent'>");
            out.writeln("<b>Requirements (check constraints):</b>");
            out.writeln("<table class='dataTable' border='1' rules='groups'><colgroup><colgroup>");
            out.writeln("<thead>");
            out.writeln(" <tr>");
            out.writeln("  <th>Constraint</th>");
            out.writeln("  <th class='constraint' style='text-align:left;'>Constraint Name</th>");
            out.writeln(" </tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");
            for (String name : constraints.keySet()) {
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                out.write(constraints.get(name).toString());
                out.writeln("</td>");
                out.write("  <td class='constraint' style='text-align:left;'>");
                out.write(name);
                out.writeln("</td>");
                out.writeln(" </tr>");
            }
            out.writeln("</table></div><p>");
        }
    }

    private void writeIndexes(Table table, LineWriter out) throws IOException {
        boolean showId = table.getId() != null;
        Set<TableIndex> indexes = table.getIndexes();
        if (indexes != null && !indexes.isEmpty()) {
            // see if we've got any strangeness so we can have the correct number of colgroups
            boolean containsAnomalies = false;
            for (TableIndex index : indexes) {
                containsAnomalies = index.isUniqueNullable();
                if (containsAnomalies)
                    break;
            }

            out.writeln("<div class='indent'>");
            out.writeln("<b>Indexes:</b>");
            out.writeln("<table class='dataTable' border='1' rules='groups'><colgroup><colgroup><colgroup><colgroup>" + (showId ? "<colgroup>" : "") + (containsAnomalies ? "<colgroup>" : ""));
            out.writeln("<thead>");
            out.writeln(" <tr>");
            if (showId)
                out.writeln("  <th>ID</th>");
            out.writeln("  <th>Column(s)</th>");
            out.writeln("  <th>Type</th>");
            out.writeln("  <th>Sort</th>");
            out.writeln("  <th class='constraint' style='text-align:left;'>Constraint Name</th>");
            if (containsAnomalies)
                out.writeln("  <th>Anomalies</th>");
            out.writeln(" </tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");

            indexes = new TreeSet<TableIndex>(indexes); // sort primary keys first

            for (TableIndex index : indexes) {
                out.writeln(" <tr>");

                if (showId) {
                    out.write("  <td class='detail' align='right'>");
                    out.write(String.valueOf(index.getId()));
                    out.writeln("</td>");
                }

                if (index.isPrimaryKey())
                    out.write("  <td class='primaryKey'>");
                else
                    out.write("  <td class='indexedColumn'>");
                String columns = index.getColumnsAsString();
                if (columns.startsWith("+"))
                    columns = columns.substring(1);
                out.write(columns);
                out.writeln("</td>");

                out.write("  <td class='detail'>");
                out.write(index.getType());
                out.writeln("</td>");

                out.write("  <td class='detail' style='text-align:left;'>");
                Iterator<TableColumn> columnsIter = index.getColumns().iterator();
                while (columnsIter.hasNext()) {
                    TableColumn column = columnsIter.next();
                    if (index.isAscending(column))
                        out.write("<span title='Ascending'>Asc</span>");
                    else
                        out.write("<span title='Descending'>Desc</span>");
                    if (columnsIter.hasNext())
                        out.write("/");
                }
                out.writeln("</td>");

                out.write("  <td class='constraint' style='text-align:left;'>");
                out.write(index.getName());
                out.writeln("</td>");

                if (index.isUniqueNullable()) {
                    if (index.getColumns().size() == 1)
                        out.writeln("  <td class='detail'>This unique column is also nullable</td>");
                    else
                        out.writeln("  <td class='detail'>These unique columns are also nullable</td>");
                } else if (containsAnomalies) {
                    out.writeln("  <td>&nbsp;</td>");
                }
                out.writeln(" </tr>");
            }
            out.writeln("</table>");
            out.writeln("</div>");
        }
    }

    private void writeView(Table table, Database db, LineWriter out) throws IOException {
        String sql;
        if (table.isView() && (sql = table.getViewSql()) != null) {
            Map<String, Table> tables = new CaseInsensitiveMap<Table>();
            
            for (Table t : db.getTables())
                tables.put(t.getName(), t);
            for (View v : db.getViews())
                tables.put(v.getName(), v);
            
            out.writeln("<div class='indent'>");
            out.writeln("View SQL:");
            out.writeln("<table class='dataTable' border='1' width='100%'>");
            out.writeln("<tbody>");
            out.writeln(" <tr>");
            out.write("  <td class='detail'>");

            @SuppressWarnings("hiding")
            Set<String> keywords = getKeywords(db.getMetaData());
            StringTokenizer tokenizer = new StringTokenizer(sql, " \t\n\r\f()<>|.,", true);
            while (tokenizer.hasMoreTokens()) {
                String nextToken = tokenizer.nextToken();
                if (keywords.contains(nextToken.toUpperCase())) {
                    out.write("<b>");
                    out.write(nextToken);
                    out.write("</b>");
                } else {
                    Table t = tables.get(nextToken);
                    if (t != null) {
                        out.write("<a href='");
                        out.write(t.getName());
                        out.write(".html'>");
                        out.write(t.getName());
                        out.write("</a>");
                    } else {
                        out.write(HtmlEncoder.encode(nextToken));
                    }
                }
            }

            out.writeln("</td>");
            out.writeln(" </tr>");
            out.writeln("</table>");
            out.writeln("</div>");
        }
    }

    /**
     * getSqlKeywords
     *
     * @return Object
     */
    private Set<String> getKeywords(DatabaseMetaData meta) {
        if (keywords == null) {
            keywords = new HashSet<String>(Arrays.asList(new String[] {
                "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND",
                "ANY", "ARE", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG",
                "BEGIN", "BETWEEN", "BIT", "BIT_LENGTH", "BOTH", "BY",
                "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER",
                "CHAR_LENGTH", "CHARACTER_LENGTH", "CHECK", "CLOSE", "COALESCE",
                "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONNECT", "CONNECTION",
                "CONSTRAINT", "CONSTRAINTS", "CONTINUE", "CONVERT", "CORRESPONDING",
                "COUNT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME",
                "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR",
                "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT",
                "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DESCRIBE", "DESCRIPTOR",
                "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DOMAIN", "DOUBLE", "DROP",
                "ELSE", "END", "END - EXEC", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC",
                "EXECUTE", "EXISTS", "EXTERNAL", "EXTRACT",
                "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND", "FROM", "FULL",
                "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP",
                "HAVING", "HOUR",
                "IDENTITY", "IMMEDIATE", "IN", "INDICATOR", "INITIALLY", "INNER", "INPUT",
                "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO",
                "IS", "ISOLATION",
                "JOIN",
                "KEY",
                "LANGUAGE", "LAST", "LEADING", "LEFT", "LEVEL", "LIKE", "LOCAL", "LOWER",
                "MATCH", "MAX", "MIN", "MINUTE", "MODULE", "MONTH",
                "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NEXT", "NO", "NOT", "NULL",
                "NULLIF", "NUMERIC",
                "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR", "ORDER",
                "OUTER", "OUTPUT", "OVERLAPS",
                "PAD", "PARTIAL", "POSITION", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY",
                "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC",
                "READ", "REAL", "REFERENCES", "RELATIVE", "RESTRICT", "REVOKE", "RIGHT",
                "ROLLBACK", "ROWS",
                "SCHEMA", "SCROLL", "SECOND", "SECTION", "SELECT", "SESSION", "SESSION_USER",
                "SET", "SIZE", "SMALLINT", "SOME", "SPACE", "SQL", "SQLCODE", "SQLERROR",
                "SQLSTATE", "SUBSTRING", "SUM", "SYSTEM_USER",
                "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR",
                "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION", "TRANSLATE",
                "TRANSLATION", "TRIM", "TRUE",
                "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USAGE", "USER", "USING",
                "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW",
                "WHEN", "WHENEVER", "WHERE", "WITH", "WORK", "WRITE",
                "YEAR",
                "ZONE"
            }));

            try {
                String keywordsArray[] = new String[] {
                    meta.getSQLKeywords(),
                    meta.getSystemFunctions(),
                    meta.getNumericFunctions(),
                    meta.getStringFunctions(),
                    meta.getTimeDateFunctions()
                };
                for (int i = 0; i < keywordsArray.length; ++i) {
                    StringTokenizer tokenizer = new StringTokenizer(keywordsArray[i].toUpperCase(), ",");

                    while (tokenizer.hasMoreTokens()) {
                        keywords.add(tokenizer.nextToken().trim());
                    }
                }
            } catch (Exception exc) {
                // don't totally fail just because we can't extract these details...
                System.err.println(exc);
            }
        }

        return keywords;
    }

    /**
     * Generate the .dot file(s) to represent the specified table's relationships.
     *
     * Generates a <TABLENAME>.dot if the table has real relatives.
     *
     * Also generates a <TABLENAME>..implied2degrees.dot if the table has implied relatives within
     * two degrees of separation.
     *
     * @param table Table
     * @param diagramsDir File
     * @throws IOException
     * @return boolean <code>true</code> if the table has implied relatives within two
     *                 degrees of separation.
     */
    private boolean generateDots(Table table, File diagramDir, WriteStats stats) throws IOException {
        File oneDegreeDotFile = new File(diagramDir, table.getName() + ".1degree.dot");
        File oneDegreeDiagramFile = new File(diagramDir, table.getName() + ".1degree.png");
        File twoDegreesDotFile = new File(diagramDir, table.getName() + ".2degrees.dot");
        File twoDegreesDiagramFile = new File(diagramDir, table.getName() + ".2degrees.png");
        File impliedDotFile = new File(diagramDir, table.getName() + ".implied2degrees.dot");
        File impliedDiagramFile = new File(diagramDir, table.getName() + ".implied2degrees.png");
        
        // delete before we start because we'll use the existence of these files to determine
        // if they should be turned into pngs & presented
        oneDegreeDotFile.delete();
        oneDegreeDiagramFile.delete();
        twoDegreesDotFile.delete();
        twoDegreesDiagramFile.delete();
        impliedDotFile.delete();
        impliedDiagramFile.delete();

        if (table.getMaxChildren() + table.getMaxParents() > 0) {
            Set<ForeignKeyConstraint> impliedConstraints;
            
            DotFormatter formatter = DotFormatter.getInstance();
            LineWriter dotOut = new LineWriter(oneDegreeDotFile, Config.DOT_CHARSET);
            formatter.writeRealRelationships(table, false, stats, dotOut);
            dotOut.close();

            dotOut = new LineWriter(twoDegreesDotFile, Config.DOT_CHARSET);
            WriteStats twoStats = new WriteStats(stats);
            impliedConstraints = formatter.writeRealRelationships(table, true, twoStats, dotOut);
            dotOut.close();
            if (stats.getNumTablesWritten() + stats.getNumViewsWritten() == twoStats.getNumTablesWritten() + twoStats.getNumViewsWritten())
                twoDegreesDotFile.delete(); // no different than before, so don't show it

            if (!impliedConstraints.isEmpty()) {
                dotOut = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
                formatter.writeAllRelationships(table, true, stats, dotOut);
                dotOut.close();
                return true;
            }
        }
        
        return false;
    }

    private void writeDiagram(Table table, WriteStats stats, File diagramsDir, LineWriter html) throws IOException {
        if (table.getMaxChildren() + table.getMaxParents() > 0) {
            html.writeln("<table width='100%' border='0'><tr><td class='container'>");
            if (HtmlTableDiagrammer.getInstance().write(table, diagramsDir, html)) {
                html.writeln("</td></tr></table>");
                writeExcludedColumns(stats.getExcludedColumns(), table, html);
            } else {
                html.writeln("</td></tr></table><p>");
                writeInvalidGraphvizInstallation(html);
            }
        }
    }

    protected void writeFooter(boolean onCascadeDelete, LineWriter out) throws IOException {
        if (onCascadeDelete)
            out.writeln("<br><span style='font-size: 85%;'>Related tables marked with * are involved in an 'on delete cascade' relationship.</span>");
        super.writeFooter(out);
    }

    @Override
    protected String getPathToRoot() {
        return "../";
    }
}