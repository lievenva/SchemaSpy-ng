package net.sourceforge.schemaspy.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * BufferedWriter that adds a <code>writeln()</code> method
 * to output a <i>lineDelimited</i> line of text without
 * cluttering up code.
 */
public class LineWriter extends BufferedWriter {

    public LineWriter(Writer out) {
	super(out);
    }

    public LineWriter(Writer out, int sz) {
	super(out, sz);
    }

    public void writeln(String str) throws IOException {
	write(str);
	newLine();
    }
}
