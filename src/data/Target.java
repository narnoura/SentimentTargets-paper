/**
 * 
 */
package data;

import java.util.ArrayList;
import java.util.List;

import util.Tokenizer;
import util.BuckwalterConverter;

/**
 * @author Narnoura
 * Object representing entity targets of opinions
 */
public class Target {

	public String sentiment_;
	public String text_;
	public String comment_id_;
	// List of start indices where this target appears in the comment
	// Can have multiple occurrences in the comment of the exact same target
	public List<Integer> comment_offsets_;
	public List<Token> tokens_;
	// If text is only head
	public String full_text;
	
	// optional for DEFT
	public String ere_id;
	
	// true if target is an entity (optional)
	public boolean entity;
		
	public Target() {
		comment_offsets_ = new ArrayList<Integer>();
		text_ = "";
		sentiment_ = "";
	}
	
	public Target(String sentiment, String text) {
		this.sentiment_ = sentiment;
		this.text_ = text;
		comment_offsets_ = new ArrayList<Integer>();
	}
	
	public Target(String text, List<Token> tokens) {
		this.text_ = text;
		this.tokens_ = tokens;
		this.sentiment_ = "";
	}
	
	public void SetOffsets (List<Integer> comment_offsets) {
		this.comment_offsets_ = comment_offsets;
	}
	
	public void SetTokens (List<Token> tokens) {
		this.tokens_ = tokens;
	}
	
	public void SetSentiment (String sentiment) {
		this.sentiment_ = sentiment;
	}
	
	public void SetText (String text) {
		this.text_ = text;
	}
	
	public void SetFullText (String full_text) {
		this.full_text = full_text;
	}
	
	public void SetCommentID (String comment_id) {
		this.comment_id_ = comment_id;
	}
	public void SetEntity (boolean entity) {
		this.entity = entity;
	}
	
	// Tokenize comment text
	// Token type can be : word (if just segmenting the raw text),
	// ATB, or MyD3 (if tokenized by Madamira)
	public List<Token> Tokenize(String token_type) {
		List<Token> tokens = new ArrayList<Token>();
		tokens = Tokenizer.SimpleTokenize(this.text_, token_type);
		return tokens;
	}
	
	// Restores original word tokenization from ATB tokenization
	// Attaches separated clitics
	// Agnostic to original tokenization, so only use this for 
	// targets which we know are noun phrases and don't have 
	// ambiguous tokens such as conjunction 'w'
	// If input encoding is in utf8, it converts to BW first
	// and returns the target in utf8
	/*public void DetokenizeATB(String input_encoding) {
	try {
		if (input_encoding.equals("utf8") || input_encoding.equals("utf8ar")) {
			this.text_ = util.BuckwalterConverter.ConvertToBuckwalter(this.text_);
			for (Token t: this.tokens_) {
				t.text_ = util.BuckwalterConverter.ConvertToBuckwalter(t.text_);
			}
		}
		String detokenized = util.Tokenizer.ATBDetokenize(this.text_);
		
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	}*/

	
}
