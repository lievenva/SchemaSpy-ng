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

        int rc = 1;

        try {
            rc = analyzer.analyze(new Config(argv));
        } catch (Exception exc) {
            System.err.println(exc.getClass().getSimpleName() + ": " + exc.getMessage());
        }
        
        System.exit(rc);
    }
}