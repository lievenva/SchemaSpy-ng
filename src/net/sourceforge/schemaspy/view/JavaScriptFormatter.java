package net.sourceforge.schemaspy.view;

import java.io.IOException;
import net.sourceforge.schemaspy.util.LineWriter;

public class JavaScriptFormatter {
    public static void write(LineWriter out) throws IOException {
        out.writeln("function toggle(styleIndex) {");
        out.writeln("  var rules = document.styleSheets[0].cssRules;");
        out.writeln("  if (rules == null) rules = document.styleSheets[0].rules;");
        out.writeln("  var style = rules[styleIndex].style;");
        out.writeln("  if (style.display == 'none') {");
        out.writeln("    style.display='';");
        out.writeln("  } else {");
        out.writeln("    style.display='none';");
        out.writeln("  }");
        out.writeln("}");
        out.writeln("");
        out.writeln("function selectGraph(imageName, map) {");
        out.writeln("  var image = document.getElementById('relationships');");
        out.writeln("  image.setAttribute('useMap', map);");  // IE is case sensitive here
        out.writeln("  image.setAttribute('src', imageName);");
        out.writeln("}");
        out.writeln("");
        out.writeln("function syncOptions() {");
        out.writeln("  var options = document.options;");
        out.writeln("  if (options) {");
        out.writeln("    var cb = options.showRelatedCols;");
        out.writeln("    if (cb && cb.checked) {");
        out.writeln("      cb.checked=false;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("    cb = options.showConstNames;");
        out.writeln("    if (cb && cb.checked) {");
        out.writeln("      cb.checked=false;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("    cb = options.showLegend;");
        out.writeln("    if (cb && !cb.checked) {");
        out.writeln("      cb.checked=true;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("    cb = options.graphType;");
        out.writeln("    if (cb && cb.checked) {");
        out.writeln("      cb.checked=false;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("  var removeImpliedOrphans = document.getElementById('removeImpliedOrphans');");
        out.writeln("  if (removeImpliedOrphans) {");
        out.writeln("    if (removeImpliedOrphans.checked) {");
        out.writeln("      removeImpliedOrphans.checked=false;");
        out.writeln("      removeImpliedOrphans.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("  syncDegrees();");
        out.writeln("}");
        out.writeln("");
        out.writeln("function syncDegrees() {");
        out.writeln("  var rules = document.styleSheets[0].cssRules;");
        out.writeln("  if (rules == null) rules = document.styleSheets[0].rules;");
        out.writeln("  var degreesStyle = rules[" + StyleSheet.getOffsetOf(".degrees") + "].style;");
        out.writeln("  var degrees = document.getElementById('degrees');");
        out.writeln("  if (degreesStyle.display != 'none' && degrees) {");
        out.writeln("    var oneDegree = document.getElementById('oneDegree');");
        out.writeln("    var twoDegrees = document.getElementById('twoDegrees');");
        out.writeln("    var useMap = document.getElementById('relationships').useMap;");
        out.writeln("    if (oneDegree.checked && useMap != '#focusedRelationshipsGraph') {");
        out.writeln("      oneDegree.checked=false;");
        out.writeln("      oneDegree.click();");
        out.writeln("    } else if (twoDegrees.checked && useMap != '#realRelationshipsGraph') {");
        out.writeln("      twoDegrees.checked=false;");
        out.writeln("      twoDegrees.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("}");
    }
}
