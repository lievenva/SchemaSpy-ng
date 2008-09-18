package net.sourceforge.schemaspy;

import net.sourceforge.schemaspy.view.StyleSheet;

import net.sourceforge.schemaspy.model.InvalidConfigurationException;
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
        } catch (InvalidConfigurationException badConfig) {
            System.err.println(badConfig.getClass().getSimpleName() + ": " + badConfig.getMessage());
        } catch (StyleSheet.MissingCssPropertyException badCss) {
            System.err.println();
            System.err.println(badCss.getClass().getSimpleName() + ": " + badCss.getMessage());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        
        System.exit(rc);
    }
}