package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static final HtmlGraphFormatter instance = new HtmlGraphFormatter();
    private boolean printedNoDotWarning = false;
    private boolean printedInvalidVersionWarning = false;

    /**
     * Singleton...don't allow creation
     */
    private HtmlGraphFormatter() {
    }

    public static HtmlGraphFormatter getInstance() {
        return instance;
    }

    public boolean write(Table table, File graphDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(graphDir, table.getName() + ".1degree.dot");
        File oneDegreeGraphFile = new File(graphDir, table.getName() + ".1degree.png");
        File impliedDotFile = new File(graphDir, table.getName() + ".implied2degrees.dot");
        File impliedGraphFile = new File(graphDir, table.getName() + ".implied2degrees.png");
        File twoDegreesDotFile = new File(graphDir, table.getName() + ".2degrees.dot");
        File twoDegreesGraphFile = new File(graphDir, table.getName() + ".2degrees.png");

        try {
            Dot dot = getDot();
            if (dot == null)
                return false;

            dot.generateGraph(oneDegreeDotFile, oneDegreeGraphFile);

            html.write("<br/><form><b>Close relationships");
            if (stats.wroteTwoDegrees()) {
                html.writeln("</b><span class='degrees' id='degrees'>");
                html.write("&nbsp;within <input type='radio' name='degrees' id='oneDegree' onclick=\"");
                html.write("if (!this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\" checked>one");
                html.write("  <input type='radio' name='degrees' id='twoDegrees' onclick=\"");
                html.write("if (this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\">two degrees of separation");
                html.write("</span><b>:</b>");
                html.writeln("</form>");
            } else {
                html.write(":</b></form>");
            }
            html.writeln("  <a name='graph'><img src='../graphs/" + oneDegreeGraphFile.getName() + "' usemap='#oneDegreeRelationshipsGraph' id='relationships' border='0' alt='' align='left'></a>");
            dot.writeMap(oneDegreeDotFile, html);
            if (stats.wroteImplied()) {
                dot.generateGraph(impliedDotFile, impliedGraphFile);
                dot.writeMap(impliedDotFile, html);
            } else {
                impliedDotFile.delete();
                impliedGraphFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                dot.generateGraph(twoDegreesDotFile, twoDegreesGraphFile);
                dot.writeMap(twoDegreesDotFile, html);
            } else {
                twoDegreesDotFile.delete();
                twoDegreesGraphFile.delete();
            }
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasOrphans, boolean hasImpliedRelationships, Set excludedColumns, LineWriter html) throws IOException {
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

            dot.generateGraph(compactRelationshipsDotFile, compactRelationshipsGraphFile);
            System.out.print(".");
            dot.generateGraph(largeRelationshipsDotFile, largeRelationshipsGraphFile);
            System.out.print(".");
            writeRelationshipsHeader(db, compactRelationshipsGraphFile, largeRelationshipsGraphFile, compactImpliedGraphFile, largeImpliedGraphFile, "Relationships Graph", hasOrphans, hasImpliedRelationships, html);
            html.writeln("<table width=\"100%\"><tr><td class=\"container\">");
            html.writeln("  <a name='graph'><img src='graphs/summary/" + compactRelationshipsGraphFile.getName() + "' usemap='#compactRelationshipsGraph' id='relationships' border='0' alt=''></a>");
            html.writeln("</td></tr></table>");
            writeExcludedColumns(excludedColumns, html);

            dot.writeMap(compactRelationshipsDotFile, html);
            dot.writeMap(largeRelationshipsDotFile, html);

            if (hasImpliedRelationships) {
                dot.generateGraph(compactImpliedDotFile, compactImpliedGraphFile);
                dot.writeMap(compactImpliedDotFile, html);
                System.out.print(".");

                dot.generateGraph(largeImpliedDotFile, largeImpliedGraphFile);
                dot.writeMap(largeImpliedDotFile, html);
                System.out.print(".");
            }

            writeFooter(html);
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean writeOrphans(Database db, List orphanTables, boolean hasRelationships, File graphDir, LineWriter html) throws IOException {
        Dot dot = getDot();
        if (dot == null)
            return false;

        Set orphansWithImpliedRelationships = new HashSet();
        Iterator iter = orphanTables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeOrphansHeader(db, "Utility Tables Graph", hasRelationships, !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".1degree.png");

                LineWriter dotOut = new LineWriter(new FileOutputStream(dotFile));
                DotFormatter.getInstance().writeOrphan(table, dotOut);
                dotOut.close();
                try {
                    dot.generateGraph(dotFile, graphFile);
                } catch (Dot.DotFailure dotFailure) {
                    System.err.println(dotFailure);
                    return false;
                }

                html.write("  <img src='graphs/summary/" + graphFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                dot.writeMap(dotFile, html);
            }

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeRelationshipsHeader(Database db, File compactRelationshipsGraphFile, File largeRelationshipsGraphFile, File compactImpliedGraphFile, File largeImpliedGraphFile, String title, boolean hasOrphans, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='container' align='left' valign='top'>");
        html.write("<br/>");
        writeTableOfContents(false, hasOrphans, html);

        // this is some UGLY code!
        html.writeln("<p/><form name='options' action=''>");
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

        // more butt-ugly 'code' follows
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
        html.writeln("</form>");

        html.writeln("<td class='container' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void writeOrphansHeader(Database db, String title, boolean hasRelationships, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='container' align='left' valign='top'>");
        html.writeln("<br/>");
        writeTableOfContents(hasRelationships, false, html);
        if (hasImpliedRelationships) {
            html.writeln("<p/><form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getInstance().getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='container' align='right' valign='top'>");
        writeLegend(false, false, html);
        html.writeln("</td></tr></table>");
    }

    private Dot getDot() {
        Dot dot = Dot.getInstance();
        if (!dot.exists()) {
            if (!printedNoDotWarning) {
                printedNoDotWarning = true;
                System.err.println();
                System.err.println("Warning: Failed to run dot.");
                System.err.println("   Download " + dot.getSupportedVersions());
                System.err.println("   from www.graphviz.org and make sure dot is in your path.");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }

            return null;
        }

        if (!dot.isValid()) {
            if (!printedInvalidVersionWarning) {
                printedInvalidVersionWarning = true;
                System.err.println();
                System.err.println("Warning: Invalid version of dot detected (" + dot.getVersion() + ").");
                System.err.println("   SchemaSpy requires " + dot.getSupportedVersions() + ".");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }

            return null;
        }

        return dot;
    }
}
