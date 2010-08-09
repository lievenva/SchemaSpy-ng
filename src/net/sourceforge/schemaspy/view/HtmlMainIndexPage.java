package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.HtmlEncoder;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The main index that contains all tables and views that were evaluated
 *
 * @author John Currier
 */
public class HtmlMainIndexPage extends HtmlFormatter {
    private static HtmlMainIndexPage instance = new HtmlMainIndexPage();
    private final NumberFormat integerFormatter = NumberFormat.getIntegerInstance();

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlMainIndexPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlMainIndexPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection<Table> tables, boolean showOrphansDiagram, LineWriter html) throws IOException {
        Set<Table> byName = new TreeSet<Table>(new Comparator<Table>() {
            public int compare(Table table1, Table table2) {
                return table1.compareTo(table2);
            }
        });
        byName.addAll(tables);

        boolean showIds = false;
        int numViews = 0;
        boolean comments = false;

        for (Table table : byName) {
            if (table.isView())
                ++numViews;
            showIds |= table.getId() != null;
            if (table.getComments() != null)
                comments = true;
        }

        writeHeader(database, byName.size() - numViews, numViews, showIds, showOrphansDiagram, comments, html);

        int numTableCols = 0;
        int numViewCols = 0;
        int numRows = 0;
        for (Table table : byName) {
            writeLineItem(table, showIds, html);

            if (!table.isView())
                numTableCols += table.getColumns().size();
            else
                numViewCols += table.getColumns().size();
            numRows += table.getNumRows();
        }

        writeFooter(numTableCols, numViewCols, numRows, html);
    }

    private void writeHeader(Database db, int numberOfTables, int numberOfViews, boolean showIds, boolean hasOrphans, boolean hasComments, LineWriter html) throws IOException {
        List<String> javascript = new ArrayList<String>();
        javascript.add("$(function(){");
        javascript.add("  associate($('#showTables'), $('.tbl'));");
        javascript.add("  associate($('#showViews'),  $('.view'))");
        javascript.add("})");

        writeHeader(db, null, null, hasOrphans, javascript, html);
        html.writeln("<table width='100%'>");
        html.writeln(" <tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln(" </td></tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>Database Type: ");
        html.write(db.getDatabaseProduct());
        html.writeln("  </td>");
        html.writeln("  <td class='container' align='right' valign='top' rowspan='3'>");
        if (sourceForgeLogoEnabled())
            html.writeln("    <a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a><br>");
        html.write("    <br>");
        writeFeedMe(html);
        html.writeln("  </td>");
        html.writeln(" </tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>");
        String xmlName = db.getName();
        if (db.getSchema() != null)
            xmlName += '.' + db.getSchema();
        html.write("<br><a href='" + xmlName + ".xml' title='XML Representation'>XML Representation</a>");
        html.write("<br><a href='insertionOrder.txt' title='Useful for loading data into a database'>Insertion Order</a>&nbsp;");
        html.write("<a href='deletionOrder.txt' title='Useful for purging data from a database'>Deletion Order</a>");
        html.write("&nbsp;(for database loading/purging scripts)");
        html.writeln("</td>");
        html.writeln(" </tr>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<p>");
        html.write("<b>");
        if (numberOfViews == 0) {
            html.write(String.valueOf(numberOfTables));
            html.writeln(" Tables");
            html.writeln("<label for='showTables' style='display:none;'><input type='checkbox' id='showTables' checked></label>");
        } else {
            html.write("<label for='showTables'><input type='checkbox' id='showTables' checked>");
            html.write(String.valueOf(numberOfTables));
            html.write(" Tables</label>");
            html.write(" <label for='showViews'><input type='checkbox' id='showViews' checked>");
            html.write(String.valueOf(numberOfViews));
            html.write(" View");
            if (numberOfViews != 1)
                html.write("s");
            html.write("</label>");
        }

        html.writeln("<br><label for='showComments' style='font-size: 85%;'><input type=checkbox " + (hasComments  ? "checked " : "") + "id='showComments'>Comments</label>");
        html.writeln("</b>");

        html.writeln("<table class='dataTable' border='1' rules='groups'>");
        int numGroups = 5 + (showIds ? 1 : 0) + (displayNumRows ? 1 : 0);
        for (int i = 0; i < numGroups; ++i)
            html.writeln("<colgroup>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        html.writeln("  <th valign='bottom'>Table</th>");
        if (showIds)
            html.writeln("  <th align='center' valign='bottom'>ID</th>");
        html.writeln("  <th align='right' valign='bottom'>Children</th>");
        html.writeln("  <th align='right' valign='bottom'>Parents</th>");
        html.writeln("  <th align='right' valign='bottom'>Columns</th>");
        if (displayNumRows)
            html.writeln("  <th align='right' valign='bottom'>Rows</th>");
        html.writeln("  <th class='comment' align='left' valign='bottom'>Comments</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");
    }

    private void writeLineItem(Table table, boolean showIds, LineWriter html) throws IOException {
        html.write(" <tr class='");
        html.write(table.isView() ? "view" : "tbl");
        html.writeln("' valign='top'>");
        html.write("  <td class='detail'><a href='tables/");
        html.write(table.getName());
        html.write(".html'>");
        html.write(table.getName());
        html.writeln("</a></td>");

        if (showIds) {
            html.write("  <td class='detail' align='right'>");
            Object id = table.getId();
            if (id != null)
                html.write(String.valueOf(id));
            else
                html.writeln("&nbsp;");
            html.writeln("</td>");
        }

        html.write("  <td class='detail' align='right'>");
        int numRelatives = table.getNumNonImpliedChildren();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");
        html.write("  <td class='detail' align='right'>");
        numRelatives = table.getNumNonImpliedParents();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");

        html.write("  <td class='detail' align='right'>");
        html.write(String.valueOf(integerFormatter.format(table.getColumns().size())));
        html.writeln("</td>");

        if (displayNumRows) {
            html.write("  <td class='detail' align='right'>");
            if (!table.isView())
                html.write(String.valueOf(integerFormatter.format(table.getNumRows())));
            else
                html.write("<span title='Views contain no real rows'>view</span>");
            html.writeln("</td>");
        }
        html.write("  <td class='comment detail'>");
        String comments = table.getComments();
        if (comments != null) {
            if (encodeComments)
                for (int i = 0; i < comments.length(); ++i)
                    html.write(HtmlEncoder.encodeToken(comments.charAt(i)));
            else
                html.write(comments);
        }
        html.writeln("</td>");
        html.writeln("  </tr>");
    }

    protected void writeFooter(int numTableCols, int numViewCols, int numRows, LineWriter html) throws IOException {
        html.writeln("</table>");
        html.writeln("<p>");

        html.writeln("<table class='container' border='0' style='font-size: 85%;'>");
        html.writeln("<tr class='tbl'>");
        html.write("<td class='container'>");
        if (numViewCols > 0)
            html.write("Table ");
        html.writeln("Columns:</td>");
        html.write("<td class='container' style='padding: 0px 2px'>");
        html.write(String.valueOf(integerFormatter.format(numTableCols)));
        html.writeln("</td>");
        if (displayNumRows) {
            html.write("<td class='container' style='padding: 0px 4px'>");
            if (numViewCols > 0)
                html.write("Table ");
            html.writeln("Rows:</td>");
            html.write("<td class='container' style='padding: 0px 2px'>");
            html.write(String.valueOf(integerFormatter.format(numRows)));
            html.writeln("</td>");
        }
        html.writeln("</tr>");
        if (numViewCols > 0) {
            html.writeln("<tr class='view'>");
            html.writeln("<td class='container'>View Columns:</td>");
            html.write("<td class='container' style='padding: 0px 2px'>");
            html.write(String.valueOf(integerFormatter.format(numViewCols)));
            html.writeln("</td>");
            html.writeln("</tr>");
        }
        html.writeln("</table>");
        super.writeFooter(html);
    }

    @Override
    protected boolean isMainIndex() {
        return true;
    }
}
