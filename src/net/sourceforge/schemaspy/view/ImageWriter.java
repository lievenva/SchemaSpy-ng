package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import net.sourceforge.schemaspy.util.ResourceWriter;

public class ImageWriter extends ResourceWriter {
    private static ImageWriter instance = new ImageWriter();

    private ImageWriter() {
    }

    public static ImageWriter getInstance() {
        return instance;
    }

    public void writeImages(File outputDir) throws IOException {
        new File(outputDir, "images").mkdir();

        writeResource("/images/tabLeft.gif", new File(outputDir, "/images/tabLeft.gif"));
        writeResource("/images/tabRight.gif", new File(outputDir, "/images/tabRight.gif"));
        writeResource("/images/background.gif", new File(outputDir, "/images/background.gif"));
    }
}
