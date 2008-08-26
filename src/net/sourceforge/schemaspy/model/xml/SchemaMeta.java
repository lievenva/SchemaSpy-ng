package net.sourceforge.schemaspy.model.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;
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
    
    public SchemaMeta(String xmlMeta, String dbName, String schema) throws InvalidConfigurationException {
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

                throw new InvalidConfigurationException("Meta directory \"" + xmlMeta + "\" must contain a file named \"" + filename + '\"');
            }
        } else if (!meta.exists()) {
            throw new InvalidConfigurationException("Specified meta file \"" + xmlMeta + "\" does not exist");
        }
        
        metaFile = meta;
        
        Document doc = parse(metaFile);
        if (doc == null) {
            throw new InvalidConfigurationException();
        }
    
        NodeList commentsNodes = doc.getElementsByTagName("comments");
        if (commentsNodes != null && commentsNodes.getLength() > 0)
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
    
    private void validate(Document document) throws SAXException, IOException {
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // load a WXS schema, represented by a Schema instance
        InputStream xsl = getClass().getResourceAsStream("/schemaspy.meta.xsd");
        
        Schema schema = factory.newSchema(new StreamSource(xsl));

        // create a Validator instance, which can be used to validate an instance document
        Validator validator = schema.newValidator();

        // validate the DOM tree
        validator.validate(new DOMSource(document));
    }
    
    private Document parse(File file) {
        DocumentBuilder docBuilder;
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        
        try {
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            System.err.println("Invalid parser configuration: " + exc.getMessage());
            return null;
        }

        try {
            doc = docBuilder.parse(file);
            validate(doc);
        } catch (SAXException exc) {
            // exception already reported to stderr by error handler
            System.err.println(file + " failed XML validation");
            return null;
        } catch (IOException exc) {
            System.err.println("Could not read file: " + exc.getMessage());
            return null;
        }

        return doc;
    }
}