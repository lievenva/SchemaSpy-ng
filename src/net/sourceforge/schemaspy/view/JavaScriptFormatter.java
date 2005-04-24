package net.sourceforge.schemaspy.view;

import java.io.IOException;
import net.sourceforge.schemaspy.LineWriter;

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
        out.writeln("function syncOptions() {");
        out.writeln("  var cb = document.options.showRelatedCols;");
        out.writeln("  if (cb.checked) {");
        out.writeln("    cb.checked=false;");
        out.writeln("    cb.click();");
        out.writeln("  }");
        out.writeln("  cb = document.options.showConstNames;");
        out.writeln("  if (cb.checked) {");
        out.writeln("    cb.checked=false;");
        out.writeln("    cb.click();");
        out.writeln("  }");
        out.writeln("  cb = document.options.showLegend;");
        out.writeln("  if (!cb.checked) {");
        out.writeln("    cb.checked=true;");
        out.writeln("    cb.click();");
        out.writeln("  }");
        out.writeln("}");
    }
}
