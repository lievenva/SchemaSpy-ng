package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlTableDiagrammer extends HtmlDiagramFormatter {
    private static HtmlTableDiagrammer instance = new HtmlTableDiagrammer();

    private HtmlTableDiagrammer() {
    }

    public static HtmlTableDiagrammer getInstance() {
        return instance;
    }

    public boolean write(Table table, File diagramDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(diagramDir, table.getName() + ".1degree.dot");
        File oneDegreeDiagramFile = new File(diagramDir, table.getName() + ".1degree.png");
        File twoDegreesDotFile = new File(diagramDir, table.getName() + ".2degrees.dot");
        File twoDegreesDiagramFile = new File(diagramDir, table.getName() + ".2degrees.png");
        File impliedDotFile = new File(diagramDir, table.getName() + ".implied2degrees.dot");
        File impliedDiagramFile = new File(diagramDir, table.getName() + ".implied2degrees.png");

        try {
            Dot dot = getDot();
            if (dot == null)
                return false;

            String map = dot.generateDiagram(oneDegreeDotFile, oneDegreeDiagramFile);

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
            html.writeln("  <a name='diagram'><img id='oneDegreeImg' src='../diagrams/" + oneDegreeDiagramFile.getName() + "' usemap='#oneDegreeRelationshipsDiagram' class='diagram' border='0' alt='' align='left'></a>");
            
            if (stats.wroteImplied()) {
                html.writeln(dot.generateDiagram(impliedDotFile, impliedDiagramFile));
                html.writeln("  <a name='diagram'><img id='impliedTwoDegreesImg' src='../diagrams/" + impliedDiagramFile.getName() + "' usemap='#impliedTwoDegreesRelationshipsDiagram' class='diagram' border='0' alt='' align='left'></a>");
            } else {
                impliedDotFile.delete();
                impliedDiagramFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                html.writeln(dot.generateDiagram(twoDegreesDotFile, twoDegreesDiagramFile));
                html.writeln("  <a name='diagram'><img id='twoDegreesImg' src='../diagrams/" + twoDegreesDiagramFile.getName() + "' usemap='#twoDegreesRelationshipsDiagram' class='diagram' border='0' alt='' align='left'></a>");
            } else {
                twoDegreesDotFile.delete();
                twoDegreesDiagramFile.delete();
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
