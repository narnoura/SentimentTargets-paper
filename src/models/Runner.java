/**
 * 
 */
package models;

import data.Comment;
import data.Target;
import data.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import main.Constants;
import models.baselines.ALL_NP;
import models.ExternalCRF;

import java.io.File;

import processor.InputReader;
import processor.LexiconProcessor;
import processor.ToolsProcessor.MadamiraProcessor;
import processor.ToolsProcessor.StanfordParserProcessor;
import processor.ToolsProcessor.DependencyProcessor;
import topicmodeling.LdaComments;
import topicmodeling.TopicSalience;
import topicmodeling.CoreferenceFeatures;
import topicmodeling.HobbsCoreference;
import util.BuckwalterConverter;
import util.FileReader;
import util.FileWriter;
import util.Tokenizer;

/**
 * @author Narnoura
 * Runs or trains specified models
 * with specified data and language files
 * 
 */

public class Runner {
	
	public String madamira_directory;
	public String catib_directory = "../data/catib-parses";
	public String parse_directory = "../data/stanford-parses";
	public String word_cluster_file = "../word-clusters/ar-wiki-classes-500-D3.sorted.txt";
	public String wiki_lemmas = "../WordClusters/Arabic/arwiki-20160820-pages-articles.txt.mada.lemmacounts.sorted";
	public static Integer DOC_SIZE = 277516;
	public String lexicon_file;
	public String base_input_file;
	public List<String> binary_features;
	public List<String> continuous_features;
	// set for running CRF on predicted tags
	public String test_file;
	// If true, represent comments in tokenized space
	public boolean tokenized_space;
	public String tok_option = "D3";
	public boolean D3_and_ATB_for_pipeline_ = false;
	// For test mode, can use LDA model from training data 
	// for from current data
	boolean LDA_from_train = false;
	// If true, increase training data by lemma matching or other techniques
	boolean increase_training = true;
	// When boosting training data by lemma, update clitics (Al+)
	boolean update_clitics = true;
	// When boosting training data by pronominal mentions,
	// replace pronouns by lemmas & their morph features
	boolean replace_morph_features = false;
	
	public Runner() {
		binary_features = new ArrayList<String>();
		continuous_features = new ArrayList<String>();
	}
	
	public void SetBaseInput (String input) {
		input = input.replaceAll("xml", "raw");
		File input_file = new File(input);
		base_input_file = input_file.getName();
		// for undet experiments
		base_input_file = base_input_file.replace("-undet", "");
	}
	
	public void SetTestFile (String test_file) {
		this.test_file = test_file;
	}
	
	public void SetMadamiraDir (String dir) {
		madamira_directory = dir;
	}

	public void SetLexiconFile (String path) {
		lexicon_file = path;
	}
	
	public void SetTokenizeOption (String tok_option) {
		this.tok_option = tok_option;
		if (!tok_option.equals("D3") && !tok_option.equals("ATB")) {
			tokenized_space = false;
		} else{
			tokenized_space = true;
		}
	}
	
	//Only valid for pipeline mode
	public void SetD3andATB (boolean D3andATB) {
		this.D3_and_ATB_for_pipeline_ = D3andATB;
	}

	public void BoostTraining(boolean boost_train) {
		increase_training = boost_train;
	}
	
	public void SetClusterFile(String cluster_file) {
		word_cluster_file = cluster_file;
	}

	public void PrepareFeatureLists (String binary, String continuous) {
		try {
			String[] bin = binary.split(",");
			String[] cont = continuous.split(",");
			binary_features = new ArrayList<String>();
			continuous_features = new ArrayList<String>();
			for (String b: bin) {
				binary_features.add(b);
			}
			for (String c: cont) {
				continuous_features.add(c);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
	}

	public void UpdateCommentsWithTargets(List<Comment> mada_comments, 
			List<Comment> input_comments) {

		for (Comment c: mada_comments) {
			Comment input_comment = input_comments.get(mada_comments.indexOf(c));
			assert input_comment.tokens_.size() == c.tokens_.size();
			for (Token t: c.tokens_) {
				t.SetTargetOffset(-1);
			}
			if(input_comment.targets_ != null) {
				c.SetTargets(input_comment.targets_);
				// Target text in mada_comments will be consistent with gold target text
				// (from input_comments)
			}
			// Update the target offsets
			for (Target t: c.targets_) {
				// Separate punctuations and digits, remove tatweel
				String processed_text = Tokenizer.ProcessLikeMadamira(t.text_);
				// Remove punctuations from the end of targets
				processed_text = Tokenizer.RemovePunctuationFromEndAndBeginning(processed_text);
				List<Token> target_tokens = Tokenizer.SimpleTokenize(processed_text, "word");
				List<Integer> comment_offsets = c.Find(processed_text, target_tokens);
				if (!comment_offsets.isEmpty()) {	
					t.SetOffsets(comment_offsets);
					for (int o: comment_offsets) {			
						for (int k=0; k<target_tokens.size(); k++) {
						Token token_with_target = c.tokens_.get(k+o);
						token_with_target.SetTargetOffset(k);
						token_with_target.SetSentiment(t.sentiment_);
						}
					}	
				 t.SetTokens(target_tokens);
				}
				else{
					System.out.println("\nRunner::Did not find target: " + processed_text);
					System.out.println("Comment id:" + c.comment_id_); 
					System.out.println("Comment:" + c.raw_text_);
					System.exit(0);
					}
			}
			
		// Update input comments
		//input_comment.SetProcessed(processed_tokens, c.targets_, processed_comment_text);
		
		// I AM SETTING THE INPUT COMMENT TARGETS TO MADA (PROCESSED TARGETS)
		// IF YOU WANT TO KEEP THE ORIGINAL COMMENTS IN THEIR NOISY FORM USE SET PROCESSED INSTEAD
		input_comment.SetTargets(c.targets_);
		input_comment.SetText(c.raw_text_);
		input_comment.SetTokens(c.tokens_);
		
		}
		}

	public FeatureExtractor SetUpFeatures (String madamira_directory, 
			String base_input_file, String lexicon_file, 
			List<Comment> input_comments, boolean use_existing_parse) {
		
		if (input_comments.isEmpty() && madamira_directory.isEmpty()) {
			System.out.println("Runner: Empty input comments and no madamira "
					+ "comments specified.. Exiting \n");
			System.exit(0);
		}
		
		System.out.println("\nReading madamira files \n");
		String mada_file = (new File(madamira_directory, base_input_file).getAbsolutePath());
		mada_file += ".mada";
		List<Comment> mada_comments = MadamiraProcessor.ReadMadaFile(mada_file, "bw");
		List<Comment> feature_comments = mada_comments;
		UpdateCommentsWithTargets(mada_comments, input_comments);
		String tok_file = (new File(madamira_directory, base_input_file).getAbsolutePath());
		List<Comment> tokenized_comments = new ArrayList<Comment>();
		
		if (tokenized_space == true) {
			if (tok_option.equals("D3")) {
				tok_file +=  ".MyD3.tok";
			}
			else if (tok_option.equals("ATB")) {
				tok_file += ".ATB.tok";
			}
			else {
				System.out.println("Invalid tokenization option " + tok_option + " . Defaulting to D3");
				tok_file +=  ".MyD3.tok";
			}
			tokenized_comments = MadamiraProcessor.ReadTokenizedComments(tok_file, "utf8ar");
		}
		
		 if (tokenized_space == true) {
			feature_comments
			= UpdateTokenized(tokenized_comments, feature_comments);
				if (binary_features.contains("NER")) {
				  String NER_file = new File(madamira_directory, base_input_file).getAbsolutePath();
				  NER_file += ".ner-bio";
				  feature_comments = MadamiraProcessor.UpdateNER(NER_file, "bw", feature_comments, tok_option);
				}
				if (binary_features.contains("BPC")) {
					 String BPC_file = new File(madamira_directory, base_input_file).getAbsolutePath();
					 BPC_file += ".bpc-bio";
					 feature_comments = MadamiraProcessor.UpdateBPC(BPC_file, "bw", feature_comments, tok_option);
				}
		 }
		 
		 // Boost training data
		 if (increase_training) {
			 	//IncreaseTrainingTargets boost_lemmas = new IncreaseTrainingTargets();
				IncreaseTrainingTargets boost_coreference_lemmas = new IncreaseTrainingTargets();
				//feature_comments = boost_lemmas_coreference.IncreaseTrainingByLemma(feature_comments, update_clitics);
				//feature_comments = boost_coreference_lemmas.IncreaseTrainingByLemma(feature_comments, update_clitics);
				//feature_comments = UpdateCoreference(feature_comments, use_existing_parse);
				//feature_comments = boost_coreference_lemmas.IncreaseTrainingWithFilters(feature_comments, update_clitics);

			//feature_comments = boost_coreference_lemmas.IncreaseTrainingByCoreference(feature_comments, replace_morph_features);
			feature_comments = boost_coreference_lemmas.IncreaseTrainingByLemma(feature_comments, update_clitics);
		 }
		 
		 // Update pronominal coreferences 
		 if (binary_features.contains("Coreference")) {
			 feature_comments = UpdateCoreference(feature_comments, use_existing_parse);
			 feature_comments = CoreferenceFeatures.AnnotateDataWithCoreference(feature_comments);
			 //feature_comments = CoreferenceFeatures.AnnotateDataWithAllCoreference(feature_comments);
		 }
		 
		 // Update topic salience features (// The order of this matters if we replace morph in coreference)
		 if (binary_features.contains("Salience")) {
			 System.out.println("Updating salience and topic signatures\n");
			 HashMap<String,Double> corpus_counts = 
					 TopicSalience.ReadCorpusCountsFromFile(wiki_lemmas);
			 for (Comment c: feature_comments) {
				//TopicSalience.UpdateTokenFrequenciesArabic(c);
				TopicSalience.UpdateWordProbs(c);
				TopicSalience.UpdateTFIDF(c, DOC_SIZE, corpus_counts);
				TopicSalience.UpdateTopicSignatures(c, feature_comments);
			 }
			}
		 FeatureExtractor fe = new FeatureExtractor(feature_comments);
		 fe.SetBinaryFeatures(binary_features);
		 fe.SetContinuousFeatures(continuous_features);
		 fe.SetNontokenized(mada_comments);
		 fe.SetWordClusters(word_cluster_file);
		    
		// Set up dependency features
		String dep_file = base_input_file;
		dep_file += ".mada.rewrite2.mada+parse";
		String catib_file = (new File(catib_directory, dep_file)).getAbsolutePath();
		DependencyProcessor dp = new DependencyProcessor();
		dp.SetPath(catib_file);
		fe.SetDependencyProcessor(dp);
		
		// Set up topic modeling features
		// will need lingpipe for cp
		/*LdaComments comment_model = new LdaComments();
		if (LDA_from_train) {
			/*String train_file = base_input_file;
			train_file = train_file.replace("dev", "train");
			train_file = train_file.replace("test", "train");
			List<Comment> train_comments = InputReader.ReadCommentsFromXML(train_file, "utf8ar", true);*/
			
		/*} else {
		fe.SetTopicModel(comment_model, null);
		}*/
		
		// SetUpLexicons(String lexicon_file)
		//LexiconProcessor lp = new LexiconProcessor();
		/*if (lexicon_file.endsWith("SLSA.txt")) {
			System.out.println("Reading SLSA \n");
			lp.ReadSlsa(lexicon_file);
		}
		else if (lexicon_file.endsWith("ArSenL_v1.0A.txt")) {
			lp.ReadArsenl(lexicon_file);
			lp.PrintArsenl();
		}*/
		// fe.SetLexiconProcessor(lp);
		fe.SetLexiconProcessor(new LexiconProcessor());
		
		return fe;
	} 
	
	// Updates mada comments by tokenizing and updating morphological features
	// Particularly, if morpheme is a proclitic or suffix, updates the part 
	// of speech by looking up the type of morpheme in 'bw' feature
	//
	// Also updates all comment tokens with their target offsets, sentiment and 
	// target info
	List<Comment> UpdateTokenized (List<Comment> tokenized_comments, 
			List<Comment> mada_comments) {
		
		List<Comment> output = tokenized_comments;
		int i =0;
		for (Comment c: output) {
			Comment mada_comment = mada_comments.get(i);
			for (int j=0; j< c.tokens_.size(); j++) {
				Token t = c.tokens_.get(j);
				int original_offset = t.comment_offset_;
				Token m = mada_comment.tokens_.get(original_offset);
				t.SetTargetOffset(m.target_offset_);
				t.SetSentiment(m.sentiment_);
				// Set morph features to clitics here
				t.SetMorphFeatures(m.morph_features);
				// HERE THIS TEXT IS NORMALIZED (from tokenization output) I assume because we get 
				// t from the tokenized comments not from mada_comments. But it is only in training
				// and not in evaluation.
				// Tokenized Madamira input is in utf8, convert it back
				t.SetText(BuckwalterConverter.ConvertToBuckwalter(t.text_)); 
				t.SetType(m.type_);
				t.SetPOS(m.pos_);
				
			if (t.clitic.equals("suf")) {
					String sufbw = t.morph_features.get("bw");
					String[] sufparts = sufbw.split("\\+");
					String pos3 = sufparts[sufparts.length-1];
					pos3 = pos3.split("\\/")[1];
					 if (pos3.equals("CONJ")) {
							pos3 = "conj"; 
					  }
					 if (pos3.equals("PREP")) {
							pos3 = "prep"; 
					  }
					t.SetPOS(pos3);
				}
			if (!t.clitic.equals("none") && !t.clitic.equals("word")
						&& !t.morph_features.containsKey("NO_ANALYSIS")) {
					String bw = t.morph_features.get("bw");
					String[] parts = bw.split("\\+");
					if (t.clitic.equals("pref")) {
						String pos =  parts[0];
						pos = pos.split("\\/")[1];
						if (pos.equals("CONJ")) {
							pos = "conj"; //keep consistent for 'conj' pos
						}
						 if (pos.equals("PREP")) {
								pos = "prep"; 
						 }
							t.SetPOS(pos);
						if (j!=c.tokens_.size()-1 && c.tokens_.get(j+1).equals("pref")) // bug? // getting the wrong POS?
							{
							 String pos2 = parts[1];
							 pos2 = pos2.split("/")[1];
							 if (pos2.equals("CONJ")) {
									pos2 = "conj"; 
							  }
							 if (pos2.equals("PREP")) {
									pos2 = "prep"; 
							  }
							 c.tokens_.get(j+1).SetTargetOffset(m.target_offset_);
							 // TODO: does it help to set this as neutral instead to reduce noise? No.
							 c.tokens_.get(j+1).SetSentiment(m.sentiment_);
							 c.tokens_.get(j+1).SetMorphFeatures(m.morph_features);
							 c.tokens_.get(j+1).SetText(BuckwalterConverter.
									 ConvertToBuckwalter(t.text_));
							 c.tokens_.get(j+1).SetType(m.type_);
							 c.tokens_.get(j+1).SetPOS(pos2);
							 j+=1;
							 // for D3
							 if (j!=c.tokens_.size()-2 && c.tokens_.get(j+2).equals("pref")) //bug?
								 {
								 String pos3 = parts[2];
								 System.out.println("prefix pos3:" + pos3);
								 pos3 = pos3.split("/")[1];
								 if (pos3.equals("CONJ")) {
										pos3 = "conj"; 
								  }
								 if (pos3.equals("PREP")) {
										pos3 = "prep"; 
								  }
								 c.tokens_.get(j+2).SetTargetOffset(m.target_offset_);
								 c.tokens_.get(j+2).SetSentiment(m.sentiment_);
								 c.tokens_.get(j+2).SetMorphFeatures(m.morph_features);
								 c.tokens_.get(j+2).SetText(BuckwalterConverter.
										 ConvertToBuckwalter(t.text_));
								 c.tokens_.get(j+2).SetType(m.type_);
								 c.tokens_.get(j+2).SetPOS(pos2);
								 j+=1;
							 } // end for D3
					} // end first j
				  } // end pref
				else if (t.clitic.equals("suf")) {
							String sufbw = t.morph_features.get("bw");
							String[] sufparts = sufbw.split("\\+");
							String pos3 = sufparts[sufparts.length-1];
							pos3 = pos3.split("\\/")[1];
							 if (pos3.equals("CONJ")) {
									pos3 = "conj"; 
							  };
							  if (pos3.equals("PREP")) {
									pos3 = "prep"; 
							  };
							t.SetPOS(pos3);
						}
				} // end clitic
			} // end tokens
			i+=1;
		} // end comment
		return output;
	} 
	
	// Only keeps the tokenized word without the separated clitics
	List<Comment> UpdateTokenizedExcludeTokens (List<Comment> tokenized_comments, 
			List<Comment> mada_comments) {
		List<Comment> output = new ArrayList<Comment>();
		int i =0;
		for (Comment c: tokenized_comments) {
			Comment mada_comment = mada_comments.get(i);
			Comment out = mada_comment;
			List<Token> out_tokens = new ArrayList<Token>();
			for (int j=0; j< c.tokens_.size(); j++) {
				Token t = c.tokens_.get(j);
				if (t.clitic.equals("suf") || t.clitic.equals("pref")) {
					continue; }
				else {
				int original_offset = t.comment_offset_;
				Token m = mada_comment.tokens_.get(original_offset);
				t.SetTargetOffset(m.target_offset_);
				t.SetSentiment(m.sentiment_);
				t.SetMorphFeatures(m.morph_features);
				t.SetText(BuckwalterConverter.ConvertToBuckwalter(t.text_));
				t.SetType(m.type_);
				t.SetPOS(m.pos_);
				out_tokens.add(t);
				}
			} // end tokens
			out.SetTokens(out_tokens);
			output.add(out);
			i+=1;
		} // end comment
		return output;
	} 
	
	// Train model
	public void Train(List<Comment> train_comments, String model_type, boolean use_existing_parse,
			String output_directory) {
		
		if (train_comments.isEmpty()) {
			System.out.println("Runner: Empty train comments. Exiting \n");
			System.exit(0);
		}

		FeatureExtractor fe =  SetUpFeatures(madamira_directory, base_input_file,
				lexicon_file, train_comments, use_existing_parse);
	
		String file_path = new File(output_directory, base_input_file).getAbsolutePath();
		
		if (model_type.equals("CRF-target")) {
			fe.SetLabelType("target");
			ExternalCRF TargetCRF = new ExternalCRF(fe);
			TargetCRF.SetOutputDir(output_directory);
			TargetCRF.WriteFeatureFile("train", file_path);
			// this function is not doing anything right now. We're just writing the file.
			TargetCRF.TrainFromFiles(file_path); 
		}
		else if (model_type.equals("CRF-sentiment")) { // Can add sentiment features to this 
			fe.SetLabelType("sentiment"); 
			ExternalCRF TargetCRF = new ExternalCRF(fe);
			TargetCRF.SetOutputDir(output_directory);
			TargetCRF.WriteFeatureFile("train", file_path);
			TargetCRF.TrainFromFiles(file_path);
		}
		// This option is not available now.
		else if (model_type.equals("CRF-pipeline")) {
			
		}
		else if (model_type.equals("CRF-target+sentiment")) {
			System.out.println("Setting up feature extractor for "
					+ "target+sentiment model\n");
			fe.SetLabelType("target+sentiment");
			ExternalCRF TargetCRF = new ExternalCRF(fe);
			TargetCRF.SetOutputDir(output_directory);
			TargetCRF.WriteFeatureFile("train", file_path);
			TargetCRF.TrainFromFiles(file_path);
		}
	}
	
	// Run model on test data
	// add features
	public List<Comment> Run(List<Comment> input_comments,
			String model_type, boolean use_existing_parse, String output_dir,
			String model_file) {
		
		List<Comment> output_comments = new ArrayList<Comment>();
		String file_path = new File(output_dir, base_input_file).getAbsolutePath();
		
		FeatureExtractor fe = new FeatureExtractor();
		if (!model_type.equals("ALL_NP")) {
			fe = SetUpFeatures(madamira_directory, base_input_file, lexicon_file,
					input_comments, use_existing_parse);
		}
		if (model_type.equals("ALL_NP")) {
			ALL_NP NP_baseline_runner = new ALL_NP();
			NP_baseline_runner.SetInputFile(base_input_file);
			output_comments = 
			NP_baseline_runner.Run(input_comments, 
					madamira_directory, lexicon_file, use_existing_parse);
		}
		else if (model_type.equals("CRF-target")) {
			fe.SetLabelType("target");
			ExternalCRF TargetCRF = new ExternalCRF(model_file);
			TargetCRF.SetFeatureExtractor(fe);
			String test_file = new File(output_dir, this.test_file).getAbsolutePath();
			output_comments = TargetCRF.TestFromFile(test_file,
					input_comments, tokenized_space);
			
			// Evaluate sentiment using a baseline method
			System.out.println("Evaluating sentiment of targets\n");
			ALL_NP baseline_sentiment_predictor = new ALL_NP();
			LexiconProcessor SlsaProcessor = new LexiconProcessor();
			SlsaProcessor.ReadSlsa(SlsaProcessor.lexicon_files.get("Slsa"));
			output_comments = baseline_sentiment_predictor.
					UpdateSentimentForTargetsSentenceLevel(output_comments, SlsaProcessor);
			// output_comments = baseline_sentiment_predictor.
				//	UpdateSentiment(output_comments, SlsaProcessor);
		}
		// This model trains only sentiment (positive or negative)
		// assuming the target is already known
		else if (model_type.equals("CRF-sentiment")) {
			// labels: positive,negative,neutral. essentially same as target+sentiment
			fe.SetLabelType("sentiment"); 
			ExternalCRF TargetCRF = new ExternalCRF(model_file);
			TargetCRF.SetFeatureExtractor(fe);
			String sentiment_test_file = new File(output_dir, this.test_file).getAbsolutePath();
			output_comments = TargetCRF.TestTargetsAndSentimentFromFile(sentiment_test_file,
					input_comments, tokenized_space);
		}
		else if (model_type.equals("CRF-pipeline")) {
			System.out.println("Running target model\n");
			fe.SetLabelType("target");
			ExternalCRF TargetCRF = new ExternalCRF(model_file);
			TargetCRF.SetFeatureExtractor(fe);
			String test_file = new File(output_dir, this.test_file).getAbsolutePath();
			output_comments = TargetCRF.TestFromFile(test_file,
					input_comments, tokenized_space);

			// boost targets in output here
			if (increase_training) {
				IncreaseTrainingTargets increase_output_coreference = new IncreaseTrainingTargets();
				//feature_comments = boost_lemmas_coreference.IncreaseTrainingByLemma(feature_comments, update_clitics);
				output_comments = UpdateCoreference(output_comments, use_existing_parse);
				output_comments = 
			 increase_output_coreference.IncreaseTrainingByCoreference(output_comments, replace_morph_features);
			}
			
			// D3+ATB
			// Remove the Al+ tokens, and for the targets we predicted with Al+,
			// make sure their full words get to be targets
			if (this.D3_and_ATB_for_pipeline_) {
			this.SetTokenizeOption("ATB");
			for (Comment c: output_comments) {
				List<Token> without_Al = new ArrayList<Token>();
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t= c.tokens_.get(i);
					if (!t.text_.equals("Al+")) {
						if(i>=1 && c.tokens_.get(i-1).text_.equals("Al+")) {
							Token prev = c.tokens_.get(i-1);
							if (prev.target_offset_ >=0) {
								t.SetTargetOffset(0);
								t.SetSentiment(prev.sentiment_); // doesn't matter
							}
						}
						without_Al.add(t);
					}
				}
				c.SetTokens(without_Al);
			}
			}

			System.out.println("Running sentiment model\n");
			fe.SetInput(output_comments); // don't care about the tokenization because we're not evaluating in the second model 
			fe.SetLabelType("sentiment"); // just writing features
			fe.SetPipelineModel(true); // for creating sentiment field when undetermined
			// NOTE: We don't call IncreaseTraining again, but we can also boost the targets here in the output
			ExternalCRF SentimentCRF = new ExternalCRF(model_file);
			SentimentCRF.SetFeatureExtractor(fe);
			SentimentCRF.SetOutputDir(output_dir);
			// actually doesn't matter
			SentimentCRF.WriteFeatureFile("dev", file_path);
			//SentimentCRF.WriteFeatureFile("test", file_path);
		}
		else if (model_type.equals("CRF-target+sentiment")) {
			fe.SetLabelType("target+sentiment");
			ExternalCRF TargetCRF = new ExternalCRF(model_file);
			TargetCRF.SetFeatureExtractor(fe);
			String test_file = new File(output_dir, this.test_file).getAbsolutePath();
			output_comments = TargetCRF.TestTargetsAndSentimentFromFile(test_file,
					input_comments, tokenized_space);
		}
		return output_comments;
	}
	
	// For coreferring pronominal mentions, updates their coreference fields
	// Fills in Al+ for ATB
	// Updates parse trees for comments
	public List<Comment> UpdateCoreference (List<Comment> feature_comments,
			boolean use_existing_parse) {
		
		System.out.println("Updating coreference\n");
		System.out.println("Use existing parse:" + use_existing_parse);
		if (!use_existing_parse) {
			StanfordParserProcessor sp 
				= new StanfordParserProcessor("edu/stanford/nlp/models/lexparser/arabicFactored.ser.gz", "ATB");
			System.out.println("Parsing comments \n");
			sp.ParseComments(feature_comments,
					new File(parse_directory, base_input_file).getAbsolutePath());
		}
		Document xml_trees = FileReader.ReadXMLFile
			(new File(parse_directory, base_input_file).getAbsolutePath(), "utf8ar");
		xml_trees.getDocumentElement().normalize();
		
		feature_comments = HobbsCoreference.Update(feature_comments, xml_trees);
		return feature_comments;
	}
	
}
