/**
 * 
 */
package models;

import data.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Constants;
import models.baselines.ALL_NP;
import models.ExternalCRF;

import java.io.File;

import processor.DeftEntityReader;
import processor.InputReader;
import processor.LexiconProcessor;
import processor.ToolsProcessor.MadamiraProcessor;
import processor.ToolsProcessor.StanfordCoreNLPProcessor;
import processor.ToolsProcessor.StanfordPOSTagger;
import processor.ToolsProcessor.DependencyProcessor;
import topicmodeling.CoreferenceFeatures;
import topicmodeling.LdaComments;
import topicmodeling.TopicSalience;
import util.BuckwalterConverter;
import util.FileReader;
import util.FileWriter;
import util.Tokenizer;


/**
 * @author noura
 *
 */
public class DeftRunner extends Runner{

	/**
	 * Similar to Runner, but for DEFT/English data
	 */
	public static boolean tokenized_space = false;
	public static String tok_option = "";
	public static String base_input_file = "DEFT_out";
	public static String language_directory = "";
	//public String cluster_file = "../WordClusters/English/KMeansClusters500.pickle";
	public boolean predict_mentions;
	boolean LDA_from_train = false;
	public DeftRunner() {
	}
	public void SetTestFile (String test_file) {
		this.test_file = test_file;
	}
	public void SetPredictMentions (boolean predict_mentions) {
		this.predict_mentions = predict_mentions;
	}
	
	// Train model
	public void TrainDEFT(List<Comment> train_comments, String model_type, boolean use_existing_parse,
				String output_directory) {
			
			if (train_comments.isEmpty()) {
				System.out.println("Runner: Empty train comments. Exiting \n");
				System.exit(0);
			}
			DeftFeatureExtractor fe = new DeftFeatureExtractor();
			fe = DEFTSetUpFeatures(language_directory, base_input_file,
					lexicon_file, train_comments, false);
			String file_path = new File(output_directory, base_input_file).getAbsolutePath();
			if (model_type.equals("CRF-target")) {
				fe.SetLabelType("target");
				DeftExternalCRF TargetCRF = new DeftExternalCRF(fe);
				TargetCRF.SetOutputDir(output_directory);
				TargetCRF.DEFTWriteFeatureFile("train", file_path);
				// this function is not doing anything right now. We're just writing the feature file.
				TargetCRF.TrainFromFiles(file_path); 
			}
			else if (model_type.equals("CRF-sentiment")) {
				System.out.println("Setting up feature extractor for"
						+ " sentiment model\n");
				fe.SetLabelType("sentiment");
				DeftExternalCRF SentimentCRF = new DeftExternalCRF(fe);
				SentimentCRF.SetOutputDir(output_directory);
				SentimentCRF.DEFTWriteFeatureFile("train", file_path);
				SentimentCRF.TrainFromFiles(file_path);
			}
			else if (model_type.equals("CRF-pipeline")) {
			}
			else if (model_type.equals("CRF-target+sentiment")) {
				System.out.println("Setting up feature extractor for"
						+ "target+sentiment model\n");
				fe.SetLabelType("target+sentiment");
				DeftExternalCRF TargetCRF = new DeftExternalCRF(fe);
				TargetCRF.SetOutputDir(output_directory);
				TargetCRF.DEFTWriteFeatureFile("train", file_path);
				TargetCRF.TrainFromFiles(file_path);
			}
		}
		
	public List<Comment> RunDEFT(List<Comment> input_comments,
				String model_type, boolean use_existing_parse, String output_dir,
				String model_file) {
			if (input_comments.isEmpty()) {
				System.out.println("Runner: Empty input comments. Exiting \n");
				System.exit(0);
			}
			
			//System.out.println("RUNNING DEFT: INPUT COMMENTS:");
			/*for (Comment c: input_comments) {
				 System.out.println("\nComment:" + c.comment_id_ + "\n" + c.raw_text_);
				 System.out.println("Targets:");
				 for (Target t: c.targets_) {
					 System.out.println(t.text_ + ":" + t.sentiment_);
				 }
			 }*/
			List<Comment> output_comments = new ArrayList<Comment>();
			String file_path = new File(output_dir, base_input_file).getAbsolutePath();
			DeftFeatureExtractor fe = new DeftFeatureExtractor();
			fe = DEFTSetUpFeatures(language_directory, base_input_file, lexicon_file,
					input_comments, true);
			/*if (!model_type.equals("ALL_NP")) {
				fe = DEFTSetUpFeatures(language_directory, base_input_file, lexicon_file,
						input_comments, true);
			}*/
			
			// removed comments with empty tokens
			input_comments = fe.input_comments;
			
			if (model_type.equals("ALL_NP")) {
				ALL_NP NP_baseline_runner = new ALL_NP();
				NP_baseline_runner.SetInputFile(base_input_file);
				// left static madamira directory although it's not needed
				output_comments = NP_baseline_runner.Run(input_comments, 
						madamira_directory, lexicon_file, use_existing_parse);
			}
			else if (model_type.equals("CRF-target")) {
				fe.SetLabelType("target");
				DeftExternalCRF TargetCRF = new DeftExternalCRF(model_file);
				TargetCRF.SetFeatureExtractor(fe);
				test_file = new File(output_dir, this.test_file).getAbsolutePath();
				System.out.println("Test file:" + test_file);
				/*System.out.println("DeftRunner: Before test from file: Printing input comments:");
				for (Comment c: input_comments) {
					 System.out.println("\nComment:" + c.comment_id_ + "\n" + c.raw_text_);
					 System.out.println("Targets:");
					 for (Target t: c.targets_) {
						 System.out.println(t.text_ + ":" + t.sentiment_);
					 }
				 }*/
				//System.exit(0);
                output_comments = TargetCRF.TestFromFileDEFT(test_file,
						input_comments, tokenized_space);
				//output_comments.addAll(input_comments);
				System.out.println("DEFT Test file:" + test_file);
				System.out.println("Read DEFT file");
				
				// Evaluate sentiment using a baseline method
				System.out.println("Evaluating sentiment of targets\n");
				ALL_NP baseline_sentiment_predictor = new ALL_NP();
				LexiconProcessor MPQAProcessor = new LexiconProcessor();
				MPQAProcessor.ReadMPQA(MPQAProcessor.lexicon_files.get("MPQA"));
				output_comments = baseline_sentiment_predictor.
						UpdateEnglishSentimentForTargetsSentenceLevel(output_comments, MPQAProcessor);
			}
			else if (model_type.equals("CRF-sentiment")) {
				fe.SetLabelType("sentiment"); 
				DeftExternalCRF TargetCRF = new DeftExternalCRF(model_file);
				TargetCRF.SetFeatureExtractor(fe);
				String sentiment_test_file = new File(output_dir, this.test_file).getAbsolutePath();
				output_comments = TargetCRF.TestTargetsAndSentimentFromFileDeft(sentiment_test_file,
						input_comments, tokenized_space);
			}
			else if (model_type.equals("CRF-pipeline")) {
				System.out.println("Running target model\n");
				System.out.println("Size of input comments:" + input_comments.size());
				fe.SetLabelType("target");
				DeftExternalCRF TargetCRF = new DeftExternalCRF(model_file);
				TargetCRF.SetFeatureExtractor(fe); 
				System.out.println("Fe label type:" + fe.label_type);
				test_file = new File(output_dir, test_file).getAbsolutePath();
				output_comments = TargetCRF.TestFromFileDEFT(test_file,
						input_comments, tokenized_space);
				if (output_comments.isEmpty()) {
					System.out.println("Output comments empty");
					System.exit(0);
				}
				
				// boost targets in output here
				/*if (increase_training) {
					IncreaseTrainingTargets increase_output_coreference = new IncreaseTrainingTargets();
					//feature_comments = boost_lemmas_coreference.IncreaseTrainingByLemma(feature_comments, update_clitics);
					output_comments = UpdateCoreference(output_comments, use_existing_parse);
					output_comments = 
				 increase_output_coreference.IncreaseTrainingByCoreference(output_comments, replace_morph_features);
				}*/
				
				//System.out.println("Running sentiment model\n");
				fe.SetInput(output_comments);
				fe.SetNontokenized(output_comments);
				fe.SetLabelType("sentiment"); // just writing features
				fe.SetPipelineModel(true); // for creating sentiment field when undetermined
				//System.out.println("Fe label type:" + fe.label_type);
				DeftExternalCRF SentimentCRF = new DeftExternalCRF(model_file);
				SentimentCRF.SetFeatureExtractor(fe);
				SentimentCRF.SetOutputDir(output_dir);
				SentimentCRF.DEFTWriteFeatureFile("dev", file_path);
				//SentimentCRF.WriteFeatureFile("test", file_path);
			}
			// new
			else if (model_type.equals("CRF-target+sentiment")) {
				System.out.println("Test file:" + this.test_file);
				fe.SetLabelType("target+sentiment");
				DeftExternalCRF TargetCRF = new DeftExternalCRF(model_file);
				TargetCRF.SetFeatureExtractor(fe); // NOTE not re-extracting features here
				test_file = new File(output_dir, this.test_file).getAbsolutePath();
				System.out.println("Test file is:" + this.test_file);
				output_comments = TargetCRF.TestTargetsAndSentimentFromFileDEFT(this.test_file,
						input_comments, tokenized_space);
			}

			// For each output comment, for each target, set source to be the author of the comment
			MarkSourceAsAuthor(output_comments);
			//MarkTargetAndSourceIDsNew(output_comments,input_comments);
			MarkTargetAndSourceIDs(output_comments,input_comments);
			//MarkNonTargetEntities(output_comments,input_comments);
			return output_comments;
		}

	public DeftFeatureExtractor DEFTSetUpFeatures (String lang_directory, 
				String base_input_file, String lexicon_file, 
				List<Comment> input_comments, boolean eval) {
			
			if (input_comments.isEmpty() && lang_directory.isEmpty()) {
				System.out.println("Runner: Empty input comments and no madamira "
						+ "comments specified.. Exiting \n");
				System.exit(0);
			}
			 List<Comment> feature_comments = new ArrayList<Comment>();
			 for (Comment c: input_comments) {
				 if (c.tokens_.size()!=0 && !c.tokens_.isEmpty()){
					 feature_comments.add(c);
				 }
			 }
			 // Boost training data
			 if (increase_training) {
				  // IncreaseTrainingTargets boost_word = new IncreaseTrainingTargets();
				  // feature_comments = boost_word.IncreaseTrainingByWordMatch(feature_comments);
				 CoreferenceFeatures.AnnotateDeftCoreference(feature_comments);
				 IncreaseTrainingTargets boost_coreference = new IncreaseTrainingTargets();
				 feature_comments = boost_coreference.IncreaseTrainingByCoreference(feature_comments, false);
			 }
			 if (binary_features.contains("Salience")) {
				 System.out.println("Updating salience and topic signatures\n");
				 HashMap<String,Integer> corpus_counts = 
						 TopicSalience.GetCorpusCounts(feature_comments);
				 for (Comment c: feature_comments) {
					TopicSalience.UpdateWordProbs(c);
					//TopicSalience.UpdateTFIDF(c, feature_comments, corpus_counts);
					TopicSalience.UpdateTopicSignatures(c, feature_comments);
				 }
				}
			
			 DeftFeatureExtractor fe = new DeftFeatureExtractor(feature_comments);
			 fe.SetInput(feature_comments);
			 fe.SetNontokenized(feature_comments);
			 fe.SetBinaryFeatures(binary_features);
			 fe.SetContinuousFeatures(continuous_features);
			 fe.SetWordClusters(word_cluster_file);
			 
			// Set up topic modeling features 
			LdaComments comment_model = new LdaComments();
			if (LDA_from_train) {
					/*String train_file = base_input_file;
					train_file = train_file.replace("dev", "train");
					train_file = train_file.replace("test", "train");
					List<Comment> train_comments = InputReader.ReadCommentsFromXML(train_file, "utf8ar", true);*/
					// maybe read topic model from a file.
			} else {
				fe.SetTopicModel(comment_model, null);
			}
			fe.SetLexiconProcessor(new LexiconProcessor());
			
			return fe;
		}
		
		public void MarkSourceAsAuthor(List<Comment> output_comments) {
			for (Comment c: output_comments) {
				String author = c.author; // will be none if not reset
				Integer author_offset = c.author_offset;
				String author_id = ""; 
				String author_length = "";
				String string_author_offset = "";
				DeftEntityReader er = c.entity_reader;
				List<Target> targets = c.targets_;
				List<Target> output_targets = new ArrayList<Target>();
				HashMap<String,String[]> entity_mentions = er.entity_mentions;
				for (Target t: targets) {
					DeftTarget dt = new DeftTarget(t.sentiment_,t.text_);
					dt.SetTokens(t.tokens_);
					// Mark author of quotes
					for (Token tok: dt.tokens_) { 
						DeftToken dtok = (DeftToken) tok;
						if (dtok.quote) {
							author = dtok.quote_author;
							author_offset = dtok.quote_author_offset;
							System.out.println("Found quote author:"+author);
						}
					}
					string_author_offset = author_offset.toString();
					// Only adding targets with valid sources
					if (entity_mentions.containsKey(string_author_offset)) {
						author_id = entity_mentions.get(string_author_offset)[0];
						author_length = entity_mentions.get(string_author_offset)[1];
						author = entity_mentions.get(string_author_offset)[2];
						System.out.println("Found author:"+author+" offset:" + string_author_offset);
						dt.SetSource(author);
						dt.SetSourceID(author_id);
						dt.SetSourceOffset(string_author_offset);
						dt.SetSourceLength(author_length);
						output_targets.add(dt);
					}
				}
				c.SetTargets(output_targets);
			}
		}
		
		// Use ERE reader to mark up target ids and source ids for output targets
		public void MarkTargetAndSourceIDs(List<Comment>output_comments,
				List<Comment>input_comments) {
			int i=0;
			for (Comment c: input_comments) {
				Comment output_comment = output_comments.get(i);
				DeftEntityReader er = c.entity_reader;
				 // list of entities. for each entity, a list of mention offsets
				 // for each char offset, return the ere id and source id
				HashMap<String,List<String>> entities = er.entities;
				HashMap<String,String[]> entity_mentions = er.entity_mentions; 
				HashMap<String,String[]> relation_mentions = er.relation_mentions;
				HashMap<String,String[]> event_mentions = er.event_mentions;
				String ere_id = "";
				String string_char_offset = "";
		
				List<Target> new_output_targets = new ArrayList<Target>();
				for (Target t: output_comment.targets_) {
					DeftTarget dt = (DeftTarget) t;
					List<Token> target_tokens = dt.tokens_;
					boolean found = false;
					for (Token tok: target_tokens) { 
						Integer comment_offset = tok.comment_offset_;
						DeftToken dtok = (DeftToken) c.tokens_.get(comment_offset);
						Integer char_offset = dtok.char_offset;
						string_char_offset = char_offset.toString();
						if (entity_mentions.containsKey(string_char_offset)) {
							ere_id = entity_mentions.get(string_char_offset)[0];
							String length = entity_mentions.get(string_char_offset)[1];
							String text = entity_mentions.get(string_char_offset)[2];
							String full_text = entity_mentions.get(string_char_offset)[7];
							dt.SetID(ere_id);
							dt.SetLength(length);
							dt.SetDeftOffset(string_char_offset);
							//dt.SetText(text);
							dt.SetText(text);
							dt.SetFullText(full_text);
							found = true;
							//break;
						} 
						else if (relation_mentions.containsKey(string_char_offset)) {
							ere_id = relation_mentions.get(string_char_offset)[0];
							String length = relation_mentions.get(string_char_offset)[1];
							String text = relation_mentions.get(string_char_offset)[2];
							dt.SetID(ere_id);
							dt.SetLength(length);
							dt.SetDeftOffset(string_char_offset);
							dt.SetText(text);
							found = true;
							//break;
						} 
						else if (event_mentions.containsKey(string_char_offset)) {
							System.out.println("Found event mention for predicted target!" + dt.text_);
							ere_id = event_mentions.get(string_char_offset)[0];
							String length = event_mentions.get(string_char_offset)[1];
							String text = event_mentions.get(string_char_offset)[2];
							dt.SetID(ere_id);
							dt.SetLength(length);
							dt.SetDeftOffset(string_char_offset);
							dt.SetText(text);
							found = true;
							//break;
						} 	
	
						// checking for each token since we can have consecutive entities within a target
						if (found == true) {
							if (!new_output_targets.contains(dt)) {
								new_output_targets.add(dt);
								System.out.println("Added mention target: " + dt.text_ + " text:" + dt.full_text);
								if (predict_mentions) {
									AddAllMentions(new_output_targets,
											dt,string_char_offset,entities,entity_mentions);
								} 
							}
						}
						
					}
				} // end targets
				output_comment.SetTargets(new_output_targets);
				i+=1;
			}
			
		}
		
		// Update entities for new DEFT output format
		public void MarkNonTargetEntities(List<Comment>output_comments,
				List<Comment>input_comments) {
			int i=0;
			for (Comment c: input_comments) {
				Comment output_comment = output_comments.get(i);
				DeftEntityReader er = c.entity_reader;
				 // list of entities. for each entity, a list of mention offsets
				 // for each char offset, return the ere id and source id
				HashMap<String,List<String>> entities = er.entities;
				HashMap<String,String[]> entity_mentions = er.entity_mentions; 
				HashMap<String,String[]> relation_mentions = er.relation_mentions;
				HashMap<String,String[]> event_mentions = er.event_mentions;
				String ere_id = "";
				String string_char_offset = "";
		
					List<Entity> nontarget_entities = new ArrayList<Entity>();
					boolean found = false;
					for (Token tok: c.tokens_) { 
						if (tok.target_offset_>=0) {
							continue;
						}
						DeftToken dtok = (DeftToken) tok;
						Integer char_offset = dtok.char_offset;
						string_char_offset = char_offset.toString();
						if (entity_mentions.containsKey(string_char_offset)) {
							ere_id = entity_mentions.get(string_char_offset)[0];
							String length = entity_mentions.get(string_char_offset)[1];
							String text = entity_mentions.get(string_char_offset)[2];
							String full_text = entity_mentions.get(string_char_offset)[7];
							String sentiment = "none";
							Entity e = new Entity();
							e.SetEntity(ere_id, string_char_offset, length, text,
									sentiment, "", "", "", "", "", "entity");
							if (!nontarget_entities.contains(e) && !ere_id.isEmpty()
									&& !ere_id.equals(" ")) {
							nontarget_entities.add(e);
							}
						}
						else if (relation_mentions.containsKey(string_char_offset)) {
							ere_id = relation_mentions.get(string_char_offset)[0];
							String length = relation_mentions.get(string_char_offset)[1];
							String full_text = relation_mentions.get(string_char_offset)[2];
							String sentiment = "none";
							Entity e = new Entity();
							e.SetEntity(ere_id, string_char_offset, length, full_text,
									sentiment, "", "", "", "", "", "relation");
							if (!nontarget_entities.contains(e) && !ere_id.isEmpty()
									&& !ere_id.equals(" ")) {
								nontarget_entities.add(e);
								}
						} 
						else if (event_mentions.containsKey(string_char_offset)) {
							ere_id = event_mentions.get(string_char_offset)[0];
							String length = event_mentions.get(string_char_offset)[1];
							String full_text = event_mentions.get(string_char_offset)[2];
							String sentiment = "none";
							Entity e = new Entity();
							e.SetEntity(ere_id, string_char_offset, length, full_text,
									sentiment, "", "", "", "", "", "event");
							if (!nontarget_entities.contains(e) && !ere_id.isEmpty()
									&& !ere_id.equals("")) {
								nontarget_entities.add(e);
								}
						} 	
					} // end tokens
			
				output_comment.SetEntities(nontarget_entities);
				i+=1;
			} // end comments
		}
		
		// Uses the E/BIO markers instead, and looks at the whole span
		// TODO: Add mentions for relations and events also (also for coref?)
		public void MarkTargetAndSourceIDsNew(List<Comment>output_comments,
				List<Comment>input_comments) {
			int i=0;
			for (Comment c: input_comments) {
				Comment output_comment = output_comments.get(i);
				DeftEntityReader er = c.entity_reader;
				 // list of entities. for each entity, a list of mention offsets
				 // for each char offset, return the ere id and source id
				HashMap<String,List<String>> entities = er.entities;
				HashMap<String,String[]> entity_mentions = er.entity_mentions; 
				HashMap<String,String[]> relation_mentions = er.relation_mentions;
				HashMap<String,String[]> event_mentions = er.event_mentions;
				String ere_id = "";
				String string_char_offset = "";
		
				List<Target> new_output_targets = new ArrayList<Target>();
				for (Target t: output_comment.targets_) {
					DeftTarget dt = (DeftTarget) t;
					List<Token> target_tokens = dt.tokens_;
					boolean found = false;
					for (Token tok: target_tokens) { 
						Integer comment_offset = tok.comment_offset_;
						DeftToken dtok = (DeftToken) c.tokens_.get(comment_offset);
						Integer char_offset = dtok.char_offset;
						string_char_offset = char_offset.toString();
						boolean entity = tok.entity;
						String entity_type = tok.entity_type;
					//	if (entity) {
							//String entity_type = tok.entity_type;
							//if (entity_type.equals("E-B")) {
						if (entity_mentions.containsKey(string_char_offset)) {
								//try {
								ere_id = entity_mentions.get(string_char_offset)[0];
								String length = entity_mentions.get(string_char_offset)[1];
								String text = entity_mentions.get(string_char_offset)[2];
								String full_text = entity_mentions.get(string_char_offset)[7];
								dt.SetID(ere_id);
								dt.SetLength(length);
								dt.SetDeftOffset(string_char_offset);
								//dt.SetText(text);
								dt.SetText(full_text);
								dt.SetFullText(full_text);
								found = true;
								//}
							//	catch (Exception e) {
							//		e.printStackTrace();
								//}
							} //else if (entity_type.equals("R-B")) {
						else if (relation_mentions.containsKey(string_char_offset)) {
								//try {
								ere_id = relation_mentions.get(string_char_offset)[0];
								String length = relation_mentions.get(string_char_offset)[1];
								String text = relation_mentions.get(string_char_offset)[2];
								dt.SetID(ere_id);
								dt.SetLength(length);
								dt.SetDeftOffset(string_char_offset);
								dt.SetText(text);
								found = true; 
								//}
							//	catch (Exception e) {
								//	e.printStackTrace();
								//} 
							}
							//else if (entity_type.equals("H-B")) {
						else if (event_mentions.containsKey(string_char_offset)) {
							//	try {
								ere_id = event_mentions.get(string_char_offset)[0];
								String length = event_mentions.get(string_char_offset)[1];
								String text = event_mentions.get(string_char_offset)[2];
								dt.SetID(ere_id);
								dt.SetLength(length);
								dt.SetDeftOffset(string_char_offset);
								dt.SetText(text);
								found = true; 
							//	}
								//catch (Exception e ){
								//	e.printStackTrace();
								//}
							}
							else if (entity_type.equals("E-I")) {
								System.out.println("E-I");
								Integer k = comment_offset-1;
								WH: while (k>=0) {
									System.out.println("k:"+k);
									Token prevtoken = c.tokens_.get(k);
									System.out.println("Target:" + dt.text_);
									System.out.println("Token:" + tok.text_);
									System.out.println("Prevtoken:" + prevtoken.text_);
									DeftToken previous = (DeftToken) c.tokens_.get(k);
									System.out.println("Prevdefttoken:" + previous.text_);
									if (previous.entity_type.equals("E-B")) {
										Integer previous_char_offset = (Integer) previous.char_offset;
										String string_previous_char_offset = previous_char_offset.toString();
										try {
											System.out.println("Trying previous mention");
											ere_id = entity_mentions.get(string_previous_char_offset)[0];
											String length = entity_mentions.get(string_previous_char_offset)[1];
											String text = entity_mentions.get(string_previous_char_offset)[2];
											String full_text = entity_mentions.get(string_previous_char_offset)[7];
											dt.SetID(ere_id);
											dt.SetLength(length);
											dt.SetDeftOffset(string_previous_char_offset);
											dt.SetText(full_text);
											//dt.SetText(text);
											dt.SetFullText(full_text);
											System.out.println("Found previous mention:"+text 
													+ " with full text:" + full_text );
											found = true; }
											catch (Exception e) {
												e.printStackTrace();
											}
										break WH;
									}
									k=k-1;
								}
							}
							else if (entity_type.equals("R-I")) {
								Integer k = comment_offset-1;
								WH2: while (k>=0) {
									    DeftToken previous = (DeftToken) c.tokens_.get(k);
									    if (previous.entity_type.equals("R-B")) {
										Integer previous_char_offset = (Integer) previous.char_offset;
										String string_previous_char_offset = previous_char_offset.toString();
										try {
											ere_id = relation_mentions.get(string_previous_char_offset)[0];
											String length = relation_mentions.get(string_previous_char_offset)[1];
											String text = relation_mentions.get(string_previous_char_offset)[2];
											dt.SetID(ere_id);
											dt.SetLength(length);
											dt.SetDeftOffset(string_previous_char_offset);
											dt.SetText(text);
											found = true; }
											catch (Exception e) {
												e.printStackTrace();
											}
										break WH2;
									}
									    k-=1;
								}
							}
							else if (entity_type.equals("H-I")) {
								Integer k = comment_offset-1;
								WH3: while (k>=0) {
									    DeftToken previous = (DeftToken) c.tokens_.get(k);
										Integer previous_char_offset = (Integer) previous.char_offset;
										String string_previous_char_offset = previous_char_offset.toString();
										try {
											ere_id = event_mentions.get(string_previous_char_offset)[0];
											String length = event_mentions.get(string_previous_char_offset)[1];
											String text = event_mentions.get(string_previous_char_offset)[2];
											dt.SetID(ere_id);
											dt.SetLength(length);
											dt.SetDeftOffset(string_previous_char_offset);
											dt.SetText(text);
											found = true; }
											catch (Exception e) {
												e.printStackTrace();
											}
										break WH3;
									}
									k-=1;
								}
							
						//	} // end if entity	
						
						// checking for each token since we can have consecutive entities within a target
						if (found == true) {
							if (!new_output_targets.contains(dt)) {
								new_output_targets.add(dt);
								if (predict_mentions) {
									AddAllMentions(new_output_targets,
											dt,string_char_offset,entities,entity_mentions);
								} 
							}
						}	
					} // end token loop
				} // end targets
				output_comment.SetTargets(new_output_targets);
				i+=1;
			}
			
		}
		
		// Given a target and its mention id, add all mentions of this entity to targets
		public void AddAllMentions(List<Target> targets,
				DeftTarget dt, String offset, 
				HashMap<String,List<String>> entities,
				HashMap<String,String[]> entity_mentions ) {
			
			for (String entity_id: entities.keySet()) {
				List<String> mention_offsets = entities.get(entity_id);
				if (mention_offsets.contains(offset)) {
					// found the entity
					for (String mention: mention_offsets) {
						if (entity_mentions.containsKey(mention)) {
						DeftTarget dt_mention = new DeftTarget();
						String mention_id = entity_mentions.get(mention)[0];
						String length = entity_mentions.get(mention)[1];
						String text = entity_mentions.get(mention)[2];
						dt_mention.SetID(mention_id);
						dt_mention.SetLength(length);
						dt_mention.SetDeftOffset(mention);
						dt_mention.SetText(text);
						dt_mention.SetSource(dt.source);
						dt_mention.SetSourceID(dt.source_ere_id);
						dt_mention.SetSourceOffset(dt.source_offset);
						dt_mention.SetSourceLength(dt.source_length);
						// TODO: For pipeline model, can do different sentiment for different metnions
						dt_mention.SetSentiment(dt.sentiment_);
						
						if (!targets.contains(dt_mention)) {
							targets.add(dt_mention);
							System.out.println("Added new mention!" + text);
						}
						}
					}
				break;
				}
			}
		}
		
		
	}

	

