/**
 * 
 */

package models;

import data.Comment;
import data.Target;
import data.Token;
import topicmodeling.CoreferenceFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

import util.BuckwalterConverter;
import util.FileReader;
import util.FileWriter;


/**
 * @author Narnoura
 * Prepares files for a simple linear-chain CRF 
 * for tagging targets and/or sentiment
 */

public class ExternalCRF {

	public HashMap<String, String> crf_config;
	public String model_file;
	public FeatureExtractor fe;
	public String output_dir;
	// Create new CRF
	public ExternalCRF() {
		this.fe = new FeatureExtractor();
	}
	public ExternalCRF(FeatureExtractor fe) {
		this.fe = fe;
	}
	public ExternalCRF(HashMap<String, String> crf_config) {
		this.crf_config = crf_config;
	}
	public ExternalCRF(String model_file) {
		this.model_file = model_file;
	}
	public ExternalCRF(String model_file, HashMap<String, String> crf_config) {
		this.crf_config = crf_config;
		this.model_file = model_file;
	}
	
	public void SetFeatureExtractor(FeatureExtractor fe) {
		this.fe = fe;
	}
	public void SetOutputDir (String output_dir) {
		this.output_dir = output_dir;
	}
	public void SetCRFConfig (HashMap<String,String> crf_config) {
		this.crf_config = crf_config;
	}
	
	// Writes feature file with train or test data
	public void WriteFeatureFile(String train_or_test,
		String input_path) {
	
		if (this.fe.input_comments.isEmpty()) {
			System.out.println("External CRF: Feature extractor has no input comments. Please set"
					+ "comments. Exiting \n");
			System.exit(0);
		}
		String feature_file = input_path + ".features";
		System.out.println("Input path:" + input_path);
		System.out.println("Feature file:" + feature_file);
		System.out.println("Extracting features \n");
		String all_features = "";
		StringBuilder builder = new StringBuilder(); // Much faster!
		int i =0;
		for (Comment c: fe.input_comments) {
			List<List<String>> data = fe.Data(c);
			for (List<String> tokens : data ) {
				String token_features = String.join(" ", tokens);
				builder.append(token_features);
				builder.append("\n");
			}
			builder.append("\n");
			i+=1;
		}
		all_features = builder.toString();
		System.out.println("Writing features");
		util.FileWriter.WriteFile(feature_file, all_features);
	}

	// May not be void. May take train parameters
	public void TrainFromFiles(String file_path) {
		
	}
	// Tests CRF-Target model from file
	public List<Comment> TestFromFile (String file_path,
			List<Comment> input_comments, boolean tokenized_space) {
		boolean skipempty = false;
		List<String> predicted_this_comment = new ArrayList<String>();
		List<Comment> output_comments = new ArrayList<Comment>();
		List<String> predicted_labels = new ArrayList<String>();
		List<String> true_labels = new ArrayList<String>();
		List<String> lines = FileReader.ReadFile(file_path, "", skipempty);
		if (lines.isEmpty()) {
			System.out.println("Lines empty! Exiting");
			System.exit(0);
		}
		int comment = 0;
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty() || line.equals(" ") || line.equals("")) {
				Comment input = fe.nontok_comments.get(comment);
				Comment output = new Comment();
				if (FeatureExtractor.include_labels) {
				List<String> gold_labels =
						fe.ExtractTokenTargetLabels(fe.input_comments.get(comment));
				true_labels.addAll(gold_labels);
				}
				if (!tokenized_space) {
				output = FindTargets(fe.nontok_comments.get(comment), predicted_this_comment);
				}
				else {
					output = FindTargetsFromTokenized(fe.nontok_comments.get(comment), 
							fe.input_comments.get(comment),
							predicted_this_comment);
				}
				output_comments.add(output);
				comment +=1;
				predicted_this_comment = new ArrayList<String>();
			}
			else {
				String[] features = new String[2];
				if (line.contains("\t")) {
				features = line.split("\t"); 	
				} else if (line.contains(" ")) {
					features = line.split(" "); 
				}
				String label = features[features.length-1];
				if (label.equals("0")) {
					label = "O";
				}
				label = label.trim();
				predicted_labels.add(label);
				predicted_this_comment.add(label);	
			}	
		}
		if (FeatureExtractor.include_labels) {
		List<Double> crf_eval = EvaluateLabels(predicted_labels, true_labels);
		}
		return output_comments;
	}
	
	// Tests CRF pos-neg-neutral ("collapsed") model from file
	public List<Comment> TestTargetsAndSentimentFromFile (String file_path,
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
				Comment input = fe.nontok_comments.get(comment);
				Comment output = new Comment();
				List<String> gold_labels =
						fe.ExtractTokenSentimentLabels(fe.input_comments.get(comment));
				true_labels.addAll(gold_labels);
				if (!tokenized_space) {
				output = FindTargetsAndSentiment(output, fe.input_comments.get(comment), 
						predicted_this_comment);
				}
				else {
				output = FindTargetsAndSentimentFromTokenized(fe.nontok_comments.get(comment), 
						fe.input_comments.get(comment),
						predicted_this_comment);
				}
				output_comments.add(output);
				comment +=1;
				predicted_this_comment = new ArrayList<String>();
			}
			else {	
				String[] features = new String[2];
				if (line.contains("\t")) {
				features = line.split("\t"); 	
				} else if (line.contains(" ")) {
					features = line.split(" "); 
				}
				String label = features[features.length-1];
				predicted_labels.add(label);
				predicted_this_comment.add(label);	
			}	
		}
		List<Double> crf_eval = EvaluateLabels(predicted_labels, true_labels);
		return output_comments;
	}
	
	// Tests CRF pos-neg-neutral ("collapsed") model from file
	// Should be in DeftExternalCRF
		public List<Comment> TestTargetsAndSentimentFromFileDEFT (String file_path,
				List<Comment> input_comments, boolean tokenized_space) {
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
					Comment output= new Comment();
					Comment input = input_comments.get(comment);
					List<String> gold_labels =
							fe.ExtractTokenSentimentLabels(fe.input_comments.get(comment));
				
					true_labels.addAll(gold_labels);
					if (!tokenized_space) {
					output = FindTargetsAndSentiment(output, input, 
							predicted_this_comment);
					}
					else {
					output = FindTargetsAndSentimentFromTokenized(
							fe.nontok_comments.get(comment), fe.input_comments.get(comment),
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
	

	// Evaluate label accuracy, precision, recall, fscore of predicted labels
	// TODO can have evaluator struct (prec, recall, fscore, acc) or make it a hash or string 
	// to return prf for all labels
	public List<Double> EvaluateLabels(List<String> predicted, List<String> gold) {
		List<Double> eval = new ArrayList<Double>();
		
		double acc = 0;
		double tot = 0;
		HashMap<String, Double> num_true = new HashMap<String,Double>();
		HashMap<String, Double> num_predicted = new HashMap<String,Double>();
		HashMap<String, Double> num_actual = new HashMap<String,Double>();

		if (predicted.size()!=gold.size()) {
			System.out.println("ExternalCRF:"
					+ "Size of predicted labels is not equal to size of gold labels. Exiting\n");
			System.out.println("Size predicted:" + predicted.size());
			System.out.println("Size gold:" + gold.size());
			System.exit(0);
		}
		for (int i =0; i<predicted.size(); i++) {
			if (predicted.get(i).equals(gold.get(i))) {
				acc +=1;
				if (num_true.containsKey(predicted.get(i))) {
				num_true.put(predicted.get(i), num_true.get(predicted.get(i))+1);
				}
				else {
					num_true.put(predicted.get(i), 1.0);
				}
			}
			if (num_predicted.containsKey(predicted.get(i))) {
				num_predicted.put(predicted.get(i), num_predicted.get(predicted.get(i))+1);
				}
				else {
					num_predicted.put(predicted.get(i), 1.0);
				}
			if (num_actual.containsKey(gold.get(i))) {
				num_actual.put(gold.get(i), num_actual.get(gold.get(i))+1);
				}
				else {
					num_actual.put(gold.get(i), 1.0);
				}
			
			tot +=1;
		}
		
		System.out.println("Correct labels:" + acc);
		acc = acc /tot *100;
		eval.add(acc);
		System.out.println("Label accuracy:" + acc);

		for (String key:  num_actual.keySet()) {
			double precision;
			double recall;
			double f_score;
			// added in Deft
			if (!num_true.containsKey(key)) {
				precision = 0.0;
				recall = 0.0;
			}
			else if (num_predicted.containsKey(key)) {
				precision = num_true.get(key)/num_predicted.get(key);
			}
			else {
				precision = 1.0;
			}
			if (num_true.containsKey(key)) {
			recall = num_true.get(key)/num_actual.get(key); }
			else {
				recall = 0.0;
			}
			if (precision == 0 || recall == 0) {
				f_score = 0.0;
			}
			else {
				f_score = 2 * precision * recall / (precision + recall);
			}
			System.out.println("Precision for " + key + ":" + precision);
			System.out.println("Recall for " + key + ":" + recall);
			System.out.println("F-score for " + key + ":" + f_score);
		}
		
		return eval;
	}
	
	public Comment FindTargets(Comment input, List<String> predicted_labels) {
		if (input.tokens_.isEmpty()) {
			System.out.println("Comment has no tokens!");
		}
		List<Target> targets = new ArrayList<Target>();
		Comment output = new Comment();
		output.SetText(input.raw_text_);
		output.SetCommentID(input.comment_id_);
		output.SetOriginalText(input.original_text);
		output.SetTokens(input.tokens_);
		output.SetAuthor(input.author);
		output.SetAuthorOffset(input.author_offset);
		output.SetEntityReader(input.entity_reader);
		if (predicted_labels.size() != input.tokens_.size()) {
			System.out.println("Size of comment tokens is not"
					+ " equal to size of predicted labels. Exiting \n");
			System.out.println("Size of comment tokens:" + input.tokens_.size());
			System.out.println("Size of predicted labels:" + predicted_labels.size());
			System.exit(0);
		}
		int i=0;
		String target = "";
		List<Token> target_tokens = new ArrayList<Token>();
		int k=0;
		for (String label: predicted_labels) {
			if (label.equals("T") || label.equals("BT") || label.equals("IT")) {
				Token t = output.tokens_.get(i);
				t.SetCommentOffset(i);
				target += t.text_ + " ";
				target_tokens.add(t);
				if ((i<predicted_labels.size()-1 && predicted_labels.get(i+1).equals("O"))
						|| i==predicted_labels.size()-1) {
					target = target.trim();
					Target output_target = new Target(target,target_tokens); // changed for DEFT
					List<Integer> offsets = new ArrayList<Integer>();
					Integer first_offset = target_tokens.get(0).comment_offset_;
					offsets.add(first_offset);
					output_target.SetOffsets(offsets);
					targets.add(output_target);
					target = "";
					target_tokens = new ArrayList<Token>();
					k=0;
				}
				// Now update the tokens themselves with target offsets
				
				t.SetTargetOffset(k);
				k+=1;
				
			} else {
				output.tokens_.get(i).SetTargetOffset(-1); // added DEFT
				input.tokens_.get(i).SetTargetOffset(-1);
			}
			
			i+=1;
		}
		output.SetTargets(targets);
		return output;
	}
	
	// For the (collapsed) CRF that predicts sentiment labels (positive, negative,
	// neutral)
		public Comment FindTargetsAndSentiment(Comment untok,
				Comment input, List<String> predicted_labels) {
			Comment output = new Comment();
			output.SetText(input.raw_text_);
			output.SetOriginalText(input.original_text);
			output.SetTokens(input.tokens_);
			output.SetCommentID(input.comment_id_);
			output.SetAuthor(input.author);
			output.SetAuthorOffset(input.author_offset);
			output.SetEntityReader(input.entity_reader);
			List<Target> targets = new ArrayList<Target>();
			if (predicted_labels.size() != input.tokens_.size()) {
				System.out.println("Size of comment tokens is not"
						+ " equal to size of predicted labels. Exiting \n");
				System.out.println("Size of comment tokens:" + input.tokens_.size());
				System.out.println("Size of predicted labels:" + predicted_labels.size());
				System.out.println("Predicted labels:");
				for (int p=0;p<predicted_labels.size();p++) {
					System.out.println(predicted_labels.get(p)+ " ");
				}
				System.out.println("Comment labels:");
				for (int k=0;k<input.tokens_.size();k++) {
					System.out.println(input.tokens_.get(k).text_ + " ");
				}
				System.exit(0);
			}
			int i=0;
			String target = "";
			List<Token> target_tokens = new ArrayList<Token>();
			int k=0;
			for (String label: predicted_labels) {
				if (label.equals("positive") || label.equals("negative")
						|| label.equals("pos") || label.equals("neg")) {
					Token t = input.tokens_.get(i);
					target += t.text_ + " ";
					t.SetCommentOffset(i);
					target_tokens.add(t);
					if ((i<predicted_labels.size()-1 && !predicted_labels.get(i+1).equals(label))
							|| i==predicted_labels.size()-1) {
						target = target.trim();
						Target output_target = new Target(target,target_tokens);
						output_target.SetSentiment(label);
						List<Integer> offsets = new ArrayList<Integer>();
						//offsets.add(i-k);
						Integer first_offset = target_tokens.get(0).comment_offset_;
						offsets.add(first_offset);
						output_target.SetOffsets(offsets);
						targets.add(output_target);
						target = "";
						target_tokens = new ArrayList<Token>();
						k=0;
					}
					// Now update the tokens themselves with target offsets
					t.SetTargetOffset(k);
					k+=1;
				}
				i+=1;
			}
			output.SetTargets(targets);
			return output;
		}
	
	// The word will be mapped to its original untokenized form
	//
	// So, if a word 'AlRa2ys' is tokenized into 'Al+' and 'Ra2ys'
	// and only one of them is tagged as T, the original word
	// 'AlRa2ys' will be tagged as T
	public Comment FindTargetsFromTokenized(Comment untok,
			Comment input, List<String> predicted_labels) {
		
		List<Target> targets = new ArrayList<Target>();
		if (predicted_labels.size() != input.tokens_.size()) {
			System.out.println("Size of comment tokens is not"
					+ " equal to size of predicted labels. Exiting \n");
			System.out.println("Size of comment tokens:" + input.tokens_.size());
			System.out.println("Size of predicted labels:" + predicted_labels.size());
			System.exit(0);
		}
		int i=0;
		String target = "";
		List<Token> target_tokens = new ArrayList<Token>();
		int k=0;
		for (String label: predicted_labels) {
			if (label.equals("T") || label.equals("BT") || label.equals("IT")) {
				Token t = input.tokens_.get(i);
				int original_offset = t.comment_offset_;
				Token original = untok.tokens_.get(original_offset);
				if (original.text_.contains("@@LAT@@")) {
					continue;
				}
				if (!target_tokens.contains(original)) {
				target += original.text_ + " ";
				target_tokens.add(original);
				}
				if ((i<predicted_labels.size()-1 && predicted_labels.get(i+1).equals("O"))
						|| i==predicted_labels.size()-1) {
					target = target.trim();
					Target output = new Target(target,target_tokens);
					List<Integer> offsets = new ArrayList<Integer>();
					Integer first_offset = target_tokens.get(0).comment_offset_;
					offsets.add(first_offset);
					//offsets.add(original_offset-k);
					output.SetOffsets(offsets);
					targets.add(output);
					target = "";
					target_tokens = new ArrayList<Token>();
					k=0;
				}
				// Now update the tokens themselves with target offsets
				t.SetTargetOffset(k);
				k+=1;
			} else {
				input.tokens_.get(i).SetTargetOffset(-1);
			}
			i+=1;
		}
		input.SetTargets(targets);
		// Keep original text when returning output comments
		input.SetText(untok.raw_text_);
		return input;
	}
	
	// For the (collapsed) CRF that predicts sentiment labels (positive, negative,
	// neutral) // parameters: output, fe.input_comments.get(), predicted_this_comment
	public Comment FindTargetsAndSentimentFromTokenized(Comment untok,
			Comment input, List<String> predicted_labels) {
		
		List<Target> targets = new ArrayList<Target>();
		if (predicted_labels.size() != input.tokens_.size()) {
			System.out.println("Size of comment tokens is not"
					+ " equal to size of predicted labels. Exiting \n");
			System.out.println("Size of comment tokens:" + input.tokens_.size());
			System.out.println("Size of predicted labels:" + predicted_labels.size());
			System.exit(0);
		}
		
		int i=0;
		String target = "";
		List<Token> target_tokens = new ArrayList<Token>();
		int k=0;
		for (String label: predicted_labels) {
			if (label.equals("positive") || label.equals("negative")
				||	label.equals("pos") || label.equals("neg")
				) {
				Token t = input.tokens_.get(i);
				int original_offset = t.comment_offset_;
				Token original = untok.tokens_.get(original_offset);
				original.SetSentiment(label);
				if (original.text_.contains("@@LAT@@")) {
					continue;
				}
				if (!target_tokens.contains(original)) {
				target += original.text_ + " ";
				target_tokens.add(original);
				}
				// each block of pos or neg tokens together will be considered a target
				if ((i<predicted_labels.size()-1 && !predicted_labels.get(i+1).equals(label))
						|| i==predicted_labels.size()-1) {
					target = target.trim();
					Target output_target = new Target(target,target_tokens);
					output_target.SetSentiment(label);
					List<Integer> offsets = new ArrayList<Integer>();
					Integer first_offset = target_tokens.get(0).comment_offset_;
					offsets.add(first_offset);
					output_target.SetOffsets(offsets);
					targets.add(output_target);
					target = "";
					target_tokens = new ArrayList<Token>();
					k=0;
				}
				// Now update the tokens themselves with target offsets
				t.SetTargetOffset(k);
				k+=1;
			}
			i+=1;
		}
		input.SetTargets(targets);
		// Keep original text when returning output comments
		input.SetText(untok.raw_text_);
		return input;
	}
}
