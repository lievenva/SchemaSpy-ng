package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.util.LineWriter;

public class StyleSheet {
    private static StyleSheet instance;
    private final String css;
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

    public static StyleSheet getInstance() throws ParseException {
        if (instance == null) {
            try {
                instance = new StyleSheet(new BufferedReader(getReader(Config.getInstance().getCss())));
            } catch (IOException exc) {
                throw new ParseException(exc);
            }
        }
        
        return instance;
    }
    
    private static Reader getReader(String cssName) throws IOException {
        File cssFile = new File(cssName);
        if (cssFile.exists())
            return new FileReader(cssFile);
        cssFile = new File(System.getProperty("user.dir"), cssName);
        if (cssFile.exists())
            return new FileReader(cssFile);
        
        InputStream cssStream = StyleSheet.class.getClassLoader().getResourceAsStream(cssName);
        if (cssStream == null)
            throw new ParseException("Unable to find requested style sheet: " + cssName);
        
        return new InputStreamReader(cssStream);
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
        if (bodyBackgroundColor == null)
            throw new MissingCssPropertyException(".content", "background");
        
        return bodyBackgroundColor;
    }

    public String getTableBackground() {
        if (tableBackgroundColor == null)
            throw new MissingCssPropertyException("td", "background-color");
        
        return tableBackgroundColor;
    }

    public String getTableHeadBackground() {
        if (tableHeadBackgroundColor == null)
            throw new MissingCssPropertyException("th", "background-color");
        
        return tableHeadBackgroundColor;
    }

    public String getPrimaryKeyBackground() {
        if (primaryKeyBackgroundColor == null)
            throw new MissingCssPropertyException(".primaryKey", "background");
        
        return primaryKeyBackgroundColor;
    }

    public String getIndexedColumnBackground() {
        if (indexedColumnBackgroundColor == null)
            throw new MissingCssPropertyException(".indexedColumn", "background");
        
        return indexedColumnBackgroundColor;
    }

    public String getSelectedTableBackground() {
        if (selectedTableBackgroundColor == null)
            throw new MissingCssPropertyException(".selectedTable", "background");
        
        return selectedTableBackgroundColor;
    }

    public String getExcludedColumnBackgroundColor() {
        if (excludedColumnBackgroundColor == null)
            throw new MissingCssPropertyException(".excludedColumn", "background");
        
        return excludedColumnBackgroundColor;
    }
    
    public String getLinkColor() {
        if (linkColor == null)
            throw new MissingCssPropertyException("a:link", "color");
        
        return linkColor;
    }
    
    public String getLinkVisitedColor() {
        if (linkVisitedColor == null)
            throw new MissingCssPropertyException("a:visited", "color");
        
        return linkVisitedColor;
    }

    public static class MissingCssPropertyException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MissingCssPropertyException(String cssSection, String propName) {
            super("Required property '" + propName + "' was not found for the definition of '" + cssSection + "' in " + Config.getInstance().getCss());
        }
    }
    
    public static class ParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ParseException(Exception cause) {
            super(cause);
        }
        
        public ParseException(String msg) {
            super(msg);
        }
    }
}