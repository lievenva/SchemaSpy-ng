package net.sourceforge.schemaspy.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * BufferedWriter that adds a <code>writeln()</code> method
 * to output a <i>lineDelimited</i> line of text without
 * cluttering up code.
 */
public class LineWriter extends BufferedWriter {
    private final Writer out;
    
    public LineWriter(String filename, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(filename), charset);
    }

    public LineWriter(String filename, int sz, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(filename), sz, charset);
    }
    
    public LineWriter(File file, String charset) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(file), charset);
    }

    public LineWriter(File file, int sz, String charset) throws UnsupportedEncodingException, IOException {
        this(new FileOutputStream(file), sz, charset);
    }
    
    public LineWriter(OutputStream out, String charset) throws UnsupportedEncodingException {
        this(new OutputStreamWriter(out, charset), 8192);
    }

    public LineWriter(OutputStream out, int sz, String charset) throws UnsupportedEncodingException {
        this(new OutputStreamWriter(out, charset), sz);
    }

    private LineWriter(Writer out, int sz) {
        // by this point a charset has already been specified
        super(out, sz);
        this.out = out;
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
    @Override
    public String toString() {
        try {
            flush();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
        
        return out.toString();
    }
}