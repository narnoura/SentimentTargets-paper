/**
 * 
 */
package models.baselines;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * @author Narnoura
 * Couples xml Node with visited attribute
 */
public class VisitedNode {

	public Node node;
	public boolean visited;
	
	public VisitedNode () {
		
	}
	
	public VisitedNode (Node node) { 
		
	};
	
	public VisitedNode (Node node, boolean visited) {
		
	};
	
	public void SetVisited (boolean visited) {
		visited = visited;
	}
}
