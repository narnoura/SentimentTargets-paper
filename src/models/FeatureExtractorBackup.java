/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.aliasi.cluster.LatentDirichletAllocation;

import data.Comment;
import data.Target;
import data.Token;
import models.baselines.ALL_NP;
import processor.LexiconProcessor;
import processor.Sentiment;
import processor.ToolsProcessor.DependencyProcessor;
import topicmodeling.LdaComments;
import topicmodeling.CoreferenceFeatures;
import models.IncreaseTrainingTargets;


/**
 * @author Narnoura
 * Extract features from comments.
 *
 */
public class FeatureExtractorBackup {
	
	// Can be mada comments, or any comments
	public List<Comment> input_comments;
	// Used if input comments are tokenized comments
	public List<Comment> nontok_comments;
	public LexiconProcessor lp;
	public DependencyProcessor dp;
	public LdaComments topic_model;
	// Feature types
	// Word, POS (madamira for now. can also try stanford since ATB normalized space), 
	// madamira features (lex, asp, mod, stt, cas?, gen? mod? all?)
	// Sentiment (pos score, neg score, neut score)
	// Dependency 
	// Topic
	public List<String> binary_feature_types;
	public List<String> continuous_feature_types;
	public List<String> morphological_features = Arrays.asList("gloss","prc3", "prc2", "prc1",
			"prc0", "per", "asp", "vox", "mod", "gen", "num", "stt", "cas", "enc0", "rat", "source", 
			"stemcat");
	public String label_type;
	// CHANGE BACK NOURA (DEFT)
	public static boolean include_labels = true;
	public static boolean exclude_undetermined = false;
	
	public FeatureExtractorBackup() {
		binary_feature_types = new ArrayList<String>();
		continuous_feature_types = new ArrayList<String>();
	}
	
	public FeatureExtractorBackup(List<Comment> input_comments) {
		this.input_comments = input_comments;
		this.nontok_comments = input_comments;
		binary_feature_types = new ArrayList<String>();
		continuous_feature_types = new ArrayList<String>();
		label_type = "target";
	}
	
	// Sets label type to 'target' or 'target+sentiment' for the pos-neg-neut model
	public void SetLabelType(String label_type) {
		this.label_type = label_type;
		/*if (this.label_type.equals("sentiment")) {
			exclude_undetermined = true;
		}*/
	}
	/*public boolean SetIncludeLabels(boolean include_labels){
		this.include_labels = include_labels;
	}*/
	
	public void SetBinaryFeatures (List<String> binary_features) {
		this.binary_feature_types = binary_features;
	}
	
	public void SetContinuousFeatures (List<String> continuous_features) {
		this.continuous_feature_types = continuous_features;
	}

	public void SetLexiconProcessor (LexiconProcessor lp) {
		this.lp = lp;
		if (this.binary_feature_types.contains("Sentiment") 
				|| this.binary_feature_types.contains("Dependency")) {
			System.out.println("Reading sentiment lexicons \n");
			lp.ReadSlsa(lp.lexicon_files.get("Slsa"));
			lp.ReadArsenl(lp.lexicon_files.get("Arsenl"));
			lp.ReadSifaat(lp.lexicon_files.get("Sifaat"));
			lp.ReadMPQA(lp.lexicon_files.get("MPQA"));
		}
	}
	
	public void SetDependencyProcessor (DependencyProcessor dp) {
		if (this.binary_feature_types.contains("Dependency")) {
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
	
	// note remember we should distinguish between train and test topic model. (in Runner?)
	// in Runner should figure out how to pass a model that's already been trained 
	// (may have to fix the way it's set up, i.e currently 'Train' mode is extracting features
	// for both train and test and 'test' mode is evaluating. since we're reading the model
	// from a file rather than running CRF right away. so may have to have a 'train_comments'
	// version of the input comments or set a flag that it's test and we should read a 
	// previously trained topic model model.
	//
	// maybe instead of passing comments for lda, pass lc that contains previously trained model
	// and a boolean on whether to train from existing model.
	public void SetTopicModel (LdaComments lc, List<Comment> comments_for_lda) {
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
	} 
	
	public void SetInput(List<Comment> comments) {
		input_comments = comments;
	}
	public void SetNontokenized (List<Comment> comments) {
		nontok_comments = comments;
	}
	
	List<List<String>> Data (Comment c) {
		//System.out.println("New comment " + c.comment_id_);
		List<List<String>> data = new ArrayList<List<String>>();
		//System.out.println("Extracting basic features");
		List<List<String>> basic_features = ExtractBasicFeatures(c);
		//System.out.println("Extracting binary features");
		List<List<String>> bin_features = ExtractBinaryFeatures(c);
		//System.out.println("Extracting labels");
	
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
			/*if (features.size() != 5) {
				System.out.println("Missing or extra column in comment " + c.comment_id_ + "!");
				for (String f: features) {
					System.out.println(f);
				}
				System.exit(0);
			}*/
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
	// Put features in outer loop instead? (less if statements). 
	// Then we have to save the token features per token (e.g hash)
	List<List<String>> ExtractBinaryFeatures (Comment c) {
		List<List<String>> bin_features = new ArrayList<List<String>>();
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
				//HashMap<String,String[]> tree = c.dependency_tree;
				List<String[]> tree = c.dependency_tree;
				//Iterator<String[]> itr = tree.iterator();
				int node = 0;
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					if (t.text_.equals("Al+")) {
						continue;
					}
					else {
						// itr.next contains the node. Subtract 1 to get the array index.
						//Integer node = Integer.parseInt(itr.next()[0])-1; // can do a function in dep. processor
						String word = tree.get(node)[1];
						String catib_pos = tree.get(node)[2];
						String catib_pos_detailed = tree.get(node)[3];
						String parent = tree.get(node)[4];
						String relation = tree.get(node)[5];
						String relation_lower = relation.toLowerCase();
						String parent_pos = "ROOT";
						/*String grandparent = "";
						String grandparent_pos = "ROOT";
						String grand_relation = "---";
						String grand_relation_lower = "---"; */
				
						if (!dp.isRoot(node, tree)) {
							Integer parent_node = Integer.parseInt(parent)-1;
							parent_pos = tree.get(parent_node)[2];
							/*if (!dp.isRoot(parent_node, tree)) {
								grandparent = tree.get(parent_node)[4];
								Integer grandparent_node = Integer.parseInt(grandparent)-1;
								grandparent_pos = tree.get(grandparent_node)[2];
								grand_relation = tree.get(parent_node)[5];
								grand_relation_lower = grand_relation.toLowerCase();
							}*/
						}
						/*if (!parent.equals("0")) { 
							Integer parent_node = Integer.parseInt(parent)-1;
							parent_pos = tree.get(parent_node)[2];
							//parent_word = tree.get(parent_node)[1];
						}*/
					
						// System.out.println("Word: " + word);
						// System.out.println("Catib pos: " + catib_pos);
						// System.out.println("Parent: " + parent_word);
						// System.out.println("Relation: " + relation);
						
						// CONTEXT FEATURES
						bin_features.get(i).add(relation);
							// grandparent
						//bin_features.get(i).add(grand_relation);
						
						// Have update function for key
						// Update target feature statistics for Relation
						if (t.target_offset_==-1) {
							if (non_target_feature_statistics.containsKey(relation)) {
								non_target_feature_statistics.put(relation, non_target_feature_statistics.get(relation)+1);
							} else{ 
								non_target_feature_statistics.put(relation,1);
							}
						} else {
							if (target_feature_statistics.containsKey(relation)) {
								target_feature_statistics.put(relation, target_feature_statistics.get(relation)+1);
							} else{ 
								target_feature_statistics.put(relation,1);
							}
						}
	
						// e.g NOMobjPRT
						String child_parent_tree = catib_pos + "_" + relation_lower + "_"  + parent_pos;
						bin_features.get(i).add(child_parent_tree);
						
						// Update target feature statistics for child parent tree
						if (t.target_offset_==-1) {
							if (non_target_feature_statistics.containsKey(child_parent_tree)) {
								non_target_feature_statistics.put(child_parent_tree, 
										non_target_feature_statistics.get(child_parent_tree)+1);
							} else{ 
								non_target_feature_statistics.put(child_parent_tree,1);
							}
						} else {
							if (target_feature_statistics.containsKey(child_parent_tree)) {
								target_feature_statistics.put(child_parent_tree, 
										target_feature_statistics.get(child_parent_tree)+1);
							} else{ 
								target_feature_statistics.put(child_parent_tree,1);
							}
						}
							//grandparent
						//String child_grandparent_tree = child_parent_tree + "_" + grand_relation_lower + "_" + grandparent_pos;
						//bin_features.get(i).add(child_grandparent_tree);
	
						// ROOT FEATURES
						
						// GLOBAL FEATURES
						//String ancestor_relation = dp.roleOfAncestor(node, tree);
						// not sure if function for ancestor relation is working as it should be. hurts results
						// bin_features.get(i).add(ancestor_relation);
						
						// SENTIMENT FEATURES
						// 1. PARENT_SENT_1 or PARENT_SENT_0
						// 2. GRANDPT_SENT_1 or GRANDPT_SENT_0
						//
						String parent_sent;
						//String grandparent_sent;
						if (!dp.isRoot(node, tree)) {
							Integer parent_node = Integer.parseInt(parent)-1;
							Token parent_token = c.tokens_.get(parent_node);
							//String sent = 
								//Sentiment.GetTokenHighLowSentimentFeatures(parent_token, lp, 0.3).toString();
							//String sent = Sentiment.GetBinarySentimentSifaat(parent_token, lp).toString();
							String sent = Sentiment.GetSubjectivityMPQA(parent_token, lp); // best
							//String sent = Sentiment.GetSubjectivitySifaatMPQA(parent_token, lp);
							if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
								//sent = Sentiment.GetSentimentSifaat(parent_token, lp).toString();
							    sent = Sentiment.GetPolarityMPQA(parent_token, lp); // best
								//sent = Sentiment.GetPolaritySifaatMPQA(parent_token, lp);
							}
							parent_sent = "PARENT_SENT_" + sent;
							/*if (!dp.isRoot(parent_node, tree)) {
								// later maybe functions for parent and grandparent
								Integer grandparent_node = Integer.parseInt(tree.get(parent_node)[4])-1;
								Token grandparent_token = c.tokens_.get(grandparent_node);
								// System.out.println("Grandparent token: " + grandparent_token.text_);
								//String sent1 = Sentiment.GetBinarySentimentSifaat(grandparent_token, lp).toString();
								String sent1 = 
										Sentiment.GetTokenHighLowSentimentFeatures(grandparent_token, lp, 0.3).toString();
								grandparent_sent = "GRANDPT_SENT_" + sent1;
							} else { grandparent_sent = "GRANDPT_SENT_0"; }*/
							
						}  else {
							//parent_sent = "PARENT_SENT_0";
							parent_sent = "PARENT_SENT_na";
							//grandparent_sent = "GRANDPT_SENT_0";
						}
						bin_features.get(i).add(parent_sent);
						//bin_features.get(i).add(grandparent_sent);
						// System.out.println("Parent sent: " + parent_sent);
						// System.out.println("Grandparent sent" + grandparent_sent);
				
						// Alternatively child_parent_tree = relation + "_" + parent_pos e.g OBJ_PRT
						// We did this only for Al+ since the others get the ATB dep features from the pipeline
						if (i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							bin_features.get(i-1).add(relation);
							//bin_features.get(i-1).add(grand_relation); // not helpful (only for sentiment)
							bin_features.get(i-1).add(child_parent_tree);
							//bin_features.get(i-1).add(child_grandparent_tree);
							//bin_features.get(i-1).add(ancestor_relation);
							bin_features.get(i-1).add(parent_sent);
							//bin_features.get(i-1).add(grandparent_sent);
						}
						node +=1;
					}	
				}
			}
			// Sentiment features don't help target extraction, so use them only
			// in the sentiment model and collapsed model
			else if (feature.equals("Sentiment") 
					&& (label_type.equals("sentiment") || label_type.equals("target+sentiment"))
					) {
				String sentfeat;
				String polfeat;
				for (int i =0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					sentfeat = "TOK_SUBJ_";
					polfeat = "TOK_POL_";
					sentfeat += Sentiment.GetSubjectivityMPQA(t, lp); // best
					polfeat += Sentiment.GetPolarityMPQA(t, lp); // best
					
					//sentfeat += Sentiment.GetSubjectivitySifaatMPQA(t, lp);
					//polfeat += Sentiment.GetPolaritySifaatMPQA(t, lp);
					
					//sentfeat += Sentiment.GetBinarySentimentSifaat(t, lp);
					//polfeat += Sentiment.GetSentimentSifaat(t,lp);
					
					/*if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						sentfeat += Sentiment.GetPolarityMPQA(t, lp);
						// can also add subjectivity here, may be useful
					} else {
						sentfeat += Sentiment.GetSubjectivityMPQA(t, lp);
					}*/
					//sentfeat += Sentiment.GetSentimentSifaat(t, lp).toString();
					//sentfeat = Sentiment.GetBinarySentimentSifaat(t, lp).toString();
					//sentfeat = Sentiment.GetTokenHighLowSentimentFeatures(t, lp, 0.3);
					bin_features.get(i).add(sentfeat);
					bin_features.get(i).add(polfeat);
					/*if (label_type.equals("sentiment") || label_type.equals("target+sentiment")) {
						polfeat += Sentiment.GetPolaritySifaatMPQA(t, lp);
						bin_features.get(i).add(polfeat);
					}*/
				}
			}
			else if (feature.equals("Salience") && 
					(label_type.equals("target") || label_type.equals("target+sentiment"))) {
			     // can remove target restriction when running collapsed model
				// target+sentiment?
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t=c.tokens_.get(i);
					//System.out.println("Token:" + t.text_);
					//Integer frequency = t.frequency_in_comment;
					/*String salience = "SALIENCE_low";
					if (frequency >=3) {
						salience = "SALIENCE_high";
					}*/
					String salience = t.frequency_in_comment.toString();
					//System.out.println("Salience feature:" + freq);
					bin_features.get(i).add(salience);
				}
			}
			else if (feature.equals("Topic") && 
					(label_type.equals("target") || label_type.equals("target+sentiment"))) {
				// Find if token is a topic for this comment
				Integer doc_number = input_comments.indexOf(c);
				HashMap<Integer,Double> doc_topics = topic_model.documents.get(doc_number);
				for (int i=0; i< c.tokens_.size(); i++) {
					boolean topic_word = false;
					Token t = c.tokens_.get(i);
					String text;
					if (t.morph_features.containsKey("lex") && (t.clitic.equals("none") ||
							t.clitic.equals("word"))) {
						text = t.morph_features.get("lex");
					} else {
						text = t.text_;
					}
					for (Integer topic: doc_topics.keySet()) { 
						// I assume the doc_topics are sorted in order of probability so highest topics will come first
						// probably also later will limit k best topics and words
						HashMap<String,Double> words = topic_model.topics.get(topic);
						if (words.containsKey(text)) {
							//System.out.println("LDA: Found key for topic word " + text + " !");
							//System.out.println("Probability: " + words.get(text) );
							topic_word = true;
							break;
						}
					}
					if (topic_word) {
						bin_features.get(i).add("TOPIC_WRD");
					}
					else {
						bin_features.get(i).add("0");
					}
				}
			}
			// TODO check combine morphology and dependency features in template
			// Morphology is helpful for sentiment but not for targets
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
			/* else if (features.equals("coref"))
			 * else if (features.equals("COREF_CANDIDATE")) {
			 *  
			 * Can only be done for sentiment model
			 * whether token matches in lemma, gender, number, and person to a target
			 * whether token matches in gender, number, and person to a target
			 * the target and also the sentiment of the potential candidate? (propagating the sentiment)
			 * 
			 * }
			 * 
			 */
		}
		return bin_features;		
	}
	
	// return type string?
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
				if (exclude_undetermined && t.sentiment_.equals("undetermined")) {
					labels.add("O");
				}
				else {
					labels.add("T");
				}
				//System.out.println("Target offset:" + t.target_offset_);
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
	//
	// TODO: exclude undetermined sentiment
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
		// Should we keep undetermined targets and do pos,neg,neutral?
		// or just exclude the undetermined from the sentiment evaluation 
		// since we don't know their sentiment?
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
					sentiment = "neutral"; // For pipeline models: For mispredicted targets (false positives)
					//System.exit(0);
				}
				/*if (exclude_undetermined && sentiment.equals("undetermined")) {
					labels.add("O");
				}*/
				if (!exclude_undetermined && sentiment.equals("undetermined")) {
					//labels.add("neutral");
					labels.add("negative"); // majority 
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
	
	
	// Need one more for collapsed model
}
