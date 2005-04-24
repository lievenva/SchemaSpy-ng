package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import net.sourceforge.schemaspy.LineWriter;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;

    public boolean write(Table table, File graphDir, LineWriter out) {
        File dotFile = new File(graphDir, table.getName() + ".dot");
        File graphFile = new File(graphDir, table.getName() + ".png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            out.write("<br/><b title='Tables/views within two degrees of separation from ");
            out.write(table.getName());
            out.writeln("'>Close relationships:</b><br/>");
            out.writeln("  <a name='graph'><IMG SRC=\"../graphs/" + graphFile.getName() + "\" USEMAP=\"#tables\" BORDER=\"0\"></a>");
            DotRunner.writeMap(dotFile, out);
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, LineWriter out) throws IOException {
        File dotFile = new File(graphDir, dotBaseFilespec + ".dot");
        File graphFile = new File(graphDir, dotBaseFilespec + ".png");

        try {
            if (!DotRunner.generateGraph(dotFile, graphFile))
                return false;

            writeHeader(db, graphFile, out);
            DotRunner.writeMap(dotFile, out);
            writeFooter(out);
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    private void writeHeader(Database db, File graphFile, LineWriter out) throws IOException {
        writeHeader(db, null, "Graphical View", out);
        out.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        out.writeln("<br/><a href='index.html'>Tables</a>");
        out.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, out);
        out.writeln("</td></tr></table>");
        out.writeln("<IMG SRC=\"graphs/" + graphFile.getName() + "\" USEMAP=\"#tables\" BORDER=\"0\">");
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
