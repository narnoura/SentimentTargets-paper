package data;

import java.util.ArrayList;
import java.util.List;

import util.Tokenizer;

public class DeftComment extends Comment {
    /*
     * Deft Comment can specify posts within the comment and tokenize in Deft-specific manner
     */
	/* may not be needed after all */
	
	public List<DeftToken> deft_tokens;
	
	public DeftComment() {
	}
	public DeftComment(String raw_text) {
		super(raw_text);
	}
	public DeftComment(String comment_id, String raw_text){
		super(comment_id, raw_text);
	}
	
	public List<DeftToken> DeftTokenize() {
		List<DeftToken> tokens = new ArrayList<DeftToken>();
		if (this.original_text.isEmpty()) {
			System.out.println("DeftTokenize: Empty tokens. Unable to tokenize comment \n");
			return tokens;
		}
		try {
			int last_seen = 0;
			// get the char offset from last seen, from original text
			String[] words = original_text.split(" ");
			for (int i = 0; i< words.length; i++) {
				DeftToken t = (DeftToken) new Token(words[i], "word");
				int char_offset = original_text.indexOf(words[i], last_seen);
				t.SetCharOffset(char_offset);
				last_seen = char_offset;
				tokens.add(t);
				System.out.println("Deft Token:" + words[i]);
				System.out.println("Char offset:" + char_offset);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return tokens;
	}
	
}
