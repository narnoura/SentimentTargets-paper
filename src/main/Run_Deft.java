package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import data.*;
import eval.Evaluator;
import models.Runner;
import models.DeftRunner;
import processor.DEFTInputReader;
import processor.DeftWriter;
import processor.ToolsProcessor.StanfordCoreNLPProcessor;
import util.FileWriter;


public class Run_Deft {

	public Run_Deft() {
	}
	public static void main (String[] args) {
		String annotation_dir = "../../../../DEFT/LDC2016E27_DEFT_English_Belief_"
				+ "and_Sentiment_Annotation/data/data-divided/annotation";
		String source_dir = "../../../../DEFT/LDC2016E27_DEFT_English_Belief_"
				+ "and_Sentiment_Annotation/data/data-divided/source";
		String ere_dir  = "../../../../DEFT/LDC2016E27_DEFT_English_Belief_"
				+ "and_Sentiment_Annotation/data/data-divided/ere";
		String input_dir = "../../../../DEFT/LDC2016E27_DEFT_English_Belief_"
				+ "and_Sentiment_Annotation/data/data-divided";
		
				String input_file = "";
				//String output_dir = System.getProperty("user.dir");
				String output_dir = "DEFT-output";
				String lang_files_dir = "";
				String lexicon_file = "../SLSA.txt";	
				String run_option = "train";
				String model_option = "ALL_NP"; 
				String model_file = "";
				String encoding = "utf8ar";
				String binary_features = "word,pos";
				String continuous_features = "";
				String test_file = "";
				String tokenize = "";
				String cluster_file = "";
				// Use existing Stanford parses instead of running the parser
				boolean use_existing_parse = false;
				// match type for evaluation
				String eval_match_type = "subset";
				// Process input arguments. eventually put in config file
				boolean boost_training = false;
				boolean predict_mentions = false;
				boolean all_significance = false;
				 try {
					 for (int i=0; i<args.length; i++) { 
						 String argument = args[i];	 
						 if(argument.startsWith(Constants.INPUT_FILE)) {
							input_file = argument.substring(Constants.INPUT_FILE.length());
						 } 
						 else if (argument.startsWith(Constants.INPUT_DIRECTORY)) {
							input_dir = argument.substring(Constants.INPUT_DIRECTORY.length());
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
						 else if (argument.equals(Constants.USE_PARSE)) {
							 use_existing_parse = true;
						 }
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
						 else if (argument.startsWith(Constants.TEST_FILE)) {
							 test_file = argument.substring(Constants.TEST_FILE.length());
						 }
						 else if (argument.startsWith(Constants.TOK_OPTION)) {
							 tokenize = argument.substring(Constants.TOK_OPTION.length());
						 }
						 else if (argument.startsWith(Constants.CLUSTER_FILE)) {
							 cluster_file = argument.substring(Constants.CLUSTER_FILE.length());
						 }
						 else if (argument.equals(Constants.BOOST_TRAINING)) {
							 boost_training = true;
						 }
						 else if (argument.equals(Constants.PREDICT_MENTIONS)) {
							 predict_mentions = true;
						 }
						 else if (argument.equals(Constants.ALL_SIGNIFICANCE)) {
							 all_significance = true;
						 }
						 
					 }
				 }
				 catch(Exception e){
						return;
					}
		
		List<Comment> input_comments = new ArrayList<Comment>();
		List<String> doc_ids = new ArrayList<String>();
		List<String> annotation_ids = new ArrayList<String>();
		StanfordCoreNLPProcessor core_nlp = new StanfordCoreNLPProcessor();
		// Loop over all files source dir
		// For each file, declare DEFTInputReader
		// and update final list of comments
		annotation_dir = input_dir + "/annotation";
		source_dir = input_dir + "/source";
		ere_dir = input_dir + "/ere";
		
		System.out.println("Input dir:" + input_dir);
		System.out.println("Annotation dir:" + annotation_dir);
		
		File source_folder = new File(source_dir);
		File ere_folder = new File(ere_dir);
		File annotation_folder = new File("");
		Path annotation_path = Paths.get(annotation_dir);
		
		if (Files.exists(annotation_path)) {
		annotation_folder = new File(annotation_dir);
	    }
		for (File file : source_folder.listFiles()) {
			String source_file = file.toString();
			String entity_file = file.getName();
			String doc_id=entity_file;
			entity_file = entity_file.replace(".xml", ".rich_ere.xml");
			entity_file = entity_file.replace(".cmp.txt", ".rich_ere.xml");
			doc_id = doc_id.replace(".cmp.txt", "");
			doc_id = doc_id.replace(".xml", "");
			doc_ids.add(doc_id);
			String annotation_file;
			if (!Files.exists(annotation_path)) {
				annotation_file = "";
			}
			else {
				annotation_file = entity_file;
				annotation_file = annotation_file.replace(".rich_ere.xml", ".best.xml");
			}
			//entity_file = (new File(ere_folder,entity_file).getAbsolutePath());
			
			// if multiple entity and annotation files to source file
			List<String> entity_files = new ArrayList<String>();
			List<String> annotation_files = new ArrayList<String>();
			for (File ef: ere_folder.listFiles()) {
				if (ef.getName().contains(doc_id)) {
					//System.out.println("Adding ere file:" + ef.getName());
					entity_files.add(ef.toString());
				} 
			}
			if (Files.exists(annotation_path)) {
				annotation_file = (new File(annotation_folder,annotation_file).getAbsolutePath());
				for (File af: annotation_folder.listFiles()) {
					if (af.getName().contains(doc_id)) {
						//System.out.println("Adding annotation file:" + af.toString());
						String id = af.getName().replaceAll(".best.xml","");
						annotation_ids.add(id);
						annotation_files.add(af.toString());
					}
				}
			}
			DEFTInputReader reader = new DEFTInputReader(doc_id);
			reader.SetProcessor(core_nlp);
			List<Comment> post_comments = 
					reader.GetCommentsMultipleFiles(source_file,annotation_files,entity_files);
			input_comments.addAll(post_comments);
		}
		 System.out.println("Size of input comments:" + input_comments.size());
		 System.out.println("Number of docids:" + doc_ids.size());

		 DeftRunner runner = new DeftRunner();
		 runner.PrepareFeatureLists(binary_features, continuous_features);
		 runner.SetTokenizeOption(tokenize);
		 if (boost_training) {
			 runner.BoostTraining(true);
		} else {
		     runner.BoostTraining(false);
		}
		 if (predict_mentions) {
			 runner.SetPredictMentions(true);
			 System.out.println("Predict mentions true");
		} else {
		     runner.SetPredictMentions(false);
		     System.out.println("Predict mentions false");
		}
		 // Word to cluster id from word vector clusters
		 if (!cluster_file.isEmpty()) {
		 runner.SetClusterFile(cluster_file);
		 }
		 
		// Currently this mode "train" is used for extracting features for both train and test data
		// while the mode "test" actually only does evaluation. "train" and "test" are badly named.
	    // So you should use 'train' for all feature extraction.
		if (run_option.equals("train")) {
			 System.out.println("\nTraining model " + model_option + "\n");
			 runner.TrainDEFT(input_comments,
					 model_option, use_existing_parse, output_dir);
	    }  
	    else if (run_option.equals("test")) {
	    	 /*System.out.println("Run_Deft: Printing input comments before runner:");
	    	 System.out.println("INPUT COMMENTS:");
				for (Comment c: input_comments) {
					 System.out.println("\nComment:" + c.comment_id_ + "\n" + c.raw_text_);
					 System.out.println("Targets:");
					 for (Target t: c.targets_) {
						 System.out.println(t.text_ + ":" + t.sentiment_);
					 }
				 }*/
		 System.out.println("\nEvaluating model " + model_option + "\n");
		 // The file that contains the output labels from the CRF
		 runner.SetTestFile(test_file);
		 System.out.println("Test file:" + test_file);
		 System.out.println("Model option:" + model_option);
		 
		 List<Comment> output_comments = runner.RunDEFT(input_comments,
			 model_option, use_existing_parse, output_dir, model_file);
		 if (output_comments.isEmpty()) {
			 System.out.println("Main:Output comments are empty!! \n");
			} else {
				System.out.println("\nMain: Size of output comments: " + output_comments.size());
			}
			System.out.println("Output targets after returning from runner:");
			 for (Comment c: output_comments) {
				 System.out.println("\nComment:" + c.comment_id_ + "\n" + c.raw_text_);
				 System.out.println("Targets:");
				 for (Target t: c.targets_) {
					 System.out.println(t.text_ + ":" + t.sentiment_);
				 }
			 }
		 
		 System.out.println("Size of input comments:" + input_comments.size());
		 System.out.println("Size of output comments:" + output_comments.size());
		 System.out.println("Number of docids:" + doc_ids.size());
		 // Write DEFT output to best.xml file
		 if (model_option.equals("CRF-target") || model_option.equals("CRF-sentiment")
				 || model_option.equals("CRF-target+sentiment")) {
		 System.out.println("Writing DEFT output\n");
		 for (String annotation_id: annotation_ids) {
			 List<Comment> comments_for_this_id = new ArrayList<Comment>();
			 for (Comment comment: output_comments) {
				// if (comment.comment_id_.equals(annotation_id)) {
				 if (annotation_id.contains(comment.comment_id_)) {
					 comments_for_this_id.add(comment);
				 }
			 }
			 String file_path = FileWriter.Combine(output_dir, "csv-output");
			 file_path = FileWriter.Combine(file_path, annotation_id);
			 DeftWriter dw = new DeftWriter(comments_for_this_id);
			 // should only write the relevant offsets
			 System.out.println("Annotation id:" + annotation_id);
			 dw.WriteToCSVFileWithOffsets(annotation_id, file_path);
			 //dw.WriteToCSVFile(file_path);
		     }
		 }
		 // IF all in one file
		 /*String file_path = FileWriter.Combine(output_dir, "DEFT_out.csv");
		 String output_path = FileWriter.Combine(output_dir, "DEFT_out.best.xml");
		 DeftWriter dw = new DeftWriter(output_comments);
		 dw.WriteToCSVFile(file_path);
		 String	cmd = "python SentimentXML_from_csv_v2.py " + 
					 file_path + " > " + output_path;
		System.out.println("Writing DEFT best.xml file\n");
		System.out.println(cmd);
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(cmd);
		//Runtime.getRuntime().exec(cmd); 
		}
		catch (Exception e) { 
			e.printStackTrace();	
		}	*/
		 // dw.WriteBestXMLFile();
		 
		 // 3) Evaluate (my own system evaluation, not needed for DEFT)
		 Evaluator e = new Evaluator(output_comments, input_comments);
		 e.Print(output_dir);
		 List<Double> scores = e.Evaluate(eval_match_type);
		 e.PrintScores(scores,output_dir,eval_match_type);
		 if (all_significance) {
				e.PrintFmeasures(output_dir);
			}
	}
			
}

	
}
