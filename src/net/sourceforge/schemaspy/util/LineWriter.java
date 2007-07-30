package net.sourceforge.schemaspy.util;

import java.io.*;

/**
 * BufferedWriter that adds a <code>writeln()</code> method
 * to output a <i>lineDelimited</i> line of text without
 * cluttering up code.
 */
public class LineWriter extends BufferedWriter {
    private final Writer out;
    
    public LineWriter(Writer out) {
        super(out);
        this.out = out;
    }

    public LineWriter(Writer out, int sz) {
        super(out, sz);
        this.out = out;
    }

    /**
     * Construct a <code>LineWriter</code> with UTF8 output
     * @param out OutputStream
     * @throws UnsupportedEncodingException
     */
    public LineWriter(OutputStream out) throws UnsupportedEncodingException {
        this(new OutputStreamWriter(out, "UTF8"));
    }

    public void writeln(String str) throws IOException {
        write(str);
        newLine();
    }

    public void writeln() throws IOException {
        newLine();
    }
 
    /**
     * Intended to simplify use when wrapping StringWriters.
     */
    public String toString() {
        try {
            flush();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
        
        return out.toString();
    }
}