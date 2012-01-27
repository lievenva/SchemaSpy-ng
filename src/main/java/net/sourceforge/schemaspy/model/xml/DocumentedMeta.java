package net.sourceforge.schemaspy.model.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DocumentedMeta {
  private final Element documentation;
  
  public DocumentedMeta(Element node) {
    if (node != null) {
	Node child = node.getFirstChild();
	while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
	    child = child.getNextSibling();
	}
	documentation = child != null && "documentation".equals(child.getLocalName()) 
		? (Element) child
	        : null;
    } else {
      documentation = null;
    }
  }
   
  public Element getDocumentation() {
    return documentation;
  }
}
