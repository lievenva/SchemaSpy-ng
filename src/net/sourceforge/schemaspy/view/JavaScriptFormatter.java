package net.sourceforge.schemaspy.view;

import java.io.IOException;
import net.sourceforge.schemaspy.util.LineWriter;

public class JavaScriptFormatter {
    public void write(LineWriter out) throws IOException {
        out.writeln("function showReferrers(label, id) {");
        out.writeln("  label.style.display='none';");
        out.writeln("  document.getElementById(id).style.display='';");
        out.writeln("  return false;");
        out.writeln("}");
        out.writeln("");
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
        out.writeln("    if (cb.checked) {");
        out.writeln("      cb.checked=false;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("    cb = options.showConstNames;");
        out.writeln("    if (cb.checked) {");
        out.writeln("      cb.checked=false;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("    cb = options.showLegend;");
        out.writeln("    if (!cb.checked) {");
        out.writeln("      cb.checked=true;");
        out.writeln("      cb.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("  var graphType = document.getElementById('graphType');");
        out.writeln("  if (graphType) {");
        out.writeln("    if (graphType.checked) {");
        out.writeln("      graphType.checked=false;");
        out.writeln("      graphType.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("  var removeImpliedOrphans = document.getElementById('removeImpliedOrphans');");
        out.writeln("  if (removeImpliedOrphans) {");
        out.writeln("    if (removeImpliedOrphans.checked) {");
        out.writeln("      removeImpliedOrphans.checked=false;");
        out.writeln("      removeImpliedOrphans.click();");
        out.writeln("    }");
        out.writeln("  }");
        out.writeln("}");
    }
}
