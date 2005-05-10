package net.sourceforge.schemaspy.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import net.sourceforge.schemaspy.LineWriter;

public class StyleSheet {
    private static String css;
    private static String bodyBackgroundColor;
    private static String tableHeadBackgroundColor;
    private static String tableBackgroundColor;
    private static String primaryKeyBackgroundColor;
    private static String indexedColumnBackgroundColor;
    private static String selectedTableBackgroundColor;
    private static final List ids = new ArrayList();

    public static void init(BufferedReader cssReader) throws IOException {
        String lineSeparator = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();
        String line;

        while ((line = cssReader.readLine()) != null) {
            data.append(line);
            data.append(lineSeparator);
        }

        css = data.toString();

        int startComment = data.indexOf("/*");
        while (startComment != -1) {
            int endComment = data.indexOf("*/");
            data.replace(startComment, endComment + 2, "");
            startComment = data.indexOf("/*");
        }

        StringTokenizer tokenizer = new StringTokenizer(data.toString(), "{}");
        String id = null;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (id == null) {
                id = token.toLowerCase();
                ids.add(id);
            } else {
                Map attribs = parseAttributes(token);
                if (id.equals("body"))
                    bodyBackgroundColor = attribs.get("background-color").toString();
                else if (id.equals("th"))
                    tableHeadBackgroundColor = attribs.get("background-color").toString();
                else if (id.equals("td"))
                    tableBackgroundColor = attribs.get("background-color").toString();
                else if (id.equals(".primarykey"))
                    primaryKeyBackgroundColor = attribs.get("background").toString();
                else if (id.equals(".indexedcolumn"))
                    indexedColumnBackgroundColor = attribs.get("background").toString();
                else if (id.equals(".selectedtable"))
                    selectedTableBackgroundColor = attribs.get("background").toString();
                id = null;
            }
        }
    }

    private static Map parseAttributes(String data) {
        Map attribs = new HashMap();
        StringTokenizer attrTokenizer = new StringTokenizer(data, ";");
        while (attrTokenizer.hasMoreTokens()) {
            StringTokenizer pairTokenizer = new StringTokenizer(attrTokenizer.nextToken(), ":");
            String attribute = pairTokenizer.nextToken().trim().toLowerCase();
            String value = pairTokenizer.nextToken().trim().toLowerCase();
            attribs.put(attribute, value);
        }

        return attribs;
    }

    public static void write(LineWriter out) throws IOException {
        out.write(css);
    }

    public static String getBodyBackground() {
        return bodyBackgroundColor;
    }

    public static String getTableBackground() {
        return tableBackgroundColor;
    }

    public static String getTableHeadBackground() {
        return tableHeadBackgroundColor;
    }

    public static String getPrimaryKeyBackground() {
        return primaryKeyBackgroundColor;
    }

    public static String getIndexedColumnBackground() {
        return indexedColumnBackgroundColor;
    }

    public static String getSelectedTableBackground() {
        return selectedTableBackgroundColor;
    }

    public static int getOffsetOf(String id) {
        int offset = ids.indexOf(id.toLowerCase());
        if (offset == -1)
            throw new IllegalArgumentException(id);
        return offset;
    }
}
