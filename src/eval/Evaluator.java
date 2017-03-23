
package eval;

import java.util.List;
import java.io.File;
import java.util.ArrayList;

import util.BuckwalterConverter;
import util.FileWriter;
import util.Tokenizer;
import data.Comment;
import data.DeftTarget;
import data.Target;
import data.Token;


/**
 * @author Narnoura
 * Evaluate target and sentiment classification
 * Mostly assumes that input to evaluator is in BW
 */


public class Evaluator {
	public List<Comment> output_comments;
	public List<Comment> gold_comments;
	public String metrics;
	public List<List<Target>> matching_targets;
	public static boolean exclude_undetermined = false;
	public static String encoding = "arabic"; 
	public Double num_gold_targets_with_sentiment = 0.0;
	public Double num_predicted_targets_with_sentiment = 0.0;
	

	public Evaluator() {}
	public Evaluator(List<Comment> output_comments,
			List<Comment> gold_comments) {
		matching_targets = new ArrayList<List<Target>>();
		this.output_comments = output_comments;
		this.gold_comments = gold_comments;
	}
	
	public void Set(List<Comment> output_comments, List<Comment> gold_comments) {
			num_gold_targets_with_sentiment = 0.0;
			num_predicted_targets_with_sentiment = 0.0;
			this.output_comments = output_comments;
			this.gold_comments = gold_comments;
			matching_targets = new ArrayList<List<Target>>();
	}

	// 0: recall
	// 1: precision
	// 2: f-measure
	// 3: f-pos
	// 4: f-neg
	// 5: f-sent =  ( f-pos + f-neg ) / 2
	// 6: accuracy
	public  List<Double> Evaluate(String match_type) {
		boolean exclude_undetermined = true; 
		List<Double> scores = GetPrecisionRecallFMeasure(match_type);
		List<Double> sentiment_scores = EvaluateSentiment(exclude_undetermined);
		scores.addAll(sentiment_scores);
		return scores;
	} 
	
	// Evaluates the sentiment accuracy, P,R,F for *matched targets*
	public List<Double> EvaluateSentiment(boolean exclude_undetermined) {
		if (matching_targets.isEmpty()) {
			System.out.println("\nEvaluator: Matched targets empty. Please evaluate correct targets in order to evaluate"
					+ "sentiment. Exiting \n");
			System.exit(0);
		}
		List<Double> scores = new ArrayList<Double>();
		double accuracy = 0;
		double num_matched = 0; 
		double num_matched_pos = 0;
		double num_matched_neg = 0;
		double num_pos_in_matching_targets = 0;
		double num_neg_in_matching_targets = 0;
		double num_predicted_pos = 0;
		double num_predicted_neg = 0;
		// accuracy over all targets
		double total_accuracy = 0; 
		double overall_precision = 0;
		double overall_f_measure = 0;
		
		
		for (List<Target> pair : matching_targets) {
			if (pair.isEmpty()) {
				System.out.println("Evaluator: Matched pair empty. Please fill up matched target pairs"
						+ "in order to evaluate sentiment. \n");
				System.exit(0);
			}
			Target gold_target = pair.get(0);
			Target predicted_target = pair.get(1);
			
			if (encoding.equals("english")) {
				System.out.println("\nGold target:" + gold_target.text_ 
				        +  " " + gold_target.sentiment_);
				System.out.println("Predicted target:" + 
				         predicted_target.text_ +
				          " " + predicted_target.sentiment_);
			} else {
				// COMMENTED OUT FOR APPROXIMATE RAND SIGNIFICANCE PRINTING
			System.out.println("\nGold target:" +
			        BuckwalterConverter.ConvertToUTF8(gold_target.text_) 
			        +  " " + gold_target.sentiment_);
			System.out.println("Predicted target:" + 
			         BuckwalterConverter.ConvertToUTF8(predicted_target.text_)
			         +  " " + predicted_target.sentiment_);
			}

			try {
			if (gold_target.sentiment_.equals("undetermined")) {
				this.num_predicted_targets_with_sentiment -=1; // don't count in overall precision, not fair
				continue;
			}
			num_matched +=1;
			if (gold_target.sentiment_.equals(predicted_target.sentiment_)) {
				accuracy +=1;
				if (gold_target.sentiment_.equals("positive")) {
					num_matched_pos +=1; }
				else if (gold_target.sentiment_.equals("negative")) {
					num_matched_neg +=1;
				}
			}
			// Actual sentiments in matching targets
			if (gold_target.sentiment_.equals("positive")) {
				num_pos_in_matching_targets +=1;
			} else if (gold_target.sentiment_.equals("negative")) {
				num_neg_in_matching_targets +=1;
			}
			// Predicted sentiments in matching targets
			if (predicted_target.sentiment_ != null) {
			if (predicted_target.sentiment_.equals("positive")) {
				num_predicted_pos +=1;
			}  else if (predicted_target.sentiment_.equals("negative")) {
				num_predicted_neg +=1;
			}
			}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}	// end for

		System.out.println("Total number of targets with unambiguous sentiment:" + this.num_gold_targets_with_sentiment);
		System.out.println("Number of correctly predicted targets: " + num_matched);
		System.out.println("Percentage of correctly predicted positive targets: " + num_matched_pos/accuracy );
		System.out.println("Percentage of correctly predicted negative targets: " + num_matched_neg/accuracy);
		System.out.println("Number of correctly predicted sentiment targets: " + accuracy);
		
		overall_precision = 100 * accuracy/this.num_predicted_targets_with_sentiment;
		total_accuracy = 100 * accuracy/this.num_gold_targets_with_sentiment;
		overall_f_measure = 2 * overall_precision * total_accuracy / (overall_precision + total_accuracy);
		accuracy = 100 * accuracy/num_matched;
		
		System.out.println("Sentiment accuracy: " + accuracy);
		
		double pos_precision = 1;
		double pos_recall  = 1;
		double neg_precision = 1;
		double neg_recall = 1;
		if (num_pos_in_matching_targets!=0) {
			pos_recall = num_matched_pos/num_pos_in_matching_targets;
		}
		if (num_predicted_pos!=0) {
		pos_precision = num_matched_pos/num_predicted_pos;
		}
		double pos_f = 2 * pos_precision * pos_recall / (pos_precision + pos_recall);
		
		if (num_neg_in_matching_targets!=0) {
			neg_recall = num_matched_neg/num_neg_in_matching_targets;
		}
		if (num_predicted_neg!=0) {
		neg_precision = num_matched_neg/num_predicted_neg;
		}
		double neg_f = 2 * neg_precision * neg_recall / (neg_precision + neg_recall);
		
		System.out.println("Recall for positive targets: " + pos_recall);
		System.out.println("Precision for positive targets: " + pos_precision);
		System.out.println("F-measure for positive targets: " + pos_f);
		System.out.println("Recall for negative targets: " + neg_recall);
		System.out.println("Precision for negative targets: " + neg_precision);
		System.out.println("F-measure for negative targets: " + neg_f);
		
		System.out.println("Overall recall: " + total_accuracy);
		System.out.println("Overall precision: " + overall_precision);
		System.out.println("Overall f-measure: " + overall_f_measure);
		
		scores.add(pos_f*100);
		scores.add(neg_f*100);
		scores.add(100*(pos_f+neg_f)/2);
		scores.add(accuracy);
		scores.add(total_accuracy);
		scores.add(overall_f_measure);
		return scores;
	}
	
	public Double GetTargetRecall (String match_type) {
		if (this.gold_comments.isEmpty() || this.output_comments.isEmpty()) {
			return null;
		}
		double match_true = 0;
		double num_gold_targets = 0;
		int i=0;
		for (Comment c: gold_comments) {
			Comment output = output_comments.get(i);
			assert(c.comment_id_.equals(output.comment_id_));
			if (c.targets_ != null) {
			for (Target g: c.targets_) {
				if (exclude_undetermined && g.sentiment_.equals("undetermined")) {
					continue;
				}
				if (!g.sentiment_.equals("undetermined")) {
					this.num_gold_targets_with_sentiment +=1;
				}
				boolean found = false;
				num_gold_targets +=1;
				if (output.targets_ !=null) {
				for (Target o: output.targets_) { 	
					double score = CompareTargets(g,o,match_type,c,output);
					if (score  > 0) {
						match_true += score;
						List<Target> gold_out_pair = new ArrayList<Target>();
						g.SetCommentID(c.comment_id_);
						o.SetCommentID(c.comment_id_);
						gold_out_pair.add(g);
						gold_out_pair.add(o);
						matching_targets.add(gold_out_pair); 
						found = true;
						break;
					}
				}
			   } // end output targets not null
			} // end for target g
			} // end  c targets not null
		    i+=1;
		} // end for gold comments
		System.out.println ("\nRecall: Number of correct matching targets: " + match_true);
		System.out.println ("Recall: Number of gold targets: " + num_gold_targets);
		System.out.println ("Recall: " + match_true / num_gold_targets);
		if (num_gold_targets !=0 ) {
			return	match_true / num_gold_targets;
		}
		else{
			return 1.0;
		}
	}

	public Double GetTargetPrecision (String match_type) {	
		if (this.gold_comments.isEmpty() || this.output_comments.isEmpty()) {
			return null;
		}
		double match_true = 0;
		double num_predicted_targets = 0;
		int i=0;
		for (Comment c: output_comments) {
			Comment gold = gold_comments.get(i);
			if (c.targets_ != null) {
			for (Target o: c.targets_) {
				num_predicted_targets +=1;
				if (gold.targets_ != null) {
				for (Target g: gold.targets_) {
					if (exclude_undetermined && g.sentiment_.equals("undetermined")) {
						continue; }
					double score = CompareTargets(g,o,match_type,gold,c);
					if (score  > 0) {
						match_true += score;
						break;
						}
					} // gold loop
				} // gold not null
			} // predicted 
		} // targets not null
		i+=1;
		} // comments
	
		System.out.println ("\nPrecision: Number of correct matching targets: " + match_true);
		System.out.println ("Precision: Number of predicted targets: " + num_predicted_targets);
		System.out.println ("Precision: "  + match_true / num_predicted_targets);
		this.num_predicted_targets_with_sentiment = num_predicted_targets; 
		if (num_predicted_targets !=0 ) {
			return	match_true / num_predicted_targets;
		} else{
			return 1.0;
		}
		
	}
	
	public List<Double> GetCommentFmeasures(Comment pred, Comment gold) {
		List<Double> f_measures = new ArrayList<Double>();
		
		List<Double> overlap = GetCommentFmeasure(pred,gold,"overlap");
		f_measures.addAll(overlap);
		List<Double> subset_overlap = GetCommentFmeasure(pred,gold,"subset-overlap");
		f_measures.addAll(subset_overlap);
		List<Double> prop_overlap = GetCommentFmeasure(pred,gold,"prop-overlap");
		f_measures.addAll(prop_overlap);
		List<Double> subset = GetCommentFmeasure(pred,gold,"subset");
		f_measures.addAll(subset);
		List<Double> exact = GetCommentFmeasure(pred,gold,"exact");
		f_measures.addAll(exact);
	
		return f_measures;
	}
	
	// Returns fmeasure for target and sentiment according to match type
	// Fmeasure for sentiment depends on the targets matched in recall
	public List<Double> GetCommentFmeasure(Comment pred, Comment gold, String match_type) {
		List<Double> pair = new ArrayList<Double>();
		Double fmeasure = 0.0;
		Double sentiment_fmeasure = 0.0; // average f-measure
		Double recall = 0.0;
		Double prec = 0.0;
		Double match_true = 0.0;
		Double match_true_r = 0.0;
		// Sentiment variables
		Double num_matched_pos = 0.0;
		Double num_matched_neg = 0.0;
		Double num_pos_in_matching_targets = 0.0;
		Double num_neg_in_matching_targets = 0.0;
		Double num_predicted_pos = 0.0;
		Double num_predicted_neg = 0.0;
		
		// Precision
		if (pred.targets_ !=null && pred.targets_.size()!=0) {
		for (Target t: pred.targets_) {
			if (gold.targets_ != null) {
			for (Target g: gold.targets_) {
				if (exclude_undetermined && g.sentiment_.equals("undetermined")) {
					continue; }
				double score = CompareTargets(g,t,match_type,gold,pred);
				if (score  > 0) {
					match_true += score;
					break;
					}
				} // gold loop
			} // gold not null
		} // predicted 
		prec = match_true/pred.targets_.size();
		}// targets not null
		else {
			prec = 1.00;
		}
		
		// Recall
		if (gold.targets_ !=null && gold.targets_.size()!=0) {
			for (Target g: gold.targets_) { 
				if (pred.targets_ != null) {
					for (Target t: pred.targets_) {
						if (exclude_undetermined && g.sentiment_.equals("undetermined")) {
							continue; }
						double score = CompareTargets(g,t,match_type,gold,pred);
						if (score  > 0) {
							match_true_r += score;
							if (g.sentiment_.equals("undetermined")) {
								break;
							}
							if (g.sentiment_.equals(t.sentiment_)) {
								if (g.sentiment_.equals("positive")) {
									num_matched_pos +=1; }
								else if (g.sentiment_.equals("negative")) {
									num_matched_neg +=1;
								}
							}
							// Actual sentiments in matching targets
							if (g.sentiment_.equals("positive")) {
								num_pos_in_matching_targets +=1;
							} else if (g.sentiment_.equals("negative")) {
								num_neg_in_matching_targets +=1;
							}
							// Predicted sentiments in matching targets
						
							if (t.sentiment_.equals("positive")) {
								num_predicted_pos +=1;
							}  else if (t.sentiment_.equals("negative")) {
								num_predicted_neg +=1;
							}
							break;
							}
						} 
				}
			}
			recall = match_true_r/gold.targets_.size();
		} // gold targets not null
		else {
			recall = 1.0;
		}
		
		if (recall == 0.0 && prec == 0.0) {
			fmeasure = 0.0;
		} else {
			fmeasure = 2* recall* prec / (recall + prec);
		}
		
		// Sentiment
		double pos_precision = 1;
		double pos_recall  = 1;
		double neg_precision = 1;
		double neg_recall = 1;
		if (num_pos_in_matching_targets!=0) {
			pos_recall = num_matched_pos/num_pos_in_matching_targets;
		}
		if (num_predicted_pos!=0) {
		    pos_precision = num_matched_pos/num_predicted_pos;
		}
		double pos_f = 0;
		if (pos_precision!=0 || pos_recall !=0) {
			 pos_f = 2 * pos_precision * pos_recall / (pos_precision + pos_recall);
		}
		
		if (num_neg_in_matching_targets!=0) {
			neg_recall = num_matched_neg/num_neg_in_matching_targets;
		}
		if (num_predicted_neg!=0) {
		    neg_precision = num_matched_neg/num_predicted_neg;
		}
		double neg_f = 0;
		if (neg_precision!=0 || neg_recall !=0) {
		 neg_f = 2 * neg_precision * neg_recall / (neg_precision + neg_recall);
		}
		sentiment_fmeasure = (pos_f + neg_f) / 2 ;
		
		pair.add(fmeasure);
		pair.add(sentiment_fmeasure);
		
		return pair;
	}
	
	
	// Print table of f-measures for each comment, for pairwise t-test
	public void PrintFmeasures(String output_dir) {
		String file =  (new File(output_dir, "fmeasures.out").getAbsolutePath());
		// First line:
		// Comment_id /t Overlap /t Subset-overlap /t Prop/t Subset /t Exact /t Sentiment
		String first_line = "Comment_id\t Overlap\t Overlap-sent\t Subset-overlap\t Subset-overlap-sent\t "
				+ " Prop\t Prop-sent\t Subset\t Subset-sent\t Exact\t Exact-sent\n";
		int i =0;
		StringBuilder builder = new StringBuilder();
		builder.append(first_line);
		for (Comment c: output_comments) {
			Comment gold = gold_comments.get(i);
			List<Double> fmeasures = GetCommentFmeasures(c,gold);
			builder.append(c.comment_id_ + "\t");
			for (int j=0; j<fmeasures.size(); j++) {
				builder.append(fmeasures.get(j) + "\t");
			}
			builder.append("\n");
			i+=1;
		}
		String output = builder.toString();
		FileWriter.WriteFile(file, output); 
	}
	
	public void PrintScores (List<Double> scores, String output_dir, String match_type) {
		String file =  (new File(output_dir, match_type +"_allscores.out").getAbsolutePath());
		String first_line = "Recall\t Precision\t F-measure\t F-pos\t F-neg\t "
				+ " Average F\t Accuracy\t Overall accuracy \t Overall F\n";
		StringBuilder builder = new StringBuilder();
		builder.append(first_line);
		for (int j=0; j <scores.size(); j++) {
			builder.append(scores.get(j) + "\t");
		}
		builder.append("\n");
		String output = builder.toString();
		FileWriter.WriteFile(file, output);
	}

	// Prints the comments with the gold and predicted targets for analysis
	// Convert to utf8 for viewing and output
	public void Print(String output_dir) {
		int i=0;
		for (Comment c: output_comments) {
			Comment gold = gold_comments.get(i);
			System.out.println("\nOutput comment:" + c.comment_id_);
			// Will print the tokenized version
			if (encoding.equals("english")) {
				System.out.println(c.raw_text_);
				System.out.println("Author:" + c.author + " " + c.author_offset);
			} else {
			System.out.println(BuckwalterConverter.ConvertToUTF8(c.raw_text_));
			}
			System.out.println("Gold targets:");
			if (gold.targets_ != null) {
			for (Target g: gold.targets_) {
				if (encoding.equals("english")) {
					System.out.println(g.text_);
				} else {
				System.out.println(BuckwalterConverter.ConvertToUTF8(g.text_) 
						+ ":" + g.sentiment_);
				}
				}
			}
			else {
				System.out.println("No gold targets \n");
			}
			
			System.out.println("Predicted targets:");
			System.out.println("Number of predicted targets:" + c.targets_.size());
			if (c.targets_ != null) {
			for (Target o: c.targets_) {
				//System.out.println(o.text_ + ":" + o.sentiment_);
				if (encoding.equals("english")) {
					System.out.println(o.text_ + ":" + o.sentiment_);
				}
				else {
				System.out.println(BuckwalterConverter.ConvertToUTF8(o.text_ )
						+ ":" + o.sentiment_);
				}
				}
			}
			else {
				System.out.println("No predicted targets \n");
			}
			
			i+=1;
		}	
	}

	boolean CompareText(String t1, String t2, String match_type) {
		t1 = t1.trim();
		t2 = t2.trim();
		if (match_type.equals("exact")) {
			return (t1.equals(t2));
		}
		else if (match_type.equals("subset")) {
			return (t1.contains(t2) || t2.contains(t1));
		}
		// assumes BW
		else if (match_type.equals("norm_match")) {
			return (Tokenizer.ArabicNormalize(t1).contains(Tokenizer.ArabicNormalize(t2))
					|| Tokenizer.ArabicNormalize(t2).contains(Tokenizer.ArabicNormalize(t1)));
		}
		else if (match_type.equals("norm_match_exact")) {
			return (Tokenizer.ArabicNormalize(t1).equals(Tokenizer.ArabicNormalize(t2)));
		}
		else {
			System.out.println("Please specify a valid match type for string match \n");
			return false;
		}
		
	}
	
	public Double CompareTargets(Target gold, Target predicted, String match_type,
			Comment g, Comment c) {
		double match = 0.0;
		if (match_type.equals("overlap")) {
			boolean overlap = HasOverlap(gold,predicted);
			if (overlap) {
				match =1.0;
			}
		}
		else if (match_type.equals("prop-overlap")) {
			match = GetProportionalOverlap(gold,predicted);
			
		} 
		// same as subset but token-based ('overlap' is not the right word)
		else if (match_type.equals("subset-overlap")) {
			if (HasSubsetOverlap(gold,predicted) || HasSubsetOverlap(predicted,gold)) {
				match = 1.0;
			}
		}
		else if (match_type.equals("mention-overlap")) {
			boolean overlap = HasMentionOverlap(gold,predicted,g,c);
			if (overlap) {
				match =1.0;
			}
		}
		else if (match_type.equals("prop-mention")) {
			match = GetProportionalMentionOverlap(gold,predicted,c);
		}
		else  {
			boolean matched = CompareText(gold.text_,predicted.text_,match_type);
			if (matched) {
				match = 1.0;
			}
		}
		return match;
	}
	
	// Quite lenient
	public boolean HasOverlap(Target t1, Target t2) {
		boolean has_overlap = false;
		List<Token> t1_tokens = t1.tokens_;
		List<Token> t2_tokens = t2.tokens_;
		for (Token tok1: t1_tokens) {
			for (Token tok2: t2_tokens) {
				if (tok1.text_.equals(tok2.text_)) {
					has_overlap = true;
					break;
				}
			}
			if (has_overlap == true) {
				break;
			}
		}
		return has_overlap;
	}

	public boolean HasMentionOverlap(Target t1, Target t2, Comment c1, Comment c2) {
		boolean has_overlap = false;
		List<Token> t1_tokens = t1.tokens_;
		List<Token> t2_tokens = t2.tokens_;
		List<Integer> indices_1 = t1.comment_offsets_;
		List<Integer> indices_2 = t2.comment_offsets_;
		
		for (Token tok1: t1_tokens) {
			for (Token tok2: t2_tokens) {
				if (tok1.text_.equals(tok2.text_)) {
					for (Integer begin_1: indices_1) {
						for (Integer begin_2: indices_2) {
							int end_1 = begin_1 + t1_tokens.size() - 1;
							int end_2 = begin_2 + t2_tokens.size() - 1;
							if ((begin_2 >= begin_1 && begin_2 <= end_1) 
									|| (begin_1 >= begin_2 && begin_1 <= end_2)) {
								has_overlap = true;
								break;
							}
						} // ind2
						if (has_overlap==true) {
							break;
						}
					} // ind1
				} // end if
				if (has_overlap == true) {
					break;
				}
			}
			if (has_overlap == true) {
				break;
			}
		}
		
		return has_overlap;
	}
	
	public Double GetProportionalMentionOverlap(Target gold, Target predicted,Comment c) {
		double prop_overlap = 0;
		boolean has_overlap = true;
		List<Token> gold_tokens = gold.tokens_;
		List<Token> pred_tokens = predicted.tokens_;
		List<Integer> indices_1 = gold.comment_offsets_;
		List<Integer> indices_2 = predicted.comment_offsets_;
		
		for (Token tok1: gold_tokens) {
			for (Token tok2: pred_tokens) {
				if (tok1.text_.equals(tok2.text_)) {
					for (Integer begin_1: indices_1) {
						for (Integer begin_2: indices_2) {
							int end_1 = begin_1 + gold_tokens.size() - 1;
							int end_2 = begin_2 + pred_tokens.size() - 1;
							if ((begin_2 >= begin_1 && begin_2 <= end_1) 
									|| (begin_1 >= begin_2 && begin_1 <= end_2)) {
								prop_overlap +=1;
								has_overlap = true;
								break;
							}
						} // ind2
						if (has_overlap==true) {
							break;
						}
					} // ind1
				} // end if
				if (has_overlap == true) {
					break;
				}
			}
			if (has_overlap == true) {
				break;
			}
		}
		
		prop_overlap = prop_overlap/pred_tokens.size();
		return prop_overlap;
	}
	
	// Returns true if t2 is inside or equal to t1
	public boolean HasSubsetOverlap(Target t1, Target t2) {
		boolean has_overlap = false;
		List<Token> t1_tokens = t1.tokens_;
		List<Token> t2_tokens = t2.tokens_;
		if (t2.tokens_.size() > t1.tokens_.size()) {
			return false;
		}
		for (int i=0; i< t1_tokens.size(); i++) {
			boolean found_here = false;
			for (int j=0; j<t2_tokens.size(); j++) {
				found_here = true;
				if (j+i >= t1_tokens.size() || 
						!t2_tokens.get(j).text_.equals(t1_tokens.get(j+i).text_)) {
					found_here = false;
					break;
				}
			}
			if (found_here == true) {
				has_overlap = true;
				break;
			} 
			}
	  return has_overlap;
	}
	
	public Double GetProportionalOverlap(Target gold, Target predicted) {
		double prop_overlap = 0;
		List<Token> gold_tokens = gold.tokens_;
		List<Token> pred_tokens = predicted.tokens_;
		for (Token g: gold_tokens) {
			for (Token p: pred_tokens) {
				if (g.text_.equals(p.text_)) {
					prop_overlap+=1;
					break;
				}
			}
		}
		prop_overlap = prop_overlap/pred_tokens.size();
		return prop_overlap;
	}

	public List<Double> GetPrecisionRecallFMeasure (String match_type) {
		List<Double> scores = new ArrayList<Double>();
		double r = GetTargetRecall(match_type);
		double p = GetTargetPrecision(match_type);
		double f = 0;
		if (p == 0.0 && r==0.0) {
			f= 0.0;
		}
		else{
			f=  (2*p*r)/(p+r);
		}
		System.out.println ("\nF-measure "  + f);
		f=f*100;
		p=p*100;
		r=r*100;
		
		scores.add(r);
		scores.add(p);
		scores.add(f);
		return scores;
	}	
}
