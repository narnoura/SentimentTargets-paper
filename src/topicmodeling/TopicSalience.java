package topicmodeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import org.apache.commons.math3.distribution.BinomialDistribution;
import data.*;


public class TopicSalience {
	/*
	 * Extracts topical features for tokens based on salience in comment. Mainly looking at 
	 * word frequency of nouns (If nouns are mentioned repeatedly, they are likely to be
	 * part of the topic)
	 *
	 * Uses summarization-based features to estimate salience of words
	 *
	 */
	
	public static double EPSILON = 0.000001;
	public TopicSalience() {
		// TODO Auto-generated constructor stub
	}
	
	// Actually works for English as well
	public static void UpdateTokenFrequenciesArabic(Comment input_comment) {
		if (input_comment.tokens_.isEmpty()) {
			return;
		}
		HashMap<String,Integer> lemma_counts = new HashMap<String,Integer>();
		for (int i=0; i<input_comment.tokens_.size();i++) {
			Token t = input_comment.tokens_.get(i);
			String lemma = WordRepresentation(t);
			String pos = t.pos_;
			if (!lemma_counts.containsKey(lemma)) {
				lemma_counts.put(lemma, 0);
			}
			// Most targets will fall under these. When we have co-ref, can include pronouns
			if (IsNominal(t)){
				int count = lemma_counts.get(lemma);
				lemma_counts.put(lemma, count+1);
			}
		}
		for (Token tok: input_comment.tokens_) {
			String lemma = WordRepresentation(tok);
			Integer token_frequency = lemma_counts.get(lemma);
			tok.SetFrequency(token_frequency);
		}
	}
	
	// p(w)
	// We only care about word probs for nouns and adjectives, don't want to bias 
	// verbs into being targets
	public static void UpdateWordProbs(Comment input_comment) {
		if (input_comment.tokens_.isEmpty()) {
			return;
		}
		// num nouns/adj in input
		int N=0;
		HashMap<String,Integer> lemma_counts = new HashMap<String,Integer>();
		for (int i=0; i<input_comment.tokens_.size();i++) {
			Token t = input_comment.tokens_.get(i);
			String pos = t.pos_; // will be NO_ANALYSIS if no analysis
			String lemma = WordRepresentation(t);
			if (!lemma_counts.containsKey(lemma)) {
				lemma_counts.put(lemma, 0);
			}
			// Most targets will fall under these. When we have co-ref, can include pronouns
			// (or actually, the noun they refer to)
			//if (pos.equals("noun") || pos.equals("noun_prop") || pos.equals("adj"))
			if (IsNominal(t)) {
				int count = lemma_counts.get(lemma);
				lemma_counts.put(lemma, count+1);
				N +=1;
			}
		}
		input_comment.SetUniqueWordCount(N);
		for (Token tok: input_comment.tokens_) {
			// Use lemma if it exists, otherwise use the text
			String lemma = WordRepresentation(tok);
			Integer token_frequency = lemma_counts.get(lemma);
			tok.SetFrequency(token_frequency);
			Double word_prob = 0.0;
			if (N!=0) {
			word_prob =  ((double) token_frequency / N); 
			//System.out.println("Word prob:" + word_prob);
			}
			//System.out.println("Setting word prob");
			tok.SetWordProb(word_prob);
			// Then should filter for k, sort by top k
		}
	}
	
	
	// Returns the probability of a word (specifically, noun) in a corpus
	public static HashMap<String,Double> WordProbsInCorpus(List<Comment> input_comments) {
		HashMap<String,Double> word_probs = new HashMap<String,Double>();
		HashMap<String,Integer> lemma_counts = new HashMap<String,Integer>();
		int N=0;
		for (Comment c: input_comments) {
			for (Token t: c.tokens_) {
				String pos = t.pos_; 
				String lemma = WordRepresentation(t);
				if (!lemma_counts.containsKey(lemma)) {
					lemma_counts.put(lemma, 0);
				}
				if (IsNominal(t)) {
					int count = lemma_counts.get(lemma);
					lemma_counts.put(lemma, count+1);
					N +=1;
				}
			}
		}
		for (String word: lemma_counts.keySet()) {
			Double prob = (double)lemma_counts.get(word) / N; 
			word_probs.put(word, prob);
		}
		return word_probs;
	}
	
	// Returns the frequency of a word (specifically, noun) in a corpus
	public static HashMap<String,Integer> WordFreqsInCorpus(List<Comment> input_comments) {
		HashMap<String,Integer> lemma_counts = new HashMap<String,Integer>();
		//int N=0;
		for (Comment c: input_comments) {
			for (Token t: c.tokens_) {
				String pos = t.pos_; 
				String lemma = WordRepresentation(t);
				if (!lemma_counts.containsKey(lemma)) {
					lemma_counts.put(lemma, 0);
				}
				if (IsNominal(t)) {
					int count = lemma_counts.get(lemma);
					lemma_counts.put(lemma, count+1);
					//N +=1;
				}
			}
		}
		return lemma_counts;
	}
	
	// Returns total frequency (specifically, of nouns) in a corpus
	public static Integer CountNouns(List<Comment> input_comments){
		int N=0;
		for (Comment c: input_comments) {
			for (Token t: c.tokens_) {
				String pos = t.pos_; 
				if (IsNominal(t)) {
					N +=1;
				}
			}
		}
		return N;
	}
	
	// Again, I am just creating TFIDF for *entities*
	// and everything else is effectively 0
	public static void UpdateTFIDF(Comment input_comment, 
			Integer num_background_docs, 
			HashMap<String,Double> corpus_counts) {
		
		if (input_comment.tokens_.isEmpty()) {
			return;
		}
		HashMap<String,Integer> lemma_counts = new HashMap<String,Integer>();
		for (int i=0; i<input_comment.tokens_.size();i++) {
			Token t = input_comment.tokens_.get(i);
			String pos = t.pos_;
			String lemma = WordRepresentation(t);
			if (!lemma_counts.containsKey(lemma)) {
				lemma_counts.put(lemma, 0);
			}
			if (IsNominal(t)) {
				int count = lemma_counts.get(lemma);
				lemma_counts.put(lemma, count+1);
			}
		}
		// Calculate and set TF-IDF values
		for (Token tok: input_comment.tokens_) {
			String lemma = WordRepresentation(tok);
			Integer c_w = lemma_counts.get(lemma);
			//Integer d_w = background_comments.size();
			Integer d_w = num_background_docs;
			Double tf_idf;
			if (!corpus_counts.containsKey(lemma)) {
				tf_idf = 0.0;
				//System.out.println("TF-IDF 0 AND corpus counts 0 for lemma " + lemma + " " + tok.clitic);
			} else if (c_w == 0) {
				tf_idf = 0.0;
				//System.out.println("TF-IDF 0 AND c_w 0 for lemma " + lemma);
			}
			else {
				Double D = corpus_counts.get(lemma);
				tf_idf = ((double) c_w) * Math.log((double)d_w / D);
			}
			tok.SetTFIDF(tf_idf);	
		}
	}
	
	// Returns lemmas/token texts (nouns and adj) with top k word probs in comment
	public static Set<String> TopProbKeyWords(Comment input_comment, int k) {
		//System.out.println("Calling topprobkeywords");
		List<Token> sorted_tokens = new ArrayList<Token>();
		Set<String> top_lemmas = new HashSet<String>();
		for (Token t: input_comment.tokens_) {
			sorted_tokens.add(t);
		}
		System.out.println("Size of sorted tokens:" + sorted_tokens.size());
		Collections.sort(sorted_tokens, new Comparator<Token>() {
			@Override
			public int compare(Token o1, Token o2) {
				return Double.compare(o2.word_prob, o1.word_prob);
				//return o1.word_prob.compareTo(o2.word_prob);
			}	
		} );
		
		for (int i=0; i<Math.min(k,sorted_tokens.size()) ;i++){
			Token top = sorted_tokens.get(i);
			String lemma = WordRepresentation(top);	
			if (top_lemmas.contains(lemma)) {
				continue;
			}
			//if (!top_lemmas.contains(lemma)) {
			if (top.word_prob > 0 ) {
				top_lemmas.add(lemma);
				System.out.println("Top lemma for word prob: " 
				+ lemma + " " + top.pos_ + " " + top.word_prob);
			}
		}
		return top_lemmas;
	}
	
	public static Set<String> TopTFIDFKeyWords(Comment input_comment, int k) {
		//System.out.println("Calling toptfidfwords");
		List<Token> sorted_tokens = new ArrayList<Token>();
		Set<String> top_lemmas = new HashSet<String>();
		for (Token t: input_comment.tokens_) {
			sorted_tokens.add(t);
		}
		Collections.sort(sorted_tokens, new Comparator<Token>() {
			@Override
			public int compare(Token o1, Token o2) {
				return Double.compare(o2.TF_IDF_value, o1.TF_IDF_value);
			}	
		} );
		for (int i=0; i<Math.min(k,sorted_tokens.size()) ;i++){
			Token top = sorted_tokens.get(i);
			String lemma = WordRepresentation(top);
			if (top_lemmas.contains(lemma)) {
				continue;
			}
			//if (!top_lemmas.contains(lemma) && top.TF_IDF_value > 0) {
			if (top.TF_IDF_value > 0) {
			top_lemmas.add(lemma);
			System.out.println("Top lemma for TF-IDF " 
			+ lemma + " " + top.pos_ + " " + top.TF_IDF_value);
			}
		}
		return top_lemmas;
	}
	
	// This is based on the contingency table implementation (described in 
	// Lin & Hovy, 2000 and Conroy, 2006 and Harabigu,2005)
	public static void UpdateTopicSignatures(Comment input_comment,
			List<Comment> background_comments) {
		
		if (input_comment.tokens_.isEmpty()) {
			return;
		}
		// Input probs (R or I)
		UpdateTokenFrequenciesArabic(input_comment);
		Integer NI = input_comment.noun_word_count;
		
		// All word counts (R + ~R or I + B)
		HashMap<String,Integer> word_counts = WordFreqsInCorpus(background_comments);
		Integer N = CountNouns(background_comments);
		
		// Background probs
		String comment_id = input_comment.comment_id_;
		List<Comment> only_background_comments = new ArrayList<Comment>();
		for (Comment c: background_comments){
			if (c.comment_id_ != comment_id) {
				only_background_comments.add(c);
			}
		}
		// First, get probability p = p(R)
		Double p = ((double) NI / N);
		for (Token t: input_comment.tokens_) {
			// Get probability p1 = p(R|ti)
			Integer o11 = t.frequency_in_comment;
			String lemma = WordRepresentation(t);
			if (!word_counts.containsKey(lemma)) {
				System.out.println("Topic signatures: this token is not part"
						+ " of the input corpus provided. Exiting");
				return;
			}
			Integer denominator_p1 = word_counts.get(lemma); // ti counts
			Double p1 = 0.0;
			if (denominator_p1 !=0) {
				p1 = ((double) o11 / denominator_p1);
			}
			// Get probability p2 = p(R|~ti) 
			Double p2 = 0.0;
			Integer o21 = NI - o11;
			Integer denominator_p2 = N - denominator_p1;   // ~ti counts
			if (denominator_p2 != 0) {
				p2 = ((double) o21 / denominator_p2);
			}
			// Get log-likelihood ratio
			if (p1 == 0.0) {
				p1 = EPSILON;
			} else if (p2 == 0.0) {
				p2 = EPSILON;
			} else if (p == 0.0) {
				p = EPSILON;  // unlikely that this happens
			}
		  if (p1 == 1.0) {
				p1 = 1 - EPSILON;
			} else if (p2 == 1.0) {
				p2 = 1 - EPSILON;
			} else if (p == 1.0) { // unlikely
				p = 1 - EPSILON;
			}
			Integer o12 = denominator_p1 - o11;  
			Integer o22 = denominator_p2 - o21; 
			Double log_likelihood_ratio = 
					(o11 + o21) * Math.log(p) + (o12 + o22) * Math.log(1-p)
					-  ( 
					o11 * Math.log(p1) + o12 * Math.log(1-p1) + o21 * Math.log(p2)
					+ o22 * Math.log(1-p2)	
						) ;
			log_likelihood_ratio = - 2 * log_likelihood_ratio;
			t.SetLogLikelihood(log_likelihood_ratio);
		}
	}
	
	// This method is based on the description of Ani Nekova, which does not use the 
	// contingency table.
	/*public static void UpdateTopicSignatures1(Comment input_comment,
			List<Comment> background_comments) {
		
		if (input_comment.tokens_.isEmpty()) {
			return;
		}
		// Input probs
		UpdateWordProbs(input_comment);
		UpdateTokenFrequenciesArabic(input_comment);
		Integer NI = input_comment.noun_word_count;
		
		// All word probs
		HashMap<String,Double> word_probs = WordProbsInCorpus(background_comments);
		Integer N = word_probs.size();
		String comment_id = input_comment.comment_id_;
		// Background probs
		List<Comment> only_background_comments = new ArrayList<Comment>();
		for (Comment c: background_comments){
			if (c.comment_id_ != comment_id) {
				only_background_comments.add(c);
			}
		}
		HashMap<String,Double> background_probs = 
				WordProbsInCorpus(only_background_comments);
		Integer NB = background_probs.size();
		for (Token t: input_comment.tokens_) {
			// First, get probability p from input and background corpus
			String lemma = WordRepresentation(t);
			if (!word_probs.containsKey(lemma)) {
				System.out.println("Topic signatures: this token is not part"
						+ " of the input corpus provided. Exiting");
				return;
			}
			Double p = word_probs.get(lemma);
			//System.out.println("N:"+N);
			//System.out.println("Lemma:"+lemma);
			//System.out.println("p:"+p);
			Integer k =  (int) (N * p); 
			// Next, get pB from only the background corpus
			Double pB = 0.0;
			if (background_probs.containsKey(lemma)) {
				pB = background_probs.get(lemma);
			}
			//System.out.println("pB:"+pB);
			Integer kB = (int) (NB * pB);
			// Finally, get pI from input
			Double pI = t.word_prob;
			//System.out.println("pI:"+pI);
			Integer kI = t.frequency_in_comment;
			
			// Get log-likelihood ratio
			BinomialDistribution binomial_H1 = new BinomialDistribution(N,p);
			Double b_H1 = binomial_H1.probability(k);
			BinomialDistribution binomial_H2_I = new BinomialDistribution(NI,pI);
			Double b_H2_I = binomial_H2_I.probability(kI);
			BinomialDistribution binomial_H2_B = new BinomialDistribution(NB,pB);
			Double b_H2_B = binomial_H2_B.probability(kB);
			Double lambda = b_H1 / (b_H2_I * b_H2_B);
			//System.out.println("Lambda for token " + lemma + " " + lambda);
			Double log_likelihood_ratio = (-2 * Math.log(lambda));
			//Double log_likelihood_ratio = - 2 * lambda;
			//System.out.println("LLR for token " + lemma + " " + log_likelihood_ratio);
			t.SetLogLikelihood(log_likelihood_ratio);
		}
	
	}*/
	
	// Chance_prob is the max probability
	// we accept for the hypothesis being true by chance
	/* 
	 * https://www.medcalc.org/manual/chi-square-table.php
	 */
	// If doesn't work, change word probs to word freqs and use the topic sig papers way
	public static Set<String> TopSignatures(Comment input_comment, double chance_prob) {
		Double threshold = 12.116; // chance_prob 0.0005
		if (chance_prob == 0.001) {
			threshold = 10.828;
		} else if (chance_prob == 0.002) {
			threshold = 9.55;
		} else if (chance_prob == 0.005) {
			threshold = 7.879;
		} else if (chance_prob == 0.01) {
			threshold = 6.635;
		} else if (chance_prob == 0.02) {
			threshold = 5.412;
		} else if (chance_prob == 0.025) {
			threshold = 5.024;
		} else if (chance_prob == 0.05) {
			threshold = 3.841;
		} 
		List<Token> sorted_tokens = new ArrayList<Token>();
		Set<String> top_lemmas = new HashSet<String>();
		for (Token t: input_comment.tokens_) {
			sorted_tokens.add(t);
		}
		System.out.println("Size of sorted tokens:" + sorted_tokens.size());
		Collections.sort(sorted_tokens, new Comparator<Token>() {
			@Override
			public int compare(Token o1, Token o2) {
				//switch back to o2, o1
				return Double.compare(o2.log_likelihood_ratio,
						o1.log_likelihood_ratio);
			}	
		} );
		for (int i=0; i<sorted_tokens.size() ;i++){
			Token top = sorted_tokens.get(i);
			String lemma = WordRepresentation(top);	
			if (top.log_likelihood_ratio > threshold
				&& !top_lemmas.contains(lemma))  {
				top_lemmas.add(lemma);
				System.out.println("Top lemma for LLR: " 
				+ lemma + " " + top.pos_ + " " + top.log_likelihood_ratio);
			}
		}
		return top_lemmas;
	}
	
	
	// Instead of cutoff, get top k signatures
	public static Set<String> TopKSignatures(Comment input_comment, int k) {
			List<Token> sorted_tokens = new ArrayList<Token>();
			Set<String> top_lemmas = new HashSet<String>();
			for (Token t: input_comment.tokens_) {
				sorted_tokens.add(t);
			}
			System.out.println("Size of sorted tokens:" + sorted_tokens.size());
			Collections.sort(sorted_tokens, new Comparator<Token>() {
				@Override
				public int compare(Token o1, Token o2) {
					return Double.compare(o2.log_likelihood_ratio,
							o1.log_likelihood_ratio);
				}	
			} );
			for (int i=0; i<Math.min(k,sorted_tokens.size()) ;i++){
				Token top = sorted_tokens.get(i);
				String lemma = WordRepresentation(top);	
				if (top_lemmas.contains(lemma)) {
					continue;
				}
				//if (!top_lemmas.contains(lemma))  {
				if (top.log_likelihood_ratio > 0)
					top_lemmas.add(lemma);
					System.out.println("Top lemma for LLR: " 
					+ lemma + " " + top.pos_ + " " + top.log_likelihood_ratio);
				//}
			}
			return top_lemmas;
		}
	
	// Returns a hashmap for each token with the number of comments it occurs in
	// Again, lemma-based, and done only for nouns and adjectives (we are only
	// looking for salient entities)
	//
	// Assumes token frequencies are already calculated
	public static HashMap<String,Integer> GetCorpusCounts(List<Comment> input_comments) {
		HashMap<String, Set<String>> token_counts = 
				 new HashMap<String,Set<String>>();
		HashMap<String,Integer> corpus_counts = new HashMap<String,Integer>();
		// First, fill up mentioned comments for all tokens
		for (Comment c: input_comments) { 
			String comment_id = c.comment_id_;
			for (Token t: c.tokens_) {
				String lemma = WordRepresentation(t);
				if (!token_counts.containsKey(lemma)) {
					Set<String> comment_counts = new HashSet<String>();
					comment_counts.add(comment_id);
					token_counts.put(lemma, comment_counts);
				} else {
					if (!token_counts.get(lemma).contains(comment_id)) {
						token_counts.get(lemma).add(comment_id);
					}
				}
			}
		}
		// Next, get counts
		//System.out.println("Printing corpus counts for all tokens:\n");
		for (String lemma: token_counts.keySet()) {
			Set<String> comments = token_counts.get(lemma);
			corpus_counts.put(lemma,comments.size());
			//System.out.println("Lemma: " + lemma  +  " " + comments.size() );
		}
		return corpus_counts;
	}
	
	public static HashMap<String,Double> ReadCorpusCountsFromFile (String file_path) {
		HashMap<String,Double> corpus_counts = new HashMap<String,Double>();
		List<String> lines = util.FileReader.ReadFile(file_path, "", true);
		for (String l :lines){
			String[] lems = l.split("\t");
			//System.out.println(l);
			//System.out.println(lems[0]);
			//System.out.println(lems[1]);
			/*System.out.print("l:"+l + "\n");
			System.out.print("lemma:"+lems[0] + "\n");
			System.out.print("count"+lems[1] + "\n");
			*/
			corpus_counts.put(lems[0], Double.valueOf(lems[1]));
		}
		return corpus_counts;
	}
	
	public static String WordRepresentation (Token t) {
		if (t.morph_features!=null && !t.morph_features.isEmpty() 
				 &&  (!t.clitic.equals("pref"))
				 &&  (!t.clitic.equals("suf"))
				 // For clitics, we want to return the actual clitic 
				 // text, not the lemma they're modifying
				 //
				// for some reason, some words get 'none' as clitic 
				// even when they have a content-word lemma
				// TODO: check content word function
				&& !t.morph_features.containsKey("NO_ANALYSIS") ) {
			return t.morph_features.get("lex");
		}  else {
			return t.text_;
		}
	}
	
	public static boolean IsNominal(Token t) {
				// Arabic
		return ((t.pos_.equals("noun") || t.pos_.equals("noun_prop") || t.pos_.equals("adj")
				|| 
				t.pos_.equals("NN") || t.pos_.equals("NNS") || t.pos_.equals("NNP") || t.pos_.equals("NNPS") 
				||
				t.pos_.equals("JJ") || t.pos_.equals("JJR") || t.pos_.equals("JJS")) 
				&&
				!t.text_.contains("quote") && !t.text_.contains("img") && !t.text_.contains("post")
				&& 
				!t.text_.contains("http") && !t.text_.contains("</a>") && !t.text_.contains("<a")
				&& !t.text_.contains("<post") && !t.text_.contains("author")
				);
	}

}
