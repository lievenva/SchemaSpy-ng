package net.sourceforge.schemaspy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author John Currier
 */
public class Revision {
    private static String rev = "Unknown";
    private static final String resourceName = "/schemaSpy.rev";
    
    static {
        initialize();
    }

    private static void initialize() {
        InputStream in = null;
        BufferedReader reader = null;
        
        try {
            in = Revision.class.getResourceAsStream(resourceName);
    
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
                try {
                    rev = reader.readLine();
                } catch (IOException exc) {
                }
            }
        } finally {
            try {
                if (reader != null)
                    reader.close();
                else if (in != null)
                    in.close();
            } catch (IOException ignore) {}
        }
    }
    
    @Override
    public String toString() {
        return rev;
    }
    
    public static void main(String[] args) throws IOException {
        File entriesFile = new File(".svn", "entries");
        BufferedReader entries = new BufferedReader(new FileReader(entriesFile));
        entries.readLine(); // lines
        entries.readLine(); // blank
        entries.readLine(); // type
        String revision = entries.readLine(); // rev
        entries.close();

        String buildDir = "output";
        if (args.length < 1)
            buildDir = args[0];
        File revFile = new File(buildDir, resourceName);
        FileWriter out = new FileWriter(revFile);
        out.write(revision);
        out.close();
        
        initialize();
        System.out.println("Subversion revision " + new Revision());
    }
}