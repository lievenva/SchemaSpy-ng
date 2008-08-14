package net.sourceforge.schemaspy.model.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    private final File metaFile;
    
    public SchemaMeta(String xmlMeta, String dbName, String schema) {
        File meta = new File(xmlMeta);
        if (meta.isDirectory()) {
            String filename = (schema == null ? dbName : schema) + ".meta.xml";
            meta = new File(meta, filename);
            
            if (!meta.exists()) {
                System.err.println("Meta directory \"" + xmlMeta + "\" must contain a file named \"" + filename + '\"');
                System.exit(2);
            }
        } else if (!meta.exists()) {
            System.err.println("Specified meta file \"" + xmlMeta + "\" does not exist");
            System.exit(2);
        }
        
        this.metaFile = meta;
        
        Document doc = parse(metaFile);
        if (doc == null) {
            System.exit(1);
            return; // bogus return to avoid warnings
        }
        
        NodeList tablesNode = ((Element)doc.getElementsByTagName("tables").item(0)).getElementsByTagName("table");

        for (int i = 0; i < tablesNode.getLength(); ++i) {
            Node tableNode = tablesNode.item(i);
            TableMeta tableMeta = new TableMeta(tableNode);
            tables.add(tableMeta);
        }
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
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            System.out.println("Wrong parser configuration: " + e.getMessage());
            return null;
        }
        
        try {
            doc = docBuilder.parse(file);
        } catch (SAXException e) {
            System.out.println("Wrong XML file structure: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("Could not read source file: " + e.getMessage());
        }

        return doc;
    }
}