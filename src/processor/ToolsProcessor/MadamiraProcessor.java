package processor.ToolsProcessor;

/**
 * @author Narnoura
 * Processes output files from Madamira
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processor.InputReader;
import util.BuckwalterConverter;
import util.FileReader;
import util.Tokenizer;
import main.Constants;
import data.Token;
import data.Comment;


public class MadamiraProcessor {

	// Reads a tokenized file (.MyD3.tok, .ATB.tok) and removes '+' from the tokens
	// $inputdir + $inputfile + "ATB.tok"
	public static List<String> ReadTokenizedFile (String file_path, String encoding) {
		if (!file_path.endsWith(".MyD3.tok") && !file_path.endsWith(".ATB.tok")) {
			System.out.println("Please specify a tokenized file in D3 or ATB format\n");
			System.out.println("You tried to read this file: " + file_path);
			return null; 
		}
		List<String> lines = new ArrayList<String>();
		try {
			if (encoding == "utf8ar") {
				lines = FileReader.ReadUTF8File(file_path);	
			} else if (encoding == "bw") {
				lines = FileReader.ReadFile(file_path, encoding, true);
			} 
		}
		catch (Exception e) {
			e.printStackTrace(); 
		}
		for (String l : lines) {
			System.out.println("MadamiraProcessor: Tokenized line:" + l + "\n");
		}
		return lines;
	}
	
	// Reads tokenized file into comments
	public static List<Comment> ReadTokenizedComments (String file_path, String encoding) {
		if (!file_path.endsWith("MyD3.tok") && !file_path.endsWith("ATB.tok")) {
			System.out.println("Please specify a tokenized file in D3 or ATB format\n");
			System.out.println("You tried to read this file: " + file_path);
			return null;
		}
		List<Comment> tokenized_comments = InputReader.ReadCommentsFromRaw(file_path, encoding, true);
		for (Comment c: tokenized_comments) {
			List<Token> tokens = new ArrayList<Token>();
			if (file_path.endsWith("ATB.tok")) {
			tokens = c.Tokenize("ATB");
			}
			else if (file_path.endsWith("MyD3.tok")) {
			tokens = c.Tokenize("D3");
			}
			c.SetTokens(tokens);
			// remove + right before parsing instead
			// c.SetText(c.GetText().replaceAll("\\+", "")); 
		}
		return tokenized_comments;
	}
	
	// Updated to take in D3 or ATB tokenized comments
	// Given ***D3 tokenized**** comments, 
	// reads Madamira file and updates NER for tokens
	// Update for ATB comments
	public static List<Comment> UpdateNER (String file_path, String encoding,
			List<Comment> D3_tokenized_comments, String ATB_or_D3) {
		List<Comment> updated_comments = D3_tokenized_comments;
		List<String> NER_tokens = ReadNERorBPCfile(file_path, encoding);
		List<String> without_Al = new ArrayList<String>();
		if (ATB_or_D3.equals("ATB")) {
			for (String t: NER_tokens) {
				if (t.equals("")) {
					without_Al.add("");
				}  else {
				//System.out.println("t:"+t);
				String word = t.split("\t")[0];
				String ner = t.split("\t")[1];
				if (!word.equals("Al+")) {
					Integer i = NER_tokens.indexOf(t);
					if (i>0 && NER_tokens.get(i-1).split("\t")[0].equals("Al+") 
							&& NER_tokens.get(i-1).split("\t")[1].startsWith("B-")) {
						ner = ner.replaceAll("I-", "B-");
						without_Al.add(word+"\t"+ner);
					} else {
						without_Al.add(t);
					}
				} 
			   }
			}
			NER_tokens = without_Al;
		}
		int i =0;
		for (Comment c: updated_comments) {
			for (Token t: c.tokens_) {
				String NER_token = NER_tokens.get(i);
				String NER = NER_token.split("\t")[1];
				t.SetNER(NER);
				i+=1;
			}
			String space = NER_tokens.get(i);
			if (!(space.equals(""))) {
				System.out.println("Should be at end of comment, got " + space + " instead\n");
				System.exit(0);
		    }
			i+=1;
		}
		return updated_comments;
	}
	
	// Updated to take in D3 or ATB tokenized comments
	// Given ***D3 tokenized**** comments, 
	// reads Madamira file and updates BPC for tokens
	public static List<Comment> UpdateBPC (String file_path, String encoding,
			List<Comment> D3_tokenized_comments, String ATB_or_D3) {	
		List<Comment> updated_comments = D3_tokenized_comments;
		List<String> BPC_tokens = ReadNERorBPCfile(file_path, encoding);
		List<String> without_Al = new ArrayList<String>();
		if (ATB_or_D3.equals("ATB")) {
			for (String t: BPC_tokens) {
				if (t.equals("")) {
					without_Al.add("");
				} else {
				String word = t.split("\t")[0];
				String ner = t.split("\t")[1];
				if (!word.equals("Al+")) {
					Integer i = BPC_tokens.indexOf(t);
					if (i>0 && BPC_tokens.get(i-1).split("\t")[0].equals("Al+") 
							&& BPC_tokens.get(i-1).split("\t")[1].equals("B-NP")) {
						ner = "B-NP";
						without_Al.add(word+"\t"+ner);
					} else {
						without_Al.add(t);
					} 
				} 
			}
		}
		BPC_tokens = without_Al;
		}
		int i =0;
		for (Comment c: updated_comments) {
			for (Token t: c.tokens_) {
				String BPC_token = BPC_tokens.get(i);
				String BPC = BPC_token.split("\t")[1];
				t.SetBPC(BPC);
				i+=1;
			}
			String space = BPC_tokens.get(i);
			if (!(space.equals(""))) {
				System.out.println("Should be at end of comment, got " + space + " instead\n");
				System.exit(0);	
			}
			i+=1;
		}
		return updated_comments;
	}
	
	
	// Reads NER or BPC file into tokens
    public static List<String> ReadNERorBPCfile (String file_path, String encoding) {	
		if (!file_path.endsWith(".ner-bio") && !file_path.endsWith(".bpc-bio")) {
			System.out.println("Please specify a tokenized file in NER or BPC bio format\n");
			System.out.println("You tried to read this file: " + file_path);
			return null;
		}
	    List<String> lines = new ArrayList<String>();
		try {
			 lines = FileReader.ReadFile(file_path, encoding, false);
			}
		catch (Exception e) {
			e.printStackTrace();
		}	
		return lines;
	}
	
    
	// Runs Madamira, given the following config options:
	// output encoding: utf8ar|bw
	// input file
	// output directory
	// config file optional
	// Runs with utf8 input only unless you specify a config file
	//
	// Someone on StackOverFlow posted that it was not "in the spirit" of java
	// to run unix scripts from within the code. While hoping that my code 
	// preserves the "spirit" of Java in all other aspects, the Madamira API
	// still forces you to use an input file and write to an output file.
	// You can't access the data structures (at least, not that I am aware of)
	// So, it's not worth it when you can run a script to do the same thing.
	public static void RunMadamira (String input_file, String output_dir, 
			String output_encoding, String config_file) {
		
		if (config_file.equals("")) {
			try {
				if (output_encoding.equals("bw")) {
					config_file = Constants.MADAMIRA_BW_CONFIG;
				} else if (output_encoding.equals("utf8ar")) {
					config_file = Constants.MADAMIRA_UTF8_CONFIG;
				}
			}
			catch (Exception e) {
				System.out.println("RunMadamira:Invalid output encoding. "
						+ "Please specify a valid output encoding. (bw|utf8ar)");
				e.printStackTrace();
			}
		}	
		String	cmd = "java -Xmx2500m -Xms2500m -XX:NewRatio=3 -jar "
					+ "MADAMIRA-release-20150421-2.1/MADAMIRA-release-20150421-2.1.jar "
					+ "-rawinput " + input_file
					+ "-rawoutdir " +  output_dir + "-rawconfig " + config_file;	
		try {
		Runtime.getRuntime().exec(cmd); 
		}
		catch (Exception e) { 
			e.printStackTrace();	
		}	
	}
	
	
	// Reads a .mada file and stores the SVM predictions for each word
	// input_encoding: utf8, utf8ar, bw
	// ! Offset is a character offset. 
	public static List<Comment> ReadMadaFile (String file_path, String input_encoding) {
		List<Comment> madamira_comments = new ArrayList<Comment>();
		List<String> mada_lines = new ArrayList<String>();
		if (input_encoding.equals("utf8ar") || input_encoding.equals("utf8")) {
			mada_lines = FileReader.ReadUTF8File(file_path);
		} else {
			mada_lines = FileReader.ReadFile(file_path, "bw", true);
		}
		
		// Process mada line
		Comment this_comment = new Comment();
		List<Token> these_tokens = new ArrayList<Token>();
		String sentence_id;
		String sentence_text;
		Token this_token = new Token();
		HashMap<String, String> token_features = new HashMap<String,String>();
		int comment_offset=0;
		
		try {
		for (String line: mada_lines) {	
			line = line.trim();
			if (line.startsWith(";;; SENTENCE_ID")) {
				sentence_id = line.substring(";;; SENTENCE_ID ".length());
				sentence_id = sentence_id.replaceAll("\\(|\\)", "");
				this_comment = new Comment();
				these_tokens = new ArrayList<Token>();
				this_comment.SetCommentID(sentence_id);
				comment_offset = 0;
			}
			else if (line.startsWith(";;; SENTENCE ")) {
				sentence_text = line.substring(";;; SENTENCE ".length());
				//sentence_text = Tokenizer.RemoveExtraWhiteSpace(sentence_text);
				// Checked training data: all @@LAT words are strange characters like stars 
				// &gt, or    '  ,   {  ,  }   from the original utf8 text
				// Actual latin words are marked 'no analysis'
				sentence_text = sentence_text.replaceAll("@@LAT(\\S+)", "");
				sentence_text = sentence_text.trim();
				sentence_text = Tokenizer.RemoveExtraWhiteSpace(sentence_text);
				this_comment.SetText(sentence_text);
			}
			else if (line.startsWith(";;WORD ")) {
				String word = line.substring(";;WORD ".length());
				word = Tokenizer.RemoveExtraWhiteSpace(word);
				/*if (word.contains("@@LAT@@")) {
					word = "";
				}*/
				this_token = new Token(word,"word");
				if (word.contains("@@LAT@@") && comment_offset!=0) {
					this_token.SetCommentOffset(comment_offset-1);
				} else {
				this_token.SetCommentOffset(comment_offset);
				}
				token_features = new HashMap<String, String>();
				token_features.put("WORD", word);	
			}
			else if (line.startsWith(";;LENGTH ")) {
				String length = line.substring(";;LENGTH ".length());
				length = Tokenizer.RemoveExtraWhiteSpace(length);
				token_features.put("LENGTH", length);	
			}
			else if (line.startsWith(";;OFFSET ")) {
				String offset = line.substring(";;OFFSET ".length());
				offset = Tokenizer.RemoveExtraWhiteSpace(offset);
				token_features.put("OFFSET", offset);	
			}
			/*else if (line.startsWith(";;SVM_PREDICTIONS ")) {
				String predictions = line.substring(";;SVM_PREDICTIONS ".length());
			}*/
			// Final SVM predictions (could also store, for no-analysis?)
			else if (line.startsWith("*")) {
				String[] morph = line.split(" ");
				for (int i=1; i< morph.length; i++) {
					String[] feature=morph[i].split(":",2); //avoid ::f
					token_features.put(feature[0], feature[1]);
				}
				this_token.SetMorphFeatures(token_features);
				this_token.SetPOS(this_token.morph_features.get("pos"));
				if (this_token.text_ != null && !(this_token.text_.equals(""))) {
				these_tokens.add(this_token);
				comment_offset+=1;
				} 
			}
			else if (line.startsWith("NO-ANALYSIS")) {
				token_features.put("NO_ANALYSIS", "NO_ANALYSIS");
				this_token.SetMorphFeatures(token_features); 
				this_token.SetPOS("NO_ANALYSIS");
				if (this_token.text_ != null && !(this_token.text_.equals(""))) {
				these_tokens.add(this_token);	
				comment_offset+=1; }
			}
			else if (line.startsWith("SENTENCE BREAK")) {
				this_comment.SetTokens(these_tokens);
				madamira_comments.add(this_comment);
			}
				
		} // end for
		} // end try
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return madamira_comments;
	}
	

}
