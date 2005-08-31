package net.sourceforge.schemaspy.util;

import java.io.*;
import java.util.*;

public class Dot {
    private static Dot instance = new Dot();
    private final Version version;
    private final Version supportedVersion = new Version("2.2.1");
    private final Version badVersion = new Version("2.4");

    private Dot() {
        String tempVersion = null;
        try {
            String dotCommand = "dot -V";
            Process process = Runtime.getRuntime().exec(dotCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringTokenizer tokenizer = new StringTokenizer(reader.readLine());
            tokenizer.nextToken(); // skip 'dot'
            tokenizer.nextToken(); // skip 'version'
            tempVersion = tokenizer.nextToken();
        } catch (Exception validDotDoesntExist) {
        }

        version = new Version(tempVersion);
    }

    public static Dot getInstance() {
        return instance;
    }

    public boolean exists() {
        return version != null;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isValid() {
        return exists() && (getVersion().equals(supportedVersion) || getVersion().compareTo(badVersion) > 0);
    }

    public String getSupportedVersions() {
        return "dot version " + supportedVersion + " or versions greater than " + badVersion;
    }

    public boolean supportsCenteredEastWestEdges() {
        return getVersion().compareTo(new Version("2.6")) >= 0;
    }

    public boolean generateGraph(File dotFile, File graphFile) throws IOException {
        try {
            String dotCommand = "dot -Tpng \"" + dotFile + "\" -o\"" + graphFile + "\"";
            Process process = Runtime.getRuntime().exec(dotCommand);
            new ProcessOutputReader(dotCommand, process.getErrorStream()).start();
            new ProcessOutputReader(dotCommand, process.getInputStream()).start();
            int rc = process.waitFor();
            if (rc != 0) {
                System.err.println("'" + dotCommand + "' failed with return code " + rc);
                return false;
            }
        } catch (InterruptedException interrupted) {
            interrupted.printStackTrace();
        }

        return true;
    }

    public boolean writeMap(File dotFile, LineWriter out) throws IOException {
        BufferedReader mapReader = null;

        try {
            String dotCommand = "dot -Tcmapx \"" + dotFile + "\"";
            Process process = Runtime.getRuntime().exec(dotCommand);
            new ProcessOutputReader(dotCommand, process.getErrorStream()).start();
            mapReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = mapReader.readLine()) != null)
                out.writeln(line);
            int rc = process.waitFor();
            if (rc != 0) {
                System.err.println("'" + dotCommand + "' failed with return code " + rc);
                return false;
            }
        } catch (InterruptedException interrupted) {
            interrupted.printStackTrace();
        } finally {
            try {
                mapReader.close();
            } catch (Exception ignore) {}
        }

        return true;
    }

    private static class ProcessOutputReader extends Thread {
        private final BufferedReader processReader;
        private final String command;

        ProcessOutputReader(String command, InputStream processStream) {
            processReader = new BufferedReader(new InputStreamReader(processStream));
            this.command = command;
            setDaemon(true);
        }

        public void run() {
            try {
                String line;
                while ((line = processReader.readLine()) != null) {
                    // don't report port id unrecognized or unrecognized port
                    if (line.indexOf("unrecognized") == -1 && line.indexOf("port") == -1)
                        System.err.println(command + ": " + line);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                try {
                    processReader.close();
                } catch (Exception exc) {
                    exc.printStackTrace(); // shouldn't ever get here...but...
                }
            }
        }
    }
}
