/**
 * 
 */
package processor;

//import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;

import util.FileReader;
import util.BuckwalterConverter;
import util.Tokenizer;
import data.Comment;
import data.Target;
import data.Token;
import main.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


/**
 * @author Narnoura
 * 
 * Reads input files into opinionated Comments or text.
 * File can be a blind test file or a file with 
 * gold labels for targets and sentiment.
 * For blind test files, use raw file
 * For files with gold labels, use xml file
 * Files with gold labels can be used for both train
 * and test.
 * 
 * For the ReadCommentsFromXML method, if input file 
 * is in utf8 Arabic encoding, it's
 * automatically converted into the Buckwalter 
 * transliteration.
 * 
 */
public class InputReader {

	// Read and store Comments from xml file which contains gold targetlabels
	// Check arabic-finegrained folder for sample of xml file
	// If encoding is 'utf8ar', it is automatically converted into Buckwalter.
	public static List<Comment> ReadCommentsFromXML (String xml_file, String input_encoding,
			boolean convertbw) {
		List<Comment> comments = new ArrayList<Comment>();
		Document xml_doc = FileReader.ReadXMLFile(xml_file, input_encoding);
		xml_doc.getDocumentElement().normalize();
		NodeList n_list = xml_doc.getElementsByTagName("comment");
		
		int num_comments = 0;
		int num_targets = 0;
		int num_matching_targets = 0;
		
		for (int c = 0; c < n_list.getLength(); c++) {
			Node n_node = n_list.item(c);
			if (n_node.getNodeType() == Node.ELEMENT_NODE) {
				Element e_element = (Element) n_node;
				String comment_id = e_element.getAttribute("comment_id");
				String text = e_element.getElementsByTagName("text").item(0).getTextContent();
				if (input_encoding.equals("utf8ar") || 
						input_encoding.equals("utf8") && convertbw) {
					text = BuckwalterConverter.IgnoreUnsafeBuckwalterforMada(text);
					text = BuckwalterConverter.ConvertToBuckwalter(text);
				}
				
				// Create new Comment	
				text = text.trim();
				comment_id = comment_id.trim();
				text = Tokenizer.RemoveNewLines(text);
				comment_id = Tokenizer.RemoveNewLines(comment_id);
				
				Comment comment = new Comment(comment_id, text);
				
				num_comments +=1;
			
				// Tokenize
				List<Token> tokens = comment.Tokenize("word");
				comment.SetTokens(tokens); 
				for (Token t: comment.tokens_) {
					t.SetTargetOffset(-1);
				}
				
				//Get targets
				try {
				NodeList target_list = e_element.getElementsByTagName("target");
				for (int t=0; t<target_list.getLength(); t++) {
					
					Node t_node = target_list.item(t);
					if (t_node.getNodeType() == Node.ELEMENT_NODE) {
						Element t_element = (Element) t_node;
						String polarity = t_element.getAttribute("polarity");
						String term = t_element.getAttribute("term");
						
						// Strip all buckwalter punctuations from target 
						// BEFORE converting to buckwalter then set new tokens
						if (input_encoding.equals("utf8ar") || 
								input_encoding.equals("utf8") && convertbw) {
						term = BuckwalterConverter.IgnoreUnsafeBuckwalterforMada(term);
						term = BuckwalterConverter.ConvertToBuckwalter(term);
						}
						term = term.trim();
						term = Tokenizer.RemoveNewLines(term);	
				
						// Find the positions where the target appears in the token
						List<Token> target_tokens = Tokenizer.SimpleTokenize(term, "word");
					
						List<Integer> comment_offsets = comment.Find(
								term, target_tokens);
						
						Target target = new Target(polarity, term);
						if (!target.sentiment_.equals("undetermined")) {
							num_targets +=1;
						}
						
						target_tokens = target.Tokenize("word");
						target.SetTokens(target_tokens);
						// Check if empty then don't add. may lose targets like wAlqSf AlAmryky
						// But if we search for targets after tokenization, we can find them
						if (!comment_offsets.isEmpty()) {	
							target.SetOffsets(comment_offsets);
							// Update offsets in comment tokens.
							for (int o: comment_offsets) {
									for (int k=0; k<target_tokens.size(); k++) {
										Token token_with_target = comment.tokens_.get(k+o);
										token_with_target.SetTargetOffset(k);
										token_with_target.SetSentiment(polarity);
									}
									
							}
							comment.AddTarget(target);
							if (!target.sentiment_.equals("undetermined")) {
							num_matching_targets+=1;
							}
						}
						else{
							System.out.println("\nInputReader::Did not find target: " + target.text_
									+ " comment id:" + comment.comment_id_);
						}
					} // end if t_node
				  } // end for target_list
				} // end try
				catch (Exception e){
					e.printStackTrace();
				}
			
				comments.add(comment);
			}
	
		}
		System.out.println("Number of comments: " + num_comments);
		System.out.println("Number of targets: " + num_targets);
		System.out.println("Number of matching targets: " + num_matching_targets);
		return comments;
	}
	 
	// Read and store Comments from raw test file.
	// Format for id: id will be the first word (space separated)
	// encoding represents the input encoding. Comments will be stored in this encoding.
	public static List<Comment> ReadCommentsFromRaw (String raw_file, String encoding,
			boolean has_comment_id) {
		List<Comment> comments = new ArrayList<Comment>();
		List<String> file_lines = new ArrayList<String>();
		if (encoding == "utf8ar") {
			file_lines = FileReader.ReadUTF8File(raw_file);
		}
		else if (encoding == "bw") {
			file_lines = FileReader.ReadFile(raw_file, encoding,true);
		}
		
		for (String line: file_lines) {
			if (line == "") {
				System.out.println("Empty line. Exiting \n");
				System.exit(0);
			}
			Comment comment = new Comment();
			if(has_comment_id && line.contains(" ")) {
				String comment_id = line.substring(0, line.indexOf(" "));
				String text = line.substring(comment_id.length());
				text = text.trim();
				comment_id = comment_id.replaceAll("\\(|\\)", "");
				comment_id = comment_id.trim();
				text = Tokenizer.RemoveNewLines(text);
				comment_id = Tokenizer.RemoveNewLines(comment_id);
				comment.SetCommentID(comment_id);
				comment.SetText(text);
			}
			else{
				comment.SetText(line);
			}
			comments.add(comment);
		}
		return comments;
	}

	// Read and store Comments from raw test file.
	// Format for id: id will be the first word (space separated)
	// encoding represents the input encoding. Comments will be stored in this encoding.
	public static List<Comment> ReadSemEvalTweets (String raw_file, String encoding,
													 boolean has_comment_id) {
		List<Comment> comments = new ArrayList<Comment>();
		List<String> file_lines = new ArrayList<String>();
		if (encoding == "utf8ar") {
			file_lines = FileReader.ReadUTF8File(raw_file);
		}
		else if (encoding == "bw") {
			file_lines = FileReader.ReadFile(raw_file, encoding,true);
		}

		for (String line: file_lines) {
			if (line == "") {
				System.out.println("Empty line. Exiting \n");
				System.exit(0);
			}
			Comment comment = new Comment();
			if(has_comment_id && line.contains(" ")) {
				String comment_id = line.substring(0, line.indexOf(" "));
				String text = line.substring(comment_id.length());
				text = text.trim();
				comment_id = comment_id.replaceAll("\\(|\\)", "");
				comment_id = comment_id.trim();
				text = Tokenizer.RemoveNewLines(text);
				comment_id = Tokenizer.RemoveNewLines(comment_id);
				comment.SetCommentID(comment_id);
				comment.SetText(text);
			}
			else{
				comment.SetText(line);
			}
			comments.add(comment);
		}
		return comments;
	}


	// Don't need, can use the process comments from Raw
	// Read and store processed Comments from Madamira output files.
	// Set token-type to the tokenization scheme applied in the text
	/*public static List<Comment> ReadCommentsFromTokenizedFile (String tokenized_file,
			String token_type, String encoding) {
		List<Comment> comments = new ArrayList<Comment>();
		List<String> file_lines = new ArrayList<String>();
		if (encoding == "utf8ar") {
			file_lines = FileReader.ReadUTF8File(tokenized_file);
		}
		else if (encoding == "bw") {
			file_lines = FileReader.ReadFile(tokenized_File, encoding);
		}
		for (String line: file_lines) {
			if (line == "") {
				System.out.println("Empty line. Exiting \n");
				System.exit(0);
			}
			Comment comment = new Comment();
			if(line.contains(" ")) {
				String comment_id = line.substring(0, line.indexOf(" "));
				String text = line.substring(comment_id.length());
				System.out.println("InputReader: raw file: comment id: " + comment_id);
				System.out.println("InputReader: raw file: comment text: " + text);
				
				comment.SetCommentID(comment_id);
				comment.SetText(text);
			}
			else{
				comment.SetText(line);
			}
			comments.add(comment);
		}
		return comments;
		
	}*/
	
}
