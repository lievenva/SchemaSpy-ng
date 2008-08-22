package net.sourceforge.schemaspy.model.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.sourceforge.schemaspy.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author John Currier
 */
public class SchemaMeta {
    private final List<TableMeta> tables = new ArrayList<TableMeta>();
    private final String comments;
    private final File metaFile;
    
    @SuppressWarnings("null") // System.exit() results in compiler complaints about null doc refs
    public SchemaMeta(String xmlMeta, String dbName, String schema) {
        File meta = new File(xmlMeta);
        if (meta.isDirectory()) {
            String filename = (schema == null ? dbName : schema) + ".meta.xml";
            meta = new File(meta, filename);
            
            if (!meta.exists()) {
                if (Config.getInstance().isOneOfMultipleSchemas()) {
                    // don't force all of the "one of many" schemas to have metafiles
                    System.out.println("Meta directory \"" + xmlMeta + "\" should contain a file named \"" + filename + '\"');
                    comments = null;
                    metaFile = null;
                    return;
                }

                System.err.println("Meta directory \"" + xmlMeta + "\" must contain a file named \"" + filename + '\"');
                System.exit(2);
            }
        } else if (!meta.exists()) {
            System.err.println("Specified meta file \"" + xmlMeta + "\" does not exist");
            System.exit(2);
        }
        
        metaFile = meta;
        
        Document doc = parse(metaFile);
        if (doc == null) {
            System.exit(1);
        }
    
        NodeList commentsNodes = doc.getElementsByTagName("comments");
        if (commentsNodes != null)
            comments = commentsNodes.item(0).getTextContent();
        else
            comments = null;

        NodeList tablesNodes = doc.getElementsByTagName("tables");
        if (tablesNodes != null) {
            NodeList tableNodes = ((Element)tablesNodes.item(0)).getElementsByTagName("table");
    
            for (int i = 0; i < tableNodes.getLength(); ++i) {
                Node tableNode = tableNodes.item(i);
                TableMeta tableMeta = new TableMeta(tableNode);
                tables.add(tableMeta);
            }
        }
    }

    /**
     * Comments that describe the schema
     */
    public String getComments() {
        return comments;
    }
    
    public File getFile() {
        return metaFile;
    }
    
    public List<TableMeta> getTables() {
        return tables;
    }
    
    private Document parse(File file) {
        DocumentBuilder docBuilder;
        Document doc = null;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setIgnoringComments(true);
        
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            System.err.println("Wrong parser configuration: " + e.getMessage());
            return null;
        }
        
        try {
            doc = docBuilder.parse(file);
        } catch (SAXException e) {
            System.err.println("Wrong XML file structure: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not read source file: " + e.getMessage());
        }

        return doc;
    }
}