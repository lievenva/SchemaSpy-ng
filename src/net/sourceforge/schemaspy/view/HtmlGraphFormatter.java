package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;

    public boolean write(Table table, File graphDir, boolean hasImpliedRelationships, LineWriter html) {
        File dotFile = new File(graphDir, table.getName() + ".dot");
        File allDotFile = new File(graphDir, table.getName() + "_all_.dot");
        File graphFile = new File(graphDir, table.getName() + ".png");
        File allGraphFile = new File(graphDir, table.getName() + "_all_.png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            html.write("<br/><b title='Tables/views within two degrees of separation from ");
            html.write(table.getName());
            html.write("'>Close relationships:</b><br/>");

            html.writeln("  <a name='graph'><img src=\"../graphs/" + graphFile.getName() + "\" usemap=\"#realRelationshipsGraph\" id='relationships' border=\"0\" alt=\"\"></a>");
            DotRunner.writeMap(dotFile, html);
            if (hasImpliedRelationships) {
                DotRunner.generateGraph(allDotFile, allGraphFile);
                DotRunner.writeMap(allDotFile, html);
            }
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        File dotFile = new File(graphDir, dotBaseFilespec + ".dot");
        File graphFile = new File(graphDir, dotBaseFilespec + ".png");
        File allDotFile = new File(graphDir, dotBaseFilespec + "_all_.dot");
        File allGraphFile = new File(graphDir, dotBaseFilespec + "_all_.png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            writeRelationshipsHeader(db, graphFile, allGraphFile, "Relationships Graph", hasImpliedRelationships, html);
            html.writeln("  <a name='graph'><img src=\"graphs/summary/" + graphFile.getName() + "\" usemap=\"#realRelationshipsGraph\" id='relationships' border=\"0\" alt=\"\"></a>");
            DotRunner.writeMap(dotFile, html);

            if (hasImpliedRelationships) {
                DotRunner.generateGraph(allDotFile, allGraphFile);
                DotRunner.writeMap(allDotFile, html);
            }

            writeFooter(html);
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean writeOrphans(Database db, List orphanTables, File graphDir, LineWriter html) throws IOException {
        Set orphansWithImpliedRelationships = new HashSet();
        Iterator iter = orphanTables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeOrphansHeader(db, "Utility Tables Graph", !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".png");

                LineWriter dot = new LineWriter(new FileWriter(dotFile));
                new DotFormatter().writeOrphan(table, dot);
                dot.close();
                try {
                    if (!DotRunner.generateGraph(dotFile, graphFile))
                        return false;
                } catch (IOException noDot) {
                    printNoDotWarning();
                    return false;
                }

                html.write("  <img src=\"graphs/summary/" + graphFile.getName() + "\" usemap=\"#" + table + "\" border=\"0\" alt=\"\" align=\"top\"");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".dot");
                DotRunner.writeMap(dotFile, html);
            }

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeRelationshipsHeader(Database db, File graphFile, File allGraphFile, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.writeln("<br/><a href='index.html'>Tables</a>");
        if (hasImpliedRelationships) {
            html.writeln("<p/><form action=''>");
            html.write("  <input type='checkbox' id='graphType' onclick=\"if (!this.checked) selectGraph('graphs/summary/" + graphFile.getName() + "', '#realRelationshipsGraph'); else selectGraph('graphs/summary/" + allGraphFile.getName() + "', '#allRelationshipsGraph');\">");
            html.writeln("Include implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void writeOrphansHeader(Database db, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.writeln("<br/><a href='index.html'>Tables</a>");
        if (hasImpliedRelationships) {
            html.writeln("<p/><form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void printNoDotWarning() {
        if (!printedNoDotWarning) {
            printedNoDotWarning = true;
            System.err.println();
            System.err.println("Warning: Failed to run dot.");
            System.err.println("   Download it from www.graphviz.org and make sure dot is in your path.");
            System.err.println("   Generated pages will not contain a graphical view of table relationships.");
        }
    }
}
