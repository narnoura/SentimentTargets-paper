/**
 * 
 */
package data;

import util.BuckwalterConverter;
import util.Tokenizer;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.*;

import processor.DeftEntityReader;


/**
 * @author Narnoura
 * Object class representing an article comment.
 * For input with arabic utf8 encoding, text is represented in Buckwalter.
 */

public class Comment {
	
	public String comment_id_;
	public String raw_text_;
	public List<Target> targets_;
	public List<Token> tokens_;
	// optional
	public List<Entity> entities_;
	//public HashMap<String,String[]> dependency_tree;
	public List<String[]> dependency_tree;
	public Tree parse_tree;
	public SemanticGraph dependencies;
	public Map<Integer, CorefChain> coreference_chain;
	
	
	// These are optional for DEFT
	public String author;
	public String author_id;
	public Integer author_offset;
	public DeftEntityReader entity_reader;
	//  For multiple posts per thread (DEFT)
	public String post_id;
	// Keeps all xml offsets
	public String original_text;
	// Used in topic salience
	public Integer noun_word_count;
	
	// These are the madamira processed versions of the comment
	public List<Target> processed_targets_;
	public List<Token> processed_tokens_;
	public String processed_text_;

	public Comment() {
		targets_ = new ArrayList<Target>();
		entities_ = new ArrayList<Entity>();
	}
	// Initialize a comment with its text
	public Comment(String raw_text) {
		targets_ = new ArrayList<Target>();
		this.raw_text_ = raw_text;
		entities_ = new ArrayList<Entity>();
	}
	// Initialize a comment with its text and comment id
	public Comment(String comment_id, String raw_text) {
		targets_ = new ArrayList<Target>();
		this.comment_id_ = comment_id;
		this.raw_text_ = raw_text;
		entities_ = new ArrayList<Entity>();
	}
	// Set comment with a list of opinion targets
	public void SetTargets(List<Target> targets) {
		this.targets_ = targets;
	}
	// Set comment with a list of tokens
	public void SetTokens(List<Token> tokens) {
		this.tokens_ = tokens;
	}
	// Set comment with a list of entities
	public void SetEntities(List<Entity> entities) {
		this.entities_ = entities;
	}
	// Set dependency tree
	public void SetDependencyTree (List<String[]> dt) {
		dependency_tree = dt;
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
	
	public void SetCommentID (String comment_id) {
		this.comment_id_ = comment_id;
	}
	
	public void SetText (String text) {
		this.raw_text_ = text; 
	}
	
	public void SetUniqueWordCount (Integer count){
		this.noun_word_count = count;
	}
	
	// Set the mada versions of the targets and tokens
	public void SetProcessed(List<Token> p_tokens,List<Target> p_targets,String p_text) {
		this.processed_tokens_ = p_tokens;
		this.processed_targets_ = p_targets;
		this.processed_text_ = p_text;
	}

	// Add opinion target to comment
	public void AddTarget(Target target) {
	 if (this.targets_ == null) {
			this.targets_ = new ArrayList<Target>();
		}
		try{
			this.targets_.add(target);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void AddEntity(Entity entity) {
		 if (this.entities_ == null) {
				this.entities_ = new ArrayList<Entity>();
			}
			try{
				this.entities_.add(entity);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	// Optional
	public void SetAuthor(String author) {
		this.author = author;
	}
	public void SetAuthorID(String author_id) {
		this.author_id = author_id;
	}
	public void SetAuthorOffset(Integer offset) {
		this.author_offset = offset;
	}
	public void SetPostId(String post_id){
		this.post_id = post_id;
	}
	public void SetOriginalText(String original){
		original_text = original;
	}
	public void SetEntityReader (DeftEntityReader entity_reader){
		this.entity_reader = entity_reader;
	}
	// Given a text and its phrase of tokens, find the indicies
	// where the phrase occurs in the comment 
	// Returns -1 if the phrase doesn't occur 
	// NOTE: now comparing the token text rather than the actual token
	// since offsets may or may not be set
	public List<Integer> Find (String phrase_text,
			List<Token> phrase_tokens) {
		
		List<Token> tokens_ = new ArrayList<Token>();
		for (Token t: this.tokens_) {
			if (!t.text_.contains("@@LAT@@")) {
				tokens_.add(t);
			}
		}
		List<Integer> indices = new ArrayList<Integer>();
		if (this.tokens_.isEmpty() || this.raw_text_.isEmpty()) { 
			System.out.println ("This comment has no tokens or no text. "
					+ "Please specify a list of tokens. \n");
			System.exit(0);
		}
		try {
			if (!this.raw_text_.contains(phrase_text)) {
				System.out.println("Phrase not found in comment.");
			} 
			else{
				for (int i = 0; i < tokens_.size(); i++ ) {
					if (tokens_.get(i).HasSameTextAs(phrase_tokens.get(0))) {
						// Found the first matching token
						if (phrase_tokens.size() > (tokens_.size() - i)) {
							continue;
						}
						boolean found = true;
						for (int j=1; j < phrase_tokens.size(); j++) {
							if (!tokens_.get(i+j).HasSameTextAs(phrase_tokens.get(j))){
								found = false;
								break;
							}
						}
						if (found == true) {
							indices.add(i);
							i+= phrase_tokens.size();
						}
					}
				}
			}		
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return indices;
	}
	
	public String[] SplitSentences() {
		String[] sentences = this.raw_text_.split(".");
		return sentences;
	}
	// Tokenize comment text
	// Token type can be "word", "MyD3", or "ATB" 
	// (if reading Comments from Madamira files)
	// For clitics: keeps the original offsets
	// Records the type of clitic to help during feature extraction
	// from the mada feature 'bw'
	public List<Token> Tokenize(String token_type) {
		List<Token> tokens = new ArrayList<Token>();
		try {
		tokens = Tokenizer.SimpleTokenize(this.raw_text_, token_type);
		if (token_type.equals("simple")) {
			for (int i=0; i<tokens.size(); i++) {
				tokens.get(i).SetCommentOffset(i);
				//tokens.get(i).SetTargetOffset(-1);
			}
		}
		// If ATB: keep the token's original offset. 
		else if (token_type.equals("ATB")) {
			int j=0; 
			for (int i=0; i<tokens.size(); i++) {
				if (tokens.get(i).text_.endsWith("+") 
						&!(tokens.get(i).text_.equals("+"))) {
					tokens.get(i).SetCommentOffset(j);
					tokens.get(i).SetClitic("pref");
					if (i!=tokens.size()-1) {
						tokens.get(i+1).SetCommentOffset(j);
						tokens.get(i+1).SetClitic("word");
						/* case: w+ k+ thlk*/
						if (i!=tokens.size()-2 && tokens.get(i+1).text_.endsWith("+")
								&!(tokens.get(i+1).text_.equals("+"))) {
							tokens.get(i+2).SetCommentOffset(j);
							tokens.get(i+1).SetClitic("pref");
							tokens.get(i+2).SetClitic("word");
							/*case: w+ b+ rhmt +h*/
							if (i!=tokens.size()-3 && tokens.get(i+3).text_.startsWith("+")
									&!(tokens.get(i+3).text_.equals("+"))) {
								tokens.get(i+3).SetCommentOffset(j);
								tokens.get(i+3).SetClitic("suf");
								i+=1;
							}
							i+=1;
						}
						/*case : l+ ktb +h */
						else if (i!=tokens.size()-2 && tokens.get(i+2).text_.startsWith("+")
								&!(tokens.get(i+2).text_.equals("+"))) {
							tokens.get(i+2).SetCommentOffset(j);
							tokens.get(i+2).SetClitic("suf");
							tokens.get(i+1).SetClitic("word");
							i+=1;
						}
						i+=1;
					}	
				}
				else if (i!=tokens.size()-1 && tokens.get(i+1).text_.startsWith("+")
						&!(tokens.get(i+1).text_.equals("+"))) {
					tokens.get(i).SetCommentOffset(j);
					tokens.get(i+1).SetCommentOffset(j);
					tokens.get(i).SetClitic("word");
					tokens.get(i+1).SetClitic("suf");	
					i+=1;
				}
				else {
					tokens.get(i).SetCommentOffset(j);
				}	
			j+=1;	
			}
		}
		// If D3: can add one more proclitic '+Al'
		else if (token_type.equals("D3")) {
			int j=0; 
			for (int i=0; i<tokens.size(); i++) {	
				if (tokens.get(i).text_.endsWith("+") 
						&!(tokens.get(i).text_.equals("+"))) {
					tokens.get(i).SetCommentOffset(j);
					tokens.get(i).SetClitic("pref");
					if (i!=tokens.size()-1) {
						tokens.get(i+1).SetCommentOffset(j);
						tokens.get(i+1).SetClitic("word");
						/* case: w+ k+ thlk*/
						if (i!=tokens.size()-2 && tokens.get(i+1).text_.endsWith("+")
								&!(tokens.get(i+1).text_.equals("+"))) {
							tokens.get(i+2).SetCommentOffset(j);
							tokens.get(i+1).SetClitic("pref");
							tokens.get(i+2).SetClitic("word");
							/*case: w+ l+ Al+ byt */
							if (i!=tokens.size()-3 && tokens.get(i+2).text_.endsWith("+")
									&!(tokens.get(i+2).text_.equals("+"))) {
								tokens.get(i+3).SetCommentOffset(j);
								//tokens.get(i+2).SetClitic("det");	
								tokens.get(i+2).SetClitic("pref");
								tokens.get(i+3).SetClitic("word");
								i+=1;
							}
							/*case: w+ b+ rhmt +h*/
							else if (i!=tokens.size()-3 && tokens.get(i+3).text_.startsWith("+")
									&!(tokens.get(i+3).text_.equals("+"))) {
								tokens.get(i+3).SetCommentOffset(j);
								tokens.get(i+3).SetClitic("suf");
								i+=1;
							}
							i+=1;
						}
						/*case : l+ ktb +h */
						else if (i!=tokens.size()-2 && tokens.get(i+2).text_.startsWith("+")
								&!(tokens.get(i+2).text_.equals("+"))) {
							tokens.get(i+2).SetCommentOffset(j);
							tokens.get(i+2).SetClitic("suf");
							tokens.get(i+1).SetClitic("word");
							i+=1;
						}
						i+=1;
					}
				}
				else if (i!=tokens.size()-1 && tokens.get(i+1).text_.startsWith("+")
						&!(tokens.get(i+1).text_.equals("+"))) {
					tokens.get(i).SetCommentOffset(j);
					tokens.get(i+1).SetCommentOffset(j);
					tokens.get(i).SetClitic("word");
					tokens.get(i+1).SetClitic("suf");
					i+=1;
				}
				else {
					tokens.get(i).SetCommentOffset(j);
				}	
			j+=1;	
			}
		}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return tokens;
	}
	
	// Split by separator. Return comment with token offsets modified
	// by excluding separators.
	public List<Token> Split(Set<String> separators) {
		List<Token> separatorless = new ArrayList<Token>();
		for (Token tok: this.tokens_) {
			if (!separators.contains(tok.text_)) {
				separatorless.add(tok);
			}
		}
		return separatorless;
	}
}
