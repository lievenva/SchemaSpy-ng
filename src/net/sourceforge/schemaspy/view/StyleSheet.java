package net.sourceforge.schemaspy.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import net.sourceforge.schemaspy.util.LineWriter;

public class StyleSheet {
    private static StyleSheet instance;
    private String css;
    private String bodyBackgroundColor;
    private String tableHeadBackgroundColor;
    private String tableBackgroundColor;
    private String linkColor;
    private String linkVisitedColor;
    private String primaryKeyBackgroundColor;
    private String indexedColumnBackgroundColor;
    private String selectedTableBackgroundColor;
    private String excludedColumnBackgroundColor;
    private final List<String> ids = new ArrayList<String>();

    private StyleSheet(BufferedReader cssReader) throws IOException {
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
                Map<String, String> attribs = parseAttributes(token);
                if (id.equals(".content"))
                    bodyBackgroundColor = attribs.get("background");
                else if (id.equals("th"))
                    tableHeadBackgroundColor = attribs.get("background-color");
                else if (id.equals("td"))
                    tableBackgroundColor = attribs.get("background-color");
                else if (id.equals(".primarykey"))
                    primaryKeyBackgroundColor = attribs.get("background");
                else if (id.equals(".indexedcolumn"))
                    indexedColumnBackgroundColor = attribs.get("background");
                else if (id.equals(".selectedtable"))
                    selectedTableBackgroundColor = attribs.get("background");
                else if (id.equals(".excludedcolumn"))
                    excludedColumnBackgroundColor = attribs.get("background");
                else if (id.equals("a:link"))
                    linkColor = attribs.get("color");
                else if (id.equals("a:visited"))
                    linkVisitedColor = attribs.get("color");
                id = null;
            }
        }
    }

    public static StyleSheet getInstance() {
        return instance;
    }

    public static void init(BufferedReader cssReader) throws IOException {
        instance = new StyleSheet(cssReader);
    }

    private Map<String, String> parseAttributes(String data) {
        Map<String, String> attribs = new HashMap<String, String>();

        try {
            StringTokenizer attrTokenizer = new StringTokenizer(data, ";");
            while (attrTokenizer.hasMoreTokens()) {
                StringTokenizer pairTokenizer = new StringTokenizer(attrTokenizer.nextToken(), ":");
                String attribute = pairTokenizer.nextToken().trim().toLowerCase();
                String value = pairTokenizer.nextToken().trim().toLowerCase();
                attribs.put(attribute, value);
            }
        } catch (NoSuchElementException badToken) {
            System.err.println("Failed to extract attributes from '" + data + "'");
            throw badToken;
        }

        return attribs;
    }

    public void write(LineWriter out) throws IOException {
        out.write(css);
    }

    public String getBodyBackground() {
        return bodyBackgroundColor;
    }

    public String getTableBackground() {
        return tableBackgroundColor;
    }

    public String getTableHeadBackground() {
        return tableHeadBackgroundColor;
    }

    public String getPrimaryKeyBackground() {
        return primaryKeyBackgroundColor;
    }

    public String getIndexedColumnBackground() {
        return indexedColumnBackgroundColor;
    }

    public String getSelectedTableBackground() {
        return selectedTableBackgroundColor;
    }

    public String getExcludedColumnBackgroundColor() {
        return excludedColumnBackgroundColor;
    }
    
    public String getLinkColor() {
        return linkColor;
    }
    
    public String getLinkVisitedColor() {
        return linkVisitedColor;
    }

    public int getOffsetOf(String id) {
        int offset = ids.indexOf(id.toLowerCase());
        if (offset == -1)
            throw new IllegalArgumentException(id);
        return offset;
    }
}