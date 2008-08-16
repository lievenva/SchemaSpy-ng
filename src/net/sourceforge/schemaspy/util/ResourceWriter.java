package net.sourceforge.schemaspy.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResourceWriter {
    private static ResourceWriter instance = new ResourceWriter();

    protected ResourceWriter() {
    }

    public static ResourceWriter getInstance() {
        return instance;
    }

    /**
     * Write the specified resource to the specified filename
     * 
     * @param resourceName
     * @param writeTo
     * @throws IOException
     */
    public void writeResource(String resourceName, File writeTo) throws IOException {
        writeTo.getParentFile().mkdirs();
        InputStream in = getClass().getResourceAsStream(resourceName);
        if (in == null)
            throw new IOException("Resource \"" + resourceName + "\" not found");
        
        byte[] buf = new byte[4096];

        OutputStream out = new FileOutputStream(writeTo);
        int numBytes = 0;
        while ((numBytes = in.read(buf)) != -1) {
            out.write(buf, 0, numBytes);
        }
        in.close();
        out.close();
    }
}