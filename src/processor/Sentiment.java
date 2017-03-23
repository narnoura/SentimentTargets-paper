/**
 * 
 */
package processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.Constants;
import util.Tokenizer;
import data.Token;

/**
 * @author Narnoura
 *
 */
public class Sentiment {

	/**
	 * Sentiment handling methods
	 */
	public Sentiment() {
	}
	
	// Returns sentiment scores in string form for Slsa
	public static HashMap<String, String> GetSentimentScoreFeatures (Token t, 
		LexiconProcessor lp) {
		
		HashMap<String, String> sentiment_scores = new HashMap<String,String>();
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			sentiment_scores.put("pos", "0.0");
			sentiment_scores.put("neg", "0.0");
			sentiment_scores.put("neut", "0.0");
			return sentiment_scores;
		}
		String lemma = t.morph_features.get("lex");
		String pos = t.morph_features.get("pos");
		pos = Tokenizer.ResolvePOS(pos);
		
		if (!lp.HasSlsaKey(lemma,pos)) {
			sentiment_scores.put("pos", "0.0");
			sentiment_scores.put("neg", "0.0");
			sentiment_scores.put("neut", "0.0");
			
		}
		else {
			double pos_score = lp.SlsaPositiveScore(lemma, pos);
			double neg_score = lp.SlsaNegativeScore(lemma, pos);
			double neut_score = 1 - pos_score - neg_score;
			
			pos_score = (double) Math.round(pos_score * 10)/ 10;
			neg_score = (double) Math.round(neg_score * 10)/ 10;
			neut_score = (double) Math.round(neut_score * 10)/ 10;
			
			String positive = Double.toString(pos_score);
			String negative = Double.toString(neg_score);
			String neutral = Double.toString(neut_score);
			
			sentiment_scores.put("pos", positive);
			sentiment_scores.put("neg", negative);
			sentiment_scores.put("neut", neutral);		
		}
		return sentiment_scores;
	}
	
	// Returns sentiment polarities from Sifaat
	public static Integer GetSentimentSifaat (Token t,
			LexiconProcessor lp) {
		Integer sentiment = 0;
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			return sentiment;
		}
		String lemma = t.morph_features.get("lex");
		String stripped = lemma;
		stripped = stripped.replaceAll("(\\_)(\\d)+\\z", "");
		if (lp.HasSifaatKey(stripped)) {
			sentiment =  lp.Sifaat.get(stripped);
		}
		// backs off to look for a word that contains the lemma
		// If key is not found, 0 will be returned (neutral).
		// (worth also trying NA)
		// Sifaat contains many (all?) adjectives (adding iy
		// to lemmas)
		//
		// Any smarter way to do it? e.g somehow get the stem or match
		// the characters, or edit distance
		/*else {
			for (String key : lp.Sifaat.keySet()) { // or and key ends with y~ and stripped doesn't
				if (key.contains(stripped) || stripped.contains(key)) {
					sentiment = lp.Sifaat.get(key);
					System.out.println("\nBackoff: found key!");
					System.out.println("Lemma:" + stripped);
					System.out.println("Key:" + key);
					System.out.println("Sentiment:" + sentiment);
					break;
				}
			}
		}*/
		return sentiment;
	}
	
	// Returns subjectivity based only on MPQA
	// Returns "na", "strongsubj", or "weaksubj"
	public static String GetSubjectivityMPQA (Token t, 
			LexiconProcessor lp) {
		String subj = "na";
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			return subj ;
		} 
		else{
			String english_word = GetMPQAFromGloss(t,lp);
			if (english_word.equals("na")) {
				subj = "na";
			} else 
			{ 
				subj =lp.GetMPQASubjectivity(english_word);
			}
		}
		return subj;
	}
	
	// Returns polarity based only on MPQA
	// Returns "na", "positive", or "negative", or "neutral"
	public static String GetPolarityMPQA (Token t, 
			LexiconProcessor lp) {
		String pol = "na";
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			return pol ;
		} 
		else{
			String english_word = GetMPQAFromGloss(t,lp);
			if (english_word.equals("na")) {
				pol = "na";
			} else 
			{ 
			  pol =lp.GetMPQAPriorPolarity(english_word);
			}
		}
		return pol;
	}
	
	// Returns subjectivity based only on MPQA
	// Returns "na", "strongsubj", or "weaksubj"
	public static String GetEnglishSubjectivityMPQA (Token t, 
			LexiconProcessor lp) {
		String subj = "na";
		String english_word = t.text_;
		if(lp.HasMPQAKey(english_word)) {
			subj = lp.GetMPQASubjectivity(english_word);
		} 
		return subj;
	}
	
	// Returns polarity based only on MPQA
	// Returns "na", "positive", or "negative", or "neutral"
	public static String GetEnglishPolarityMPQA (Token t, 
			LexiconProcessor lp) {
		String pol = "na";
		String english_word = t.text_;
		if(lp.HasMPQAKey(english_word)) {
		  pol = lp.GetMPQAPriorPolarity(english_word);
		} 
		return pol;
	}
	
	// Returns subjectivity from Sifaat and backing off to MPQA
	// Returns "na", "strongsubj", or "weaksubj"
	public static String GetSubjectivitySifaatMPQA (Token t,
				LexiconProcessor lp) {
			String subj = "na";
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				return subj ;
			}
			Integer subjectivity = GetBinarySentimentSifaat(t,lp);
			if (subjectivity == 1) {
				subj = "strongsubj";
			} else if (subjectivity == 0) {
				String english_word = GetMPQAFromGloss(t,lp);
				if (english_word.equals("na")) {
					subj = "na";
				} else 
				{ 
					subj =lp.GetMPQASubjectivity(english_word);
				}
			}
			return subj;
		}
	
	// Returns polarity from Sifaat and backing off to MPQA
	// Returns positive, negative, or neutral
	public static String GetPolaritySifaatMPQA (Token t,
				LexiconProcessor lp) {
			String pol = "neutral";
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				return pol ;
			}
			Integer polarity = GetSentimentSifaat(t,lp);
			if (polarity == 1) {
				pol = "positive";
			}  else if (polarity == 2) {
				pol = "negative";
			}
			else if (polarity == 0) // neutral or no sentiment 
				{
				String english_word = GetMPQAFromGloss(t,lp);
				if (english_word.equals("na")) {
					pol = "neutral";
				} else 
				{ 
				  pol = lp.GetMPQAPriorPolarity(english_word);
				}
			}
			return pol;
		}
	
	// Searches and finds MPQA entry from English gloss
	// If none, returns na
	// Tries each gloss entry in turn
	//
	// Because entries are so few anyway, we don't match POS
	public static String GetMPQAFromGloss(Token t, LexiconProcessor lp) {
		String MPQA_entry = "na";
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			return MPQA_entry;
		}  
		String gloss = t.morph_features.get("gloss");
		String[] potentials = gloss.split(";");
		for (String p : potentials) {
			if (lp.HasMPQAKey(p)) {
				MPQA_entry = p;
				break;
			} 
		} 
		return MPQA_entry;
	}
	
	
	// returns 1 for sentiment, 0 for not
	public static Integer GetBinarySentimentSifaat (Token t,
			LexiconProcessor lp) {
			Integer sentiment = 0;
		
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			return sentiment;
		}
		String lemma = t.morph_features.get("lex");
		String stripped = lemma;
		stripped = stripped.replaceAll("(\\_)(\\d)+\\z", "");
		if (lp.HasSifaatKey(stripped)) {
			Integer trisent =  lp.Sifaat.get(stripped);
			if (trisent.equals(1) || trisent.equals(2)) {
				sentiment = 1;
			}
		}
		return sentiment;
	}
	
	// Returns sentiment scores in string form for Slsa
	public static HashMap<String, String> GetSentimentScoreFeaturesArsenl (Token t, 
			LexiconProcessor lp) {
			
			HashMap<String, String> sentiment_scores = new HashMap<String,String>();
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				sentiment_scores.put("pos", "0.0");
				sentiment_scores.put("neg", "0.0");
				sentiment_scores.put("neut", "0.0");
				return sentiment_scores;
			}
			String lemma = t.morph_features.get("lex");
			
			if (!lp.HasArsenlKey(lemma)) {
				sentiment_scores.put("pos", "0.0");
				sentiment_scores.put("neg", "0.0");
				sentiment_scores.put("neut", "0.0");
				
			}
			else {
				double pos_score = lp.ArsenlPositiveScore(lemma);
				double neg_score = lp.ArsenlNegativeScore(lemma);
				double neut_score = 1 - pos_score - neg_score;
				
				pos_score = (double) Math.round(pos_score * 10)/ 10;
				neg_score = (double) Math.round(neg_score * 10)/ 10;
				neut_score = (double) Math.round(neut_score * 10)/ 10;
				
				String positive = Double.toString(pos_score);
				String negative = Double.toString(neg_score);
				String neutral = Double.toString(neut_score);
				
				sentiment_scores.put("pos", positive);
				sentiment_scores.put("neg", negative);
				sentiment_scores.put("neut", neutral);	
				
			}
			return sentiment_scores;
		}
		
	
	// pos:poshigh, poslow, poszero, neg:neghigh, neglow, negzero  
	// If the word is not found in the lexicon, fire poszero and negzero
	// Sentiment features are taken from Slsa lexicon
	public static HashMap<String, String> GetTokenBinarySentimentFeatures (Token t,
			LexiconProcessor lp, double threshold) {
		
		HashMap<String, String> binary_sentiment = new HashMap<String,String>();
		
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			binary_sentiment.put("pos", "poszero");
			binary_sentiment.put("neg", "negzero");
			return binary_sentiment;
		}
		String lemma = t.morph_features.get("lex");
		String pos = t.morph_features.get("pos");
		
		pos = Tokenizer.ResolvePOS(pos);
		
		if (pos.equals("STOP") || pos.equals("NEG")) {
			binary_sentiment.put("pos", "poszero");
			binary_sentiment.put("neg", "negzero");
			return binary_sentiment;
		}
		if (!lp.HasSlsaKey(lemma,pos)) {
			binary_sentiment.put("pos", "poszero");
			binary_sentiment.put("neg", "negzero");
		}
		else {
			double pos_score = lp.SlsaPositiveScore(lemma, pos);
			double neg_score = lp.SlsaNegativeScore(lemma, pos);
			
			if (pos_score > 0 && pos_score < threshold) {
				binary_sentiment.put("pos", "poslow");
			}
			else if (pos_score >= threshold) {
				binary_sentiment.put("pos", "poshigh");
			}
			else if (pos_score == 0){
				binary_sentiment.put("pos", "poszero");
			}
			else {
				System.out.println("Invalid pos score. Exiting \n");
				System.exit(0);
			}
			if (neg_score > 0 && neg_score < threshold) {
				binary_sentiment.put("neg", "neglow");
			}
			else if (neg_score >= threshold) {
				binary_sentiment.put("neg", "neghigh");
			}
			else if (neg_score == 0){
				binary_sentiment.put("neg", "negzero");
			}
			else {
				System.out.println("Invalid neg score. Exiting \n");
				System.exit(0);
			}
		}
		
		return binary_sentiment;
	}
	
	// Same but for ArSenL
	public static HashMap<String, String> GetTokenBinarySentimentFeaturesArsenl (Token t,
			LexiconProcessor lp, double threshold) {
		
		HashMap<String, String> binary_sentiment = new HashMap<String,String>();
		
		if (t.morph_features.containsKey("NO_ANALYSIS")) {
			binary_sentiment.put("pos", "poszero");
			binary_sentiment.put("neg", "negzero");
			return binary_sentiment;
		}
		String lemma = t.morph_features.get("lex");
		if (!lp.HasArsenlKey(lemma)) {
			binary_sentiment.put("pos", "poszero");
			binary_sentiment.put("neg", "negzero");
		}

		else {
			double pos_score = lp.ArsenlPositiveScore(lemma);
			double neg_score = lp.ArsenlNegativeScore(lemma);
			
			if (pos_score > 0 && pos_score < threshold) {
				binary_sentiment.put("pos", "poslow");
			}
			else if (pos_score >= threshold) {
				binary_sentiment.put("pos", "poshigh");
			}
			else if (pos_score == 0){
				binary_sentiment.put("pos", "poszero");
			}
			else {
				System.out.println("Invalid pos score. Exiting \n");
				System.exit(0);
			}
			if (neg_score > 0 && neg_score < threshold) {
				binary_sentiment.put("neg", "neglow");
			}
			else if (neg_score >= threshold) {
				binary_sentiment.put("neg", "neghigh");
			}
			else if (neg_score == 0){
				binary_sentiment.put("neg", "negzero");
			}
			else {
				System.out.println("Invalid neg score. Exiting \n");
				System.exit(0);
			}
		}
		
		return binary_sentiment;
	}
	
	// returns only senthigh and sentlow
	// Sentiment features are taken from Slsa lexicon
	public static String GetTokenHighLowSentimentFeatures (Token t,
						LexiconProcessor lp, double threshold) {
					
					String binary_sentiment = "";
					
					if (t.morph_features.containsKey("NO_ANALYSIS")) {
						return "sentlow";
					}
					String lemma = t.morph_features.get("lex");
					String pos = t.morph_features.get("pos");
					
					pos = Tokenizer.ResolvePOS(pos);
				
					if (!pos.equals("ADJ")) {
						return "sentlow";
					}
					
					if (!lp.HasSlsaKey(lemma,pos)) {
						return "sentlow";
					}
					else {
						double pos_score = lp.SlsaPositiveScore(lemma, pos);
						double neg_score = lp.SlsaNegativeScore(lemma, pos);
						
						if (pos_score >= threshold || neg_score >= threshold) {
							return "senthigh";
						}
						else if (pos_score < threshold && neg_score < threshold) {
							return "sentlow";
						}
						else {
							System.out.println("Invalid scores. Exiting \n");
							System.exit(0);
						}
				}
				return binary_sentiment;
					
			}
	// Same but using Arsenl
	public static String GetTokenHighLowSentimentFeaturesArsenl (Token t,
				LexiconProcessor lp, double threshold) {
			
			String binary_sentiment = "";
			
			if (t.morph_features.containsKey("NO_ANALYSIS")) {
				return "sentlow";
			}
			String lemma = t.morph_features.get("lex");
			if (!lp.HasArsenlKey(lemma)) {
				return "sentlow";
			}
		
			else {
				double pos_score = lp.ArsenlPositiveScore(lemma);
				double neg_score = lp.ArsenlNegativeScore(lemma);
				
				if (pos_score >= threshold || neg_score >= threshold) {
					return "senthigh";
				}
				else if (pos_score < threshold && neg_score < threshold) {
					return "sentlow";
				}
				else {
					System.out.println("Invalid scores. Exiting \n");
					System.exit(0);
				}
		}
		return binary_sentiment;
			
	}
	
	// Returns the category sentiment (positive or negative)
	// of a single token based on its lexicon score
	public static String GetTokenSentimentCategory (Token t, LexiconProcessor lp, double threshold) {
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
	
	
	// Returns sentiment based on the sentiment of the token having
		// the highest sentiment score.
		// Considers negations
		// TODO: In future can return neutral and use that to filter out targets
		// and increase precision
	public static String GetTokensMaxSentiment (List<Token> tokens, LexiconProcessor lp) {
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
				if (lp.HasSlsaKey(lemma, pos)) {
					double this_pos = lp.SlsaPositiveScore(lemma, pos);
					double this_neg = lp.SlsaNegativeScore(lemma, pos);
					double this_neut = (1- this_pos- this_neg);
					
					// does not improve
					/*if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
						double temp = this_pos;
						this_pos = this_neg;
						this_neg = temp;
					}*/
					
					if (this_pos > max_pos) {
						max_pos = this_pos;
					}
					if (this_neg > max_neg) {
						max_neg = this_neg;
					}
				}
				
				
			}
			
			if (max_pos > max_neg) {
				sentiment = "positive";
			}
			else {
				sentiment = "negative";
			}
			
			return sentiment;
		}
	
	public static String GetTokensCategorySentiment (List<Token> tokens, LexiconProcessor lp, double threshold) {
		 Double[] scores = GetTokensSentiment(tokens,lp);
		double pos_score = scores[0];
		double neg_score = scores[1];
	
		if (pos_score >= neg_score) {
			return "positive";
		}
		else  {
			return "negative";
		}
		
}
	
public static String GetTokensSentimentSifaat (List<Token> tokens, LexiconProcessor lp) {
		int num_pos =0;
		int num_neg = 0;
		int num_neut = 0;
		for (Token t: tokens) {
			Integer sentiment = GetSentimentSifaat(t,lp);
			int i = tokens.indexOf(t);
			String previous_text = "";
			if (i > 1) {
				Token previous_token = tokens.get(i-1);
				previous_text = previous_token.morph_features.get("WORD");
				}
			// If the word is preceded by a negation, switch the positive
			// and negative annotations
			if (Constants.MY_BW_NEGATIONS.contains(previous_text)) {
				if (sentiment == 1) {
					sentiment = 2;
				}
				else if (sentiment == 2) {
					sentiment = 1;
				}
			}
			if (sentiment == 1) {
				num_pos +=1;
			}
			else if (sentiment == 2) {
				num_neg +=1;
			} else {
				num_neut +=1;
			}
		
			if (num_pos > num_neg) {
				return "positive";
			} else {
				return "negative";}
				
			}

		return null;	
	}

	// Returns average scores for positive, negative, neutral across tokens
	// Only averages the positive/negative score of positive/negative sentiment words
	// instead of all
	// Threshold determines whether a word is positive or negative, otherwise it is neutral
	// If it's both positive and negative, the max is taken
	public static Double[] GetSelectiveTokensSentiment (List<Token> tokens, LexiconProcessor lp, double threshold) {
			
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
		
	
	public static Double[] GetTokensSentiment (List<Token> tokens, LexiconProcessor lp) {
		
		double avg_pos = 0;
		double avg_neg = 0;
		double avg_neut = 0;
		double tot=0;

		for (Token t: tokens) {
			String lemma = t.morph_features.get("lex");
			String pos = t.morph_features.get("pos");
			pos = Tokenizer.ResolvePOS(pos);
			if (!lp.HasSlsaKey(lemma, pos)) {
				
			}
			else if (!pos.equals("STOP")  && !pos.equals("NEG")) {
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

}
