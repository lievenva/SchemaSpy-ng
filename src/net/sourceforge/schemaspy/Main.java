package net.sourceforge.schemaspy;


/**
 * @author John Currier
 */
public class Main {
    public static void main(String[] argv) throws Exception {
        SchemaAnalyzer analyzer = new SchemaAnalyzer();

        System.exit(analyzer.analyze(new Config(argv), argv));
    }
}