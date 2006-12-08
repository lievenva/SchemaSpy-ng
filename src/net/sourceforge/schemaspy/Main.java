package net.sourceforge.schemaspy;

import net.sourceforge.schemaspy.ui.*;

/**
 * @author John Currier
 */
public class Main {
    public static void main(String[] argv) throws Exception {
        if (argv.length == 0) {
            new MainFrame().setVisible(true);
            return;
        }
        
        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        System.exit(analyzer.analyze(new Config(argv)));
    }
}