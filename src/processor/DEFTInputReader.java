package processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import processor.DeftEntityReader; 
import processor.OutputWriter;
import processor.ToolsProcessor.StanfordCoreNLPProcessor;
import data.*;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import models.DeftRunner;
import util.FileReader;
import util.FileWriter;
import util.Tokenizer;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


public class DEFTInputReader {
	
	// It will read two files, post and annotations
	
	public DeftEntityReader entity_reader;
	// Each comment will contain a post text by one author. can have multiple comments within a file 
	public List<Comment> comments;
	public String source_file;
	public String entity_file;
	public String annotation_file;
	public String doc_id;
	public StanfordCoreNLPProcessor core_nlp;
	
	public DEFTInputReader() {
		comments = new ArrayList<Comment>();
	}
	public DEFTInputReader(String id) {
		doc_id = id;
		entity_reader = new DeftEntityReader(doc_id);
		comments = new ArrayList<Comment>();
	}
	public void SetProcessor(StanfordCoreNLPProcessor core_nlp) {
		this.core_nlp = core_nlp;
	}
	public void SetDocId(String id){
		doc_id = id;
	}
	public void SetEntityReader(DeftEntityReader er){
		entity_reader = er;
	}
	// Reads posts and annotations, and returns comments
	public List<Comment> GetComments
			(String source_file, String annotation_file, String entity_file) {
		entity_reader.ReadEREFile(entity_file);
		//ReadDeftFile(source_file);
		ReadDeftFileStanfordNLP(source_file);
		// removes meta tags even although note they are part of the stanford tok
		CleanUpDeftComments(this.comments);
		if (!annotation_file.isEmpty()) {
		ReadAnnotations(annotation_file);
		}
		// mark all entities, relations and events with E
		AnnotateAllMentions(this.comments);
		return this.comments;
	}
	// Reads posts and annotations, and returns comments
	public List<Comment> GetCommentsMultipleFiles
			(String source_file, List<String> annotation_files, List<String> entity_files) {
		for (String ef: entity_files) {
		//System.out.println("Deft reader: Reading entities for file:"+ef);
		entity_reader.ReadEREFile(ef);
		//System.out.println("Number of entity mentions:" + entity_reader.entity_mentions.size());
		//System.out.println("Number of entities:" + entity_reader.entities.size());
		}
		//ReadDeftFile(source_file);
		ReadDeftFileStanfordNLP(source_file);
		// removes meta tags evenq although note they are part of the stanford tok
		CleanUpDeftComments(this.comments);
		if (!annotation_files.isEmpty()) {
		for (String af: annotation_files) {
		ReadAnnotations(af);
		//System.out.println("Deft reader: Reading annotations for file:"+af);
		}
		}
		// mark all entities, relations and events with E
		AnnotateAllMentions(this.comments);
		/*for (Comment c: this.comments) {
			if (c.tokens_.isEmpty()) {
				System.out.println("DeftInputReader: comment has empty tokens");	
			} else {
				System.out.println("DeftInputReader: comment does not have empty tokens");
			}
		}*/
		// Clean up comments
		List<Comment> cleaned_up = new ArrayList<Comment>();
		for (Comment c: this.comments) {
			if (!c.tokens_.isEmpty()) {
				cleaned_up.add(c);
			}
		}
		this.comments = cleaned_up;
		return this.comments;
	}
	public void ReadEntities(String entity_file) {	
		if (entity_reader == null || doc_id.isEmpty()) {
			return;
		}
		entity_reader.ReadEREFile(entity_file);
		if (entity_reader.entity_mentions.isEmpty()) {
			return;
		}
	}
	
	public void ReadDeftFileStanfordNLP(String source_file) {
		if (this.doc_id.isEmpty()) {
			System.out.println("DEFT Input Reader: Empty doc id. Returning");
			return;
		}
		String original_text = FileReader.ReadFileAsItIs(source_file);
		String author = "none";
		int author_offset = -1;
		
		Comment comment = new Comment();
		comment.SetCommentID(doc_id);
		comment.SetOriginalText(original_text);
		comment.SetEntityReader(entity_reader);
		comment.SetAuthor(author);
		comment.SetAuthorOffset(author_offset);
		List<Token> tokens = StanfordTokenize(original_text);
		List<Token> comment_tokens = new ArrayList<Token>();
		String comment_text = "";
		for (int i=0; i<tokens.size(); i++) {
			tokens.get(i).SetTargetOffset(-1);
			while(i<tokens.size() && !tokens.get(i).text_.equals("</post>")
					&& !tokens.get(i).text_.equals("</P>")) { 
				DeftToken dt = (DeftToken) tokens.get(i);
				dt.SetTargetOffset(-1);
				dt.SetSentiment("neutral");
				comment_tokens.add(dt);
				comment_text += dt.text_ + " ";
				String text = dt.text_;
				//Integer off = dt.char_offset;
				// TODO: use a regular expression (GetPostAuthor) or something to 
				// get the exact author name (not the entire substring)
				//if (text.startsWith("author=\"")) {   
				if (text.contains("author=\"")) { 
					// TODO
					author_offset = original_text.indexOf("author=\"") 
							+ ("author=\"").length();
					//System.out.println("Found comment author in token " + text);
					//author = GetPostAuthor(text);
					//author = author.replaceAll("\"", "");
					//author_offset = original_text.indexOf(author);
					//System.out.println("Author is " + author);
					//comment.SetAuthor(author);
					comment.SetAuthorOffset(author_offset); // may be enough if author is not working
				}
				i+=1;
			}
			comment.SetTokens(comment_tokens);
			comment_text = Tokenizer.RemoveExtraWhiteSpace(comment_text);
			comment.SetText(comment_text);
			if (!comment_text.equals("") && comment_tokens.size()!=0) {
			this.comments.add(comment);
			}
			comment = new Comment();
			comment.SetCommentID(doc_id);
			comment.SetOriginalText(original_text);
			comment.SetEntityReader(entity_reader);
			comment.SetAuthorOffset(author_offset);
			comment.SetAuthor(author);
			comment_tokens = new ArrayList<Token>();
			comment_text = "";
		}
	}
	
	// Reads Deft file and updates comments, each post within a separate comment
	public void ReadDeftFile(String source_file) {
		if (this.doc_id.isEmpty()) {
			System.out.println("DEFT Input Reader: Empty doc id. Returning");
			return;
		}
		String original_text = FileReader.ReadFileAsItIs(source_file);
		String author = "none";
		int author_offset = -1;
		List<Token> tokens = DeftTokenize(original_text);
		Comment comment = new Comment();
		comment.SetCommentID(doc_id);
		comment.SetOriginalText(original_text);
		comment.SetEntityReader(entity_reader);
		comment.SetAuthor(author);
		comment.SetAuthorOffset(author_offset);
		List<Token> comment_tokens = new ArrayList<Token>();
		String comment_text = "";
		for (int i=0; i<tokens.size(); i++) {
			tokens.get(i).SetTargetOffset(-1);
			while(i<tokens.size() && !tokens.get(i).text_.equals("</post>")) { 
				Token dt = (DeftToken) tokens.get(i);
				dt.SetTargetOffset(-1);
				dt.SetSentiment("neutral");
				comment_tokens.add(tokens.get(i));
				comment_text += tokens.get(i).text_ + " ";
				String text = tokens.get(i).text_;
				comment_tokens.add(dt);
				comment_text += dt.text_ + " ";
			if (text.startsWith("author=\"")) {   
				// NOTE! This is not perfect. Author names with punctuations in them will be disintegrated. :|
				// Quote authors with orig_author are not considered at this point
					//author = GetPostAuthor(text);
					/*author = text.substring(("author=\"").length());
					author = author.replaceAll("\"", "");
					author_offset = original_text.indexOf(author,last_seen_author);*/
					//author = original_text.substring(off,off+)
				    author = text.substring(("author=\"").length());
					//author_offset = off + "author=\"".length();
					author = author.replaceAll("\"", "");
					//System.out.println("Author:" + author);
					//System.out.println("Author offset:" + author_offset);
					//last_seen_author = author_offset + 1;
					comment.SetAuthor(author);
					author_offset = original_text.indexOf(author);
					comment.SetAuthorOffset(author_offset);
				}
				i+=1;
			}
			comment.SetTokens(comment_tokens);
			comment_text = Tokenizer.RemoveExtraWhiteSpace(comment_text);
			comment.SetText(comment_text);
			if (!comment_text.equals("") && comment_tokens.size()!=0) {
			this.comments.add(comment);
			}
			comment = new Comment();
			comment.SetCommentID(doc_id);
			comment.SetOriginalText(original_text);
			comment.SetEntityReader(entity_reader);
			comment.SetAuthor(author);
			comment.SetAuthorOffset(author_offset);
			comment_tokens = new ArrayList<Token>();
			comment_text = "";
		}
	}
	
	// Reads annotations and updates comments with gold target and sentiment data
	public void ReadAnnotations(String annotation_file) {
		if (this.comments.isEmpty()) {
			System.out.println("Comments are empty. Please read DEFT source file first\n");
			return;
		}
		Document annotation_doc = FileReader.ReadXMLFile(annotation_file, "english");
		annotation_doc.getDocumentElement().normalize();
		Node sentiment_annotations =  
				annotation_doc.getElementsByTagName("sentiment_annotations").item(0);
		Element sentiment = (Element) sentiment_annotations;
		NodeList entities = sentiment.getElementsByTagName("entity");
		NodeList relations = sentiment.getElementsByTagName("relation");
		NodeList events = sentiment.getElementsByTagName("event");
		// Read sentiment annotations and update comments with targets
		AnnotateEntityTargets(entities); 
		AnnotateEventTargets(events);
		AnnotateRelationTargets(relations);
		
		/*String out_file = doc_id + ".comments";
	    out_file = (new File("DEFT-output", out_file).getAbsolutePath());
		OutputWriter.WriteCommentsToXML(comments, out_file, "english");*/
		}

	// Annotate entity mentions as entities
	public void AnnotateAllMentions(List<Comment> comments) {
		for (Comment c: comments) {
			DeftEntityReader er = c.entity_reader;
			HashMap<String,List<String>> entities = er.entities;
			HashMap<String,String[]> entity_mentions = er.entity_mentions;
			HashMap<String,String[]> relation_mentions = er.relation_mentions;
			HashMap<String,String[]> event_mentions = er.event_mentions;
			for (Token t: c.tokens_) {
				int i = c.tokens_.indexOf(t);
				DeftToken dtok = (DeftToken) t;
				Integer offset = dtok.char_offset;
				String soffset = offset.toString();
				if (entity_mentions.containsKey(soffset)) {
					String length = entity_mentions.get(soffset)[1];
					String mention_text = entity_mentions.get(soffset)[2];
					dtok.SetEntity(true);
					dtok.SetEntityType("E-B");
					dtok.SetEntityText(mention_text);
					Integer ilength = Integer.parseInt(length);
					Integer temp_offset = offset;
					Integer temp_length = dtok.text_.length();
					while (i<c.tokens_.size()-1 && 
							temp_offset + temp_length < offset + ilength) {
						DeftToken dtok_next = (DeftToken) c.tokens_.get(i+1);
						dtok_next.SetEntity(true);
						dtok_next.SetEntityType("E-I");
						dtok_next.SetEntityText(mention_text);
						temp_offset = dtok_next.char_offset;
						temp_length = dtok_next.text_.length();
						i+=1;
					}
				}
				else if (relation_mentions.containsKey(soffset)) {
					String length = relation_mentions.get(soffset)[1];
					String mention_text = relation_mentions.get(soffset)[2];
					if (length.isEmpty() || mention_text.isEmpty()) {
						continue;
					}
					dtok.SetEntity(true);
					dtok.SetEntityType("R-B");
					dtok.SetEntityText(mention_text);
					//System.out.println("Length:"+length);
					Integer ilength = Integer.parseInt(length);
					Integer temp_offset = offset;
					Integer temp_length = dtok.text_.length();
					while (i<c.tokens_.size()-1 && 
							temp_offset + temp_length < offset + ilength) {
						DeftToken dtok_next = (DeftToken) c.tokens_.get(i+1);
						dtok_next.SetEntity(true);
						dtok_next.SetEntityType("R-I");
						dtok_next.SetEntityText(mention_text);
						temp_offset = dtok_next.char_offset;
						temp_length = dtok_next.text_.length();
						i+=1;
					}
				}
				else if (event_mentions.containsKey(soffset)) {
					String length = event_mentions.get(soffset)[1];
					String mention_text = event_mentions.get(soffset)[2];
					if (length.isEmpty() || mention_text.isEmpty()) {
						continue;
					}
					dtok.SetEntity(true);
					dtok.SetEntityType("H-B");
					dtok.SetEntityText(mention_text);
					Integer ilength = Integer.parseInt(length);
					Integer temp_offset = offset;
					Integer temp_length = dtok.text_.length();
					while (i<c.tokens_.size()-1 && 
							temp_offset + temp_length < offset + ilength) {
						DeftToken dtok_next = (DeftToken) c.tokens_.get(i+1);
						dtok_next.SetEntity(true);
						dtok_next.SetEntityType("H-I");
						dtok_next.SetEntityText(mention_text);
						temp_offset = dtok_next.char_offset;
						temp_length = dtok_next.text_.length();
						i+=1;
					}
				}
			}
		}
	}
	
	public void AnnotateEntityTargets(NodeList entities) {
		if (entities.getLength()==0) {
			return;
		}
		for (int e=0; e<entities.getLength(); e++) {
			Node e_node = entities.item(e);
			Element entity = (Element) e_node;
			String ere_id = entity.getAttribute("ere_id");
			String entity_text = entity.getElementsByTagName("text").item(0).getTextContent();
			String offset = entity.getAttribute("offset");
			String length = entity.getAttribute("length");
			Node sentiment_node = entity.getElementsByTagName("sentiment").item(0); 
			/*if (entity.getElementsByTagName("sentiment").item(0).hasChildNodes()) {
			sentiment_node = entity.getElementsByTagName("sentiment").item(0); 
			} else {
				
			}*/
			Element sentiment_element = (Element) sentiment_node;
			String polarity = sentiment_element.getAttribute("polarity");
			String sarcasm = sentiment_element.getAttribute("sarcasm");
			String source_id = "";
			String source_text = "";
			
			if (polarity.equals("pos") || polarity.equals("neg")) {
			//System.out.println("entity text:" + entity_text);
			//System.out.println("polarity:" + polarity);
			//System.out.println("offset:"+offset);
			// probably have to iterate over ElementsByTagName("sentiment").item(i) until
			// we find a source
			if (sentiment_node.hasChildNodes()) {
			Node source_node = sentiment_element.getElementsByTagName("source").item(0);
			Element source_element = (Element) source_node;
			source_id = source_element.getAttribute("ere_id");
			source_text = source_element.getTextContent();
			} else {
					// check sentiment_node = entity.getElementsByTagName("sentiment").item(1);
				/*if (entity.getElementsByTagName("sentiment").item(1).hasChildNodes()) {
					Node source_node = sentiment_element.getElementsByTagName("source").item(1);
					Element source_element = (Element) source_node;
					source_id = source_element.getAttribute("ere_id");
					source_text = source_element.getTextContent();
				 else {*/
					source_id = "";
					source_text = "";
					System.out.println("Weird, no source for this sentiment node");
				//}
			}
		
			// Update the comments that contain these entities
			for (Comment c: this.comments) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					DeftToken dt = (DeftToken) t;
					Integer char_offset = dt.char_offset;
					if (char_offset.toString().equals(offset)) {
						/*System.out.println("Found target! Type: entity");
						System.out.println("First target token:" + dt.text_);
						System.out.println("Char offset:" + char_offset);
						System.out.println("Deft offset:" + offset);*/
						DeftTarget dtarg = (DeftTarget) new DeftTarget(polarity,entity_text);
						dtarg.SetDeftOffset(offset);
						dtarg.SetSarcasm(sarcasm);
						dtarg.SetLength(length);
						dtarg.SetSourceID(source_id);
						dtarg.SetSource(source_text);
						dtarg.SetID(ere_id);
						dtarg.SetType("entity");
						List<Token> target_tokens = new ArrayList<Token>();
						dtarg.comment_offsets_.add(i);
						dt.SetCommentOffset(i);
						dt.SetTargetOffset(0);
						dt.SetSentiment(polarity);
						target_tokens.add(dt);
						int token_offset = char_offset;
						int token_length = dt.text_.length();
						int k=0;
						while (i< c.tokens_.size()-1
								&& token_offset + token_length < char_offset + Integer.parseInt(length)) {
							i+=1;
							k+=1;
							//System.out.println("In target offset loop: i is " + i + "k is " + k);
							DeftToken t_next =  (DeftToken) c.tokens_.get(i);
							t_next.SetCommentOffset(i);
							t_next.SetSentiment(polarity);
							t_next.SetTargetOffset(k);
							target_tokens.add(t_next); 
							token_offset = t_next.char_offset;
							token_length = t_next.text_.length();
						}
						/*System.out.println("Target:" + entity_text);
						System.out.println("Target type: entity");
						System.out.println("Target tokens:");
						for (int tt=0;tt<target_tokens.size();tt++){
							System.out.println(target_tokens.get(tt).text_ +"\t");
						}
						System.out.println("Sentiment:" + polarity);
						System.out.println("Ere id:" + ere_id);
						System.out.println("Source:" + source_text);
						System.out.println("Target offset:" + k);*/
						dtarg.SetTokens(target_tokens);
						dtarg.SetSourceID(source_id);
						c.AddTarget(dtarg);
						break;
					}
				}	
				} // end for
			} // end if polarity is positive or negative
			}
	}
	
	public void AnnotateEventTargets(NodeList events) {
		if (events.getLength()==0) {
			return;
		}
		for (int e=0; e<events.getLength(); e++) {
			Node e_node = events.item(e);
			String source_id = "";
			String source_text = "";
			Element entity = (Element) e_node;
			String ere_id = entity.getAttribute("ere_id");
			Node trigger_node = entity.getElementsByTagName("trigger").item(0);
			Element trigger_element = (Element) trigger_node;
			String offset = trigger_element.getAttribute("offset");
			String length = trigger_element.getAttribute("length");
			String entity_text = trigger_element.getTextContent();
			Node sentiment_node = entity.getElementsByTagName("sentiment").item(0);
			Element sentiment_element = (Element) sentiment_node;
			String polarity = sentiment_element.getAttribute("polarity");
			String sarcasm = sentiment_element.getAttribute("sarcasm");
			
			if (polarity.equals("pos") || polarity.equals("neg")) {
				
				//System.out.println("event text:" + entity_text);
				//System.out.println("polarity:" + polarity);
				
			if (sentiment_node.hasChildNodes()) {
			Node source_node = sentiment_element.getElementsByTagName("source").item(0);
			Element source_element = (Element) source_node;
			source_id = source_element.getAttribute("ere_id");
			source_text = source_element.getTextContent();
			} else {
				// check sentiment_node = entity.getElementsByTagName("sentiment").item(1);
			/*	if (entity.getElementsByTagName("sentiment").item(1).hasChildNodes()) {
					Node source_node = sentiment_element.getElementsByTagName("source").item(1);
					Element source_element = (Element) source_node;
					source_id = source_element.getAttribute("ere_id");
					source_text = source_element.getTextContent();
				} else {*/
				source_id = "";
				source_text = "";
				System.out.println("Weird, no source for this sentiment node");
			//	}
			}
			
			// Update the comments that contain these entities
			// Can also process the comments here (remove quotes from text and tokens, https, and such)
			for (Comment c: this.comments) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					DeftToken dt = (DeftToken) t;
					String text = t.text_;
					Integer char_offset = dt.char_offset;
					if (char_offset.toString().equals(offset)) {
						/*System.out.println("Found target! Type: event");
						System.out.println("First target token:" + dt.text_);
						System.out.println("Char offset:" + char_offset);
						System.out.println("Deft offset:" + offset);*/
						DeftTarget dtarg = (DeftTarget) new DeftTarget(polarity,entity_text);
						dtarg.SetDeftOffset(offset);
						dtarg.SetSarcasm(sarcasm);
						dtarg.SetLength(length);
						dtarg.SetSourceID(source_id);
						dtarg.SetSource(source_text);
						dtarg.SetID(ere_id);
						dtarg.SetType("event");
						List<Token> target_tokens = new ArrayList<Token>();
						dtarg.comment_offsets_.add(i);
						dt.SetCommentOffset(i);
						dt.SetSentiment(polarity);
						dt.SetTargetOffset(0);
						target_tokens.add(dt);
						int token_offset = char_offset;
						int token_length = t.text_.length();
						int k=0;
						while (i< c.tokens_.size()-1 && token_offset + token_length < char_offset + Integer.parseInt(length)) {
							i+=1;
							k+=1;
							DeftToken t_next =  (DeftToken) c.tokens_.get(i);
							t_next.SetCommentOffset(i);
							t_next.SetSentiment(polarity);
							t_next.SetTargetOffset(k);
							target_tokens.add(t_next); 
							token_offset = t_next.char_offset;
							token_length = t_next.text_.length();
						}
						/*System.out.println("Target:" + entity_text);
						System.out.println("Target type: event");
						System.out.println("Target tokens:");*/
						
						/*for (int tt=0;tt<target_tokens.size();tt++){
							System.out.println(target_tokens.get(tt).text_ +"\t");
						}*/
						
						/*System.out.println("Sentiment:" + polarity);
						System.out.println("Ere id:" + ere_id);
						System.out.println("Source:" + source_text);
						System.out.println("Target offset:" + k);*/
						// output system will put source as author for all 
						//System.out.println("Comment author:" + c.author); 
						dtarg.SetTokens(target_tokens);
						dtarg.SetSourceID(source_id);
						c.AddTarget(dtarg);
						break;
					}
				}	
				} // end for	
			} // end if pos or neg
		}
	}
	
	public void AnnotateRelationTargets(NodeList relations) {
		if (relations.getLength()==0) {
			return;
		}
		for (int e=0; e<relations.getLength(); e++) {
			Node e_node = relations.item(e);
			Element entity = (Element) e_node;
			String ere_id = entity.getAttribute("ere_id");
			String source_id = "";
			String source_text = "";
			if (entity.getElementsByTagName("trigger").getLength()==0) {
				continue; // TODO: deal with this later. some targets are not gonna be annotated
			}
			Node trigger_node = entity.getElementsByTagName("trigger").item(0);
			Element trigger_element = (Element) trigger_node; 
			String offset = trigger_element.getAttribute("offset");
			String length = trigger_element.getAttribute("length");
			String entity_text = trigger_element.getTextContent();
			Node sentiment_node = entity.getElementsByTagName("sentiment").item(0);
			Element sentiment_element = (Element) sentiment_node;
			String polarity = sentiment_element.getAttribute("polarity");
			String sarcasm = sentiment_element.getAttribute("sarcasm");
			
			if (polarity.equals("pos") || polarity.equals("neg")) {
				//System.out.println("relation text:" + entity_text);
				//System.out.println("polarity:" + polarity);
				
			if (sentiment_node.hasChildNodes()) {
			Node source_node = sentiment_element.getElementsByTagName("source").item(0);
			Element source_element = (Element) source_node;
			source_id = source_element.getAttribute("ere_id");
			source_text = source_element.getTextContent();
			} else {
				// check sentiment_node = entity.getElementsByTagName("sentiment").item(1);
				/*if (entity.getElementsByTagName("sentiment").item(1).hasChildNodes()) {
					Node source_node = sentiment_element.getElementsByTagName("source").item(1);
					Element source_element = (Element) source_node;
					source_id = source_element.getAttribute("ere_id");
					source_text = source_element.getTextContent();
				} else {*/
				source_id = "";
				source_text = "";
				System.out.println("Weird, no source for this sentiment node");
				//}
			}
			
			// Update the comments that contain these entities
			// Can also process the comments here (remove quotes from text and tokens, https, and such)
			for (Comment c: this.comments) {
				for (int i=0; i<c.tokens_.size(); i++) {
					Token t = c.tokens_.get(i);
					DeftToken dt = (DeftToken) t;
					String text = t.text_;
					Integer char_offset = dt.char_offset;  
					
					if (char_offset.toString().equals(offset)) {
						/*System.out.println("Found target! Type: relation");
						System.out.println("First target token:" + dt.text_);
						System.out.println("Char offset:" + char_offset);
						System.out.println("Deft offset:" + offset);*/
						DeftTarget dtarg = (DeftTarget) new DeftTarget(polarity,entity_text);
						dtarg.SetDeftOffset(offset);
						dtarg.SetSarcasm(sarcasm);
						dtarg.SetLength(length);
						dtarg.SetSourceID(source_id);
						dtarg.SetSource(source_text);
						dtarg.SetID(ere_id);
						dtarg.SetType("event");
						List<Token> target_tokens = new ArrayList<Token>();
						dtarg.comment_offsets_.add(i);
						dt.SetCommentOffset(i);
						dt.SetSentiment(polarity);
						dt.SetTargetOffset(0);
						target_tokens.add(dt);
						int token_offset = char_offset;
						int token_length = t.text_.length();
						int k=0;
						while (i< c.tokens_.size()-1 && token_offset + token_length < char_offset + Integer.parseInt(length)) {
							i+=1;
							k+=1;
							DeftToken t_next =  (DeftToken) c.tokens_.get(i);
							t_next.SetCommentOffset(i);
							t_next.SetSentiment(polarity);
							t_next.SetTargetOffset(k);
							target_tokens.add(t_next); 
							token_offset = t_next.char_offset;
							token_length = t_next.text_.length();
						}
						dtarg.SetTokens(target_tokens);
						dtarg.SetSourceID(source_id);
						c.AddTarget(dtarg);
						/*System.out.println("Target:" + entity_text);
						System.out.println("Target type: relation");
						System.out.println("Target tokens:");*
						for (int tt=0;tt<target_tokens.size();tt++){
							System.out.println(target_tokens.get(tt).text_ +"\t");
						}
						/*System.out.println("Sentiment:" + polarity);
						System.out.println("Ere id:" + ere_id);
						System.out.println("Source:" + source_text);
						System.out.println("Target offset:" + k);
						// output system will put source as author for all 
						System.out.println("Comment author:" + c.author); 
						System.out.println("\n");*/
						break;
					}
				}	
				} // end for
			 } // end if pos or neg
		}
	}
	
	// Removes metadata tags and url content
	public void CleanUpDeftComments(List<Comment> input_comments) {
		// First, mark tokens
		for (Comment c: input_comments) {
			if (c.tokens_.size() == 0) {
				this.comments.remove(c);
				input_comments.remove(c);
				continue;
			}
			String text = "";
			List<Token> tokens = new ArrayList<Token>();
			for (int i=0;i<c.tokens_.size();i++) {
				DeftToken t = (DeftToken) c.tokens_.get(i);
				Integer char_offset = t.char_offset;
				if (t.text_.contains("<post") || t.text_.contains("</post>") 
						|| t.text_.contains("datetime=") 
						|| t.text_.contains("id=")
						|| t.text_.contains("</quote>")
						|| t.text_.contains("</a>")
						|| t.text_.contains("<img")
						|| t.text_.contains("<P>") || t.text_.contains("</P")
						|| t.text_.contains("<DOC")
						|| t.text_.contains("</DOC")
						|| t.text_.contains("<TEXT>") || t.text_.contains("/TEXT"))
					{ //t.SetDescription("meta");
					continue;
					}
				else if (t.text_.startsWith("<quote") || t.text_.contains("orig_author=")) {
					//System.out.println("Token starts with quote!");
					//System.out.println("Token:" + t.text_);
					String quote_author = "";
					int quote_author_offset = 0;
					//t.SetDescription("meta");
					if (t.text_.contains("orig_author=")) {
						//System.out.println("Token contains orig_author");
						quote_author_offset = char_offset + t.text_.indexOf("<quote orig_author=\"");
						quote_author = t.text_.substring(("<quote orig_author=\"").length());
						quote_author = quote_author.replaceAll("\"", "");
						quote_author = quote_author.replaceAll(">", "");
						//System.out.println("Quote author:" + quote_author);
						t.SetDescription("quote"); 
						t.SetQuoteAuthor(quote_author);
						t.SetQuoteAuthorOffset(quote_author_offset);
				}
					while (i<c.tokens_.size() && !t.text_.contains("</quote>")) {
						i+=1;
						t = (DeftToken) c.tokens_.get(i);
						t.SetDescription("quote"); 
						t.SetQuoteAuthor(quote_author);
						t.SetQuoteAuthorOffset(quote_author_offset);
						if (t.text_.equals("</quote>")) {
							break;
						}
						// url within quote
						if (t.text_.startsWith("<a")) {
							while (i<c.tokens_.size() && !t.text_.equals("</a>")) {
								i+=1;
								t = (DeftToken) c.tokens_.get(i);
								if (t.text_.equals("</a>")) {
									break;
								}
								t.SetDescription("url");
							}
						}

					/*	t.SetDescription("quote"); 
						t.SetQuoteAuthor(quote_author);
						t.SetQuoteAuthorOffset(quote_author_offset);*/

						if (!t.text_.startsWith("<quote") && !t.text_.startsWith("/a>")
								&& !t.text_.startsWith("orig_author="))
								 {
						text += t.text_ + " ";
						tokens.add(t);
						}	
					}
					//t.SetDescription("meta");;
				}
				// url
				else if (t.text_.startsWith("<a")) {
					while (i<c.tokens_.size() && !t.text_.equals("</a>")) {
						i+=1;
						t = (DeftToken) c.tokens_.get(i);
						t.SetDescription("url");
						if (t.text_.equals("</a>")) {
							//System.out.println("</a>");
							break;
						}
					//text += t.text_ + " ";
					//tokens.add(t);
					//System.out.println("url");
					}
				}
				else if (t.text_.startsWith("<HEADLINE>")){
					while (i<c.tokens_.size() && !t.text_.equals("</HEADLINE>")) {
						i+=1;
						t = (DeftToken) c.tokens_.get(i);
						t.SetDescription("headline");
						if (t.text_.equals("</HEADLINE>")) {
							break;
						}
				}
				}
				else if (t.text_.startsWith("<DATELINE>")){
					while (i<c.tokens_.size() && !t.text_.equals("</DATELINE>")) {
						i+=1;
						t = (DeftToken) c.tokens_.get(i);
						t.SetDescription("headline");
						if (t.text_.equals("</DATELINE>")) {
							break;
						}
				}
				}
				else {
						if (!t.text_.equals("</quote>") 
							&& !t.text_.equals("</a>") ) {
						t.SetDescription("post");
						text += t.text_ + " ";
						tokens.add(t);
						
					}
				}
				}
			text = text.trim();
			c.SetText(text);
			c.SetTokens(tokens);
			}
	}
	

	// Tokenizes Deft text and stores character offsets for each word
	// DOES NOT MODIFY original_text
	public List<Token> DeftTokenize(String original_text) {
		List<Token> tokens = new ArrayList<Token>();
		if (original_text.isEmpty()) {
			System.out.println("DeftTokenize: Empty tokens. Unable to tokenize comment \n");
			return tokens;
		}
		try {
			int last_seen = 0;
			// get the char offset from last seen, from original text
			String modified_text = original_text;
			modified_text = Tokenizer.ProcessEnglishDeft(modified_text);
			// can do StanfordCoreNLP here, and create new token for each,
			// then update the offset with the tokens
			String[] words = modified_text.split(" ");
			for (int i = 0; i< words.length; i++) {
				//if (!words[i].equals("") && !words[i].equals(" ")) {
				DeftToken t = new DeftToken(words[i], "word");
				int char_offset = original_text.indexOf(words[i], last_seen);
				t.SetCharOffset(char_offset);
				last_seen = char_offset + 1;
				tokens.add(t);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return tokens;
	}
	
	// Tokenizes English/Deft text and stores character offsets for each word
	// DOES NOT MODIFY original_text
	// Uses Stanford CoreNLP tokenization
	//
	// TODO: Remove processEnglishDeft and probably will have to modify CleanUpDeftComments,
	// and make sure the author is getting extracted
	// Runner should not have to re-run stanfordcorenlp each time
	//
	// A similar function can be made for Madamira (use the character offset from madamira)
	// maybe once cleaning up this code
	public List<Token> StanfordTokenize(String original_text) {
		List<Token> tokens = new ArrayList<Token>();
		if (original_text.isEmpty()) {
			System.out.println("DeftTokenize: Empty tokens. Unable to tokenize comment \n");
			return tokens;
		}
		try {
			int last_seen = 0;
			// get the char offset from last seen, from original text
			//String modified_text = original_text;
			//modified_text = Tokenizer.ProcessEnglishDeft(modified_text);
			List<Token> stanford_tokens = core_nlp.AnnotateText(original_text);
			for (Token t: stanford_tokens) {
				DeftToken dt = (DeftToken) t;
				String word = dt.text_;
				String original = dt.original_token;
				//int char_offset = dt.char_offset;
				int char_offset = original_text.indexOf(original, last_seen);
				//System.out.println("Stanford tokenize: Word:"+word);
				//System.out.println("Offset:"+char_offset);
				dt.SetCharOffset(char_offset);
				last_seen = char_offset + 1;
				tokens.add(dt);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println("StanfordTokenize: Size of tokens:" + tokens.size() );
		return tokens;
	}

	public String GetPostAuthor(String original_text) {
		String author = "";
		Pattern regex = Pattern.compile("(author=\")(\\s+\")"); // not working
		Matcher regexMatcher = regex.matcher(original_text);
		if (regexMatcher.find()) {
			author = regexMatcher.group(2);
			System.out.println("Found author:"+ author);
		}
		return author;
	}

}
