package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import data.Comment;
import util.FileReader;

// I already had it!!! Move files needed here
public class DeftExternalCRF extends ExternalCRF {
	
	public HashMap<String, String> crf_config;
	public String model_file;
	public String output_dir;
	public DeftFeatureExtractor fe;
	
	public DeftExternalCRF() {
		super();
	}
	public DeftExternalCRF(DeftFeatureExtractor fe) {
		this.fe = fe;
	}
	public DeftExternalCRF(String model_file) {
		super(model_file);
	}
	// Needed because it takes DeftFeatureExtractor rather than 
	// FeatureExtractor
	public void SetFeatureExtractor(DeftFeatureExtractor fe) {
		this.fe = fe;
	}
	
	// WriteFeatureFile won't work because it accesses FeatureExtractor 
	// rather than DeftFeatureExtractor
	public void DEFTWriteFeatureFile(String train_or_test,
			String input_path) {
			
			if (fe.input_comments.isEmpty()) {
				System.out.println("External CRF: Feature extractor has no input comments. Please set"
						+ "comments. Exiting \n");
				System.exit(0);
			}

			String feature_file = input_path + ".features";
			System.out.println("Input path:" + input_path);
			System.out.println("Feature file:" + feature_file);
			System.out.println("Extracting features \n");
			String all_features = "";
			for (Comment c: fe.input_comments) {
				List<List<String>> data = fe.DeftData(c);
				for (List<String> tokens : data ) {
					String token_features = String.join(" ", tokens);
					all_features += token_features + "\n";
				}
				all_features += "\n";
				//fw.WriteSentence(data);
			}
			System.out.println("Writing features");
			util.FileWriter.WriteFile(feature_file, all_features);
			//fw.Complete();
		}
	
	// Tests CRF-Target model from file
		public List<Comment> TestFromFileDEFT (String file_path,
				List<Comment> input_comments, boolean tokenized_space) {
			/*System.out.println("TEST FILE FROM DEFT");
			for (Comment c: input_comments) {
				if (c.tokens_.isEmpty()) {
					System.out.println("Just after runner: comment has no tokens. Fuck this.");
				}
			}*/
			boolean skipempty = false;
			List<String> predicted_this_comment = new ArrayList<String>();
			List<Comment> output_comments = new ArrayList<Comment>();
			List<String> predicted_labels = new ArrayList<String>();
			List<String> true_labels = new ArrayList<String>();
			List<String> lines = FileReader.ReadFile(file_path, "english", skipempty);
			if (lines.isEmpty()) {
				System.out.println("Lines empty! Exiting");
				System.exit(0);
			}
			int comment = 0;
			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty() || line.equals(" ") || line.equals("")) {
					Comment input = input_comments.get(comment); // DEFT
					if (input.tokens_.isEmpty()) {
						System.out.println("Input comment has no tokens!");
					}
					// For nontokenized space, we can also use input_comments. For tokenized,
					// have to use nontok_Comments
					Comment output = new Comment();
					if (FeatureExtractor.include_labels) {
						List<String> gold_labels =
							fe.ExtractTokenTargetLabels(input);
							true_labels.addAll(gold_labels);
					}
					output = this.FindTargets(input_comments.get(comment), predicted_this_comment);
					
					output_comments.add(output);
					comment +=1;
					predicted_this_comment = new ArrayList<String>();
				}
				else {
					//String[] features = line.split(" ");
					String[] features = line.split("\t"); 	
					String label = features[features.length-1];
					predicted_labels.add(label);
					predicted_this_comment.add(label);	
				}	
			}
			if (FeatureExtractor.include_labels) {
			List<Double> crf_eval = EvaluateLabels(predicted_labels, true_labels);
			}
			return output_comments;
		}
		
		public List<Comment> TestTargetsAndSentimentFromFileDeft (String file_path,
				List<Comment> input_comments, boolean tokenized_space) {
			System.out.println("External CRF: Reading sentiment predictions from this file:" + file_path);
			boolean skipempty = false;
			List<String> predicted_this_comment = new ArrayList<String>();
			List<Comment> output_comments = new ArrayList<Comment>();
			List<String> predicted_labels = new ArrayList<String>();
			List<String> true_labels = new ArrayList<String>();
			
			List<String> lines = FileReader.ReadFile(file_path, "", skipempty);
			int comment = 0;
		
			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty() || line.equals(" ") || line.equals("")) {
		
					Comment input = input_comments.get(comment);
					// For nontokenized space, we can also use input_comments. For tokenized,
					// have to use nontok_Comments
					Comment output = new Comment();
					List<String> gold_labels =
							fe.ExtractTokenSentimentLabels(input);
					true_labels.addAll(gold_labels);
					if (!tokenized_space) {
					output = this.FindTargetsAndSentiment(output, input, 
							predicted_this_comment);
					}
					else {
					output = FindTargetsAndSentimentFromTokenized(fe.nontok_comments.get(comment), 
							input,
							predicted_this_comment);
					}
					output_comments.add(output);
					comment +=1;
					predicted_this_comment = new ArrayList<String>();
				}
				else {
					String[] features = line.split("\t"); 	
					String label = features[features.length-1];
					predicted_labels.add(label);
					predicted_this_comment.add(label);	
				}	
			}
			List<Double> crf_eval = EvaluateLabels(predicted_labels, true_labels);
			return output_comments;
		}

}
