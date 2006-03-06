package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlRelationshipsPage extends HtmlGraphFormatter {
    private static HtmlRelationshipsPage instance = new HtmlRelationshipsPage();

    private HtmlRelationshipsPage() {
    }

    public static HtmlRelationshipsPage getInstance() {
        return instance;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasOrphans, boolean hasImpliedRelationships, Set excludedColumns, LineWriter html) {
        File compactRelationshipsDotFile = new File(graphDir, dotBaseFilespec + ".real.compact.dot");
        File compactRelationshipsGraphFile = new File(graphDir, dotBaseFilespec + ".real.compact.png");
        File largeRelationshipsDotFile = new File(graphDir, dotBaseFilespec + ".real.large.dot");
        File largeRelationshipsGraphFile = new File(graphDir, dotBaseFilespec + ".real.large.png");
        File compactImpliedDotFile = new File(graphDir, dotBaseFilespec + ".implied.compact.dot");
        File compactImpliedGraphFile = new File(graphDir, dotBaseFilespec + ".implied.compact.png");
        File largeImpliedDotFile = new File(graphDir, dotBaseFilespec + ".implied.large.dot");
        File largeImpliedGraphFile = new File(graphDir, dotBaseFilespec + ".implied.large.png");

        try {
            Dot dot = getDot();
            if (dot == null)
                return false;

            System.out.print(".");
            dot.generateGraph(compactRelationshipsDotFile, compactRelationshipsGraphFile);
            System.out.print(".");
            writeHeader(db, compactRelationshipsGraphFile, largeRelationshipsGraphFile, compactImpliedGraphFile, largeImpliedGraphFile, "Relationships Graph", hasOrphans, hasImpliedRelationships, html);
            html.writeln("<table width=\"100%\"><tr><td class=\"container\">");
            html.writeln("  <a name='graph'><img src='graphs/summary/" + compactRelationshipsGraphFile.getName() + "' usemap='#compactRelationshipsGraph' id='relationships' border='0' alt=''></a>");
            html.writeln("</td></tr></table>");
            writeExcludedColumns(excludedColumns, html);
            dot.writeMap(compactRelationshipsDotFile, html);
            System.out.print(".");

            // we've run into instances where the first graphs get generated, but then
            // dot fails on the second one...try to recover from that scenario 'somewhat'
            // gracefully
            try {
                dot.generateGraph(largeRelationshipsDotFile, largeRelationshipsGraphFile);
                System.out.print(".");
                dot.writeMap(largeRelationshipsDotFile, html);
                System.out.print(".");
    
                if (hasImpliedRelationships) {
                    dot.generateGraph(compactImpliedDotFile, compactImpliedGraphFile);
                    System.out.print(".");
                    dot.writeMap(compactImpliedDotFile, html);
                    System.out.print(".");
    
                    dot.generateGraph(largeImpliedDotFile, largeImpliedGraphFile);
                    System.out.print(".");
                    dot.writeMap(largeImpliedDotFile, html);
                    System.out.print(".");
                }
            } catch (Dot.DotFailure dotFailure) {
                System.err.println("dot failed to generate all of the relationships graphs:");
                System.err.println(dotFailure);
                System.err.println("...but the relationships page may still be usable.");
            }

            writeFooter(html);
            return true;
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }
    }

    private void writeHeader(Database db, File compactRelationshipsGraphFile, File largeRelationshipsGraphFile, File compactImpliedGraphFile, File largeImpliedGraphFile, String title, boolean hasOrphans, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, true, hasOrphans, html);
        html.writeln("<table class='container' width='100%'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td>");
        html.writeln("<td class='container' align='right' valign='top' rowspan='2'>");
        writeLegend(false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr><td class='container' align='left' valign='top'>");

        // this is some UGLY code!
        html.writeln("<form name='options' action=''>");
        if (hasImpliedRelationships) {
            html.write("  <input type='checkbox' id='implied' onclick=\"");
            html.write("if (this.checked) {");
            html.write(" if (document.options.compact.checked)");
            html.write(" selectGraph('graphs/summary/" + compactImpliedGraphFile.getName() + "', '#compactImpliedRelationshipsGraph');");
            html.write(" else ");
            html.write(" selectGraph('graphs/summary/" + largeImpliedGraphFile.getName() + "', '#largeImpliedRelationshipsGraph'); ");
            html.write("} else {");
            html.write(" if (document.options.compact.checked)");
            html.write(" selectGraph('graphs/summary/" + compactRelationshipsGraphFile.getName() + "', '#compactRelationshipsGraph'); ");
            html.write(" else ");
            html.write(" selectGraph('graphs/summary/" + largeRelationshipsGraphFile.getName() + "', '#largeRelationshipsGraph'); ");
            html.write("}\">");
            html.writeln("Include implied relationships");
        }
        // more butt-ugly 'code' follows
        html.write("  <input type='checkbox' id='compact' checked onclick=\"");
        html.write("if (this.checked) {");
        if (hasImpliedRelationships) {
            html.write(" if (document.options.implied.checked)");
            html.write(" selectGraph('graphs/summary/" + compactImpliedGraphFile.getName() + "', '#compactImpliedRelationshipsGraph'); ");
            html.write("else");
        }
        html.write(" selectGraph('graphs/summary/" + compactRelationshipsGraphFile.getName() + "', '#compactRelationshipsGraph'); ");
        html.write("} else {");
        if (hasImpliedRelationships) {
            html.write(" if (document.options.implied.checked) ");
            html.write(" selectGraph('graphs/summary/" + largeImpliedGraphFile.getName() + "', '#largeImpliedRelationshipsGraph'); ");
            html.write(" else");
        }
        html.write(" selectGraph('graphs/summary/" + largeRelationshipsGraphFile.getName() + "', '#largeRelationshipsGraph'); ");
        html.write("}\">");
        html.writeln("Compact");
        html.writeln("</form>");

        html.writeln("</td></tr></table>");
    }

    protected boolean isRelationshipsPage() {
        return true;
    }
}