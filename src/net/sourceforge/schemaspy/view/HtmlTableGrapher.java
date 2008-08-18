package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlTableGrapher extends HtmlGraphFormatter {
    private static HtmlTableGrapher instance = new HtmlTableGrapher();

    private HtmlTableGrapher() {
    }

    public static HtmlTableGrapher getInstance() {
        return instance;
    }

    public boolean write(Table table, File graphDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(graphDir, table.getName() + ".1degree.dot");
        File oneDegreeGraphFile = new File(graphDir, table.getName() + ".1degree.png");
        File twoDegreesDotFile = new File(graphDir, table.getName() + ".2degrees.dot");
        File twoDegreesGraphFile = new File(graphDir, table.getName() + ".2degrees.png");
        File impliedDotFile = new File(graphDir, table.getName() + ".implied2degrees.dot");
        File impliedGraphFile = new File(graphDir, table.getName() + ".implied2degrees.png");

        try {
            Dot dot = getDot();
            if (dot == null)
                return false;

            String map = dot.generateGraph(oneDegreeDotFile, oneDegreeGraphFile);

            html.write("<br><form action='get'><b>Close relationships");
            if (stats.wroteTwoDegrees()) {
                html.writeln("</b><span class='degrees' id='degrees' title='Detail diminishes with increased separation from " + table.getName() + "'>");
                html.write("&nbsp;within <label><input type='radio' name='degrees' id='oneDegree' checked>one</label>");
                html.write("  <label><input type='radio' name='degrees' id='twoDegrees'>two degrees</label> of separation");
                html.write("</span><b>:</b>");
                html.writeln("</form>");
            } else {
                html.write(":</b></form>");
            }
            html.write(map);
            map = null;
            html.writeln("  <a name='graph'><img id='oneDegreeImg' src='../graphs/" + oneDegreeGraphFile.getName() + "' usemap='#oneDegreeRelationshipsGraph' class='graph' border='0' alt='' align='left'></a>");
            
            if (stats.wroteImplied()) {
                html.writeln(dot.generateGraph(impliedDotFile, impliedGraphFile));
                html.writeln("  <a name='graph'><img id='impliedTwoDegreesImg' src='../graphs/" + impliedGraphFile.getName() + "' usemap='#impliedTwoDegreesRelationshipsGraph' class='graph' border='0' alt='' align='left'></a>");
            } else {
                impliedDotFile.delete();
                impliedGraphFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                html.writeln(dot.generateGraph(twoDegreesDotFile, twoDegreesGraphFile));
                html.writeln("  <a name='graph'><img id='twoDegreesImg' src='../graphs/" + twoDegreesGraphFile.getName() + "' usemap='#twoDegreesRelationshipsGraph' class='graph' border='0' alt='' align='left'></a>");
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
}
