/**
 * 
 */
package data;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

/**
 * @author Narnoura
 * Object class representing a token. 
 * Needed for where tokens may represent morphemes or result from different 
 * tokenization schemes
 */
public class Token {
	
	public String text_;
	// Refers to the tokenization scheme
	// e.g word, morpheme, whatever tokenization applies
	// When comparing tokens, enforces them to have the same tokenization scheme
	// word, ATB, D3 (MyD3)... 
	public String type_;
	// Part of speech
	public String pos_;
	// Hashmap for madamira features
	public HashMap<String, String> morph_features;
	// NER feature
	public String NER;
	// BPC feature
	public String BPC;
	// optional
	//public String parse_tree;
	public boolean entity;
	// B,I
	public String entity_type;
	// full mention test
	public String entity_text;

	// Token index in comment
	public int comment_offset_;
	// Token index in target. set to -1 if not a target
	public int target_offset_; 
	// If part of a target, specify sentiment
	public String sentiment_;
	// "none", "pref", "suf", "word"
	// specifies whether and where 
	// the morpheme is part of a word that
	// has been separated by tokenization
	public String clitic;
	
	// Coreference features
	// true if pronominal and has antecedant
	public boolean corefers_with_token;
	// true if pronominal and antecedant is target
	public boolean corefers_with_target;
	// lexically specifies the coreferring token (may or may not be a target)
	// (likely head or lemma of NP)
	public Token coreferring_token;
	// all coreferring tokens
	public List<Token> coreferring_tokens;
	// True if this token has subsequent pronominal mentions
	public boolean has_subsequent_pronominal_mention;
	// Pronominal mention
	public Token pronominal_mention;
	
	// Summarization and salience features
	public Integer frequency_in_comment;
	public Double word_prob;
	public Double TF_IDF_value;
	public Double log_likelihood_ratio;
	
	// For stanford tokens
	public Tree parse_tree;
	public SemanticGraph dependencies;
	public Map<Integer, CorefChain> coreference_chain;

	// Methods
	public Token() {
		this.clitic = "none";
	}
	
	public Token(String token, String type) {
		this.text_ = token;
		this.type_ = type;
		this.clitic = "none";
		this.entity_type = "none";
		this.entity = false;
		this.NER = null;
		this.BPC = null;
		this.has_subsequent_pronominal_mention = false;
		this.coreferring_tokens = new ArrayList<Token>();
		this.morph_features = new HashMap<String, String>();
		this.target_offset_ = -1;
	}
	/* Set part of speech tag*/
	public void SetPOS (String pos) {
		pos_ = pos;
	}
	
	public void SetText (String text) {
		this.text_ = text;
	}
	
	public void SetType (String type) {
		this.type_ = type;
	}
	
	public void SetNER (String NER) {
		this.NER = NER;
	}
	
	public void SetBPC (String BPC) {
		this.BPC = BPC;
	}
	public void SetEntity (boolean entity) {
		this.entity = entity;
	}
	public void SetEntityType(String entity_type){
		this.entity_type = entity_type;
	}
	public void SetEntityText(String entity_text){
		this.entity_text = entity_text;
	}
	/* If token belongs to a Comment, set comment offset */
	public void SetCommentOffset(int comment_offset) {
		comment_offset_ = comment_offset;
	}
	/* If token belongs to a Target, set target offset*/
	public void SetTargetOffset(int target_offset) {
		target_offset_ = target_offset;
	}
	/* If token belongs to a Target, set sentiment */
	public void SetSentiment(String sentiment) {
		sentiment_ = sentiment;
	}
	
	public void SetClitic(String clitic) {
		this.clitic = clitic;
	}
	// Set constituent parse tree
	public void SetParseTree (Tree parse_tree) {
		this.parse_tree = parse_tree;
	}
	// Set dependencies
	public void SetDependencies (SemanticGraph dependencies) {
		this.dependencies = dependencies;
	}
	//Set coreference chain
	public void SetCoreferenceChain (Map<Integer, CorefChain> coref_chain) {
		this.coreference_chain = coref_chain;
	}
	
	/* Returns true if both Tokens are equal*/
	public boolean Equals(Token token) {
		return (this.text_.equals(token.text_) &&
				this.type_.equals(token.type_) &&
				this.comment_offset_ == token.comment_offset_ &&
				this.target_offset_ == token.target_offset_
				);
	}
	
	public boolean HasSameTextAs (Token token) {
		return (this.text_.equals(token.text_));
	}
	
	public void SetMorphFeatures (HashMap<String, 
			String> mada_features) {
		this.morph_features = mada_features;
	}
	
	public void SetCoreferrent(boolean corefers) {
		this.corefers_with_token = corefers;
	}
	public void SetCoreferrentWithTarget(boolean corefers) {
		this.corefers_with_target = corefers;
	}
	public void SetCoreferringToken (Token coreferring_token) {
		this.coreferring_token = coreferring_token;
	}
	public void SetCoreferringTokens (List<Token> coreferring_tokens) {
		this.coreferring_tokens = coreferring_tokens;
	}
	public void AddCoreferringToken (Token coreferring_token) {
		this.coreferring_tokens.add(coreferring_token);
	}
	public void SetSubsequentPronominal(boolean subsequent_pronominal) {
		this.has_subsequent_pronominal_mention = subsequent_pronominal;
	}
	public void SetPronominalMention(Token pronoun) {
		this.pronominal_mention = pronoun;
	}
	
	// Set summarization features
	public void SetFrequency (Integer frequency) {
		frequency_in_comment = frequency;
	}
	public void SetWordProb (Double prob) {
		word_prob = prob;
	}
	public void SetTFIDF (Double tf_idf) {
		TF_IDF_value = tf_idf;
	}
	public void SetLogLikelihood (Double log_likelihood) {
		log_likelihood_ratio = log_likelihood;
	}
	
	public void Print() {
		System.out.println("----Printing Token:----");
		System.out.println("Text: " + text_ + " ");
		System.out.println("Type: " + type_ + " ");
		System.out.println("Comment offset: " + comment_offset_ + " ");
		System.out.println("Target offset: " + target_offset_ + " ");
		System.out.println("\n");
		
	}
	// boolean IsPunc
	
	// boolean isSpace or something

	/*public Token(String token, int offset) {
		this.text_ = token;
		this.offset_ = offset;
	}*/

}
