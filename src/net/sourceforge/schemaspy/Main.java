package net.sourceforge.schemaspy;

import net.sourceforge.schemaspy.model.ConnectionFailure;
import net.sourceforge.schemaspy.model.EmptySchemaException;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;
import net.sourceforge.schemaspy.model.ProcessExecutionException;
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
            rc = analyzer.analyze(new Config(argv)) == null ? 1 : 0;
        } catch (ConnectionFailure couldntConnect) {
            // failure already logged
            rc = 3;
        } catch (EmptySchemaException noData) {
            // failure already logged
            rc = 2;
        } catch (InvalidConfigurationException badConfig) {
            System.err.println();
            if (badConfig.getParamName() != null)
                System.err.println("Bad parameter specified for " + badConfig.getParamName());
            System.err.println(badConfig.getMessage());
            if (badConfig.getCause() != null && !badConfig.getMessage().endsWith(badConfig.getMessage()))
                System.err.println(" caused by " + badConfig.getCause().getMessage());
        } catch (ProcessExecutionException badLaunch) {
            System.err.println(badLaunch.getMessage());
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        System.exit(rc);
    }
}