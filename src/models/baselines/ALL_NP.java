/**
 * 
 */
package models.baselines;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import data.Comment;
import data.Target;
import data.Token;
import processor.ToolsProcessor.MadamiraProcessor;
import processor.ToolsProcessor.StanfordParserProcessor;
import processor.LexiconProcessor;
import processor.Sentiment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import models.baselines.VisitedNode;
import util.BuckwalterConverter;
import util.FileReader;
import util.Tokenizer;
import main.Constants;


/**
 * @author Narnoura
 * Implements the ALL_NP baseline.
 * Parses all noun phrases
 * Assigns targets to all noun phrases 
 * and returns the aggregated sentiment of the comment
 * or optionally of only the sentence containing the target.
 * Directory with stanford parses is hard-coded and will store
 * parses there for the train and test sets.
 */
public class ALL_NP {

	public String lexicon_;
	public String base_input_file;
	public static String parse_directory = "../data/stanford-parses";
	public ALL_NP() {	
	}
	
	public void SetLexicon (String lexicon) {
		lexicon_ = lexicon;
	}
	// Sets base input file to read from madamira
	public void SetInputFile (String input_file) {
		base_input_file = input_file;
		base_input_file = base_input_file.replaceAll("xml", "raw");
		System.out.println("Base input file: " + base_input_file);
	}

	public List<Comment> Run (List<Comment> input_comments, String madamira_directory, 
			String lexicon_file, boolean use_existing_parse) {
		
		List<Comment> output_comments = null;
		System.out.println("\nRunning ALL_NP baseline \n");	
		LexiconProcessor lp = new LexiconProcessor();
		// For calculating hit rate
		lp.ReadArsenl(lp.lexicon_files.get("Arsenl"));
		lp.ReadMPQA(lp.lexicon_files.get("MPQA"));
		lp.ReadSifaat(lp.lexicon_files.get("Sifaat"));
		if (lexicon_file.endsWith("SLSA.txt")) {
			System.out.println("Reading SLSA \n");
			SetLexicon("SLSA");
			lp.ReadSlsa(lexicon_file);
		}
		else if (lexicon_file.endsWith("ArSenL_v1.0A.txt")) {
			SetLexicon("Arsenl");
			//lp.ReadArsenl(lexicon_file);
			//lp.PrintArsenl();
		}
		else if (lexicon_file.endsWith("subjclueslen1-HLTEMNLP05.tff")) {
			SetLexicon("MPQA");
			//lp.ReadMPQA(lexicon_file);
		}
		else if (lexicon_file.endsWith("ARABSENTI-LEXICON")) {
			SetLexicon("SIFAAT");
			//lp.ReadSifaat(lexicon_file);
		}
		// Read ATB tokenized file and mada file from Madamira
		// Assumed that the files are in the directory
		// and have the .raw.(madaextensiontype) extension
		System.out.println("\nReading madamira files \n");
		String tok_file = (new File(madamira_directory, base_input_file).getAbsolutePath());
		tok_file += ".ATB.tok";
		List<Comment> tokenized_comments =
				MadamiraProcessor.ReadTokenizedComments(tok_file, "utf8ar");
		String mada_file = (new File(madamira_directory, base_input_file).getAbsolutePath());
		mada_file += ".mada";
		List<Comment> mada_comments = MadamiraProcessor.ReadMadaFile(mada_file, "bw");
		
		// Get parsed trees
		if (!use_existing_parse) {
			StanfordParserProcessor sp 
				= new StanfordParserProcessor("edu/stanford/nlp/models/lexparser/arabicFactored.ser.gz", "ATB");
			System.out.println("Parsing comments \n");
			//parse_trees = sp.ParseComments(tokenized_comments,
				//new File(parse_directory, base_input_file).getAbsolutePath());
			sp.ParseComments(tokenized_comments,
					new File(parse_directory, base_input_file).getAbsolutePath());
		}
		Document xml_trees = FileReader.ReadXMLFile
			(new File(parse_directory, base_input_file).getAbsolutePath(), "utf8ar");
		
		// Generate targets using NP constituents
		// Detokenize targets from ATB by matching with original mada comments
		output_comments = FindTargets (xml_trees, input_comments, tokenized_comments,
				mada_comments);
		
		// Update comments with sentiment
		//output_comments = UpdateSentiment(output_comments, lp);
		output_comments = UpdateSentimentForTargetsSentenceLevel(output_comments, lp);
		
		if (output_comments.isEmpty()) {
		System.out.println("Output comments are empty!! \n");
		}
		else {
			System.out.println("Size of output comments: " + output_comments.size());
		}

		return output_comments;
	}
	
	// Given a list of parsed comments, creates new comments 
	// and updates target fields with all possible noun phrases
	// Deprecated. Currently using "GetNounPhrasesAsTargetsWithLeafVisits"
	public List<Comment> GetNounPhrasesAsTargets (Document xml_trees) {
		xml_trees.getDocumentElement().normalize();
		NodeList s_list = xml_trees.getElementsByTagName("s");
		List<Comment> output_comments = new ArrayList<Comment>();
	
		for (int c = 0; c < s_list.getLength(); c++) {
			Comment output_comment = new Comment();
			Node s_node = s_list.item(c);
			if (s_node.getNodeType() == Node.ELEMENT_NODE) {
				Element e_element = (Element) s_node;
				String comment_id = e_element.getAttribute("id");
				System.out.println("Comment id: " + comment_id);
				output_comment.SetCommentID(comment_id);
			
				NodeList nodes = e_element.getElementsByTagName("node");
			
				for (int n=0 ; n < nodes.getLength(); n++) {
					Node n_node = nodes.item(n);
					
					if (n_node.getNodeType() == Node.ELEMENT_NODE) {
						Element n_element = (Element) n_node;
						String value = n_element.getAttribute("value");

						if (value.equals("NP")) {
							String noun_phrase = "";
							NodeList leaf_nodes = n_element.getElementsByTagName("leaf");
							List<Token> target_tokens = new ArrayList<Token>();
							for (int l=0; l<leaf_nodes.getLength(); l++) {
								Node leaf_node = leaf_nodes.item(l);
								Node parent_node = leaf_node.getParentNode();
								Element leaf_element = (Element) leaf_node;
								Element parent_element = (Element) parent_node;
								String leaf_value = leaf_element.getAttribute("value");
								String pos = parent_element.getAttribute("value");
							
						 		noun_phrase += " " + leaf_value;
								Token token = new Token(leaf_value, "ATB");
								token.SetPOS(pos);
								token.SetTargetOffset(l);
							
								target_tokens.add(token);
								noun_phrase = noun_phrase.trim();
							}
							if (NounPhraseOK (noun_phrase)) {
								System.out.println("Noun phrase: " + noun_phrase + "\n");
								output_comment.AddTarget(new Target(noun_phrase,target_tokens));
							}
						}
					}
				}
			}
			output_comments.add(output_comment);
		}
		return output_comments;
	}
	
	
	// Given a list of parsed comments, creates new comments 
	// and updates target fields with all possible noun phrases
	// Use this one to keep track of the leaf indices
	 public List<Comment> GetNounPhrasesAsTargetsWithLeafVisits (Document xml_trees, List<Comment> input_comments) {
			xml_trees.getDocumentElement().normalize();
			NodeList s_list = xml_trees.getElementsByTagName("s");
			List<Comment> output_comments = new ArrayList<Comment>();
			
			for (int c = 0; c < s_list.getLength(); c++) {
				Comment output_comment = new Comment();
				String comment_text = "";
				Node s_node = s_list.item(c);
				if (s_node.getNodeType() == Node.ELEMENT_NODE) {
					Element e_element = (Element) s_node;
					String comment_id = e_element.getAttribute("id");
					output_comment.SetCommentID(comment_id);
					NodeList nodes = e_element.getElementsByTagName("node");
					NodeList leafs = e_element.getElementsByTagName("leaf");
				
					int leaf_num = 0;
					List<VisitedNode> visited_nodes = new ArrayList<VisitedNode>();
					for (int n=0; n< nodes.getLength(); n++) {
						visited_nodes.add(new VisitedNode(nodes.item(n),false));
					}
				
					List<Node> visited_leafs = new ArrayList<Node>();
					// Set tokens for comment and store leaf nodes
					List<Token> comment_tokens = new ArrayList<Token>();
					for (int n=0;n<leafs.getLength(); n++) {
						visited_leafs.add(leafs.item(n));
						Element leaf_element = (Element) leafs.item(n);
						Token leaf_token = new Token(leaf_element.getAttribute("value"), "ATB");
						leaf_token.text_ = leaf_token.text_.trim();
						comment_text += leaf_token.text_ + " ";
						leaf_token.SetCommentOffset(n);
						comment_tokens.add(leaf_token);
					}
					output_comment.SetTokens(comment_tokens);
					comment_text = comment_text.trim();
					output_comment.SetText(comment_text);
					
					for (int n=0 ; n < nodes.getLength(); n++) {
						Node n_node = nodes.item(n);
						if (n_node.getNodeType() == Node.ELEMENT_NODE) {
							Element n_element = (Element) n_node;
							String value = n_element.getAttribute("value");

							String noun_phrase = "";
							NodeList leaf_nodes = n_element.getElementsByTagName("leaf");
							List<Token> target_tokens = new ArrayList<Token>();
							for (int l=0; l<leaf_nodes.getLength(); l++) {
									Node leaf_node = leaf_nodes.item(l);
									Node parent_node = leaf_node.getParentNode();
									Element leaf_element = (Element) leaf_node;
									Element parent_element = (Element) parent_node;
									String leaf_value = leaf_element.getAttribute("value");
									String pos = parent_element.getAttribute("value");

									if (visited_leafs.contains(leaf_node)) {
										leaf_num = visited_leafs.indexOf(leaf_node);
									} else {
										System.out.println("Didn't find this leaf! :( \n");
										System.out.println("Leaf value:" + leaf_value);
										System.out.println("Part of speech:" + pos);
									}
									
									if (value.equals("NP")) {
									noun_phrase += " " + leaf_value;
									Token token = new Token(leaf_value, "ATB");
									token.SetPOS(pos);
									token.SetTargetOffset(l);
									token.SetCommentOffset(leaf_num);
									target_tokens.add(token);
									noun_phrase = noun_phrase.trim();
									}
								}
								if (value.equals("NP") && NounPhraseOK (noun_phrase)) {
									Target t = new Target(noun_phrase,target_tokens);
									t.comment_offsets_ = new ArrayList<Integer>();
									
									t.comment_offsets_.add(leaf_num);
									output_comment.AddTarget(t);
								}
						} // end if element type
				} // end for visited nodes
			} // end first if element typ
			output_comments.add(output_comment);
			}
			return output_comments;
	 }
	
	// Filter extra long or extra short noun phrases 
	// Later can also compare with madamira tags
	boolean NounPhraseOK (String noun_phrase) {
		String[] words = noun_phrase.split(" ");
		if (words.length > 10) {
			return false;
		}
		// One or two character phrase, probably a preposition
		// Later this might be useful to keep (for co-reference res)
		if (words.length == 1 && words[0].length() < 3) {
			return false;
		}
		return true;
	}
	
	// Updates output comments with targets from NPs
	// Restores original tokenization/normalization
	public List<Comment> FindTargets (Document xml_trees, List<Comment> input_comments,
			List<Comment> mada_tokenized_comments, List<Comment> mada_original_comments) {

		List<Comment> comments_with_targets = GetNounPhrasesAsTargetsWithLeafVisits (xml_trees,input_comments);
		comments_with_targets = RestoreFromATB (comments_with_targets, mada_tokenized_comments,
				mada_original_comments);
		
		// NOURA CHANGING INPUT COMMENTS FOR MENTION BASELINE
		// For keeping them the same, use SetProcessed
		for (Comment c: input_comments) {
			int i = input_comments.indexOf(c);
			Comment mada_original = mada_original_comments.get(i);
			c.SetText(mada_original.raw_text_);
			c.SetTokens(mada_original.tokens_);
		}
		
		return comments_with_targets;
	}
	
	public List<Comment> RestoreFromATB (List<Comment> ATB_comments,
				List<Comment> mada_tokenized_comments, List<Comment> mada_original_comments) {
		 
		 int i = 0;
		 List<Comment> output_comments = new ArrayList<Comment>();
		 for (Comment c: ATB_comments) {
			 Comment new_comment = new Comment();
			 Comment mada_tokenized_comment = mada_tokenized_comments.get(i);
			 Comment mada_original_comment = mada_original_comments.get(i);
			 assert(c.comment_id_.equals(mada_tokenized_comment.comment_id_));
			 assert(c.comment_id_.equals(mada_original_comment.comment_id_));
			 new_comment.SetCommentID(mada_original_comment.comment_id_);
			 new_comment.SetText(mada_original_comment.raw_text_);
			// Then, restore all tokens
			 int original_comment_offset = -1;
			 List<Token> comment_tokens = new ArrayList<Token>();
			 for (Token tok: c.tokens_ ) {
				int tokenized_offset = tok.comment_offset_;
				Token tokenized_token = mada_tokenized_comment.tokens_.get(tokenized_offset);
				original_comment_offset = tokenized_token.comment_offset_;
				Token original_token = mada_original_comment.tokens_.get(original_comment_offset);
				if (!comment_tokens.contains(original_token))
					{
					comment_tokens.add(original_token);
					}		
			 }
			 assert(comment_tokens.size() == mada_original_comment.tokens_.size());
			 new_comment.SetTokens(comment_tokens);
			 output_comments.add(new_comment);
			 
			 // First, restore targets
			 List<Target> output_targets = new ArrayList<Target>();
			 if (c.targets_ != null) {
				 
				 for (Target t: c.targets_) {
					 Target output_target = new Target();
					 String target_text = "";
					 List<Token> target_tokens = new ArrayList<Token>();
					 int first_target_offset=-1;
					 for (Token tok: t.tokens_ ) {
						int tokenized_offset = tok.comment_offset_;
						
						Token tokenized_token = mada_tokenized_comment.tokens_.get(tokenized_offset);
						// avoid duplicates
						int original_offset = tokenized_token.comment_offset_;
						Token original_token = mada_original_comment.tokens_.get(original_offset);
						if (!target_tokens.contains(original_token))
							{   target_tokens.add(original_token);
								target_text += original_token.text_ + " ";
								if (tok.target_offset_ == 0){
									first_target_offset = original_offset;
								}
							}
					 }
					 target_text = target_text.trim();
					 output_target.SetText(target_text);
					 output_target.SetTokens(target_tokens);
					 List<Integer> comment_offsets = new ArrayList<Integer>();
					 comment_offsets.add(first_target_offset);
					 output_target.SetOffsets(comment_offsets);
					 output_targets.add(output_target);
					 /*List<Integer> comment_offsets = t.comment_offsets_;
					 // assumed only one offset at this point
					 int comment_offset = comment_offsets.get(0); */
				 }
			 }
			 new_comment.SetTargets(output_targets);
			 i+=1;
		 }
		 return output_comments;
	 }

	
	// Modifies output_comments by updating the sentiment of the targets	
	// Assumes that output comments have been detokenized and contain mada tokens 
	public List<Comment> UpdateSentiment(List<Comment> mada_comments,
			LexiconProcessor lp) {
		
				int i=0;
				for (Comment c: mada_comments) {
					String id = c.comment_id_;
					Comment mada_comment = mada_comments.get(i);
					if (!id.equals(mada_comment.comment_id_)) {
						System.out.println("Comment ids not consistent! Exiting \n");
						System.out.println("Mada id: " + mada_comment.comment_id_);
						System.out.println("Input comment id: " + id);
						System.exit(0);
					}
					 System.out.println("Comment:" + 
					 BuckwalterConverter.ConvertToUTF8(mada_comment.raw_text_) + "\n");
					
					//String sentiment = GetTokensCategorySentiment(mada_comment.tokens_, lp, 0.3);
					//String sentiment = GetTokensMaxSentiment(mada_comment.tokens_, lp);
					 String sentiment = GetTokensSentimentMPQA(mada_comment.tokens_,lp);
					 /* System.out.println("Average Sentiment:" + sentiment + "\n");
					  System.out.println("Targets: \n");*/
					for (Target t: c.targets_ ) 
					{
						t.SetSentiment(sentiment);
					}
					i+=1;
				}	
				return mada_comments;
	}
	
	
	// Updates sentiment at sentence level
	// Assumes that output comments have been detokenized and contain mada tokens
	// Use this for updating sentiment for targets at sentence level
	// This function should ideally take a lexicon name
	public List<Comment> UpdateSentimentForTargetsSentenceLevel(List<Comment> mada_comments, 
				LexiconProcessor lp) {
		System.out.println("Updating sentiment for predicted targets\n");
		double hit_rate_arsenl = 0 ;
		double hit_rate_sifaat = 0 ;
		double hit_rate_mpqa = 0 ;
		double total_tokens = 0;
		// can do 1 variable for each lexicon
		
					for (Comment c: mada_comments) {
						String text = c.raw_text_;
					
						int c_offset = 0;
						if (c.tokens_ == null) {
							System.out.println("Output comment has no tokens! Exiting \n");
							System.exit(0);
						}
						else if (c.tokens_.isEmpty()) {
							System.out.println("Output comment has empty tokens! Exiting \n");
							System.exit(0);
						}
						
						// Calculate lexicon hit rate
						for (Token t: c.tokens_) {
							total_tokens +=1;
							// count hit rate for each lexicon
							String lemma = "";
							if (t.morph_features.containsKey("NO_ANALYSIS")) {
								lemma = t.text_;
							} else {
								lemma = t.morph_features.get("lex");
							}
			
							if (lp.HasArsenlKey(lemma)) {
								hit_rate_arsenl +=1;
							}
							String stripped = lemma;
							stripped = stripped.replaceAll("(\\_)(\\d)+\\z", "");
							if (lp.HasSifaatKey(stripped)) {
								hit_rate_sifaat +=1;
							}
							String english_word = Sentiment.GetMPQAFromGloss(t, lp);
							if (lp.HasMPQAKey(english_word)) {
								hit_rate_mpqa +=1;
							}
							
							
						}
						
						// First, set Comment level sentiment for all targets
						//String sentiment = GetTokensCategorySentiment(c.tokens_, lp,0.3);
						//String sentiment = GetTokensMaxSentiment(c.tokens_, lp);
						String sentiment = GetTokensSentimentMPQA(c.tokens_,lp);
						//String sentiment = GetTokensSentimentSifaat(c.tokens_,lp);
						//String sentiment = GetTokensSentimentArsenl(c.tokens_,lp);
						//String sentiment = Sentiment.GetTokensSentimentSifaat(c.tokens_,lp);
						for (Target t: c.targets_) {
							t.SetSentiment(sentiment);
						}
						// Now get the sentiment for tokens at the sentence (clause actually) level
						String[] sentences = c.raw_text_.split("\\?|\\!| \\.| \\,");
						List<Token> separatorless = c.Split(Constants.MY_COMMENT_SPLITTERS);
						for (int s=0; s< sentences.length; s++) {
							String sentence_text = sentences[s];
							
							sentence_text = sentence_text.trim();
							if (sentence_text.isEmpty() || sentence_text.equals(" ")
								|| Constants.MY_COMMENT_SPLITTERS.contains(sentence_text)) {
								continue;
							}
							String[] words = sentence_text.split(" ");
							List<Token> sentence_tokens = new ArrayList<Token>();
							for (int j=0; j<words.length; j++ ) {
								if (Constants.MY_COMMENT_SPLITTERS.contains(words[j])) {
									continue;
								}
								Token mt = separatorless.get(c_offset);
								sentence_tokens.add(mt);
								c_offset +=1; 
							}
						//System.out.println("Sentence text:" + sentence_text);
						//System.out.println("Sentence tokens:");
						//String sentiment1 = GetTokensCategorySentiment(sentence_tokens, lp, 0.3);
						//String sentiment1 = Sentiment.GetTokensSentimentSifaat(sentence_tokens,lp);
						//String sentiment1 = GetTokensMaxSentiment(sentence_tokens,lp);
						
						String sentiment1 = GetTokensSentimentMPQA(sentence_tokens,lp);
						//String sentiment1 = GetTokensSentimentSifaat(sentence_tokens,lp);
						//String sentiment1 = GetTokensSentimentArsenl(sentence_tokens,lp);
						
						// System.out.println("Average Sentiment:" + sentiment + "\n");
						int start_offset = sentence_tokens.get(0).comment_offset_;
						int end_offset = sentence_tokens.get(sentence_tokens.size()-1).comment_offset_;
						for (Target t: c.targets_ ) 
							{   
								// NOURA PUT BACK int target_start = t.comment_offsets_.get(0);
								// NOURA PUT BACK int target_end =  t.comment_offsets_.get(0) + t.tokens_.size() - 1;
								if (sentence_text.contains(t.text_))
										// NOURA PUT BACK && target_start >= start_offset
										// NOUT PUT BACK  && (target_end <= end_offset))
									{ t.SetSentiment(sentiment1); }
							}
						}
						} // end for comment c
					System.out.println("Total tokens:" + total_tokens);
					System.out.println("Hit rate Arsenl:" + hit_rate_arsenl);
					System.out.println("Hit rate Sifaat:" + hit_rate_sifaat);
					System.out.println("Hit rate MPQA:" + hit_rate_mpqa);
					return mada_comments;
				}
	
	
	public List<Comment> UpdateSentimentForTargetsSentenceLevelStanford(List<Comment> mada_comments, 
			LexiconProcessor lp) {
			System.out.println("Updating sentiment for predicted targets\n");
		
			double hit_rate_mpqa = 0 ;
			double total_tokens = 0;
	
	
				for (Comment c: mada_comments) {
					String text = c.raw_text_;
				
					int c_offset = 0;
					if (c.tokens_ == null) {
						System.out.println("Output comment has no tokens! Exiting \n");
						System.exit(0);
					}
					else if (c.tokens_.isEmpty()) {
						System.out.println("Output comment has empty tokens! Exiting \n");
						System.exit(0);
					}
					
					// Calculate lexicon hit rate
					for (Token t: c.tokens_) {
						total_tokens +=1;
						// count hit rate for each lexicon
						String lemma = "";
						if (t.morph_features.isEmpty()) {
							lemma = t.text_;
						} else {
							lemma = t.morph_features.get("lex");
						}
						if (lp.HasMPQAKey(lemma)) {
							hit_rate_mpqa +=1;
						}
						
						
					}
					// First, set Comment level sentiment for all targets
					String sentiment = GetEnglishTokensSentimentMPQA(c.tokens_,lp);
					for (Target t: c.targets_) {
						t.SetSentiment(sentiment);
					}
					// Now get the sentiment for tokens at the sentence (clause actually) level
					String[] sentences = c.raw_text_.split("\\?|\\!| \\.| \\,");
					List<Token> separatorless = c.Split(Constants.MY_COMMENT_SPLITTERS);
					for (int s=0; s< sentences.length; s++) {
						String sentence_text = sentences[s];
						sentence_text = sentence_text.trim();
						if (sentence_text.isEmpty() || sentence_text.equals(" ")
							|| Constants.MY_COMMENT_SPLITTERS.contains(sentence_text)) {
							continue;
						}
						String[] words = sentence_text.split(" ");
						List<Token> sentence_tokens = new ArrayList<Token>();
						for (int j=0; j<words.length; j++ ) {
							if (Constants.MY_COMMENT_SPLITTERS.contains(words[j])) {
								continue;
							}
							sentence_tokens.add(new Token(words[j],"word"));
						}
				
					String sentiment1 = GetEnglishTokensSentimentMPQA(sentence_tokens,lp);
				
					for (Target t: c.targets_ ) 
						{   
							if (sentence_text.contains(t.text_))
								{ t.SetSentiment(sentiment1); }
						}
					}
					} // end for comment c
				System.out.println("Total tokens:" + total_tokens);
				
				return mada_comments;
			}
	
	public List<Comment> UpdateEnglishSentimentForTargetsCommentLevel(List<Comment> english_comments, 
			LexiconProcessor lp) {
				System.out.println("Updating sentiment for predicted targets\n");
				for (Comment c: english_comments) {
					if (c.tokens_ == null) {
						System.out.println("Output comment has no tokens! Exiting \n");
						System.exit(0);
					}
					else if (c.tokens_.isEmpty()) {
						System.out.println("Output comment has empty tokens! Exiting \n");
						System.exit(0);
					}
					// First, set Comment level sentiment for all targets
					String sentiment = GetEnglishTokensSentimentMPQA(c.tokens_,lp);
					if (c.targets_.size() == 0) {
						System.out.println("Baseline runner: this comment has empty targets!");
					}
					for (Target t: c.targets_) {
						t.SetSentiment(sentiment);
					}
				}
			return english_comments;
	}
	
	// Updates sentiment at sentence level
	// Assumes that output comments have been detokenized and contain mada tokens
	// Use this for updating sentiment for targets at sentence level
	// Doesn't work with stanford tokens, might need to save the annotation document (from 
	// stanford tokenization)
	public List<Comment> UpdateEnglishSentimentForTargetsSentenceLevel(List<Comment> english_comments, 
				LexiconProcessor lp) {
		System.out.println("Updating sentiment for predicted targets\n");
					for (Comment c: english_comments) {
						String text = c.raw_text_;
						int c_offset = 0;
						if (c.tokens_ == null) {
							System.out.println("Output comment has no tokens! Exiting \n");
							System.exit(0);
						}
						else if (c.tokens_.isEmpty()) {
							System.out.println("Output comment has empty tokens! Exiting \n");
							System.exit(0);
						}
						// First, set Comment level sentiment for all targets
						String sentiment = GetEnglishTokensSentimentMPQA(c.tokens_,lp);
						for (Target t: c.targets_) {
							t.SetSentiment(sentiment);
						}
						// Now get the sentiment for tokens at the sentence (clause actually) level
						String[] sentences = c.raw_text_.split("\\?|\\!| \\.| \\,");
						List<Token> separatorless = c.Split(Constants.MY_COMMENT_SPLITTERS);
						for (int s=0; s< sentences.length; s++) {
							String sentence_text = sentences[s];
							sentence_text = sentence_text.trim();
							if (sentence_text.isEmpty() || sentence_text.equals(" ")
								|| Constants.MY_COMMENT_SPLITTERS.contains(sentence_text)) {
								continue;
							}
							String[] words = sentence_text.split(" ");
							List<Token> sentence_tokens = new ArrayList<Token>();
							for (int j=0; j<words.length; j++ ) {
								if (Constants.MY_COMMENT_SPLITTERS.contains(words[j])) {
									continue;
								}
								Token mt = separatorless.get(c_offset);
								sentence_tokens.add(mt);
								c_offset +=1; 
							}
						//System.out.println("Sentence text:" + sentence_text);
						//System.out.println("Sentence tokens:");
						String sentiment1 = GetEnglishTokensSentimentMPQA(sentence_tokens,lp);
						// System.out.println("Sentiment:" + sentiment1 + "\n");
						int start_offset = sentence_tokens.get(0).comment_offset_;
						int end_offset = sentence_tokens.get(sentence_tokens.size()-1).comment_offset_;
						for (Target t: c.targets_ ) 
							{   
								// NOURA PUT BACK int target_start = t.comment_offsets_.get(0);
								// NOURA PUT BACK int target_end =  t.comment_offsets_.get(0) + t.tokens_.size() - 1;
								if (sentence_text.contains(t.text_))
										// NOURA PUT BACK && target_start >= start_offset
										// NOUT PUT BACK  && (target_end <= end_offset))
									{ t.SetSentiment(sentiment1); }
							}
						}
						} // end for comment c
					return english_comments;
				}
	
	// may need options like average/max, and sentiment threshold
	String GetCommentSentiment (Comment comment, LexiconProcessor lp) {
		return "positive";
	}
	
	String GetTokensSentimentMPQA (List<Token> tokens, LexiconProcessor lp) {
		String sentiment = "";
		//int num_strongsubj = 0;
		//int num_weaksubj = 0;
		//int num_nosubj = 0;
		int num_pos = 0;
		int num_neg = 0;
		for (Token t: tokens) {
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				continue;
			}
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");
			String previous_text = "";
			int i = tokens.indexOf(t);
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.morph_features.get("WORD");
			}
			//pos = Tokenizer.ResolvePOS(pos);
			String subjectivity = Sentiment.GetSubjectivityMPQA(t, lp);
			String polarity = Sentiment.GetPolarityMPQA(t, lp);
			
			/*if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
				//System.out.println("Previous text:" + previous_text + " Current text:" + lemma);
				if (polarity.equals("positive")) {
					polarity = "negative";
				}
				else if (polarity.equals("negative")) {
					polarity = "positive";
				} else if (polarity.equals("na") || polarity.equals("neutral")) {
					polarity = "na";
				}
			}*/
			
	
			if (polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (polarity.equals("negative")) {
				num_neg +=1;
			} else {
				// do nothing
			}
		}
			
		if (num_pos > num_neg) {
				sentiment = "positive";
			}
			// bias towards majority baseline;
			else {
				sentiment = "negative";
			}
		
		return sentiment;
	}
	
	String GetEnglishTokensSentimentMPQA (List<Token> tokens, LexiconProcessor lp) {
		String sentiment = "";
		int num_pos = 0;
		int num_neg = 0;
		for (Token t: tokens) {
			//String pos = t.morph_features.get("pos");
			String previous_text = "";
			int i = tokens.indexOf(t);
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.text_;
			}
			//pos = Tokenizer.ResolvePOS(pos);
			String subjectivity = Sentiment.GetEnglishSubjectivityMPQA(t, lp);
			String polarity = Sentiment.GetEnglishPolarityMPQA(t, lp);
			/*if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
				//System.out.println("Previous text:" + previous_text + " Current text:" + lemma);
				if (polarity.equals("positive")) {
					polarity = "negative";
				}
				else if (polarity.equals("negative")) {
					polarity = "positive";
				} else if (polarity.equals("na") || polarity.equals("neutral")) {
					polarity = "na";
				}
			}*/
			if (polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (polarity.equals("negative")) {
				num_neg +=1;
			} else {
				// do nothing
			}
		}
		if (num_pos > num_neg) {
				sentiment = "positive";
			}
			// bias towards majority baseline;
			else {
				sentiment = "negative";
			}
		return sentiment;
	}

	String GetTokensSentimentSifaat (List<Token> tokens, LexiconProcessor lp) {
		String sentiment = "";
		//int num_strongsubj = 0;
		//int num_weaksubj = 0;
		//int num_nosubj = 0;
		int num_pos = 0;
		int num_neg = 0;
		for (Token t: tokens) {
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				continue;
			}
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");
			String previous_text = "";
			int i = tokens.indexOf(t);
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.morph_features.get("WORD");
			}
			//pos = Tokenizer.ResolvePOS(pos);
			
			int pol = Sentiment.GetSentimentSifaat(t, lp);
			String polarity = "";
			if (pol == 1) {
				polarity = "positive";
			} else if (pol == 2) {
				polarity = "negative";
			} else {
				polarity = "neutral";
			}
			
			/*if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
				//System.out.println("Previous text:" + previous_text + " Current text:" + lemma);
				if (polarity.equals("positive")) {
					polarity = "negative";
				}
				else if (polarity.equals("negative")) {
					polarity = "positive";
				} else if (polarity.equals("neutral")) {
					polarity = "neutral";
				}
			}*/
		/*	if (subjectivity.equals("strongsubj") && polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (subjectivity.equals("strongsubj") && polarity.equals("negative")) {
				num_neg +=1;
			}*/
			if (polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (polarity.equals("negative")) {
				num_neg +=1;
			} else {
				// do nothing
			}
		}
			if (num_pos > num_neg) {
				sentiment = "positive";
			}
			// bias towards majority baseline;
			else {
				sentiment = "negative";
			}
		return sentiment;
	}
	
	
	String GetTokensSentimentArsenl (List<Token> tokens, LexiconProcessor lp) {
		String sentiment = "";
		//int num_strongsubj = 0;
		//int num_weaksubj = 0;
		//int num_nosubj = 0;
		int num_pos = 0;
		int num_neg = 0;
		for (Token t: tokens) {
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				continue;
			}
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");
			String previous_text = "";
			int i = tokens.indexOf(t);
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.morph_features.get("WORD");
			}
			//pos = Tokenizer.ResolvePOS(pos);
			
			String polarity = GetTokenSentimentCategoryArsenl(t, lp, 0.2);
		
			/*if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
				//System.out.println("Previous text:" + previous_text + " Current text:" + lemma);
				if (polarity.equals("positive")) {
					polarity = "negative";
				}
				else if (polarity.equals("negative")) {
					polarity = "positive";
				} else if (polarity.equals("neutral")) {
					polarity = "neutral";
				}
			}*/
		/*	if (subjectivity.equals("strongsubj") && polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (subjectivity.equals("strongsubj") && polarity.equals("negative")) {
				num_neg +=1;
			}*/
			if (polarity.equals("positive")) {
				num_pos +=1 ;
			}
			else if (polarity.equals("negative")) {
				num_neg +=1;
			} else {
				// do nothing
			}
		}
			if (num_pos > num_neg) {
				sentiment = "positive";
			}
			// bias towards majority baseline;
			else {
				sentiment = "negative";
			}
		return sentiment;
	}

	// Returns sentiment based on the sentiment of the token having
	// the highest sentiment score.
	// Considers negations
	// TODO: In future can return neutral and use that to filter out targets
	// and increase precision
	// Lexicon is hard-coded
	String GetTokensMaxSentiment (List<Token> tokens, LexiconProcessor lp) {
		String sentiment = "";
		double max_pos = 0; 
		double max_neg = 0;
		for (Token t: tokens) {
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				continue;
			}
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");
			String previous_text = "";
			int i = tokens.indexOf(t);
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.morph_features.get("WORD");
			}
			pos = Tokenizer.ResolvePOS(pos);
			// using slsa
			if (lp.HasSlsaKey(lemma, pos)) {
				double this_pos = lp.SlsaPositiveScore(lemma, pos);
				double this_neg = lp.SlsaNegativeScore(lemma, pos);
				double this_neut = (1- this_pos- this_neg);
				
				if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
					double temp = this_pos;
					this_pos = this_neg;
					this_neg = temp;
				}
				
				if (this_pos > max_pos) {
					max_pos = this_pos;
				}
				if (this_neg > max_neg) {
					max_neg = this_neg;
				}
			}
		}
		if (max_pos > max_neg) {
			//System.out.println("Tokens sentiment is positive");
			sentiment = "positive";
		}
		else if (max_neg > max_pos){
			//System.out.println("Tokens sentiment is negative");
			sentiment = "negative";
		}
		else {
			//System.out.println("Tokens sentiment is neutral. Returning negative");
			sentiment = "negative"; // undetermined
		}
		
		return sentiment;
	}

	// Returns sentiment category (positive or negative) of a list of tokens
	// based on score averages
	String GetTokensCategorySentiment (List<Token> tokens, LexiconProcessor lp, double threshold) {
			//Double[] scores = GetTokensSentiment(tokens,lp);
		Double[] scores = GetSelectiveTokensSentiment(tokens,lp, threshold);
			
			/*System.out.println("Selective Tokens Sentiment: Number of positive tokens: " + scores[0]);
		    System.out.println("Selective Tokens Sentiment: Number of negative tokens: " + scores[1]);
			System.out.println("Selective Tokens Sentiment: Number of neutral tokens: " + scores[2]);*/
			
			double pos_score = scores[0];
			double neg_score = scores[1];
			//double neut_score = scores[2];
		
			
			/*if (pos_score > threshold && pos_score > neg_score) {
				return "positive";
			}
			else if (neg_score > threshold && neg_score > pos_score) {
				return "negative";
			}
			else {
				return "neutral";
			}*/
			
			if (pos_score > neg_score) {
				//System.out.println("Tokens sentiment: positive");
				return "positive";
			}
			else if (neg_score > pos_score)  {
				//System.out.println("Tokens sentiment: negative");
				return "negative";
			}
			else {
				//System.out.println("Tokens sentiment: undetermined, considering negative");
				return "negative";
			}
			/*else {
				System.out.println("Tokens sentiment: undetermined");
				return "undetermined";
			}*/
		
			
			// Note remember there are no neutral targets
			/*else {
				return "neutral";
			}*/
			
			
	}
	
	// Returns mixed if both pos and neg scores are greater than threshold
	String GetTokensCategorySentimentWithMixed (List<Token> tokens, LexiconProcessor lp, double threshold) {
		//Double[] scores = GetTokensSentiment(tokens,lp);
		// This seems to work well at knowing the sentiment of the comment (pos,neg,neutral) with
		// threshold 0.1 ( based on visual inspection) but not of the targets (based on the results)
		Double[] scores = GetSelectiveTokensSentiment(tokens,lp, threshold);
		/*System.out.println("Selective Tokens Sentiment: Average positive for tokens: " + scores[0]);
	    System.out.println("Selective Tokens Sentiment: Average negative for tokens: " + scores[1]);
		System.out.println("Selective Tokens Sentiment: Average neutral for tokens: " + scores[2]);*/
		double pos_score = scores[0];
		double neg_score = scores[1];
		double neut_score = scores[2];
		if (pos_score > threshold && neg_score > threshold) {
			return "mixed";
		}
		else if (pos_score > threshold) {
			return "positive";
		}
		else if (neg_score > threshold) {
			return "negative";
		}
		else {
			return "neutral";
		}
}
	
	// Returns average scores for positive, negative, neutral across tokens
	// Only averages the positive/negative score of positive/negative sentiment words
	// instead of all
	// Threshold determines whether a word is positive or negative, otherwise it is neutral
	// If its both positive and negative, the max is taken
	Double[] GetSelectiveTokensSentiment (List<Token> tokens, LexiconProcessor lp, double threshold) {
				double avg_pos = 0;
				double avg_neg = 0;
				double avg_neut = 0;
				double num_pos = 0;
				double num_neg = 0;
				double num_neut = 0;
				double tot=0;
		
				for (Token t: tokens) {
					if (t.morph_features.containsKey("NO_ANALYSIS")) {
						continue;
					}
					String lemma = t.morph_features.get("lex");
					String pos = t.morph_features.get("pos");
					String previous_text = "";
					int i = tokens.indexOf(t);
					if (i > 1) {
						Token previous_token = tokens.get(i-1);
						previous_text = previous_token.morph_features.get("WORD");
					}
					pos = Tokenizer.ResolvePOS(pos);
					if (!lp.HasSlsaKey(lemma, pos)) {
						//System.out.println("Slsa Key not found!\n");
						// System.out.println("\nLemma:"+ lemma );
						// System.out.println("\nPOS:"+ pos );
					}
					else if (!pos.equals("STOP")  && !pos.equals("NEG")) {
						double this_pos = lp.SlsaPositiveScore(lemma, pos);
						double this_neg = lp.SlsaNegativeScore(lemma, pos);
						double this_neut = (1- this_pos- this_neg);
						// If the word is preceded by a negation, switch the positive
						// and negative scores (e.g good (0.9,0.1) -> not good becomes (0.1,0.9))
						if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
							double temp = this_pos;
							this_pos = this_neg;
							this_neg = temp;
						}
						if (this_pos > threshold && this_pos > this_neg) {
							num_pos+=1;
							avg_pos += this_pos;
						}
						else if (this_neg > threshold && this_neg > this_pos) {
							num_neg +=1;
							avg_neg += this_neg;
						}
						else {
							avg_neut += this_neut;
							num_neut +=1;
						}
						tot +=1;
					}
				}
				if (num_pos!=0) { avg_pos = avg_pos/num_pos; }
				if (num_neg!=0) { avg_neg = avg_neg/num_neg; }
				if (num_neut!=0){  avg_neut = avg_neut/num_neut; }
				
				return new Double[]{avg_pos,avg_neg,avg_neut};	
				//return new Double[]{num_pos,num_neg,num_neut};
	}	
	
	
	// Returns average of positive, negative, and neutral scores for all tokens
	// matching the lexicon
	Double[] GetTokensSentiment (List<Token> tokens, LexiconProcessor lp) {
		
				double avg_pos = 0;
				double avg_neg = 0;
				double avg_neut = 0;
				double tot=0;
		
				for (Token t: tokens) {
					if (t.morph_features.containsKey("NO_ANALYSIS")) {
						continue;
					}
					String lemma = t.morph_features.get("lex");
					String pos = t.morph_features.get("pos");
					pos = Tokenizer.ResolvePOS(pos);
					if (!lp.HasSlsaKey(lemma, pos)) {
						//System.out.println("Slsa Key not found!\n");
						// System.out.println("\nLemma:"+ lemma );
						// System.out.println("\nPOS:"+ pos );
					}
					else if (!pos.equals("STOP")  && !pos.equals("NEG")) {
						//System.out.println("Slsa Key found!\n");
						double this_pos = lp.SlsaPositiveScore(lemma, pos);
						double this_neg = lp.SlsaNegativeScore(lemma, pos);
						avg_pos += this_pos;
						avg_neg += this_neg;
						avg_neut += (1- this_pos- this_neg);
						tot +=1;
					}
				}
				avg_pos = avg_pos/tot;
				avg_neg = avg_neg/tot;
				avg_neut = avg_neut/tot;
				
				return new Double[]{avg_pos,avg_neg,avg_neut};
				
		}
	
	
	// Currently, t holds the lemma and pos from Madamira
		// threshold applies to positive score and negative score
		// Returns mixed, positive, negative, neutral, or none
		// Ideally use threshold of 0.1-0.3
		String GetTokenSentimentCategoryArsenl(Token t, LexiconProcessor lp, double threshold) {
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");

			
			if (!lp.HasArsenlKey(lemma)) {
				//System.out.println("Arsenl Key not found!\n");
				System.out.println("\nLemma:"+ lemma );
				System.out.println("\nPOS:"+ pos );
				return "na";
			}
			
			//System.out.println("Slsa Key found!\n");
			double pos_score = lp.ArsenlPositiveScore(lemma);
			double neg_score = lp.ArsenlNegativeScore(lemma);
			// double neut_score = 1 - pos_score - neg_score;
			
			if (pos_score > neg_score && pos_score > threshold) {
				return "positive";
			}
			else if (neg_score > pos_score && neg_score > threshold) {
				return "negative";
			}
			else {
				return "neutral";
			}
		}
	
	// Returns the category sentiment (positive or negative)
	// of a single token based on its lexicon score
	//
	// should I get pos from Madamira or Stanford parser? 
	// parser seems to be good but use madamira for now easier
	// madamira also might be better because
	// more consistent with lexicon tags
	//
	// Currently, t holds the lemma and pos from Madamira
	// threshold applies to positive score and negative score
	// Returns mixed, positive, negative, neutral, or none
	// Ideally use threshold of 0.1-0.3
	String GetTokenSentimentCategory(Token t, LexiconProcessor lp, double threshold) {
		String lemma = t.morph_features.get("lex");
		String pos = t.morph_features.get("pos");
		pos = Tokenizer.ResolvePOS(pos);
		
		// stop word
		if (pos.equals("STOP")) {
			return "STOP";
		}
		if (pos.equals("NEG")) {
			return "NEG";
		}
		
		if (!lp.HasSlsaKey(lemma, pos)) {
			System.out.println("Slsa Key not found!\n");
			System.out.println("\nLemma:"+ lemma );
			System.out.println("\nPOS:"+ pos );
			return "none";
		}
		
		System.out.println("Slsa Key found!\n");
		double pos_score = lp.SlsaPositiveScore(lemma, pos);
		double neg_score = lp.SlsaNegativeScore(lemma, pos);
		// double neut_score = 1 - pos_score - neg_score;
		
		if (pos_score > threshold && neg_score > threshold) {
			return "mixed";
		}
		else if (pos_score > threshold) {
			return "positive";
		}
		else if (neg_score > threshold) {
			return "negative";
		}
		else {
			return "neutral";
		}
		
		
	}

	String GetSentenceSentiment (String text, LexiconProcessor lp) {
		return "positive";
	}
	
	// Modifies output_comments by updating the sentiment of the targets.
	// Breaks the comments into sentences and finds the sentiment at the 
	// sentence level; applies it to the targets
	// Deprecated. Use 'UpdateSentimentForTargetsForTargetsSentenceLevel'
	List<Comment> UpdateSentimentSentenceLevel(List<Comment> output_comments, List<Comment> mada_comments,
			LexiconProcessor lp) {
		
				int i=0;
				for (Comment c: output_comments) {
					String id = c.comment_id_;
					String text = c.raw_text_;
					Comment mada_comment = mada_comments.get(i);
					if (!id.equals(mada_comment.comment_id_)) {
						// Note check parenthesis are removed
						System.out.println("Comment ids not consistent! Exiting \n");
						System.out.println("Mada id: " + mada_comment.comment_id_);
						System.out.println("Input comment id: " + id);
						System.exit(0);
					}
					assert(mada_comment.tokens_.size() == c.tokens_.size());
					if (c.tokens_ == null) {
						System.out.println("Output comment has no tokens! Exiting \n");
						System.exit(0);
					}
					else if (c.tokens_.isEmpty()) {
						System.out.println("Output comment has empty tokens! Exiting \n");
						System.exit(0);
					}
					else {
						System.out.println("Size of c tokens: " + c.tokens_.size());
						System.out.println("Size of mada tokens: " + mada_comment.tokens_.size());
					}
					
		
					 System.out.println("Comment:" + 
					 BuckwalterConverter.ConvertToUTF8(mada_comment.raw_text_) + "\n");
					
					// Now get the sentiment for tokens at the sentence (clause actually) level
					List<Token> sentence_tokens = new ArrayList<Token>();
					String sentence_text = "";
					int j=0;
					
					
					for (Token mt: mada_comment.tokens_) {
					 
					//String mada_word = mt.morph_features.get("WORD");
					Token output_token = c.tokens_.get(j);
					String word = output_token.text_;
					if (word.equals(".") || word.equals("!") || word.equals("?") || word.equals(",")) {
						
						System.out.println("\nSentence:" + 
								BuckwalterConverter.ConvertToUTF8(sentence_text));
						String sentiment = GetTokensCategorySentiment(sentence_tokens, lp, 0.0);
						
						System.out.println("Average Sentiment:" + sentiment + "\n");
						
						for (Target t: c.targets_ ) 
						
						{
							if (sentence_text.contains(t.text_))
								
							 {
								t.SetSentiment(sentiment);
								System.out.println("\nTarget: " + 
								 BuckwalterConverter.ConvertToUTF8(t.text_)  + " Sentiment:" + sentiment);
							 }
							
						}
					
						sentence_tokens = new ArrayList<Token>();
						sentence_text = "";
					}
					else {
						sentence_tokens.add(mt);
						sentence_text += " " + word;
						sentence_text = sentence_text.trim();
					}
					
				j+=1;	
				} // end for Token t	
					
			i+=1;
					
			} // end for comment c
				
			return output_comments;
	}
	
	// Updates sentiment at sentence level
	// Assumes that output comments have been detokenized and contain mada tokens
	// Deprecated. Use 'UpdateSentimentForTargetsForTargetsSentenceLevel'
	public List<Comment> UpdateSentimentForTargetsSentenceLevelOld(List<Comment> mada_comments, 
			LexiconProcessor lp) {
		
				for (Comment c: mada_comments) {
					String text = c.raw_text_;
				
					if (c.tokens_ == null) {
						System.out.println("Output comment has no tokens! Exiting \n");
						System.exit(0);
					}
					else if (c.tokens_.isEmpty()) {
						System.out.println("Output comment has empty tokens! Exiting \n");
						System.exit(0);
					}
					
					 // System.out.println("\nComment:" + text + "\n");
					// System.out.println("Comment:" + 
					 //BuckwalterConverter.ConvertToUTF8(c.raw_text_) + "\n");
					
					// Now get the sentiment for tokens at the sentence (clause actually) level
					List<Token> sentence_tokens = new ArrayList<Token>();
					String sentence_text = "";
					int j=0;
					
					// First, set Comment level sentiment for all targets
					for (Target t: c.targets_) {
						String sentiment = GetTokensCategorySentiment(c.tokens_, lp,0.0);
						t.SetSentiment(sentiment);
					}
					
					
					for (Token mt: c.tokens_) {
					 
					
					String word = mt.text_;
					
					
					if (word.equals(".") || word.equals("!") || word.equals("?") || word.equals(",")) {
						
						
						//System.out.println("\nWord:" + word);
						//System.out.println("Sentence:" + 
							//	BuckwalterConverter.ConvertToUTF8(sentence_text));
						if (sentence_text.isEmpty()) {
							continue;
						}
						String sentiment = GetTokensCategorySentiment(sentence_tokens, lp, 0.0);
						
						//System.out.println("Average Sentiment:" + sentiment + "\n");
						
						int start_offset = sentence_tokens.get(0).comment_offset_;
						int end_offset = sentence_tokens.get(sentence_tokens.size()-1).comment_offset_;
						
						for (Target t: c.targets_ ) 
						
						{
							int target_start = t.comment_offsets_.get(0);
							int target_end =  t.comment_offsets_.get(0) + t.tokens_.size() - 1;
							if (sentence_text.contains(t.text_)
									|| (sentence_text + " " + word).contains(t.text_)
									&& target_start >= start_offset
									&& (target_end <= end_offset))
								
							 {
								t.SetSentiment(sentiment);
								/*System.out.println("Sentence text contains target!");
								System.out.println("Target offsets: " + target_start + ","
										+ target_end );
								System.out.println("Sentence offsets: " + start_offset + ","
										+ end_offset );
								System.out.println("Sentence text: " +
										BuckwalterConverter.ConvertToUTF8(sentence_text));
								System.out.println("Target: " + 
								BuckwalterConverter.ConvertToUTF8(t.text_)  + " Sentiment:" + sentiment);*/
							 } 
							/*else { // can set it to comment sentiment then
								System.out.println("Sentence text doesn't contain target!");
								System.out.println("Sentence text: " +
										BuckwalterConverter.ConvertToUTF8(sentence_text));
								System.out.println("Target text:" + 
										BuckwalterConverter.ConvertToUTF8(t.text_));
							}*/
							
						}
					
						sentence_tokens = new ArrayList<Token>();
						sentence_text = "";
					}
					else {
						sentence_tokens.add(mt);
						sentence_text += " " + word;
						sentence_text = sentence_text.trim();
					}
					
				j+=1;	
				} // end for Token t	
					
		
					
			} // end for comment c
				
			return mada_comments;
	}
	 
	
	// Restore original tokenization of comments by matching with madamira comments
	// Deprecated. Using 'RestoreFromATB' now.
    public List<Comment> DetokenizeATBComments (List<Comment> ATB_comments,
				List<Comment> mada_tokenized_comments, List<Comment> mada_original_comments) {
		
				int i = 0;
				for (Comment c: ATB_comments) {
					
					Comment mada_comment = mada_tokenized_comments.get(i);
					//Comment new_comment = new Comment();
					assert(c.comment_id_.equals(mada_comment.comment_id_));
				
					c.SetText(BuckwalterConverter.ConvertToBuckwalter(c.raw_text_));
					
	
					if (c.targets_.isEmpty()) { 
						System.out.println("Hey, BTW, This comment has no targets. "
								+ "Be careful that this is not the case for all comments \n");
					}
					else {
						List<Token> mada_tokens = mada_comment.tokens_;
						if (mada_tokens.isEmpty() || mada_tokens == null) {
							System.out.println("Mada comments not tokenized. Please tokenize "
									+ "Exiting\n");
							System.exit(0);
						}
						String new_text = "";
						List<Token> new_tokens = new ArrayList<Token>();
						
							for (int j=0; j<c.tokens_.size(); j++) {
								
								Token tok = c.tokens_.get(j);
								tok.text_ = BuckwalterConverter.ConvertToBuckwalter(tok.text_);
								int off = tok.comment_offset_;
								Token m = mada_comment.tokens_.get(off);
								String m_text = BuckwalterConverter.ConvertToBuckwalter(m.text_);
								String new_tok = "";
								// System.out.println("Mada Target Token:" + m.text_);
								// System.out.println("Output Target Token:" + tok.text_ );
								/*case ktb +h*/
								if (j!=(new_tokens.size()-1) && off!=(mada_comment.tokens_.size()-1)
										&& mada_comment.tokens_.get(off+1).text_.startsWith("+")) {
									
									if (mada_comment.tokens_.get(off+1).text_.equals("mA")){
										continue;
									}
									
									if  (tok.text_.endsWith("p")) {
										tok.text_ = tok.text_.replace("p","t");
									}
									new_tok = tok.text_ + c.tokens_.get(j+1).text_; 
									
									// hack
									new_tok = new_tok.replace("EndmAmA", "EndmA");
									new_tok = new_tok.replace("AnmAmA", "AnmA");
									new_tok = new_tok.replace("mnmA", "mmA");
									
									
									new_text += " " + new_tok + " ";
									
									Token new_token = new Token(new_tok,"word");
									new_tokens.add(new_token);
									j+=1;		
								}
							
								else if (m_text.endsWith("+") && j!=(c.tokens_.size()-1)) {
									
									// Note: ATB normalizes everything so all Al's are normalized at this point
									if (m_text.equals("l+") && c.tokens_.get(j+1).text_.startsWith("Al")) {
										c.tokens_.get(j+1).text_ 
										= c.tokens_.get(j+1).text_.replaceFirst("Al", "l");
									}
									
									new_tok = tok.text_ + c.tokens_.get(j+1).text_; 
									// System.out.println("New token: " + new_tok);
									
									/* Case: w+ k+ thlk*/ 
									if (mada_comment.tokens_.get(off+1).text_.endsWith("+") 
											&& j!=(c.tokens_.size()-2))  {
										
										String m_next_text = 
												BuckwalterConverter.ConvertToUTF8(mada_comment.tokens_.get(off+1).text_);
										
										if (m_next_text.equals("l+") && c.tokens_.get(j+2).text_.startsWith("Al")) {
											c.tokens_.get(j+2).text_ 
											= c.tokens_.get(j+2).text_.replaceFirst("Al", "l");
										}
											new_tok += c.tokens_.get(j+2).text_;
											j+=1;
											}
									/* Case: l+ ktb +h*/ //b+ ibdAt +h
									else if ( j!=(c.tokens_.size()-2)
											&& off!=(mada_comment.tokens_.size()-2)
											&& mada_comment.tokens_.get(off+2).text_.startsWith("+") ) {
										
										 if (c.tokens_.get(j+1).text_.endsWith("p")) {
											 new_tok = new_tok.replace("p", "t");
										 }
											 
											new_tok += c.tokens_.get(j+2).text_;
											j+=1;
											}
									// System.out.println("New token: " + new_tok);
									new_text += " " + new_tok + " ";
									Token new_token = new Token(new_tok,"word");
									new_tokens.add(new_token);
									j+=1;
								}
								else{
									new_tokens.add(tok);
									new_text += " " + tok.text_ + " ";
								
								}
								
							}// end for token
						
							new_text = Tokenizer.ProcessATBBraces(new_text);
							
							new_text = new_text.trim();
							new_text = Tokenizer.RemoveExtraWhiteSpace(new_text);
							c.SetText(new_text);
							c.SetTokens(new_tokens);
							
					}
					
					i+=1;
				}
		
				
				return ATB_comments;
		}
	
    // Deprecated. Now using 'RestoreFromATB'.
	// Restore original tokenization by matching with madamira comments
	// assumed at this point they are both in UTF8
	// Used for output noun phrases from Stanford parser which are in ATB format
    // Converts the output comments to buckwalter (does not alter mada comments)
	// Restores special cases where tokens end with Ta Marbuta or start with l+Al
	// Restores braces and removes any foreign word markers (should?)
	public List<Comment> DetokenizeATBTargets (List<Comment> comments_with_targets,
			List<Comment> mada_tokenized_comments, List<Comment> mada_original_comments) {
	
			List<Comment> output = new ArrayList<Comment>();
			int i = 0;
			for (Comment c: comments_with_targets) {
				
				Comment mada_comment = mada_tokenized_comments.get(i);
				Comment new_comment = new Comment();
				assert(c.comment_id_.equals(mada_comment.comment_id_));
			
				new_comment.SetCommentID(c.comment_id_);
				new_comment.SetText(BuckwalterConverter.ConvertToBuckwalter(c.raw_text_));
				
				// System.out.println("Detokenize targets: checking produced targets \n");
				// System.out.println("Detokenize targets: number of targets in c: " + c.targets_.size());
	
				
				if (c.targets_.isEmpty()) { //c.targets_.isEmpty() ||
					System.out.println("Hey, BTW, This comment has no targets. "
							+ "Be careful that this is not the case for all comments \n");
				}
				else {
					List<Token> mada_tokens = mada_comment.tokens_;
					if (mada_tokens.isEmpty() || mada_tokens == null) {
						System.out.println("Mada comments not tokenized. Please tokenize "
								+ "Exiting\n");
						System.exit(0);
					}
					for (Target t: c.targets_ ) {
						String new_target = "";
						List<Token> new_tokens = new ArrayList<Token>();
						List<Token> target_tokens = t.tokens_;
						if (target_tokens.isEmpty() || target_tokens == null) {
							System.out.println("Comment id:" + c.comment_id_);
							System.out.println("Target: " + t.text_);
							System.out.println("Target has no tokens. Cannot detokenize. Exiting \n");
							System.exit(0);
						}
						// Convert to Buckwalter
						for (Token to: target_tokens) {
							to.text_ = BuckwalterConverter.ConvertToBuckwalter(to.text_);
						}
		
						for (int j=0; j<target_tokens.size(); j++) {
							Token tok = target_tokens.get(j);
							int off = tok.comment_offset_;
							Token m = mada_comment.tokens_.get(off);
							String m_text = BuckwalterConverter.ConvertToBuckwalter(m.text_);
							String new_tok = "";
							// System.out.println("Mada Target Token:" + m.text_);
							// System.out.println("Output Target Token:" + tok.text_ );
							/*case ktb +h*/
							if (j!=(target_tokens.size()-1) && off!=(mada_comment.tokens_.size()-1)
									&& mada_comment.tokens_.get(off+1).text_.startsWith("+")) {
								
								if (mada_comment.tokens_.get(off+1).text_.equals("mA")){
									continue;
								}
								
								if  (tok.text_.endsWith("p")) {
									tok.text_ = tok.text_.replace("p","t");
								}
								new_tok = tok.text_ + target_tokens.get(j+1).text_; 
								
								// hack
								new_tok = new_tok.replace("EndmAmA", "EndmA");
								new_tok = new_tok.replace("AnmAmA", "AnmA");
								new_tok = new_tok.replace("mnmA", "mmA");
								
								
								new_target += " " + new_tok + " ";
								
								// System.out.println("New target: " +
								 // BuckwalterConverter.ConvertToUTF8(new_target));
								Token new_token = new Token(new_tok,"word");
								new_tokens.add(new_token);
								j+=1;		
							}
						
							else if (m_text.endsWith("+") && j!=(target_tokens.size()-1)) {
								
								// Note: ATB normalizes everything so all Al's are normalized at this point
								if (m_text.equals("l+") && target_tokens.get(j+1).text_.startsWith("Al")) {
									target_tokens.get(j+1).text_ 
									= target_tokens.get(j+1).text_.replaceFirst("Al", "l");
								}
								
								new_tok = tok.text_ + target_tokens.get(j+1).text_; 
								// System.out.println("New token: " + new_tok);
								
								/* Case: w+ k+ thlk*/ 
								if (mada_comment.tokens_.get(off+1).text_.endsWith("+") 
										&& j!=(target_tokens.size()-2))  {
									
									String m_next_text = 
											BuckwalterConverter.ConvertToUTF8(mada_comment.tokens_.get(off+1).text_);
									
									if (m_next_text.equals("l+") && target_tokens.get(j+2).text_.startsWith("Al")) {
										target_tokens.get(j+2).text_ 
										= target_tokens.get(j+2).text_.replaceFirst("Al", "l");
									}
										new_tok += target_tokens.get(j+2).text_;
										j+=1;
										}
								/* Case: l+ ktb +h*/ //b+ ibdAt +h
								else if ( j!=(target_tokens.size()-2)
										&& off!=(mada_comment.tokens_.size()-2)
										&& mada_comment.tokens_.get(off+2).text_.startsWith("+") ) {
									
									 if (target_tokens.get(j+1).text_.endsWith("p")) {
										 new_tok = new_tok.replace("p", "t");
									 }
										 
										new_tok += target_tokens.get(j+2).text_;
										j+=1;
										}
								// System.out.println("New token: " + new_tok);
								new_target += " " + new_tok + " ";
							    // System.out.println("New target: " + BuckwalterConverter.ConvertToUTF8(new_target));
								Token new_token = new Token(new_tok,"word");
								new_tokens.add(new_token);
								j+=1;
							}
							else{
								new_tokens.add(tok);
								new_target += " " + tok.text_ + " ";
								// System.out.println("New target: " + BuckwalterConverter.ConvertToUTF8(new_target));
							}
							
						// eventually need to make consistent and convert the whole comment to BW
						// better may be to get the new targets with new tokens and then
						// match with the original input
						// new_target = BuckwalterConverter.ConvertToBuckwalter(new_target);
						// new_target = new_target.replaceAll("lAl", "ll"); // not a good idea
						// new_target = new_target.replaceAll("ph", "th");
						new_target = Tokenizer.ProcessATBBraces(new_target);
						
						new_target = new_target.trim();
						new_target = Tokenizer.RemoveExtraWhiteSpace(new_target);
						new_comment.AddTarget(new Target(new_target, new_tokens));
					
						}
						
					}		
				}
				new_comment.SetTokens(c.tokens_);
				output.add(new_comment);
				i+=1;
			}
			
			// Now match with original input
			/*for (Comment mada_original : mada_original_comments) {
				int j = output.indexOf(mada_original);
				Comment output_comment = output.get(j);
				assert(output_comment.comment_id_ == mada_original.comment_id_);
				
				for (Target t: output_comment.targets_) {
					
				}
					
			}*/
			
			return output;
	}
	
}
