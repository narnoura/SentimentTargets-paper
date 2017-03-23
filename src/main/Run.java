package main;

import java.io.File;

// This is the main run file for the SentimentTarget system.

// Run like this:
// java Run 
// in=<input or xml file> 
// For xml input file containing targets and labels: in=<input file in xml> (for training or testing with evaluation)
// For raw input file containing only test data: use in=<raw input file>. Format should be one comment per line.
// outputdir=<output directory>
// runopt=<train|test|> 
// modelopt=<ALL_NP, CRF-target, CRF-sentiment, CRF-target+sentiment, CRF-pipeline>
// testfile= <CRF output prediction file> if test or pipeline mode
// binfeat = <comma separated list of binary features> (Word,MadamiraPOS,Sentiment...)
// langfiles= <directory with preproduced language files, e.g madamira files>
// eval = <eval type: subset-overlap| mention-overlap | exact | prop-overlap | overlap>
// tokopt=<D3 | ATB | none>

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import processor.InputReader;
import util.FileWriter;
import data.Comment;
import data.Target;
import models.Runner;
import eval.ApproximateRandomization;
import eval.Evaluator;
public class Run {
	public static void main (String[] args) {
		String input_file = null;
		String input_type = "xml";  // has gold labels
		String output_dir = "../experiments/paper-experiments";
		String lang_files_dir = "../data/madamira-files";	
		String lexicon_file = "../lexicons/subjectivity_clues_hltemnlp05/subjclueslen1-HLTEMNLP05.tff";	
		//String lexicon_file = "../lexicons/ARABSENTI-LEXICON";
		//String lexicon_file = "../lexicons/ArSenL/ArSenL_v1.0A.txt";
		String cluster_file = "";
		String run_option = "test";
		String model_option = "ALL_NP"; 
		String model_file = "";
		String encoding = "utf8ar";
		String binary_features = "Lex,MadamiraPOS,NER,BPC,Dependency,Sentiment,WordClusters";
		String continuous_features = "";
		String test_file = "";
		String tokenize = "D3";
		
		// For siginificance calculation
		String test_file_1 = "";
		String test_file_2 = "";
		String significance_thresholds = ""; // comma separated
		String tok_opt_1 = "D3";
		String tok_opt_2 = "D3";
	
		// If input encoding is in utf8,
		// automatically encode comments in bw.
		boolean convertbw = true;
		// Use existing Stanford parses instead of running the parser
		boolean use_existing_parse = true; // keep it true by default
		// Increase training data by lemma matching, topic or coreference
		boolean boost_training = false;
		// match type for evaluation
		String eval_match_type = "subset-overlap";
		// set true to calculate all f-measures for pairwise t-test 
		boolean all_significance = false;
		// for D3 and ATB option in pipeline
		boolean D3_and_ATB = false;
	
		// Process input arguments.
		 try {
			 for (int i=0; i<args.length; i++) { 
				 String argument = args[i];	 
				 if(argument.startsWith(Constants.INPUT_FILE)) {
					input_file = argument.substring(Constants.INPUT_FILE.length());
				 }
				 else if (argument.startsWith(Constants.OUTPUT_DIRECTORY)) {
					output_dir = argument.substring(Constants.OUTPUT_DIRECTORY.length());
				 }
				 else if (argument.startsWith(Constants.LANGUAGE_FILES_DIRECTORY)) {
					lang_files_dir = argument.substring(Constants.LANGUAGE_FILES_DIRECTORY.length());
				 }
				 else if (argument.startsWith(Constants.RUN_OPTION)) {
					run_option = argument.substring(Constants.RUN_OPTION.length());
				 } 
				 else if (argument.startsWith(Constants.MODEL_OPTION)) {
					model_option = argument.substring(Constants.MODEL_OPTION.length());
				 }
				 else if (argument.startsWith(Constants.INPUT_ENCODING)) {
						encoding = argument.substring(Constants.INPUT_ENCODING.length());
				 }
				 else if (argument.startsWith(Constants.LEXICON_FILE)) {
						lexicon_file = argument.substring(Constants.LEXICON_FILE.length());
				 }
				 //else if (argument.equals(Constants.USE_PARSE)) {
				 //	 use_existing_parse = true;
				 //}
				 else if (argument.startsWith(Constants.EVAL_MATCH)) {
					 eval_match_type = argument.substring(Constants.EVAL_MATCH.length());
				 }
				 else if (argument.startsWith(Constants.MODEL_FILE)) {
					 model_file = argument.substring(Constants.MODEL_FILE.length());
				 }
				 else if (argument.startsWith(Constants.BINARY_FEATURES)) {
					 binary_features = argument.substring(Constants.BINARY_FEATURES.length());
				 }
				 else if (argument.startsWith(Constants.CONT_FEATURES)) {
					 continuous_features = argument.substring(Constants.CONT_FEATURES.length());
				 }
				 else if (argument.startsWith(Constants.CLUSTER_FILE)) {
					 cluster_file = argument.substring(Constants.CLUSTER_FILE.length());
				 }
				 else if (argument.startsWith(Constants.TEST_FILE)) {
					 test_file = argument.substring(Constants.TEST_FILE.length());
				 }
				 else if (argument.startsWith(Constants.TOK_OPTION)) {
					 tokenize = argument.substring(Constants.TOK_OPTION.length());
				 }
				 else if (argument.equals(Constants.BOOST_TRAINING)) {
					 boost_training = true;
				 }
				 else if (argument.equals(Constants.ALL_SIGNIFICANCE)) {
					 all_significance = true;
				 }
				 else if (argument.startsWith("testfile1=")) {
					 test_file_1 = argument.substring("testfile1=".length());
				 }
				 else if (argument.startsWith("testfile2=")) {
					 test_file_2 = argument.substring("testfile2=".length());
				 }
				 else if (argument.startsWith("tokopt1=")) {
					 tok_opt_1 = argument.substring("tokopt1=".length());
				 }
				 else if (argument.startsWith("tokopt2=")) {
					 tok_opt_2 = argument.substring("tokopt2=".length());
				 }
				 
				 else if (argument.startsWith("sig=")) {
					 significance_thresholds = argument.substring("sig=".length());
				 }
				 else if (argument.startsWith("D3+ATB")) {
					 D3_and_ATB = true;
				 }
			 }
		 }
		 catch(Exception e){
				System.out.println("No input arguments specified. Please specify an input file. Exiting \n");
				return;
			}
		
		 System.out.println("\nEval type:" + eval_match_type);
		 System.out.println("Use existing parse:" + use_existing_parse);
		 System.out.println("Tokenize option:" + tokenize);
		 
		 if (input_file == null) {
			 System.out.println("Please specify an input file. Exiting \n");
			 return;
		 }
		 if (!input_file.endsWith(".xml")) {
			 input_type = "raw";
		 }
		 // 1) Read comments from input file and store in Comments.
		 // If gold labels for targets are available, store the labels.
		 System.out.println("Reading input file\n");
		 List<Comment> input_comments = new ArrayList<Comment>();
		 try {
		 if (input_type.equals("xml")) {
			 input_comments = InputReader.ReadCommentsFromXML(input_file, encoding, convertbw);
		 }
		 // Use this if we don't have annotations
		 else if (input_type.equals("raw")) {
			 boolean use_comment_id = false;
			 input_comments = InputReader.
					 ReadCommentsFromRaw(input_file, encoding, use_comment_id);
		   } 
		 }
		 catch (Exception e) {
			 System.out.println("Invalid file type. Exiting \n");
			 e.printStackTrace();
		 }
		// 2) Train model.  
		 Runner runner = new Runner();
		 runner.SetMadamiraDir(lang_files_dir);
		 runner.SetLexiconFile(lexicon_file);
		 runner.SetBaseInput(input_file);
		 runner.PrepareFeatureLists(binary_features, continuous_features);
		 runner.SetTokenizeOption(tokenize);
		 runner.SetD3andATB(D3_and_ATB);
		 if (boost_training) {
		 runner.BoostTraining(true);
		 } else {
			 runner.BoostTraining(false);
		 }
		 // Word to cluster id from word vector clusters
		 if (!cluster_file.isEmpty()) {
		 runner.SetClusterFile(cluster_file);
		 System.out.println("Cluster file:" + cluster_file);
		 }
		 if (run_option.equals("train")) {
				 System.out.println("\nTraining model " + model_option + "\n");
				 runner.Train(input_comments,
						 model_option, use_existing_parse, output_dir);
		 }  
		 else if (run_option.equals("test")) {
			 System.out.println("\nRunning model " + model_option + "\n");	 
			 runner.SetTestFile(test_file);
			 List<Comment> output_comments = runner.Run(input_comments,
				 model_option, use_existing_parse, output_dir, model_file);
			 if (output_comments.isEmpty()) {
				 System.out.println("Main:Output comments are empty!! \n");
				} else {
					System.out.println("\nMain: Size of output comments: " + output_comments.size());
				}
			 //PrintOutput(output_comments, input_comments, output_dir);
			 
			 // 3) Evaluate
			 if (input_type == "xml" && !output_comments.isEmpty() 
					 && !input_comments.isEmpty()) {
			 Evaluator e = new Evaluator(output_comments, input_comments);
			 List<Double> scores = e.Evaluate(eval_match_type);
			 e.PrintScores(scores,output_dir,eval_match_type);
			 e.Print(output_dir);
			 if (all_significance) {
					e.PrintFmeasures(output_dir);
				}
		 }
		} 
		 else if (run_option.equals("significance")) {
			 System.out.println("\nEvaluating significance using testfile1 and testfile2");	
			 System.out.println("Test file 1:"+test_file_1 +"\n");
			 System.out.println("Test file 2:"+test_file_2 +"\n");
				if (significance_thresholds.isEmpty()) {
					System.out.println("No significance thresholds, exiting.\n");
					System.exit(0);
				}
				// Get first set of comments
				runner.SetTestFile(test_file_1);
				runner.SetTokenizeOption(tok_opt_1);
			// For calculating significance with baseline
			List<Comment> output_comments_1 = runner.Run(input_comments,
				"ALL_NP" , use_existing_parse, output_dir, model_file); //make sure to set the lexicon
					 if (output_comments_1.isEmpty()) {
						 System.out.println("Main:Output comments for file 1 are empty!! \n");
						} else {
							System.out.println("\nMain: Size of output comments 1: " + output_comments_1.size());
						}
			  // Get second set of comments 
			  runner.SetTestFile(test_file_2);
			  runner.SetTokenizeOption(tok_opt_2);
			  List<Comment> output_comments_2 = runner.Run(input_comments,
						 model_option, use_existing_parse, output_dir, model_file);
					 if (output_comments_2.isEmpty()) {
						 System.out.println("Main:Output comments for file 2 are empty!! \n");
						} else {
							System.out.println("\nMain: Size of output comments 2: " + output_comments_2.size());
						}
					 
			ApproximateRandomization ar =
					new ApproximateRandomization(output_comments_1, output_comments_2, significance_thresholds);
			ar.SetGoldComments(input_comments);
			HashMap<String,Double> s = ar.GetSignificance();
			ar.Print(FileWriter.Combine(output_dir, "approxRandSig_" + (new File(test_file_1).getName())
						+ (new File(test_file_2).getName())), s);
		 	}	    
	}
}
