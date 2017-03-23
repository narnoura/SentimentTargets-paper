/**
 * 
 */
package processor.ToolsProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import data.DeftToken;
import data.Token;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import models.FeatureExtractor;
import processor.LexiconProcessor;
import processor.Sentiment;
import util.FileReader;
import util.Tokenizer;
/**
 * @author noura
 * Processes dependency relations from Catib pipeline output.
 */
public class DependencyProcessor {
	// List of hashmaps where each corresponds to a comment
	// For each comment, looks like this
	// hash[1]: 6 columns. hash[2]: 6 columns. .. hash[last token]: 6 columns
	//public List<HashMap<String, String[]>> dependency_trees;
	public List<List<String[]>> dependency_trees;
	public String file_path;
	public DependencyProcessor() {
		//dependency_trees = new ArrayList<HashMap<String, String[]>>();
		dependency_trees = new ArrayList<List<String[]>>();
	}
	public void SetPath (String catib_output) {
		file_path = catib_output;
	}
	public void ReadDependencyTrees() {
		System.out.println("Reading Catib dependency parse trees");
		List<String> trees = util.FileReader.ReadFile(file_path, "bw", false);
		List<String[]> comment_dependencies = new ArrayList<String[]>();
		for (String line: trees) {
			line = line.trim();
			if (!line.equals("")) {
				String[] fields = line.split("\t");
				// String node = fields[0];
				// System.out.println("Node: " + node);
				comment_dependencies.add(fields);
				// Fields[0]: node
				// Fields[1]: word
				// Fields[2]: catib_pos
				// Fields[3]: catib_pos_detailed
				// Fields[4]: parent
				// Fields[5]: dependency_relation
				// Fields[6]: madamira_fields
			}
			else {
				dependency_trees.add(comment_dependencies);
				comment_dependencies = new ArrayList<String[]>();
			}	
	}
}
	// Returns true if the node is the first node in the tree
	// node is the index in the array list. 
	// So it is 1 less than the node identifier. 
	public boolean isRoot(int node, List<String[]> tree) {
		String parent = tree.get(node)[4];
		return(parent.equals("0"));
	}
	
	// Return number of hops from root (first node in the tree)
	// 0 if word corresponds to root word
	public Integer numHopsFromRoot(int node, List<String[]> tree) {
		int numhops =0;
		int temp_node = node;
		if (isRoot(node,tree)) {
			return 0;
		}
		else {
			while (!isRoot(temp_node,tree)) {
				Integer parent_node = Integer.parseInt(tree.get(temp_node)[4])-1;
				numhops +=1;
				temp_node = parent_node;
			}
		}
		return numhops;
	}

	// Returns oldest ancestor that is not the root node
	// If root, returns 0
	public Integer AncestorNode(int node, List<String[]> tree) {
		int ancestor_node;
		if (isRoot(node,tree)) {
			ancestor_node = 0;
		} 
		else {
			Integer parent_node = Integer.parseInt(tree.get(node)[4])-1;
			// the ancestor is the node itself
			if (isRoot(parent_node,tree)) {
				ancestor_node = node;
			} else {
				// look for the oldest non-root ancestor
				Integer grandparent_node = Integer.parseInt(tree.get(parent_node)[4])-1;
				while(!isRoot(grandparent_node,tree)) {
					Integer temp_node = parent_node;
					parent_node = grandparent_node;
					grandparent_node = Integer.parseInt(tree.get(temp_node)[4])-1;
				}
				ancestor_node = parent_node;
			}
		}
		return ancestor_node;
	}
	
	// Checks the role of node's oldest ancestor which is not the root
	// node (i.e first node which I am calling root node), returns
	// its relation to the root
	public String roleOfAncestor(int node, List<String[]> tree) {
		String ancestor;
		if (isRoot(node,tree)) {
			ancestor = "---";
		} 
		else {
			Integer parent_node = Integer.parseInt(tree.get(node)[4])-1;
			// the ancestor is the node itself
			if (isRoot(parent_node,tree)) {
				ancestor = tree.get(node)[5];
			} else {
				// look for the oldest non-root ancestor
				Integer grandparent_node = Integer.parseInt(tree.get(parent_node)[4])-1;
				while(!isRoot(grandparent_node,tree)) {
					Integer temp_node = parent_node;
					parent_node = grandparent_node;
					grandparent_node = Integer.parseInt(tree.get(temp_node)[4])-1;
				}
				ancestor = tree.get(parent_node)[5];
			}
		}
		return ancestor;
	}

	// Finds the node's ancestor and returns the child-parent-tree
	public String ChildParentTreeOfAncestor(int node, List<String[]> tree) {
			int ancestor = AncestorNode(node,tree);
			return ChildParentTree(ancestor,tree);
	}

	// Returns the tree consisting of POS_relation_PARENTPOS for a node
	// e.g NOM_obj_VRB
	public String ChildParentTree(int node, List<String[]> tree){
		String parent_pos ="ROOT";
		String child_pos = tree.get(node)[2];
		String relation = tree.get(node)[5].toLowerCase();
		int parent_node = 0;
		if (!isRoot(node, tree)) {
			parent_node = Integer.parseInt(tree.get(node)[4])-1;
			parent_pos = tree.get(parent_node)[2];
		}
		return (child_pos + "_" + relation + "_" + parent_pos);
	}
	
	// Returns the tree consisting of CLUSTER_relation_PARENTCLUSTER for a node
	public String ClusterTree(int node, List<String[]> tree, 
			List<Token> t, HashMap<String,String> word_clusters){
			String relation = tree.get(node)[5].toLowerCase();
			int parent_node = 0;
			if (!isRoot(node, tree)) {
				parent_node = Integer.parseInt(tree.get(node)[4])-1;
			}
			Token this_token = t.get(node); // wrong it's not
			Token parent_token = t.get(parent_node);
			String this_cluster = FeatureExtractor.GetTokenCluster(this_token,word_clusters);
			String parent_cluster = FeatureExtractor.GetTokenCluster(parent_token,word_clusters);
			return (this_cluster + "_" + relation + "_" + parent_cluster);
		}
	
	// Returns the child parent tree with subjectivity
	// e.g NOM(strongsubj)_obj_VRB(weaksubj)
	public String SubjectivityTree(int node, List<String[]> tree, List<Token> t,
			LexiconProcessor lp){
			String parent_pos = "ROOT";
			String child_pos = tree.get(node)[2];
			String relation = "---";
			int parent_node = 0;
			if (!isRoot(node, tree)) {
				relation = tree.get(node)[5].toLowerCase();
				parent_node = Integer.parseInt(tree.get(node)[4])-1;
				parent_pos = tree.get(parent_node)[2];
			}
			Token this_token = t.get(node); // this is wrong as well!! node index doesn't correspond to token index
			Token parent_token = t.get(parent_node);
			String this_token_subj = Sentiment.GetSubjectivityMPQA(this_token, lp);
			String parent_token_subj = Sentiment.GetSubjectivityMPQA(parent_token, lp);
			return (child_pos + "(" + this_token_subj + ")"
					+ "_" + relation + "_" + parent_pos
					+ "(" + parent_token_subj + ")" );
		}
	
	// Returns the child parent tree with subjectivity, 
	// using Stanford dependencies
	public String SubjectivityTreeStanford(DeftToken t) {
		
		return file_path;
		
	}
	
	// Returns the child parent tree with polarity
	public String PolarityTree(int node, List<String[]> tree, List<Token> t,
			LexiconProcessor lp){
			String parent_pos ="ROOT";
			String child_pos = tree.get(node)[2];
			String relation = "---";
			int parent_node = 0;
			if (!isRoot(node, tree)) {
				relation = tree.get(node)[5].toLowerCase();
				parent_node = Integer.parseInt(tree.get(node)[4])-1;
				parent_pos = tree.get(parent_node)[2];
			}
			Token this_token = t.get(node);
			Token parent_token = t.get(parent_node);
			String this_token_pol = Sentiment.GetPolarityMPQA(this_token, lp);
			String parent_token_pol = Sentiment.GetPolarityMPQA(parent_token, lp);
			return (child_pos + "(" + this_token_pol + ")"
					+ "_" + relation + "_" + parent_pos
					+ "(" + parent_token_pol + ")" );
		}
	
	// Trace the path from this node to the root and count how many subjective tokens
	// we get
	// Global feature
	public String MaxSubjectivityInTree(int node, List<String[]> tree, List<Token> t,
			LexiconProcessor lp) {
		String subj = "";
		String temp_subj = "";
		int temp_node = node;
		int num_strongsubj = 0;
		int num_weaksubj = 0;
		int num_nosubj = 0;
		if (isRoot(node,tree)) {
			Token this_token = t.get(node);
			subj = Sentiment.GetSubjectivityMPQA(this_token, lp);
			subj = "GLOBAL_" + subj;
		} 
		else {
			while (!isRoot(temp_node,tree)) {
				Integer parent_node = Integer.parseInt(tree.get(temp_node)[4])-1;
				Token parent_token = t.get(node);
				temp_subj = Sentiment.GetSubjectivityMPQA(parent_token, lp);
				if (temp_subj.equals("strongsubj")) {
					num_strongsubj +=1;
				} else if (temp_subj.equals("weaksubj")) {
					num_weaksubj +=1;
				} else {
					num_nosubj +=1 ;
				}
				temp_node = parent_node;
			}
			// Heuristic. OpinionFinderlike . Or ratio actually seems better.
			// Can also estimate from training data.
			// Based on looking at training data. strongsubj ~= 2, weaksubj ~= q3-6 in clusters
			if (num_strongsubj >= 2) {
				subj = "GLOBAL_strongsubj";
			} else if (num_weaksubj >= 3) {
				subj = "GLOBAL_weaksubj";
			} else {
				subj = "GLOBAL_nosubj";
			}
		 }
			return subj;
		}
	
	public static String MaxSubjectivityInTreeStanford(DeftToken t, List<Token> tokens,
			LexiconProcessor lp) {
		String subj = "";
		String temp_subj = "";
		int index = t.sentence_index;
		//int temp_index = index;
		int num_strongsubj = 0;
		int num_weaksubj = 0;
		int num_nosubj = 0;
		SemanticGraph dependency_graph = t.dependencies;
		IndexedWord vertex = dependency_graph.getNodeByIndex(index);
		IndexedWord root = dependency_graph.getFirstRoot();
		IndexedWord temp_vertex = vertex;
		
		//first check if token is root
		if (vertex.equals(root)) {
			subj = Sentiment.GetEnglishSubjectivityMPQA(t, lp);
			subj = "GLOBAL_" + subj;
			//subj = "GLOBAL_" + "root";
		} else {
			while(!temp_vertex.equals(root)) {
				IndexedWord parent = dependency_graph.getParent(temp_vertex);
				String parent_word = parent.word();
				temp_subj = 
						Sentiment.GetEnglishSubjectivityMPQA(new DeftToken(parent_word,"word"), lp);
				if (temp_subj.equals("strongsubj")) {
					num_strongsubj +=1;
				} else if (temp_subj.equals("weaksubj")) {
					num_weaksubj +=1;
				} else if (temp_subj.equals("na")) {
					num_nosubj +=1 ;
				}
				temp_vertex = parent;
			}
			// Root
			String word = temp_vertex.word();
			temp_subj = 
					Sentiment.GetEnglishSubjectivityMPQA(new DeftToken(word,"word"), lp);
			if (temp_subj.equals("strongsubj")) {
				num_strongsubj +=1;
			} else if (temp_subj.equals("weaksubj")) {
				num_weaksubj +=1;
			} else if (temp_subj.equals("na")) {
				num_nosubj +=1 ;
			}
			// Count
			if (num_strongsubj >= 2) {
				subj = "GLOBAL_strongsubj";
			} else if (num_weaksubj >= 3) {
				subj = "GLOBAL_weaksubj";
			} else {
				subj = "GLOBAL_nosubj";
			}
		}
		return subj;
	}
	
	public String MaxPolarityInTree(int node, List<String[]> tree, List<Token> t,
			LexiconProcessor lp) {
		String pol;
		String temp_pol = "";
		String temp_subj = "";
		int temp_node = node;
		int num_positive = 0;
		int num_negative = 0;
		int num_neutral = 0;
		int num_strongsubj = 0; // Figure out how to include the strongsubj in the polarity counts
		if (isRoot(node,tree)) {
			Token this_token = t.get(node);
			pol = Sentiment.GetPolarityMPQA(this_token, lp);
			if (pol.equals("na")) { 
				pol = "neutral";
			}
			pol = "GLOBAL_" + pol;
		} 
		else {
			while (!isRoot(temp_node,tree)) {
				Integer parent_node = Integer.parseInt(tree.get(temp_node)[4])-1;
				Token parent_token = t.get(node);
				temp_pol = Sentiment.GetPolarityMPQA(parent_token, lp);
				temp_subj = Sentiment.GetSubjectivityMPQA(parent_token, lp);
				if (temp_pol.equals("positive")) {
					num_positive +=1;
				} else if (temp_pol.equals("negative")) {
					num_negative +=1;
				} else if (temp_pol.equals("na") || temp_pol.equals("neutral")) {
					num_neutral +=1;
				}
				temp_node = parent_node;
			}
			// Heuristic. OpinionFinderlike . Or ratio actually seems better.
			// Can also estimate from training data.
			if (num_positive >= 2 && (num_positive > num_negative) ) { // check also that it's strongly subjective?
				pol = "GLOBAL_positive";
			} else if (num_negative >= 2 && (num_negative > num_positive)) {
				pol = "GLOBAL_negative";
			} else {
				pol = "GLOBAL_neutral";
			}
		}
			return pol;
		}
	
	// modify for negations
	public static String MaxPolarityInTreeStanford(DeftToken t, List<Token> tokens,
			LexiconProcessor lp) {
		String pol = "";
		String temp_pol = "";
		int index = t.sentence_index;
		//int temp_index = index;
		int num_positive = 0;
		int num_negative = 0;
		int num_neutral = 0;
		SemanticGraph dependency_graph = t.dependencies;
		IndexedWord vertex = dependency_graph.getNodeByIndex(index);
		IndexedWord root = dependency_graph.getFirstRoot();
		IndexedWord temp_vertex = vertex;
		
		//first check if token is root
		if (vertex.equals(root)) {
			pol = Sentiment.GetEnglishPolarityMPQA(t, lp);
			pol = "GLOBAL_" + pol;
			//pol = "GLOBAL_" + "root";
			// or just return? don't include it?
		} else {
			while(!temp_vertex.equals(root)) {
				IndexedWord parent = dependency_graph.getParent(temp_vertex);
				String parent_word = parent.word();
				temp_pol = 
						Sentiment.GetEnglishPolarityMPQA(new DeftToken(parent_word,"word"), lp);
				// if parent of parent (or previous token) is a negation, switch the polarity
				if (temp_pol.equals("positive")) {
					num_positive +=1;
				} else if (temp_pol.equals("negative")) {
					num_negative +=1;
				} else if (temp_pol.equals("na") || temp_pol.equals("neutral")) {
					num_neutral +=1 ;
				}
				temp_vertex = parent;
			}
			// Root
			String word = temp_vertex.word();
			temp_pol = 
					Sentiment.GetEnglishPolarityMPQA(new DeftToken(word,"word"), lp);
			if (temp_pol.equals("positive")) {
				num_positive +=1;
			} else if (temp_pol.equals("negative")) {
				num_negative +=1;
			} else if (temp_pol.equals("na") || temp_pol.equals("neutral")) {
				num_neutral +=1 ;
			}
			// Count
			if (num_positive >= 2 && (num_positive > num_negative) ) { // check also that it's strongly subjective?
				pol = "GLOBAL_positive";
			} else if (num_negative >= 2 && (num_negative > num_positive)) {
				pol = "GLOBAL_negative";
			} else {
				pol = "GLOBAL_neutral";
			}
			
		}
		return pol;
	}
	
	/*public void ReadDependencyTrees() {
		System.out.println("Reading Catib dependency parse trees");
		List<String> trees = util.FileReader.ReadFile(file_path, "bw", false);
		HashMap<String, String[]> comment_dependencies = new HashMap<String, String[]>();
		for (String line: trees) {
			line = line.trim();
			if (!line.equals("")) {
				String[] fields = line.split("\t");
				String node = fields[0];
				System.out.println("Node: " + node);
				comment_dependencies.put(node, fields);
				// Fields[0]: node (not needed)
				// Fields[1]: word
				// Fields[2]: catib_pos
				// Fields[3]: catib_pos_detailed
				// Fields[4]: parent
				// Fields[5]: dependency_relation
				// Fields[6]: madamira_fields
			}
			else {
				dependency_trees.add(comment_dependencies);
				comment_dependencies = new HashMap<String, String[]>();
			}
				
		}
	}*/
	
	
	

}
