package topicmodeling;

import processor.ToolsProcessor.StanfordParserProcessor;
import data.Comment;
import data.Token;
import data.Target;
import topicmodeling.CoreferenceFeatures;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.FileReader;


public class HobbsCoreference {
	
	// Takes as input comments and xml parse trees, and returns comments updated with 
	// pronominal coreference fields
	public static List<Comment> Update(List<Comment> feature_comments, Document parse_trees) {
		NodeList p_list = parse_trees.getElementsByTagName("s");
		if (feature_comments.size() != p_list.getLength()) {
			System.out.println("Couldn't update coreference features. Size of comments "
					+ "does not match number of parse trees. Continuing without coreference.\n");
			return feature_comments;
		}
		for (int p=0; p < p_list.getLength(); p++) { 
			Comment c = feature_comments.get(p);
			Node s_node = p_list.item(p);
			c = UpdateTokens(c, s_node);
			// can do UpdateHeadNodes for getting the head nodes for other tasks (like IncreaseLemma)
		}
		return feature_comments;
	}

	public static List<Node> ElementChildren(Node X) {
		List<Node> Xchild_list = new ArrayList<Node>();
		NodeList Xchilds = X.getChildNodes();
		for (int j=0; j<Xchilds.getLength();j++) {
			Node childNode = Xchilds.item(j);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
					Xchild_list.add(childNode);
				}
			}
		return Xchild_list;
		// There are 5 but really only 2 are element nodes, the first child and P (in 
		// the first example) // so need to get only the element nodes and add them 
		// in a list, then do the breadth first search
	}

	public static Element AntecedantStep3(Node X, Node P) {
		System.out.println("Step 3");
		Element antecedant = null;
		List<Node> children = ElementChildren(X);
		if (children.size()==0) {
			return antecedant;
		}
		Node left_child_node = children.get(0);
		Element left_child = (Element) left_child_node;
		int sibling = 0;
		// P is the only child, do nothing
		if (left_child_node.equals(P)) {
			return antecedant;
		}
		// Breadth first search on X branches to the left of path p
		while (sibling < children.size()) {  
				if (left_child_node.equals(P)) {
					System.out.println("Left child is P, quitting loop");
					break;
				}
				 // check if there is NP node between left child and X, go up the tree again
				if (left_child.getAttribute("value").equals("NP")) {
					Node potential_parent_node = left_child_node.getParentNode();
					Element potential_parent = (Element) potential_parent_node;
					while (!potential_parent_node.equals(X)) {
						if (potential_parent.getAttribute("value").equals("NP")
									|| potential_parent.getAttributeNode("value").equals("S")) {
								antecedant = left_child;
								break;
							}
							potential_parent_node = potential_parent_node.getParentNode();
							potential_parent = (Element) potential_parent_node;
					}
				} 
				if (antecedant != null) {
						break;
				}
				System.out.println("Left child:" + left_child.getAttribute("value"));
		       if (sibling < children.size()-1 && !children.get(sibling+1).equals(P)) {
		    	   		System.out.println("Sibling:" + sibling);
		    	   		System.out.println("Children size:" + children.size());
						left_child_node = children.get(sibling+1);  
						left_child = (Element) left_child_node;
						System.out.println("Sibling node:" + left_child.getAttribute("value"));
						List<Node> new_children = ElementChildren(left_child_node);
						children.addAll(new_children);
		       } else if (children.get(sibling+1).equals(P)) {
		    	   System.out.println("Next child is P. Breaking");
		    	   break;
		       }
		       sibling +=1;
			}  // If no more children  exit
			return antecedant;
	}
	
	public static Element AntecedantStep6(Node X, Node P) {
		System.out.println("Step 6");
		Element antecedant = null;
		Element left_child = null;
		Node left_child_node = null;
		if (!((Element) X).getAttribute("value").equals("NP")) {
			return antecedant;
		}
		List<Node> children = ElementChildren(X);
		if (children.size()==1 && children.get(0).equals(P)) {
			return antecedant;
		}
		for (int sibling = 0; sibling < children.size(); sibling++) {
			left_child_node = children.get(sibling);
			left_child = (Element) left_child_node;
			if (left_child_node.equals(P)) {
				System.out.println("Child is P");
				continue;
			} else if( left_child!=null
					&& left_child.getAttribute("value").equals("NN")
					|| left_child.getAttribute("value").equals("NNS") 
					|| left_child.getAttribute("value").equals("NNP")
					|| left_child.getAttribute("value").equals("NNPS")
					|| left_child.getAttribute("value").equals("DTNN") 
					|| left_child.getAttribute("value").equals("DTNNS")
					|| left_child.getAttribute("value").equals("DTNNP")
					|| left_child.getAttribute("value").equals("DTNNPS")
					) {
				antecedant = (Element) X;
				System.out.println("Left child:"+ left_child.getAttribute("value"));
				System.out.println("Sibling:"+ sibling);
				System.out.println("Left child leaf:"+ ((Element) ElementChildren(left_child_node).get(0)).getAttribute("value"));
				break;
			}
		}
	   return antecedant;
	}
		
	
	public static Element AntecedantStep7(Node X, Node P) {
		System.out.println("Step 7\n");
		Element antecedant = null;
		List<Node> children = ElementChildren(X);
		if (children.size()==0 || children.size()==1) {
			System.out.println("No children or P is only child, returning");
			return antecedant;
		} else {
			System.out.println("Number of children:" + children.size());
		}
		Node left_child_node = children.get(0);
		Element left_child = (Element) left_child_node;
		int sibling = 0;
		// P is the only child, do nothing
		if (left_child_node.equals(P)) {
			System.out.println("Left child is P, returning");
			return antecedant;
		}
		// Breadth first search on X branches to the left of path p
		while (sibling < children.size()) {  
				if (left_child_node.equals(P)) {
					System.out.println("Left child is P, quitting loop");
					break;
				}
				if (left_child.getAttribute("value").equals("NP")) {
					System.out.println("Found first NP");
					System.out.println("Sibling:" + sibling);
					antecedant = left_child;
					break;
				} 
				if (antecedant != null) {
						break;
				}
		       if (sibling < children.size()-1 && !children.get(sibling+1).equals(P)) {
		    	   		System.out.println("Sibling:" + sibling);
		    	   		System.out.println("Children size:" + children.size());
						left_child_node = children.get(sibling+1);  
						left_child = (Element) left_child_node;
						List<Node> new_children = ElementChildren(left_child_node);
						children.addAll(new_children);
		       }
		       sibling +=1;
			}  // If no more children  exit
			return antecedant;
	}
	
	public static Element AntecedantStep8(Node X, Node P) {
		System.out.println("Step 8");
		Element antecedant = null;
		if (!((Element) X).getAttribute("value").equals("S")) {
			return antecedant;
		}
		List<Node> children = ElementChildren(X);
		if (children.size()==0) {
			return antecedant;
		}
		// P is rightmost child
		if ((children.size()==1 && children.get(0).equals(P)) 
				|| children.get(children.size()-1).equals(P)) {
			return antecedant;
		}
		int sibling = 0;
		// Breadth first search on X branches to the *right* of path p
		// First, find the branch to the right of P
		Node right_child_node = children.get(0);
		while (sibling < children.size()-1) {
			if (right_child_node.equals(P)) {
				right_child_node = children.get(sibling+1);
				sibling +=1;
				break;
			}
			right_child_node = children.get(sibling+1);
			sibling +=1;
		}
		Element right_child = (Element) right_child_node;
		// P is the rightmost child
		if (right_child_node.equals(P)) {
			System.out.println("Right child is P. Weird.");
			return antecedant;
		}
		// BFS
		while (sibling < children.size()) {  
				System.out.println("Right child:"+right_child.getAttribute("value"));
				if (right_child.getAttribute("value").equals("NP")) {
					antecedant = right_child;
					break;
				} 
				if (right_child.getAttribute("value").equals("S")) {
					break;
				}
		       if (sibling < children.size()-1) {
		    	   	System.out.println("Sibling:" + sibling);
		    	   	System.out.println("Children size:" + children.size());
					right_child_node = children.get(sibling+1);  
					right_child = (Element) right_child_node;
					List<Node> new_children = ElementChildren(right_child_node);
					children.addAll(new_children);
		       }
		       sibling +=1;
			}  // If no more children  exit	
		
		return antecedant;
	}
	
	// For coreferring pronominal mentions, updates their coreference fields
	// Fills in Al+ for ATB
	public static Comment UpdateTokens(Comment feature_comment, Node parse_tree) {
			System.out.println("\nRunning Hobbs: Comment id:" + feature_comment.comment_id_);
			Element e_element = (Element) parse_tree;
			NodeList nodes = e_element.getElementsByTagName("node");
			NodeList leafs = e_element.getElementsByTagName("leaf");
			List<Node> leaf_list = GetListFromNodes(leafs);
			List<Node> node_list = GetListFromNodes(nodes);
			// Create a version of the tokens without Al+ so we can
			// get the index right
			List<Token> tokens_without_Al = new ArrayList<Token>(); 
			for (Token t: feature_comment.tokens_) {
				if (!t.text_.equals("Al+")) {
					tokens_without_Al.add(t);
				}
			}
			// Loop over all tokens, update fields for the pronouns. Skip Al+
			// and update them later.
			int node = 0;
			for (int i=0; i<feature_comment.tokens_.size(); i++) {
				Token t = feature_comment.tokens_.get(i);
				if (t.text_.equals("Al+")) {
					continue;
				}
				Node leaf_node =  leafs.item(node);
				Element leaf_element = null;
				if (leaf_node.getNodeType() == Node.ELEMENT_NODE) {
					leaf_element = (Element) leaf_node;
					String value = leaf_element.getAttribute("value");
					System.out.println("Token: " + t.text_ + 
								" Node: i:" + i + " " + "node: " + node + " " + value);
				}
				Node parent_node = leaf_node.getParentNode();
				Element parent_element = null;
				if (parent_node.getNodeType() == Node.ELEMENT_NODE) {
					 parent_element = (Element) parent_node;
					String value = parent_element.getAttribute("value");
					System.out.println(" parent node: " + node + " " + value);
				}
				String leaf_POS = parent_element.getAttribute("value");
				
				if (leaf_POS.equals("PRP") || leaf_POS.equals("PRP$")) {        
					//FindAntecedant(leaf_element,)
					//int antecedant = FindAntecedant(leaf_element, t, parse_tree);
					Node antecedant_n = null;
					Element antecedant = null;
					int antecedant_node = -1;
					Token antecedant_token = null;
					
					// Step 1: go to NP node (or any node) immediately dominating pronoun
					Node p_node = parent_node.getParentNode();
					Element parent = (Element) p_node;
					// may not be NP. (parsing error) (e.g shklhm -> shkl is VBD, parent node is "X" (actual X). 
					// can either get the direct parent or assume all parents are NPs.
					// Pronouns can appear in these verbal compositions, in majority of cases the parent will be NP
					System.out.println("Parent:" + parent.getAttribute("value") );
					System.out.println("Comment id:" + feature_comment.comment_id_);
					System.out.println("Hobbs Step 1: NP dominating pronoun:" + parent.getAttribute("value"));
					
					// Step 2: Go up the tree to the first NP or S node encountered. 
					// Call this node X, and call the path used to reach it p.
					System.out.println("Step 2");
					Node P_node = p_node;
					Node X_node= p_node.getParentNode();
					Element P= (Element) P_node;
					Element X= (Element) X_node;
					System.out.println("P:" + P.getAttribute("value"));
					System.out.println("X:" + X.getAttribute("value"));
					while (!X.getAttribute("value").equals("NP") && !X.getAttribute("value").equals("S")
							&& !X.getAttribute("value").equals("SQ") 
							&& !((Element) X.getParentNode()).getAttribute("value").equals("ROOT")) {
						P_node = X_node;
						X_node = X_node.getParentNode();
						P = (Element) P_node;
						X = (Element) X_node;
						System.out.println("X:" + X.getAttribute("value") );
					}
					System.out.println("First NP or S node encountered:" + X.getAttribute("value"));
					System.out.println("Node P to get to X:" + P.getAttribute("value"));
					
					// Step 3: Traverse all branches below node X to the left of path p
					// in a left-to-right, breadth-first fashion. Propose as the antecedant
					// any NP node that is encountered which has an NP or S node beween it and X.
					// (This step takes care of the level of the tree where there are 
					// reflexive pronouns.) 
					antecedant = AntecedantStep3(X_node,P_node);
					// Match antecedant
					if (antecedant!=null) {
						antecedant_node = HeadNounIndexFromNPNode(antecedant, leaf_list);
						if (antecedant_node == -1) {
							System.out.println("Antecedant index is -1. No valid antecedant. ");
							antecedant = null;
						} else {
						Token t2 = tokens_without_Al.get(antecedant_node);
						if (!CoreferenceFeatures.MatchGenderArabic(t, t2) 
							|| !CoreferenceFeatures.MatchNumberArabic(t, t2)
							) {
							//|| !CoreferenceFeatures.MatchPersonArabic(t, t2)) {
							System.out.println("Antecedant doesn't match gender, number, or person! "
									+ "Moving on to next rule");
							System.out.println("Pronoun:" + t.text_);
							System.out.println("Antecedant:" + t2.text_);
							antecedant = null;
						} else {
							System.out.println("Found antecedant!");
							antecedant_token = t2;
							// Passed, make all updates
							t.SetCoreferringToken(antecedant_token);
							t.SetCoreferrent(true);
							System.out.println("Pronoun:" + t.text_);
							System.out.println("Antecedant:" + antecedant_token.text_);
							if (i>0 && feature_comment.tokens_.get(i-1).text_.equals("Al+")) {
								Token previous = feature_comment.tokens_.get(i-1);
								previous.SetCoreferrent(true);
								previous.SetCoreferringToken(antecedant_token);
							}
							// Now, update all nominals in the antecedant NP
							List<Integer> indices = IndicesFromNPNode(antecedant, leaf_list);
							System.out.println("Indices:");
							for (int k=0; k< indices.size(); k++) {
								Integer index = indices.get(k);
								System.out.println(index + " ");
								Token another_antecedant_token = tokens_without_Al.get(index);
								String pos = another_antecedant_token.pos_;
								String lemma = TopicSalience.WordRepresentation(another_antecedant_token);
								System.out.println("Lemma:" + lemma);
								if (pos.equals("noun") || pos.equals("noun_prop") || pos.equals("adj")) {
									System.out.println("Nominal, adding lemma");
									 t.AddCoreferringToken(another_antecedant_token);
								}
							}
							// go on to next token
							node +=1;
							continue; 
						}
					   }
					}
					
					// Step 4
				    // If node X is the highest node in the sentence, traverse the surface parse trees
					// of previous sentences in the text in order of recency, the most recent first; each
					// tree is traversed in a left-to-right, breadth-first manner, and when an NP node
					// is encountered, it is proposed as antecedant. If X is not the highest S node in
					// the sentence, continue to step 5.
					// 
					// I will ignore this step because we only have one sentence, technically speaking
					// if we keep going up from S we'll find another S node.
					// I am going to put steps 5-8 in here and exit when we get to root or find the antecedant
				   boolean found_antecedant = false;
				   System.out.println("Steps 5-8");
				   System.out.println("X:" + X.getAttribute("value"));
				   while (!found_antecedant && !X.getAttribute("value").equals("ROOT")) {
					   
					// Step 5
					// From node X, go up the tree to the first NP or S node encountered. Call this new node X,
					// and call the path traversed to reach it p. 
					System.out.println("Step 5");
					if (! ((Element) X_node.getParentNode()).getAttribute("value").equals("ROOT")) {
			        P_node = X_node;
				    X_node = X_node.getParentNode();
					X = (Element) X_node;
					P = (Element) P_node;
					System.out.println("X:" + X.getAttribute("value"));
					}
					while (!((Element) X_node.getParentNode()).getAttribute("value").equals("ROOT") 
							&& !X.getAttribute("value").equals("NP") && !X.getAttribute("value").equals("S")) {
						P_node = X_node;
						X_node = X_node.getParentNode();
						X = (Element) X_node;
						P = (Element) P_node;
						System.out.println("X:" + X.getAttribute("value"));
					}
					
					// Step 6
					// If X is an NP node and if the path p to X did not pass through the N node that X immediately
					// dominates, propose X as the antecedant.
					// Will assume that if the left sibling has the N node, then P doesn't
					antecedant = AntecedantStep6(X_node,P_node);
					if (antecedant!=null) {
							antecedant_node = HeadNounIndexFromNPNode(antecedant, leaf_list);
							if (antecedant_node == -1) {
								System.out.println("Antecedant index is -1 :( ");
								antecedant = null;
							}
							Token t2 = tokens_without_Al.get(antecedant_node);
							if (!CoreferenceFeatures.MatchGenderArabic(t, t2) 
								|| !CoreferenceFeatures.MatchNumberArabic(t, t2)
								) {
								//|| !CoreferenceFeatures.MatchPersonArabic(t, t2)) {
								System.out.println("Antecedant doesn't match gender, number, or person! "
										+ "Moving on to next rule");
								System.out.println("Pronoun:" + t.text_);
								System.out.println("Antecedant:" + t2.text_);
								antecedant = null;
							} else {
								System.out.println("Found antecedant!");
								antecedant_token = t2;
								// Passed, make all updates
								found_antecedant = true;
								t.SetCoreferrent(true);
								t.SetCoreferringToken(antecedant_token);
								System.out.println("Pronoun:" + t.text_);
								System.out.println("Antecedant:" + antecedant_token.text_);
								if (i>0 && feature_comment.tokens_.get(i-1).text_.equals("Al+")) {
									// actually won't be likely since it's a pronoun
									Token previous = feature_comment.tokens_.get(i-1);
									previous.SetCoreferrent(true);
									previous.SetCoreferringToken(antecedant_token);
								} 
								// Now, update all nominals in the antecedant NP
								List<Integer> indices = IndicesFromNPNode(antecedant, leaf_list);
								System.out.println("Indices:");
								for (int k=0; k< indices.size(); k++) {
									Integer index = indices.get(k);
									System.out.println(index + " ");
									Token another_antecedant_token = tokens_without_Al.get(index);
									String pos = another_antecedant_token.pos_;
									String lemma = TopicSalience.WordRepresentation(another_antecedant_token);
									System.out.println("Lemma:" + lemma);
									if (pos.equals("noun") || pos.equals("noun_prop") || pos.equals("adj")) {
										System.out.println("Nominal, adding lemma");
										 t.AddCoreferringToken(another_antecedant_token);
									}
								}
								// go on to next token
								break; // break this while loop
								//continue; 
							}
						}
					 // end step 6
					
					// Step 7
					// Traverse all branches below node X to the left of path p in a left-to-right,
					// breadth-first manner. Propose any NP node encountered as the antecedent.
					//
					// Almost done!
					antecedant = AntecedantStep7(X_node,P_node);
					if (antecedant!=null) {
						antecedant_node = HeadNounIndexFromNPNode(antecedant, leaf_list);
						if (antecedant_node == -1) {
							System.out.println("No valid antecedant. Antecedant index is -1");
							antecedant = null;
						} else {
						Token t2 = tokens_without_Al.get(antecedant_node);
						if (!CoreferenceFeatures.MatchGenderArabic(t, t2) 
							|| !CoreferenceFeatures.MatchNumberArabic(t, t2) ) {
							//|| !CoreferenceFeatures.MatchPersonArabic(t, t2)) {
							System.out.println("Antecedant doesn't match gender, number, or person! "
									+ "Moving on to next rule");
							System.out.println("Pronoun:" + t.text_);
							System.out.println("Antecedant:" + t2.text_);
							antecedant = null;
						} else {
							System.out.println("Found antecedant!");
							antecedant_token = t2;
							// Passed, make all updates
							found_antecedant = true;
							t.SetCoreferrent(true);
							t.SetCoreferringToken(antecedant_token);
							System.out.println("Pronoun:" + t.text_);
							System.out.println("Antecedant:" + antecedant_token.text_);
							if (i>0 && feature_comment.tokens_.get(i-1).text_.equals("Al+")) {
								Token previous = feature_comment.tokens_.get(i-1);
								previous.SetCoreferrent(true);
								previous.SetCoreferringToken(antecedant_token);
							}
							// Now, update all nominals in the antecedant NP
							List<Integer> indices = IndicesFromNPNode(antecedant, leaf_list);
							System.out.println("Indices:");
							for (int k=0; k< indices.size(); k++) {
								Integer index = indices.get(k);
								System.out.println(index + " ");
								Token another_antecedant_token = tokens_without_Al.get(index);
								String pos = another_antecedant_token.pos_;
								String lemma = TopicSalience.WordRepresentation(another_antecedant_token);
								System.out.println("Lemma:" + lemma);
								if (pos.equals("noun") || pos.equals("noun_prop") || pos.equals("adj")) {
									System.out.println("Nominal, adding lemma");
									 t.AddCoreferringToken(another_antecedant_token);
								}
							}
							break; // break while loop
							//continue; 
						}
					  }
					}
					
				// Step 8
				// If X is an S node, traverse all branches of node X to the *right* of path p in 
				// a left-to-right, breadth-first manner, but do not go below any NP or S node
				// encountered. Propose any NP node encountered as the antecedent. 
				antecedant = AntecedantStep8(X_node,P_node);
				if (antecedant!=null) {
					antecedant_node = HeadNounIndexFromNPNode(antecedant, leaf_list);
					if (antecedant_node == -1) {
						System.out.println("Antecedant index is -1. No valid antecedant ");
						antecedant = null;
					}  else {
					Token t2 = tokens_without_Al.get(antecedant_node);
					if (!CoreferenceFeatures.MatchGenderArabic(t, t2) 
						|| !CoreferenceFeatures.MatchNumberArabic(t, t2)
							) {
					 //	|| !CoreferenceFeatures.MatchPersonArabic(t, t2)) {
						System.out.println("Antecedant doesn't match gender, number, or person! "
								+ "Moving on to next rule");
						System.out.println("Pronoun:" + t.text_);
						System.out.println("Antecedant:" + t2.text_);
						antecedant = null;
					} else {
						System.out.println("Found antecedant!");
						antecedant_token = t2;
						// Passed, make all updates
						found_antecedant = true;
						t.SetCoreferrent(true);
						t.SetCoreferringToken(antecedant_token);
						System.out.println("Pronoun:" + t.text_);
						System.out.println("Antecedant:" + antecedant_token.text_);
						if (i>0 && feature_comment.tokens_.get(i-1).text_.equals("Al+")) {
							Token previous = feature_comment.tokens_.get(i-1);
							previous.SetCoreferrent(true);
							previous.SetCoreferringToken(antecedant_token);
						}
						// Now, update all nominals in the antecedant NP
						List<Integer> indices = IndicesFromNPNode(antecedant, leaf_list);
						System.out.println("Indices:");
						for (int k=0; k< indices.size(); k++) {
							Integer index = indices.get(k);
							System.out.println(index + " ");
							Token another_antecedant_token = tokens_without_Al.get(index);
							String pos = another_antecedant_token.pos_;
							String lemma = TopicSalience.WordRepresentation(another_antecedant_token);
							System.out.println("Lemma:" + lemma);
							if (pos.equals("noun") || pos.equals("noun_prop") || pos.equals("adj")) {
								System.out.println("Nominal, adding lemma");
								 t.AddCoreferringToken(another_antecedant_token);
							}
						}
						break; // break while loop
						// go on to next token
						//continue; 
					}
				  }
				}
				if (((Element) X_node.getParentNode()).getAttribute("value").equals("ROOT")) {
					break;
				}
			 
	       } // end while loop for steps 5-8
	     } // end if PRP
	    node +=1; 
	  } // end loop
	return feature_comment; 
	}
	
	// Returns the node index of the parse tree that corefers with the pronoun
	/*public static int FindAntecedant(Element pronoun, Token t, Node parse_tree) {
		return 0;
	}*/
	
	// Given the antecedant node, returns all leaf nodes of the noun phrase
	// Or start and end
	public static List<Integer> IndicesFromNPNode(Element antecedant_node, List<Node> leafs) {
		System.out.println("Finding indeces of antecedant.");
		List<Integer> indices = new ArrayList<Integer>();
		System.out.println("Antecedant:" + antecedant_node.getAttribute("value"));
		List<Node> children = ElementChildren(antecedant_node);
		System.out.println("Number of children:" + children.size());
		int index = -1;
		
		for (int sibling = 0; sibling < children.size(); sibling++) {
			Node child_node = children.get(sibling);
			Element child = (Element) child_node;
			List<Node> new_children = ElementChildren(child_node);
			if (new_children.size()!=0) {
				children.addAll(new_children);
			}
			if (leafs.contains(child_node)) {
				index = leafs.indexOf(child_node);
				indices.add(index);
				System.out.println("Added index: " + index + " for child " +
				((Element) child_node).getAttribute("value"));
			}
		}
		if (indices.isEmpty()) {
			System.out.println("Something wrong. Indices empty.");
			indices.add(-1);
		}
		return indices;
	}
	
	// Finds the head of the NP. Similar to Collins head rules.
	// Goes from left to right, finds the first child with NN, NNS, DTNN ....
	// Else searches for first CD or NOUN_QUANT
	// else returns -1. We don't want pronouns since we are looking for targets
	public static int HeadNounIndexFromNPNode(Element antecedant_node, List<Node> leafs) {
		System.out.println("Finding index of antecedant.");
		int index = 0;
		System.out.println("Antecedant:" + antecedant_node.getAttribute("value"));
		List<Node> children = ElementChildren(antecedant_node);
		System.out.println("Number of children:" + children.size());
		Node leaf_node = null;
		Element leaf_child = null;
		
		for (int sibling =0; sibling < children.size(); sibling++) {
			Node child_node = children.get(sibling);
			Element child = (Element) child_node;
			if (child.getAttribute("value").equals("NN") 
					|| child.getAttribute("value").equals("NNS") 
					|| child.getAttribute("value").equals("NNP") 
					|| child.getAttribute("value").equals("NNPS")
					|| child.getAttribute("value").equals("DTNN")
					|| child.getAttribute("value").equals("DTNNS")
					|| child.getAttribute("value").equals("DTNNP")
					|| child.getAttribute("value").equals("DTNNPS")
					|| child.getAttribute("value").equals("DTJJ")
					|| child.getAttribute("value").equals("JJR")) {
						leaf_node = child_node;
						leaf_child = child;
						break;
					}
			// BFS
			System.out.println("Adding children of child " + child.getAttribute("value"));
			List<Node> new_children = ElementChildren(child_node);
			System.out.println("Size of these children: " + new_children.size());
			children.addAll(new_children);
		}
		// Didn't find ANY NN, NNS, NNP ... in the NP. Search for CD
		// Search for CD or QUANT_NP
		if (leaf_node == null) {
			for (int sibling =0; sibling < children.size(); sibling++) {
				Node child_node = children.get(sibling);
				Element child = (Element) child_node;
				if (child.getAttributeNode("value").equals("CD") 
						|| child.getAttributeNode("value").equals("NOUN_QUANT")) {
					leaf_node = child_node;
					leaf_child = child;
					break;
				}
			}
		}
		// None of these, so quit. Don't want to consider pronouns, punctuations, LLRB, or weird things.
		if (leaf_node == null) {
			System.out.println("Didn't find any valid antecedant");
			index = -1;
			return index;
		}
		// Leaf should be the (only) child of the nominal
		if (ElementChildren(leaf_node).size()==0) {
			System.out.println("This bizarre nominal has no children: " +
		((Element) leaf_node).getAttribute("value"));
			index = -1;
			return index;
		}
		leaf_node = ElementChildren(leaf_node).get(0);
		if (!leafs.contains(leaf_node)) {
			System.out.println("No valid antecedant. Leafs doesn't contain this leaf node: "
		+ ((Element) leaf_node).getAttributeNode("value"));
			index = -1;
			return index;
		}
		index = leafs.indexOf(leaf_node);
		
		return index;
		}

	
	public HobbsCoreference() {
		// TODO Auto-generated constructor stub
	}
	
	static List<Node> GetListFromNodes (NodeList leafs) {
		List<Node> visited_leafs = new ArrayList<Node>();
		// Set tokens for comment and store leaf node
		for (int n=0;n<leafs.getLength(); n++) {
			visited_leafs.add(leafs.item(n));
		}
		return visited_leafs;
	}

}

// Consider different types of Arabic pronouns