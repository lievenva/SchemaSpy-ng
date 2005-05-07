package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import net.sourceforge.schemaspy.LineWriter;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;

    public boolean write(Table table, File graphDir, boolean hasImpliedRelationships, LineWriter out) {
        File dotFile = new File(graphDir, table.getName() + ".dot");
        File allDotFile = new File(graphDir, table.getName() + "_all_.dot");
        File graphFile = new File(graphDir, table.getName() + ".png");
        File allGraphFile = new File(graphDir, table.getName() + "_all_.png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            out.write("<br/><b title='Tables/views within two degrees of separation from ");
            out.write(table.getName());
            out.write("'>Close relationships:</b>");
            if (hasImpliedRelationships) {
                out.writeln("<form action=''");
                out.writeln("  <input type='checkbox' id='graphType' onclick=\"if (!this.checked) selectGraph('../graphs/" + graphFile.getName() + "', '#realRelationshipsGraph'); else selectGraph('../graphs/" + allGraphFile.getName() + "', '#allRelationshipsGraph');\">");
                out.writeln("  Include implied");
                out.writeln("</form>");
            } else {
                out.writeln("<br/>");
            }

            out.writeln("  <a name='graph'><img src=\"../graphs/" + graphFile.getName() + "\" usemap=\"#realRelationshipsGraph\" id='relationships' border=\"0\" alt=\"\"></a>");
            DotRunner.writeMap(dotFile, out);
            if (hasImpliedRelationships) {
                DotRunner.generateGraph(allDotFile, allGraphFile);
                DotRunner.writeMap(allDotFile, out);
            }
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasImpliedRelationships, LineWriter out) throws IOException {
        File dotFile = new File(graphDir, dotBaseFilespec + ".dot");
        File graphFile = new File(graphDir, dotBaseFilespec + ".png");
        File allDotFile = new File(graphDir, dotBaseFilespec + "_all_.dot");
        File allGraphFile = new File(graphDir, dotBaseFilespec + "_all_.png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            writeHeader(db, graphFile, allGraphFile, hasImpliedRelationships, out);
            out.writeln("  <a name='graph'><img src=\"graphs/" + graphFile.getName() + "\" usemap=\"#realRelationshipsGraph\" id='relationships' border=\"0\" alt=\"\"></a>");
            DotRunner.writeMap(dotFile, out);

            if (hasImpliedRelationships) {
                DotRunner.generateGraph(allDotFile, allGraphFile);
                DotRunner.writeMap(allDotFile, out);
            }

            writeFooter(out);
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    private void writeHeader(Database db, File graphFile, File allGraphFile, boolean hasImpliedRelationships, LineWriter out) throws IOException {
        writeHeader(db, null, "Graphical View", out);
        out.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        out.writeln("<br/><a href='index.html'>Tables</a>");
        if (hasImpliedRelationships) {
            out.writeln("<p/><form action=''>");
            out.writeln("  <input type='checkbox' id='graphType' onclick=\"if (!this.checked) selectGraph('graphs/" + graphFile.getName() + "', '#realRelationshipsGraph'); else selectGraph('graphs/" + allGraphFile.getName() + "', '#allRelationshipsGraph');\">");
            out.writeln("  Include implied relationships");
            out.writeln("</form>");
        }

        out.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, out);
        out.writeln("</td></tr></table>");
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
