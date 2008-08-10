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
    
    public SchemaMeta(File file) {
        Document doc = parse(file);
        
        NodeList tablesNode = ((Element)doc.getElementsByTagName("tables").item(0)).getElementsByTagName("table");

        System.out.println("numTables: " + tablesNode.getLength());        
        for (int i = 0; i < tablesNode.getLength(); ++i) {
            Node tableNode = tablesNode.item(i);
            TableMeta meta = new TableMeta(tableNode);
            tables.add(meta);
        }
    }
    
    public List<TableMeta> getTables() {
        return tables;
    }
    
    private Document parse(File file) {
        System.out.println("Parsing XML file... " + file);
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
        
        System.out.println("XML file parsed");
        return doc;
    }
    
    public static void main(String args[]) throws Exception
    {
        new SchemaMeta(new File("mangosMetadata.xml"));
    }
}