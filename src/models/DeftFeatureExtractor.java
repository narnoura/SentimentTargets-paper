package models; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aliasi.cluster.LatentDirichletAllocation;

import data.Comment;
import data.DeftToken;
import data.Target;
import data.Token;
import edu.stanford.nlp.ling.IndexedWord;
//import edu.stanford.nlp.pipeline.CoreNLPProtos.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import models.baselines.ALL_NP;
import processor.LexiconProcessor;
import processor.Sentiment;
import processor.ToolsProcessor.DependencyProcessor;
import topicmodeling.LdaComments;
import topicmodeling.TopicSalience;

/**
 * @author noura
 *
 */
public class DeftFeatureExtractor extends FeatureExtractor {

	/**
	 * Inherits Feature extractor. Used for DEFT English
	 */
	public DeftFeatureExtractor () {
		super();
	}
	public DeftFeatureExtractor (List<Comment> input_comments) {
		super(input_comments);
	}
	public void SetInput(List<Comment> comments) {
		this.input_comments = comments;
	}
	public void SetNontokenized (List<Comment> comments) {
		this.nontok_comments = comments;
	}
	public void SetLabelType(String label_type) {
		this.label_type = label_type;
	}
	

	List<List<String>> DeftData (Comment c) {
		List<List<String>> data = new ArrayList<List<String>>();
		List<List<String>> basic_features = DeftExtractBasicFeatures(c);
		List<List<String>> bin_features = DeftExtractBinaryFeatures(c);
		List<String> labels = new ArrayList<String>();
		if (include_labels && label_type.equals("target")) {
			labels = ExtractTokenTargetLabels(c);
		}  else if (include_labels && (label_type.equals("target+sentiment") 
				|| label_type.equals("sentiment"))) {
			labels = ExtractTokenSentimentLabels(c);
		}
		List<String> target_labels_as_features = new ArrayList<String>();
		if (label_type.equals("sentiment")) {
			// test to see if this is why we get wrong labels on targets
			target_labels_as_features = ExtractTokenTargetLabels(c);
		}
		for (int t=0; t< c.tokens_.size(); t++) {
			List<String> features = new ArrayList<String>();
			List<String> basic_token_features = basic_features.get(t);
			List<String> bin_token_features = bin_features.get(t);
			features.addAll(basic_token_features);
			features.addAll(bin_token_features);
			if (label_type.equals("sentiment")) {
				features.add(target_labels_as_features.get(t));
			}
			if (include_labels) {
				features.add(labels.get(t));
			}
			data.add(features);
		}
		return data;
	}
	
		// Extracted by default
	 	// Extracts only word, POS, and parse tree if available
		// Can also add stanford POS for ATB tokens
		List<List<String>> DeftExtractBasicFeatures (Comment c) {
			List<List<String>> basic_features = new ArrayList<List<String>>();
			List<String> token_features;
			for (Token t: c.tokens_) {
				token_features = new ArrayList<String>();
				basic_features.add(token_features);
				
			}
			if (this.binary_feature_types.contains("Word")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					String word = t.text_.toLowerCase();
					basic_features.get(i).add(word);
					//DeftToken dtok = (DeftToken) t;
					//dtok.token_binary_features.put("word", t.text_);
					}
			} else if (this.binary_feature_types.contains("Lex")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					String word = t.text_;
					if (t.morph_features.containsKey("lex")) {
						basic_features.get(i).add(t.morph_features.get("lex"));
					} else {
						basic_features.get(i).add(word);
					}
				} 
			}
			else { //default
				for (int i=0;i<c.tokens_.size();i++) {
					Token t = c.tokens_.get(i);
					basic_features.get(i).add(t.text_);
				}
			}
			// next, part of speech (Stanford POS)
			// use runner
			for (int i=0;i<c.tokens_.size();i++) {
				Token t = c.tokens_.get(i);
				String pos = t.pos_;
				basic_features.get(i).add(pos);
			}
				/*if (t.parse_tree != null) {
					token_features.add(t.parse_tree);
				}*/
			
			return basic_features;
		}
	
	List<List<String>> DeftExtractBinaryFeatures (Comment c) {
		List<List<String>> bin_features = new ArrayList<List<String>>();
		Set<String> top_wordprobs = new HashSet<String>();
		Set<String> top_tfidfs = new HashSet<String>();
		Set<String> top_llr = new HashSet<String>();
		List<String> token_features;
		for (Token t: c.tokens_) {
			token_features = new ArrayList<String>();
			bin_features.add(token_features);
			DeftToken dtok = (DeftToken) t;
		}
		if (this.binary_feature_types.contains("Salience")) {
			int k = 10;
			top_wordprobs = TopicSalience.TopProbKeyWords(c, k);
			top_tfidfs = TopicSalience.TopTFIDFKeyWords(c, k);
			top_llr = TopicSalience.TopKSignatures(c, k);
		}
		for (String feature: this.binary_feature_types ) {
			//Ignore default features
			if (feature.equals("NER")) {
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					bin_features.get(i).add(t.NER);
				}
			}
			// Dependency
			else if (feature.equals("Dependency")) {
				for (int i =0; i<c.tokens_.size(); i++) {
					DeftToken t = (DeftToken) c.tokens_.get(i);
					SemanticGraph dependency_graph = t.dependencies;
					int index = t.sentence_index;
					String relation = "";
					String parent_word = "";
					String subjectivity = "";
					String polarity = "";
					String child_parent_tree = "";
					String subjectivity_tree = "";
					String pos = t.pos_;
					String parent_pos = "___";
					String this_token_subj = Sentiment.GetEnglishSubjectivityMPQA(t,lp);
					IndexedWord vertex = dependency_graph.getNodeByIndex(index);
					IndexedWord parent = dependency_graph.getParent(vertex);
					// child sentiment if one of the children has sentiment (Can put the tree)
					IndexedWord root = dependency_graph.getFirstRoot();
					
					//first check if parent is root
					if (vertex.equals(root)) {
						parent_word = "ROOT";
						//? relation = "_";
						subjectivity = "na";
						polarity = "na";
					}
					else {
					parent_pos = parent.tag();
					parent_word = parent.word();
					SemanticGraphEdge edge = dependency_graph.getEdge(parent,vertex);
					relation = edge.getRelation().getShortName(); // gives exception
					subjectivity =
							Sentiment.GetEnglishSubjectivityMPQA(new Token(parent_word,"word"), lp);
					polarity = Sentiment.GetEnglishPolarityMPQA(new Token(parent_word,"word"), lp); 
					}
					child_parent_tree = pos + "_" + relation + "_" + parent_pos;
					//subjectivity_tree = pos + "(" + this_token_subj + ")"
					//+ "_" + relation + "_" + parent_pos
					//+ "(" + subjectivity + ")";
					/*System.out.println("Token:"+t.text_);
					System.out.println("Sentence index:" + index);
					System.out.println("Root:"+root.word());
					System.out.println("Parent:"+ parent_word);
					System.out.println("Relation:"+ relation);*/
					bin_features.get(i).add("REL_"+relation);
					bin_features.get(i).add("TREE_" + child_parent_tree);
					bin_features.get(i).add("PARENT_SUBJ_"+subjectivity);
					
					if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
					   bin_features.get(i).add("PARENT_POL_"+polarity);
					} 
					//bin_features.get(i).add("SUBJ_TREE_"+subjectivity_tree);
					
					// Global sentiment dependency features
					/*String global_subj = DependencyProcessor.MaxSubjectivityInTreeStanford(t, c.tokens_, lp);
					bin_features.get(i).add(global_subj);
					if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						   String global_pol = DependencyProcessor.MaxPolarityInTreeStanford(t, c.tokens_, lp);
						   bin_features.get(i).add(global_pol);
					}*/
					
					// Child sentiment features
					// not useful
					/*Set<IndexedWord> this_children = dependency_graph.getChildren(vertex);
					String child_relation = "_";
					boolean found_child_sentiment = false;
					int num_pos_child = 0;
					int num_neg_child = 0;
					int num_strongsubj_child = 0;
					int num_weaksubj_child = 0;
					String child_word = "";
					String child_subj = "";
					String child_pol = "";
					if (!this_children.isEmpty()) {
					for (IndexedWord child: this_children) {
						SemanticGraphEdge edge = dependency_graph.getEdge(vertex, child);
						child_word = child.word();
						child_subj =
								Sentiment.GetEnglishSubjectivityMPQA(new Token(child_word,"word"),lp);
						child_pol = 
								Sentiment.GetEnglishPolarityMPQA(new Token(child_word,"word"),lp);
						if (child_subj.equals("strongsubj")) {
							num_strongsubj_child +=1;
						} else if (child_subj.equals("weaksubj")) {
							num_weaksubj_child +=1;
						} 
						if (child_pol.equals("positive")) {
							num_pos_child +=1;
						} else if (child_pol.equals("negative")) {
							num_neg_child +=1;
						}
					}
				 } 
					
					// Add child subjectivity for both models
					if (num_strongsubj_child >= 1) {
						bin_features.get(i).add("CHILD_SUBJ_strongsubj");
					} else if (num_weaksubj_child >= 1) {
						bin_features.get(i).add("CHILD_SUBJ_weaksubj");
					} else {
						bin_features.get(i).add("CHILD_SUBJ_na");
					}
					
					// Add child polarity for sentiment model
					if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						if (num_pos_child >= 1 && num_pos_child > num_neg_child) {
							bin_features.get(i).add("CHILD_POL_positive");
						} else if (num_neg_child >= 1 && num_neg_child > num_pos_child) {
							bin_features.get(i).add("CHILD_POL_negative");
						} else {
							bin_features.get(i).add("CHILD_POL_na");
						}  
					}
					// Dependency topic features
					// not useful
					/*String parent_lemma = "ROOT";
					if (!vertex.equals(root)) {
					parent_lemma = parent.word();
					} 
					if (top_tfidfs.contains(parent_lemma)) {
						bin_features.get(i).add("PARENT_TOPIC_" + relation);
		 			} else {
						bin_features.get(i).add("PARENT_TOPIC_na");
					}  */
					
					// Child topic features. Not useful
					/*String child_relation = "_";
					boolean found_child_topic = false;
					if (!this_children.isEmpty()) {
					for (IndexedWord child: this_children) {
						SemanticGraphEdge edge = dependency_graph.getEdge(vertex, child);
						String child_lemma = child.lemma();
						if (top_tfidfs.contains(child_lemma)) {
							child_relation = edge.getRelation().getShortName(); 
							found_child_topic = true;
							break;
						}
						}
					}
					if (found_child_topic) {
						bin_features.get(i).add("CHILD_TOPIC_" + child_relation);
					} else {
						bin_features.get(i).add("CHILD_TOPIC_na");
					}*/
					
					// can also add parent POS and/or parent word and the other dep features
					
					// TODO
					// can check sentiment of parent
					// can also get children of vertex not just parent
					// check how to traverse tree and get typed/collapsed dependencies
			
					// dependency_graph.getChildren(index);
					// IndexedWord this_word = dependency_graph.getNodeByIndex(index);
				
				}
			}
			else if (feature.equals("Sentiment")) {
					//&& (label_type.equals("sentiment") || label_type.equals("target+sentiment"))
					//) {
				String sentfeat;
				String polfeat;
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					sentfeat = "TOK_SUBJ_";
					polfeat = "TOK_POL_";
					sentfeat += Sentiment.GetEnglishSubjectivityMPQA(t, lp);
					polfeat += Sentiment.GetEnglishPolarityMPQA(t, lp); 
					bin_features.get(i).add(sentfeat);
					bin_features.get(i).add(polfeat);
				}
			}
			else if (feature.equals("Salience")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t=c.tokens_.get(i);
					String lemma = TopicSalience.WordRepresentation(t);
					// Word Probability
					if (top_wordprobs.contains(lemma)) {
						bin_features.get(i).add("WORDPB_high");
					} else {  
						bin_features.get(i).add("WORDPB_low");
					}
					if (top_tfidfs.contains(lemma)) {
						bin_features.get(i).add("TFIDF_high");
					}
					else {
						bin_features.get(i).add("TFIDF_low");
					}
					if (top_llr.contains(lemma)) {
						bin_features.get(i).add("LLR_high");
					} else {
						bin_features.get(i).add("LLR_low");
					}
			}
		}
		else if (feature.equals("Coreference")) {
			/*for (int i=0; i<c.tokens_.size(); i++) {
				Token t=c.tokens_.get(i);
				if (t.corefers_with_target) {
					bin_features.get(i).add("COREFERS_TARG_1");
					//bin_features.get(i).add("HAS_PRON_MENT_" + syntactic_position);
				} 
				else {
					bin_features.get(i).add("COREFERS_TARG_0");
			   }
			} */
		}
		else if (feature.equals("Entity")) {
			for (int i=0; i<c.tokens_.size(); i++) {
				DeftToken t= (DeftToken)c.tokens_.get(i);
				if (t.entity) {
					// try B and I as well
					bin_features.get(i).add("E");
					bin_features.get(i).add(t.entity_type);
					//bin_features.get(i).add(t.entity_text);
				} else {
					bin_features.get(i).add("E-O");
					bin_features.get(i).add("none-entity");
					//bin_features.get(i).add("none");
				}
				//Integer ioffset = t.char_offset;
				//bin_features.get(i).add(ioffset.toString());
				
			}
		}
		// Word clusters
		else if (feature.equals("WordClusters")) {
			for (int i=0; i<c.tokens_.size(); i++) {
				DeftToken t= (DeftToken)c.tokens_.get(i);
				// First check text
				if (this.word_clusters.containsKey(t.text_)) {
					bin_features.get(i).add("CLUSTER_"+this.word_clusters.get(t.text_));
				}
				// Next check lemma
				else if (t.morph_features.containsKey("lex")
						&& this.word_clusters.containsKey(t.morph_features.get("lex"))) {
					bin_features.get(i).add("CLUSTER_"+this.word_clusters.get(t.morph_features.get("lex")));	
				}
				else {
					bin_features.get(i).add("CLUSTER_OOV");
					System.out.println("OOV cluster for token:" + t.text_);
				}
			}
		}
		}
	
		return bin_features;
	}
	
	// Extracts token labels for target tagging (T, O)
	List<String> ExtractTokenTargetLabelsDeft (Comment c) {
			System.out.println("Extracting token target labels");
			List<String> labels = new ArrayList<String>();
			for (Token t: c.tokens_) {
				DeftToken dt = (DeftToken) t;
				if (dt.target_offset_ == -1) {
					System.out.println("Token:"+dt.text_);
					System.out.println("Offset is -1");
					labels.add("O");
				} 
				else if (dt.target_offset_ >= 0) {
					System.out.println("Offset:" + dt.target_offset_ );
					/*if (!pipeline_model && exclude_undetermined && t.sentiment_.equals("undetermined")) {
						labels.add("O");
					} */
					// IF pipeline model, sentiment will be null
					if (!pipeline_model && exclude_undetermined && t.sentiment_.equals("undetermined")) {
						System.out.println("Token:"+dt.text_);
						System.out.println("Offset:"+dt.target_offset_);
						System.out.println("Offset is >=0, sentiment is undetermined");
					labels.add("O");
					} 
					// can also add O if sentiment+target model
					// and we are in training for sentiment (so we exclude undetermined)
					// so this can be true even if include undetermined
					else if (!pipeline_model && 
							(label_type.equals("target+sentiment") || label_type.equals("sentiment"))
							&& t.sentiment_.equals("undetermined")) {
						labels.add("O");
						System.out.println("Token:"+dt.text_);
						System.out.println("Offset:"+dt.target_offset_);
						System.out.println("Offset is >=0, sentiment is undetermined, target+sentiment model");
					}
					else {
						labels.add("T");
						System.out.println("Token:"+dt.text_);
						System.out.println("Offset:"+dt.target_offset_);
						System.out.println("Offset is >=0, target");
					}
				}
				else {
					System.out.println("Feature Extractor: No valid target offset for"
							+ "token " + dt.text_ + "for Comment" + c.comment_id_);
					System.exit(0);
				}
			}
			return labels;
		}
	
	// ExtractEntityLabels (?)
	
		
	/*List<List<String>> DeftExtractBinaryFeatureshash (Comment c) {
		List<List<String>> bin_features = new ArrayList<List<String>>();
		Set<String> top_wordprobs = new HashSet<String>();
		Set<String> top_tfidfs = new HashSet<String>();
		Set<String> top_llr = new HashSet<String>();
		List<String> token_features;
		if (this.binary_feature_types.contains("Salience")) {
			int k = 10;
			top_wordprobs = TopicSalience.TopProbKeyWords(c, k);
			top_tfidfs = TopicSalience.TopTFIDFKeyWords(c, k);
			top_llr = TopicSalience.TopKSignatures(c, k);
		}
		for (Token t: c.tokens_) {
			token_features = new ArrayList<String>();
			bin_features.add(token_features);
			DeftToken dtok = (DeftToken) t;
			
			if (this.binary_feature_types.contains("sentiment")) {
				// dtok.binary_features.put(sentiment,);
			}
			// if (... for each feature)
		}
		
		/*for (String feature: this.binary_feature_types ) {
			//Ignore default features
			if (feature.equals("pos") || feature.equals("Word")) {
				continue; 
		     }
		}*/
	
		/* Data:
		 * for each comment
		 *    for each token
		 * 	     for each binary feature
		 *      	add feature for that token string features
		 *    add token string features to comment
		 */

		/*return bin_features;
	}*/
	
	
	   
	
}
