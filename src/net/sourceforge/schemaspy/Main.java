package net.sourceforge.schemaspy;

import java.util.*;


/**
 * @author John Currier
 */
public class Main {
    public static void main(String[] argv) throws Exception {
        SchemaAnalyzer analyzer = new SchemaAnalyzer();
System.out.println(Arrays.asList(argv));
        System.exit(analyzer.analyze(new Config(argv), argv));
    }
}