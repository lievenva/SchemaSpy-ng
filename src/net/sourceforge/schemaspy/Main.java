package net.sourceforge.schemaspy;

import net.sourceforge.schemaspy.ui.MainFrame;

/**
 * @author John Currier
 */
public class Main {
    public static void main(String[] argv) throws Exception {
        if (argv.length == 1 && argv[0].equals("-gui")) { // warning: serious temp hack
            new MainFrame().setVisible(true);
            return;
        }
        
        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        System.exit(analyzer.analyze(new Config(argv)));
    }
}