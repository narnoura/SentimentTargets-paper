/**
 * 
 */
package models;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.soap.Text;

//import com.aliasi.cluster.LatentDirichletAllocation;

import data.Comment;
import data.DeftToken;
import data.Token;
import processor.LexiconProcessor;
import processor.Sentiment;
import processor.ToolsProcessor.DependencyProcessor;
//import processor.LoadPickle;
//import topicmodeling.LdaComments;
import topicmodeling.TopicSalience;
import topicmodeling.CoreferenceFeatures;

/**
 * @author Narnoura
 * Extract features from comments.
 *
 */
public class FeatureExtractor {
	
	// Can be mada comments, or any comments
	public List<Comment> input_comments;
	// Used if input comments are tokenized comments
	public List<Comment> nontok_comments;
	public LexiconProcessor lp;
	public DependencyProcessor dp;
	//public LdaComments topic_model;
	public List<String> binary_feature_types;
	public List<String> continuous_feature_types;
	public List<String> morphological_features = Arrays.asList("gloss","prc3", "prc2", "prc1",
			"prc0", "per", "asp", "vox", "mod", "gen", "num", "stt", "cas", "enc0", "rat", "source", 
			"stemcat");
	public boolean pipeline_model;
	public String label_type;
	public static boolean include_labels = true;
	public static boolean exclude_undetermined = false;
	public static final HashMap<String,Boolean> dependency_features;
	static {
		dependency_features = new HashMap<String,Boolean>();
		dependency_features.put("relation", true);
		dependency_features.put("rootdist", false);
		dependency_features.put("childptree", true);
		dependency_features.put("senttree", true);
		dependency_features.put("ancestortree", false);
		dependency_features.put("ancestornode", false);
		dependency_features.put("ancestorrel", false);
		dependency_features.put("ancestorsenttree", false);
		dependency_features.put("maxtreesent", false);
	}
	public HashMap<String,Integer> corpus_counts;
	public HashMap<String,String> word_clusters;
	
	public FeatureExtractor() {
		binary_feature_types = new ArrayList<String>();
		continuous_feature_types = new ArrayList<String>();
	}
	
	public FeatureExtractor(List<Comment> input_comments) {
		this.input_comments = input_comments;
		this.nontok_comments = input_comments;
		this.binary_feature_types = new ArrayList<String>();
		this.continuous_feature_types = new ArrayList<String>();
		this.label_type = "target";
		this.pipeline_model = false;
	}
	
	// Sets label type to 'target' or 'target+sentiment' for the pos-neg-neut model
	public void SetLabelType(String label_type) {
		this.label_type = label_type;
	}
	
	public void SetIncludeLabels(boolean include_labels){
		this.include_labels = include_labels;
	}
	
	public void SetPipelineModel(boolean pipeline_model){
		this.pipeline_model = pipeline_model;
	}
	
	public void SetBinaryFeatures (List<String> binary_features) {
		this.binary_feature_types = binary_features;
	}
	
	public void SetContinuousFeatures (List<String> continuous_features) {
		this.continuous_feature_types = continuous_features;
	}

	public void SetLexiconProcessor (LexiconProcessor lp) {
		this.lp = lp;
	}
	
	public void SetDependencyProcessor (DependencyProcessor dp) {
		if (this.binary_feature_types.contains("Dependency")
				|| this.binary_feature_types.contains("Coreference")
				|| this.binary_feature_types.contains("WordClusters")
				|| this.binary_feature_types.contains("WordClustersEnglish")) {
			if (input_comments.isEmpty()) {
				System.out.println("Warning: Input comments empty. Cannot use dependency features.");
				return;
			}
			System.out.println("Reading dependency trees\n");
			this.dp = dp;
			dp.ReadDependencyTrees();
			if (dp.dependency_trees.size()!=input_comments.size()) {
				System.out.println("Error: Size of dependency trees"
						+ " is not equal to size of comments. Exiting \n");
				System.exit(0);
			}
			int i = 0;
			/*for (HashMap<String,String[]> dt : dp.dependency_trees) {
				Comment c = input_comments.get(i);
				c.SetDependencyTree(dt);
				i+=1;
			}*/
			for (List<String[]> dt : dp.dependency_trees) {
			Comment c = input_comments.get(i);
			c.SetDependencyTree(dt);
			i+=1;
			}
		}
	}
	
	/*public void SetTopicModel (LdaComments lc, List<Comment> comments_for_lda) {
		if (this.binary_feature_types.contains("Topic")) {
			topic_model = lc;
			List<Comment> comments = new ArrayList<Comment>();
			if (comments_for_lda == null || comments_for_lda.isEmpty()) {
				System.out.println("No input comments specified for LDA. Running with input data\n");
				if (input_comments.isEmpty()) {
					System.out.println("Warning: Input comments empty. Cannot run topic modeling. \n");
					return;
				}
				comments = input_comments;
			} else {
				comments = comments_for_lda;
			}
		   System.out.println("Running LDA on corpus\n");
				try {
					LatentDirichletAllocation.GibbsSample final_sample = topic_model.runLDA(comments);
					topic_model.SetSample(final_sample);
					HashMap<Integer, HashMap<String, Double>> topics = topic_model.CorpusTopics(final_sample);
					HashMap<Integer, HashMap<Integer, Double>> documents = topic_model.CorpusDocuments(final_sample);
					topic_model.SetTopics(topics);
					topic_model.SetDocuments(documents);
					topic_model.PrintToFiles();
				} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}*/
	
	public void SetWordClusters(String file) {
		if (this.binary_feature_types.contains("WordClusters")
				|| this.binary_feature_types.contains("WordClustersEnglish")) {
			word_clusters = getSimpleWordToCluster(file);
		} 
	}
	
	public void SetInput(List<Comment> comments) {
		input_comments = comments;
	}
	public void SetNontokenized (List<Comment> comments) {
		nontok_comments = comments;
	}
	
	List<List<String>> Data (Comment c) {
		List<List<String>> data = new ArrayList<List<String>>();
		List<List<String>> basic_features = ExtractBasicFeatures(c);
		List<List<String>> bin_features = ExtractBinaryFeatures(c);
		List<String> labels = new ArrayList<String>();
		if (include_labels && label_type.equals("target")) {
			labels = ExtractTokenTargetLabels(c);
			assert(c.tokens_.size() == labels.size());
		}  else if (include_labels && (label_type.equals("target+sentiment") 
				|| label_type.equals("sentiment"))) {
			labels = ExtractTokenSentimentLabels(c);
		}
		List<String> target_labels_as_features = new ArrayList<String>();
		if (label_type.equals("sentiment")) {
			target_labels_as_features = ExtractTokenTargetLabels(c);
		}
		
		assert(c.tokens_.size() == basic_features.size());
		assert(c.tokens_.size() == bin_features.size());
		
		//System.out.println("Updating token vector");
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
	List<List<String>> ExtractBasicFeatures (Comment c) {
		List<List<String>> basic_features = new ArrayList<List<String>>();
		List<String> token_features;
		for (Token t: c.tokens_) {
			token_features = new ArrayList<String>();
			basic_features.add(token_features);
		}
		if (this.binary_feature_types.contains("word")) {
			for (int i=0; i<c.tokens_.size(); i++) {
				Token t = c.tokens_.get(i);
				basic_features.get(i).add(t.text_); }
		} else if (this.binary_feature_types.contains("Lex")) {
			for (int i=0; i<c.tokens_.size(); i++) {
				Token t = c.tokens_.get(i);
				String word = t.text_;
				if (t.morph_features.containsKey("lex") && (t.clitic.equals("none") ||
						t.clitic.equals("word"))) {
					basic_features.get(i).add(t.morph_features.get("lex"));
				} else {
					basic_features.get(i).add(word);
				}
			}
		} else if (this.binary_feature_types.contains("Stem") ) {
			for (int i=0; i<c.tokens_.size(); i++) {
				Token t = c.tokens_.get(i);
				if (t.morph_features.containsKey("stem") && (t.clitic.equals("none") ||
						t.clitic.equals("word"))) {
					basic_features.get(i).add(t.morph_features.get("stem"));
				} else {
					basic_features.get(i).add(t.text_);
				}
			}
		}
		else { //default
			for (int i=0;i<c.tokens_.size();i++) {
				Token t = c.tokens_.get(i);
				basic_features.get(i).add(t.text_);
			}
		}
		// next, part of speech (madamira pos)
		for (int i=0;i<c.tokens_.size();i++) {
			Token t = c.tokens_.get(i);
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				basic_features.get(i).add("NO_ANALYSIS"); }
			else{
				String pos = t.pos_;
				basic_features.get(i).add(pos);
			}
			/*if (t.parse_tree != null) {
				token_features.add(t.parse_tree);
			}*/
		}
		return basic_features;
	}
		
	// Includes lexical, dict, morphological, semantic or any binary features
	List<List<String>> ExtractBinaryFeatures (Comment c) {
		List<List<String>> bin_features = new ArrayList<List<String>>();
		Set<String> top_wordprobs = new HashSet<String>();
		Set<String> top_tfidfs = new HashSet<String>();
		Set<String> top_llr = new HashSet<String>();
		if (this.binary_feature_types.contains("Salience")) {
			int k = 15;
			top_wordprobs = TopicSalience.TopProbKeyWords(c, k);
			top_tfidfs = TopicSalience.TopTFIDFKeyWords(c, k);
			top_llr = TopicSalience.TopKSignatures(c, k);
			//top_llr = TopicSalience.TopSignatures(c, 0.001);
			// can have a TopSignaturesByTopK option
		}
		for (Token t: c.tokens_) {
			List<String> token_features = new ArrayList<String>();
			bin_features.add(token_features);
		}
		for (String feature: this.binary_feature_types ) {
			//Ignore default features
			if (feature.equals("MadamiraPos") || feature.equals("Word") 
					|| feature.equals("pos")) {
				continue; 
		     }
			else if (feature.equals("NER")) {
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					bin_features.get(i).add(t.NER);
				}
			}
			else if (feature.equals("BPC")) {
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					bin_features.get(i).add(t.BPC);
				}
			}      
			else if (feature.equals("Dependency")) {
				HashMap<String,Integer> target_feature_statistics = new HashMap<String,Integer>();
				HashMap<String,Integer> non_target_feature_statistics = new HashMap<String,Integer>();
				List<String[]> tree = c.dependency_tree;
				List<Token> no_Al = new ArrayList<Token>();
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						no_Al.add(t);
					}
				}
				int node = 0;
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						// CONTEXT FEATURES
						String relation = tree.get(node)[5];
						bin_features.get(i).add(relation);
						// CHILD PARENT TREE
						String child_parent_tree = dp.ChildParentTree(node, tree);
						bin_features.get(i).add(child_parent_tree);
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add(relation);
							bin_features.get(i-1).add(child_parent_tree);
						}
						// Update target feature statistics for Relation
						UpdateTargetStatistics(t,relation, target_feature_statistics);
						UpdateNonTargetStatistics(t, relation,non_target_feature_statistics);
						UpdateTargetStatistics(t,child_parent_tree, target_feature_statistics);
						UpdateNonTargetStatistics(t,child_parent_tree,non_target_feature_statistics);
						
						// ROOT FEATURES
						String distance_from_root = dp.numHopsFromRoot(node, tree).toString();
						if (dependency_features.get("rootdist")){
							bin_features.get(i).add(distance_from_root);
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add(distance_from_root);
							}
						}
						// GLOBAL FEATURES
						String ancestor_relation = dp.roleOfAncestor(node, tree);
						String ancestor_child_parent_tree = dp.ChildParentTreeOfAncestor(node, tree);
						Integer ancestor_node = dp.AncestorNode(node, tree);
						if (dependency_features.get("ancestorrel")){
							bin_features.get(i).add("ANC_" + ancestor_relation);
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add("ANC_" + ancestor_relation);
							}
						}
						if (dependency_features.get("ancestornode")){
							bin_features.get(i).add("ANC_" + ancestor_node.toString());
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add("ANC_" + ancestor_node.toString());
							}
						}
						if (dependency_features.get("ancestortree")) {
							bin_features.get(i).add("ANC_" + ancestor_child_parent_tree);
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add("ANC_" + ancestor_child_parent_tree);
							}
						}
						// SENTIMENT FEATURES
						String parent = tree.get(node)[4];
						String parent_sent="";
						String sentiment_tree="";
						String ancestor_sentiment_tree="";
						String max_sent="";
						String max_subj="";
						if (!dp.isRoot(node, tree)) {
							Integer parent_node = Integer.parseInt(parent)-1;
							Token parent_token = no_Al.get(parent_node);
							//Token parent_token = c.tokens_.get(parent_node);
							//String sent = Sentiment.GetBinarySentimentSifaat(parent_token, lp).toString();
							String sent = Sentiment.GetSubjectivityMPQA(parent_token, lp); // best
							//String sent = Sentiment.GetSubjectivitySifaatMPQA(parent_token, lp);
							if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
							    sent = Sentiment.GetPolarityMPQA(parent_token, lp); // best
							} 
							parent_sent = "PARENT_SENT_" + sent;
						}  else {
							parent_sent = "PARENT_SENT_na";
						}
						bin_features.get(i).add(parent_sent);
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add(parent_sent);
						}
						// sentiment trees and max sent
						if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						    sentiment_tree = dp.PolarityTree(node, tree, no_Al, lp);
							ancestor_sentiment_tree = 
									dp.PolarityTree(ancestor_node, tree, no_Al, lp);
							max_sent = dp.MaxPolarityInTree(node, tree, no_Al, lp);
							max_subj = dp.MaxSubjectivityInTree(node, tree, no_Al, lp);
						} else {
							sentiment_tree = dp.SubjectivityTree(node, tree, no_Al, lp);
							ancestor_sentiment_tree = dp.SubjectivityTree(ancestor_node, tree, no_Al, lp);
							max_sent = dp.MaxSubjectivityInTree(node, tree, no_Al, lp);
						}
						if (dependency_features.get("senttree")) {
							bin_features.get(i).add(sentiment_tree);
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add(sentiment_tree);
							}
						}
						if (dependency_features.get("ancestorsenttree")){
							bin_features.get(i).add("ANC_" + ancestor_sentiment_tree);
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add("ANC_" + ancestor_sentiment_tree);
							}
						}
						if (dependency_features.get("maxtreesent")) {
							bin_features.get(i).add(max_sent);
							if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
								bin_features.get(i).add(max_subj);
							}
							if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
								bin_features.get(i-1).add(max_sent);
								if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
									bin_features.get(i-1).add(max_subj);
								}
							}
						}
						node +=1;
					}	
				}
			}
			else if (feature.equals("Sentiment") 
					//&& (label_type.equals("sentiment") || label_type.equals("target+sentiment"))
					) {
				String sentfeat;
				String polfeat;
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					sentfeat = "TOK_SUBJ_";
					polfeat = "TOK_POL_";
					sentfeat += Sentiment.GetSubjectivityMPQA(t, lp); // best
					polfeat += Sentiment.GetPolarityMPQA(t, lp); // best
					
					bin_features.get(i).add(sentfeat);
					
					if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						bin_features.get(i).add(polfeat);
					}
				}
			}
			else if (feature.equals("Salience") 
				//	&& 
					//(label_type.equals("target") || label_type.equals("target+sentiment"))
					) {

				for (int i=0; i<c.tokens_.size(); i++) {
					Token t=c.tokens_.get(i);
					String lemma = WordRepresentationIncludeClitics(t);
					//String lemma = TopicSalience.WordRepresentation(t);
					// Word Probability
					/*if (top_wordprobs.contains(lemma)) {
						bin_features.get(i).add("WORDPB_high");
					} else {
						bin_features.get(i).add("WORDPB_low");
					}*/
					if (top_tfidfs.contains(lemma)) {
						bin_features.get(i).add("TFIDF_high");
					}
					else {
						bin_features.get(i).add("TFIDF_low");
					}
					/*if (top_llr.contains(lemma)) {
						bin_features.get(i).add("LLR_high");
					} else {
						bin_features.get(i).add("LLR_low");
					}*/
				}
			}
			else if (feature.equals("Coreference")) {
			
				// Get the pronominal mention features
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
	
					if (t.has_subsequent_pronominal_mention) {
						bin_features.get(i).add("HAS_PRON_MENT_1");
						/*
						 * Get the syntactic role in which the pronoun appears */
						Token pronoun_mention = t.pronominal_mention;
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add("HAS_PRON_MENT_1");   
						}	
						
					} else {
						bin_features.get(i).add("HAS_PRON_MENT_0");
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add("HAS_PRON_MENT_0");
						}
					}
					
				} // end loop
			}
			/*else if (feature.equals("Topic") 
					// && (label_type.equals("target") || label_type.equals("target+sentiment"))) 
					)
					{
				// Find if token is a topic for this comment
				Integer doc_number = input_comments.indexOf(c);
				HashMap<Integer,Double> doc_topics = topic_model.documents.get(doc_number);
				for (int i=0; i< c.tokens_.size(); i++) {
					boolean topic_word = false;
					Token t = c.tokens_.get(i);
					String text = WordRepresentationIncludeClitics(t);
					// including clitics to increase recall
					/*if (t.morph_features.containsKey("lex") && (t.clitic.equals("none") ||
							t.clitic.equals("word"))) {
						text = t.morph_features.get("lex");
					} else {
						text = t.text_;
					}*/
					/*for (Integer topic: doc_topics.keySet()) { 
						// I assume the doc_topics are sorted in order of probability so highest topics will come first
						HashMap<String,Double> words = topic_model.topics.get(topic);
						if (words.containsKey(text)) {
							topic_word = true;
							break;
						}
					}
					if (topic_word) {
						bin_features.get(i).add("TOPIC_WRD_high");
					}
					else {
						bin_features.get(i).add("TOPIC_WRD_low");
					}
				}
			}*/
			else if (feature.equals("Morphology") && 
					(label_type.equals("sentiment") || label_type.equals("target+sentiment"))
					) {
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t=c.tokens_.get(i);
					if (!t.morph_features.containsKey("NO_ANALYSIS") && !CoreferenceFeatures.ArabicPronoun(t)
							&& !CoreferenceFeatures.ArabicObjectPronoun(t)) {
						bin_features.get(i).add("GEN_" + t.morph_features.get("gen"));
						bin_features.get(i).add("NUM_" + t.morph_features.get("num"));
						bin_features.get(i).add("PER_" + t.morph_features.get("per"));	
						//bin_features.get(i).add("STT_" + t.morph_features.get("stt"));
					} // For suffixes, extract the gender/number/person for that suffix
					else if (!t.morph_features.containsKey("NO_ANALYSIS")) {
						String fields = CoreferenceFeatures.SuffixFields(t);
						bin_features.get(i).add("GEN_" + CoreferenceFeatures.SuffixGender(fields));
						bin_features.get(i).add("NUM_" + CoreferenceFeatures.SuffixNumber(fields));
						bin_features.get(i).add("PER_" + CoreferenceFeatures.SuffixPerson(fields));	
						//bin_features.get(i).add("STT_" + t.morph_features.get("stt"));
					}
					else if (t.morph_features.containsKey("NO_ANALYSIS")){
						bin_features.get(i).add("GEN_NO_ANALYSIS");
						bin_features.get(i).add("NUM_NO_ANALYSIS");
						bin_features.get(i).add("PER_NO_ANALYSIS");
						//bin_features.get(i).add("STT_NO_ANALYSIS");
					}
				}
			}
			else if (feature.equals("State")) {
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t=c.tokens_.get(i);
					if (!t.morph_features.containsKey("NO_ANALYSIS")) {
						bin_features.get(i).add("STT_" + t.morph_features.get("stt"));
					} else {
						bin_features.get(i).add("STT_NO_ANALYSIS");
					}
				}
			}
			// Word clusters e
			else if (feature.equals("WordClusters")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t= c.tokens_.get(i);
					// First check text
					if (this.word_clusters.containsKey(t.text_)) {
						bin_features.get(i).add("CLUSTER_"+this.word_clusters.get(t.text_));
					} 
					// Next check lemma. Tokenized morphemes will get same cluster as their lemma
					else if (t.morph_features.containsKey("lex")
							&& this.word_clusters.containsKey(t.morph_features.get("lex"))) {
						bin_features.get(i).add("CLUSTER_"+this.word_clusters.get(t.morph_features.get("lex")));	
					}
					else {
						bin_features.get(i).add("CLUSTER_OOV");
					}
				}
			}
			// Resource rich clusters 
			else if (feature.equals("WordClustersEnglish")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t= c.tokens_.get(i);
					bin_features.get(i).add(GetClusterFromGloss(t, this.word_clusters));
				}
			}
			// could use when running ATB
			else if (feature.equals("Al")) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t= c.tokens_.get(i);
					if (t.text_.startsWith("Al")) {
						bin_features.get(i).add("AL_1");
					} else {
						bin_features.get(i).add("AL_0");
					}
				}
			}
				// Cluster Trees NOT HELPFUL, DETRIMENTAL
			/*	List<String[]> tree = c.dependency_tree;
				int node = 0;
				List<Token> no_Al = new ArrayList<Token>();
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						no_Al.add(t);
					}
				}/
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						String relation = tree.get(node)[5].toLowerCase();
						int parent_node = 0;
						String cluster_tree = "";
						if (!dp.isRoot(node, tree)) {
							parent_node = Integer.parseInt(tree.get(node)[4])-1;
							//parent_word = tree.get(parent_node)[1];
						}
						Token parent_token = no_Al.get(parent_node);
						String this_cluster = GetTokenCluster(t,word_clusters);
						String parent_cluster = GetTokenCluster(parent_token,word_clusters);
						cluster_tree = this_cluster + "_" + relation + "_" + parent_cluster;
						bin_features.get(i).add(cluster_tree);
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add(cluster_tree);
						}
						node +=1;	
					}
				}*/
				/*	for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						// CONTEXT FEATURES
						String relation = tree.get(node)[5];
						bin_features.get(i).add(relation);
						// CHILD PARENT TREE
						String child_parent_tree = dp.ChildParentTree(node, tree);
						bin_features.get(i).add(child_parent_tree);
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add(relation);
							bin_features.get(i-1).add(child_parent_tree);
						}*/
			}

		return bin_features;		
	}
	

	List<List<Double>> ExtractNumericFeatures (Comment c) {
		List<List<Double>> numeric_features = new ArrayList<List<Double>>();
		return numeric_features;
	}
	
	// Extracts token labels for target tagging (T, O)
	List<String> ExtractTokenTargetLabels (Comment c) {
		List<String> labels = new ArrayList<String>();
		for (Token t: c.tokens_) {
			if (t.target_offset_ == -1) {
				labels.add("O");
			} 
			else if (t.target_offset_ >= 0) {
				if (!pipeline_model && exclude_undetermined && t.sentiment_.equals("undetermined")) {
				    labels.add("O");
				} 
				else if (!pipeline_model && 
						(label_type.equals("target+sentiment") || label_type.equals("sentiment"))
						&& t.sentiment_.equals("undetermined")) {
					labels.add("O");
				else {
					labels.add("T");
				}
			}
			else {
				System.out.println("Feature Extractor: No valid target offset for"
						+ "token " + t.text_ + "for Comment" + c.comment_id_);
				System.exit(0);
			}
		}
		return labels;
	}
	
	// Extracts token labels for target tagging (BT, IT, O)
	List<String> ExtractBIOTokenTargetLabels (Comment c) {
		List<String> labels = new ArrayList<String>();
		for (Token t: c.tokens_) {
			if (t.target_offset_ == -1) {
				labels.add("O");
			} 
			else if (t.target_offset_ == 0) {
				if (exclude_undetermined && t.sentiment_.equals("undetermined")) {
					labels.add("O");
				}
				else {
					labels.add("BT");
				}
			}
			else if (t.target_offset_ > 0) {
				if (exclude_undetermined && t.sentiment_.equals("undetermined")) {
					labels.add("O");
				}
				else {
					labels.add("IT");
				}		
			}
			else {
				System.out.println("Feature Extractor: No valid target offset for"
						+ "token " + t.text_ + "for Comment" + c.comment_id_);
				System.exit(0);
			}
		}
		return labels;
	}
	
	List<String> ExtractTokenSentimentLabels (Comment c) {
		List<String> labels = new ArrayList<String>();
		// The target offsets here are assumed to be predicted
		for (Token t: c.tokens_) {
			if (t.target_offset_ == -1) {
				labels.add("neutral");
			} 
			else if (t.target_offset_ >= 0) {
				String sentiment = t.sentiment_;
				if (sentiment == null || sentiment.isEmpty()) {
					System.out.println("Feature Extractor: Token is part of a target but has "
							+ "no sentiment: "
							+ "token: " + t.text_ + " for Comment: " + c.comment_id_);
					sentiment = "neutral"; 
				}
				if (sentiment.equals("undetermined")) {
					labels.add("neutral");
				}
				else {
				labels.add(sentiment);
				}
			}
			else {
				System.out.println("Feature Extractor: No valid target offset for"
						+ "token " + t.text_ + "for Comment" + c.comment_id_);
				System.exit(0);
			}
		}
		return labels;
	}
	
	public void UpdateTargetStatistics(Token t, String key, HashMap<String,Integer> target_feature_statistics) {
	if (t.target_offset_>=0) {
		if (target_feature_statistics.containsKey(key)) {
			target_feature_statistics.put(key, target_feature_statistics.get(key)+1);
		} else{ 
			target_feature_statistics.put(key,1);
		}
	}
	}
   public void UpdateNonTargetStatistics(Token t, String key, HashMap<String,Integer> non_target_feature_statistics) {
	if (t.target_offset_==-1) {
		if (non_target_feature_statistics.containsKey(key)) {
			non_target_feature_statistics.put(key, 
					non_target_feature_statistics.get(key)+1);
		} else{ 
			non_target_feature_statistics.put(key,1);
		}
	}
     }
   
   public static String WordRepresentationIncludeClitics (Token t) {
		if (t.morph_features!=null && !t.morph_features.isEmpty() 
				&& !t.morph_features.containsKey("NO_ANALYSIS") ) {
			return t.morph_features.get("lex");
		}  else {
			return t.text_;
		}
	}
   
	public HashMap<String, String> getSimpleWordToCluster(String file) {
		System.out.println("Reading classes file:"+file);
		boolean skipEmpty = true;
		HashMap<String, String> wordToCluster = new HashMap<String, String>();
		List<String> lines = util.FileReader.ReadFile(file, "english", skipEmpty);
		for (String line: lines) {
			String[] fields = line.split(" ");
			String word = fields[0];	
			String cluster = fields[1];
			wordToCluster.put(word, cluster);
		}
		return wordToCluster;
	}
	
	public static String GetTokenCluster(Token t, HashMap<String,String> word_clusters) {
		String cluster = "";
		if (word_clusters.containsKey(t.text_)) {
			cluster = "CLUSTER_" + word_clusters.get(t.text_);
		}
		else {
			cluster = "CLUSTER_OOV";
		}
		return cluster;
	}
	
	// Searches for English cluster using gloss
	// Tries each gloss entry in turn
	public static String GetClusterFromGloss(Token t, HashMap<String,String> english_word_clusters) {
		String cluster = "ENG_CLUSTER_OOV";
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
				return cluster;
		}  
		String gloss = t.morph_features.get("gloss"); // if clitic, has same morph features as its word
		String[] potentials = gloss.split(";");
		for (String p : potentials) {
			//System.out.println("Potential:" + p);
			if (english_word_clusters.containsKey(p)) {
				cluster = "ENG_CLUSTER_" + english_word_clusters.get(p);
				System.out.println("Found English cluster !");
				System.out.println("Word:"+t.text_);
				System.out.println("Gloss:"+p);
				break;
				} 
			} 
			return cluster;
		}
	

}
