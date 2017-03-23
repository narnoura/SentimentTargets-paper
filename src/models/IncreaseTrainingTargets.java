package models;

import java.util.ArrayList;
import java.util.List;

import data.*;
import topicmodeling.CoreferenceFeatures;
import topicmodeling.TopicSalience;

public class IncreaseTrainingTargets {
/*
 *  Increase training data for targets by adding annotation labels to string-matched, lemma-matched,
 *  co-referring or similar targets. Unsupervised module.
 *  
 *  Note that this module should be called during Runner (SetupFeatures) AFTER comment tokens
 *  are updated with tokenization and target info  (e.g After UpdateCommentsWithTargets
 *  and UpdateTokenized calls)
 */
	public List<Comment> input_comments;
	
	// For updating coreferrent targets
	// public static boolean replace_morph_features = true;
	
	public IncreaseTrainingTargets() {
		// TODO Auto-generated constructor stub
	}
	public IncreaseTrainingTargets(List<Comment> input_comments) {
		this.input_comments = input_comments;
	}
	public void SetComments(List<Comment> input_comments) {
		this.input_comments = input_comments;
	}
	// This will alter input_comments
	public List<Comment> IncreaseTrainingByLemma(List<Comment> input_comments, boolean update_clitics){
		List<Comment> output_comments = new ArrayList<Comment>();
		for (Comment c: input_comments) {
			output_comments.add(IncreaseByLemmaMatch(c, update_clitics));
		}
		return output_comments;
	}
	public List<Comment> IncreaseTrainingByWordMatch(List<Comment> input_comments){
		List<Comment> output_comments = new ArrayList<Comment>();
		for (Comment c: input_comments) {
			output_comments.add(IncreaseByWordMatch(c));
		}
		return output_comments;
	}
	public List<Comment> IncreaseTrainingWithFilters(List<Comment> input_comments, boolean update_clitics){
		List<Comment> output_comments = new ArrayList<Comment>();
		for (Comment c: input_comments) {
			output_comments.add(IncreaseByLemmaWithFilters(c, update_clitics));
		}
		return output_comments;
	}
	
	public List<Comment> IncreaseTrainingByCoreference(List<Comment> input_comments, boolean replace_morph_features){
		System.out.println("Increasing by coreference");
		List<Comment> output_comments = new ArrayList<Comment>();
		for (Comment c: input_comments) {
			output_comments.add(IncreaseByCoreference(c, replace_morph_features));
			//output_comments.add(IncreaseByCoreferenceDeft(c,replace_morph_features));
			//output_comments.add(IncreaseByAllCoreference(c, replace_morph_features));
		}
		return output_comments;
	}
	
	
	// First, by lemma. All NOUN & ADJ tokens that have the same lemma as a target will be annotated as targets
	// and receive the same sentiment.
	//
	// This can be made more sophisticated by doing this for only the head word of the noun phrase
	// Intended to increase training data and capture salience by repeated entities (in a comment)
	// 
	// If we knew the entire entity it would help more (increase the overlap match)
	public Comment IncreaseByLemmaMatch(Comment comment, boolean update_clitics_too) {
		for (Token t: comment.tokens_) {
			if (t.target_offset_ == -1) {
				continue;
			} else {
				if (ContentTargetWord(t)) {
					String lemma = t.morph_features.get("lex");
					String sentiment = t.sentiment_;
					// Update only if it's not already a target
					for (Token potential_target: comment.tokens_) {
						if (potential_target.target_offset_ == -1
								&& ContentTargetWord(potential_target)
								&& potential_target.morph_features.get("lex").equals(lemma)) {
							potential_target.SetTargetOffset(0);
							potential_target.SetSentiment(sentiment);
						if (update_clitics_too) {
							int i = comment.tokens_.indexOf(potential_target);
							if (i>0 && comment.tokens_.get(i-1).clitic.equals("pref")) {
								comment.tokens_.get(i-1).SetTargetOffset(0);
								comment.tokens_.get(i-1).SetSentiment(sentiment);
								potential_target.SetTargetOffset(1);
								if (i>1 && comment.tokens_.get(i-2).clitic.equals("pref")) {
									comment.tokens_.get(i-2).SetTargetOffset(0);
									comment.tokens_.get(i-2).SetSentiment(sentiment);
									comment.tokens_.get(i-1).SetTargetOffset(1);
									potential_target.SetTargetOffset(2);
								}
							}
							if (i<(comment.tokens_.size()-1) && comment.tokens_.get(i+1).clitic.equals("suf")) {
								comment.tokens_.get(i+1).SetTargetOffset(1);
								comment.tokens_.get(i+1).SetSentiment(sentiment);
							}
						}
						}
					}
				}
			}
		}
		return comment;
	}
	
	// Same as previous function, but filters matches to only ones which match based on gender, number & speaker
	// For suffixes it will not matter if we check because we're checking the morph features anyway (?)
	public Comment IncreaseByLemmaWithFilters(Comment comment, boolean update_clitics_too) {
	for (Token t: comment.tokens_) {
		if (t.target_offset_ == -1) {
			continue;
		} else {
			if (ContentTargetWord(t)) {
				String lemma = t.morph_features.get("lex");
				String sentiment = t.sentiment_;
				// Update only if it's not already a target
				for (Token potential_target: comment.tokens_) {
					if (potential_target.target_offset_ == -1
							&& ContentTargetWord(potential_target)
							&& potential_target.morph_features.get("lex").equals(lemma)
							&& CoreferenceFeatures.MatchGenderArabic(t,potential_target)
							&& CoreferenceFeatures.MatchNumberArabic(t,potential_target)
							&& CoreferenceFeatures.MatchPersonArabic(t,potential_target)) {
						potential_target.SetTargetOffset(0);
						potential_target.SetSentiment(sentiment);
					if (update_clitics_too) {
						int i = comment.tokens_.indexOf(potential_target);
						if (i>0 && comment.tokens_.get(i-1).clitic.equals("pref")) {
							Token prefix = comment.tokens_.get(i-1);
							prefix.SetTargetOffset(0);
							prefix.SetSentiment(sentiment);
							potential_target.SetTargetOffset(1);
							if (i>1 && comment.tokens_.get(i-2).clitic.equals("pref")) {
								comment.tokens_.get(i-2).SetTargetOffset(0);
								comment.tokens_.get(i-2).SetSentiment(sentiment);
								prefix.SetTargetOffset(1);
								potential_target.SetTargetOffset(2);
							}
						}
						if (i<(comment.tokens_.size()-1) && comment.tokens_.get(i+1).clitic.equals("suf")) {
							Token suffix = comment.tokens_.get(i+1);
							suffix.SetTargetOffset(1);
							suffix.SetSentiment(sentiment);
						}
					}
					}
				}
			}
		}
	}
	return comment;
	}
	
	public Comment IncreaseByWordMatch(Comment comment) {
		for (Token t: comment.tokens_) {
			if (t.target_offset_ == -1) {
				continue;
			} else {
					if (TopicSalience.IsNominal(t)) {
					String word = t.text_;
					String sentiment = t.sentiment_;
					// Update only if it's not already a target
					for (Token potential_target: comment.tokens_) {
						if (potential_target.target_offset_ == -1
								&& potential_target.text_.equals(t.text_)) {
							potential_target.SetTargetOffset(0);
							potential_target.SetSentiment(sentiment);
						}		
					}
				}
			}
		}
		return comment;
	}
	
	public Comment IncreaseByCoreference(Comment comment, boolean replace_morph_features) {
			//System.out.println("Increasing by coreference for this comment");
			for (Token potential_target: comment.tokens_) {
				if (potential_target.target_offset_ == -1 &&
						potential_target.corefers_with_token)  {
					Token coreferring_target = potential_target.coreferring_token;
					// Check if antecedant is a target
					if (coreferring_target.target_offset_ >= 0) {
						potential_target.SetCoreferrentWithTarget(true);
						String sentiment = coreferring_target.sentiment_;
						if (replace_morph_features) {
							String lemma = coreferring_target.morph_features.get("lex");
							potential_target.SetText(lemma);
							potential_target.SetMorphFeatures(coreferring_target.morph_features);
						}
						//System.out.println("Setting new target");
						potential_target.SetTargetOffset(0);
						potential_target.SetSentiment(sentiment);
					}
				}
				// Now check if the pronoun is a target and update the antecedant
				else if (potential_target.target_offset_ == 1 
						&& potential_target.corefers_with_token) {
					Token coreferring_target = potential_target.coreferring_token;
				    if (coreferring_target.target_offset_ == -1) {
					coreferring_target.SetCoreferrentWithTarget(true);
					//coreferring_target.SetPronominalMention(potential_target);
					//coreferring_target.SetSubsequentPronominal(true);
					String sentiment = potential_target.sentiment_;
					//System.out.println("Setting new target");
					coreferring_target.SetTargetOffset(0);
					coreferring_target.SetSentiment(sentiment);
					int i = comment.tokens_.indexOf(coreferring_target);
					if (i>0 && comment.tokens_.get(i-1).clitic.equals("pref")) {
						Token prefix = comment.tokens_.get(i-1);
						prefix.SetTargetOffset(0);
						prefix.SetSentiment(sentiment);
						coreferring_target.SetTargetOffset(1);
						if (i>1 && comment.tokens_.get(i-2).clitic.equals("pref")) {
							comment.tokens_.get(i-2).SetTargetOffset(0);
							comment.tokens_.get(i-2).SetSentiment(sentiment);
							prefix.SetTargetOffset(1);
							coreferring_target.SetTargetOffset(2);
						}
					}
					if (i<(comment.tokens_.size()-1) && comment.tokens_.get(i+1).clitic.equals("suf")) {
						Token suffix = comment.tokens_.get(i+1);
						suffix.SetTargetOffset(1);
						suffix.SetSentiment(sentiment);
					}
					}
				}
				
			} // end for
		return comment;
	}
	
	public Comment IncreaseByCoreferenceDeft(Comment comment, boolean replace_morph_features) {
		for (Token potential_target: comment.tokens_) {
			if (potential_target.target_offset_ == -1 &&
					potential_target.corefers_with_target)  {
				//Token coreferring_target = potential_target.coreferring_token;
				potential_target.SetTargetOffset(0);
				// no need for sentiment
				}
		} // end for
	 return comment;
   }
	
	// Updates coreference for all nominals in the NP instead of just the head
	public Comment IncreaseByAllCoreference(Comment comment, boolean replace_morph_features) {
		for (Token potential_target: comment.tokens_) {
			if (potential_target.target_offset_ == -1 &&
					potential_target.corefers_with_token) {
				Token coreferring_target = potential_target.coreferring_token;
				// First check if antecedant head is a target
				if (coreferring_target.target_offset_ >= 0) {
					potential_target.SetCoreferrentWithTarget(true);
					String sentiment = coreferring_target.sentiment_;
					if (replace_morph_features) {
						String lemma = coreferring_target.morph_features.get("lex");
						potential_target.SetText(lemma);
						potential_target.SetMorphFeatures(coreferring_target.morph_features);
					}
					potential_target.SetTargetOffset(0);
					potential_target.SetSentiment(sentiment);
				}
				// Now look at all nominals in the antecedant phrase
				List<Token> coreferring_tokens = potential_target.coreferring_tokens;
				for (int i=0; i<coreferring_tokens.size(); i++) {
					Token another_coreferrent = coreferring_tokens.get(i);
					if (another_coreferrent.target_offset_ >= 0){
						// If it's still not set
						if (!potential_target.corefers_with_target) {
							potential_target.SetCoreferrentWithTarget(true);
							String sentiment = another_coreferrent.sentiment_;
							if (replace_morph_features) {
								String lemma = another_coreferrent.morph_features.get("lex");
								potential_target.SetText(lemma);
								potential_target.SetMorphFeatures(another_coreferrent.morph_features);
							}
							potential_target.SetTargetOffset(0);
							potential_target.SetSentiment(sentiment);
						}
					}
				}
			}
		}
	return comment;
}
	

	// TO DO: use centering theory (see if we can relate the entity to the syntactic position
	// to increase precision) check Regina's paper
	
	// Returns true if the token is a potential
	// content target word for Arabic
	public boolean ContentTargetWord(Token t) {
		return (t.morph_features != null  
			  && !t.morph_features.containsKey("NO_ANALYSIS")
			  && !t.pos_.isEmpty() 
			  && TopicSalience.IsNominal(t));
			//  && (t.pos_.equals("noun") || t.pos_.equals("noun_prop") 
				//	  || t.pos_.equals("adj")));
	}
	
	/*if (!t.morph_features.isEmpty() 
					&& !t.morph_features.containsKey("NO_ANALYSIS") 
					&& !t.pos_.isEmpty()) {
				pos = t.pos_;
				lemma = t.morph_features.get("lex");
			}  else {
				pos = "NO_ANALYSIS";
				lemma = t.text_;
			}*/
	
	// Increase By Coreference. All tokens that corefer to a target will be annotated as targets
	// OPTION 1: Implement Eraldo's paper
	// OPTION 2: A simple unsupervised clustering system that clusters entities IN the comment, perhaps for pronoun
	// mentions returns the closest pronoun with gender match or so. (e.g consider all possessive pronouns and such,
	// make sure we're getting the right BW pos when updating tokenized POS)
	// then also incorporate the coref in the features (e.g dependency tree of coref entity, 
	//													or whether parent of coref has sentiment
	//													what else?, or binary whether it co-refers 
	//													to a target)
	
	// By entity clusters. All tokens which cluster in the same entity as a target will be annotated as
	// targets.
	
	
	// PMI. all tokens which have a high PMI with the target. (e.g top k or greater than a threshold)
	
}
