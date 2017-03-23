package data;

import java.util.HashMap;

public class DeftToken extends Token {
    /*
     * Inherits Token. Allows to keep track of quote tokens vs. original author tokens
     */
	
	public int char_offset;
	public boolean target;
	public boolean quote;
	// before tokenization
	public String original_token;
	
	// Is it part of a quote or metadata (post, quote, or meta)
	public String description;
	public String quote_author;
	public int quote_author_offset;
	
	// Which post is it part of (1,2,3 ..)
	public int post_nb; // will likely not use
	
	// Index from stanford sentence tokenization
	public int sentence_index;
	
	
	public HashMap<String,String> token_binary_features;
	
	public DeftToken() {
	}
	public DeftToken(String token, String type) {
		super(token,type);
		quote_author = "";
		token_binary_features = new HashMap<String,String>();
	}
	// Saves the offset from the original text
	public void SetCharOffset (int char_offset) {
		this.char_offset = char_offset;
	}
	public void SetTarget (boolean target){
		this.target = target;
	}
	public void SetQuote (boolean quote){
		this.quote = quote;
	}
	public void SetQuoteAuthor (String author) {
		this.quote_author = author;
	}
	public void SetQuoteAuthorOffset (Integer quote_author_offset){
		this.quote_author_offset = quote_author_offset;
	}
	public void SetPostNb (int post_nb){
		this.post_nb = post_nb;
	}
	public void SetSentenceIndex(int index) {
		this.sentence_index = index;
	}
	public void SetDescription(String post_quote_meta) {
		this.description = post_quote_meta;
	}
	public void SetOriginal(String original_token) {
		this.original_token = original_token;
	}
	
	


}
