/**
 * 
 */
package processor;

import data.Comment;
import data.Target;
import util.Tokenizer;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.FileWriter;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Narnoura
 * Writes Comments to output files
 */
public class OutputWriter {

	public static void WriteCommentsToXML(List<Comment> comments, 

			String file_path, String encoding) {
		
		try {
			DocumentBuilderFactory doc_factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder doc_builder = doc_factory.newDocumentBuilder();
			Document doc = doc_builder.newDocument();
			
			Element root = doc.createElement("comments");
			doc.appendChild(root);
			
			for (Comment c: comments) {
				Element comment = doc.createElement("comment");
				// set id
				Attr attr = doc.createAttribute("comment_id");
				attr.setValue(c.comment_id_);
				comment.setAttributeNode(attr);
				// set text
				Element text = doc.createElement("text");
				text.appendChild(doc.createTextNode(c.raw_text_));
				comment.appendChild(text);
				// set targets
				Element targets = doc.createElement("targets");
				if (c.targets_ != null) {
				for (int i=0; i<c.targets_.size(); i++) {
					Target t = c.targets_.get(i);
					Element target = doc.createElement("target");
					Attr polarity = doc.createAttribute("polarity");
					polarity.setValue(t.sentiment_);
					target.setAttributeNode(polarity);
					Attr term = doc.createAttribute("term");
					term.setValue(t.text_);
					target.setAttributeNode(term);
					// Optional. Depends on the tokenization.
					// Assuming this code is being used with the original 
					// (word, simple) tokenization
					if (t.comment_offsets_ != null) {
						Attr indices = doc.createAttribute("indices");
						String ind = "";
						for (Integer index: t.comment_offsets_) {
							ind += index.toString();
							if (t.comment_offsets_.indexOf(index)
									!= t.comment_offsets_.size()-1) {
								ind += ",";
							} 
						} // end indicies for	
						indices.setValue(ind);
						target.setAttributeNode(indices);
					} // end if
					targets.appendChild(target);
				}
			  } // targets not null
			else {
				targets.setNodeValue("none");
			}
				comment.appendChild(targets);
				root.appendChild(comment);
			} // comment loop

			// Now write to file
			FileWriter.WriteXMLFile(doc, file_path, encoding);
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/*public static boolean WriteCommentsToRaw(List<Comment> comments, String file_path,
			String encoding) {
		
		if (comments.isEmpty() || comments==null) {
			return false;
		}
		String content = "";
		for (Comment c: comments) {
			try {
				String text = c.raw_text_;
				String comment_id = c.comment_id_;
				if (comment_id.isEmpty() || text.isEmpty() ) {
					System.out.println("Empty comment or empty id\n");
					return false;
				}
			    content += "(" + comment_id + ")" + text;
				if (encoding.equals("utf8") || encoding.equals("utf8ar")) {
					FileWriter.WriteUTF8File(file_path, content);
				} 
				else{
					FileWriter.WriteFile(file_path, content);
				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}*/
	
	public static boolean WriteCommentsToRaw(List<Comment> comments, String file_path,
			String encoding) {
		
		if (comments.isEmpty() || comments==null) {
			return false;
		}
		String line;
		try {
			PrintWriter writer = new PrintWriter(file_path, "UTF-8");
			for (Comment c: comments) {
				String text = c.raw_text_;
				String comment_id = c.comment_id_;
				text = text.trim();
				comment_id = comment_id.trim();
				text = Tokenizer.RemoveNewLines(text);
				comment_id = Tokenizer.RemoveNewLines(comment_id);
				if (comment_id.isEmpty() || text.isEmpty() ) {
					System.out.println("Empty comment or empty id\n");
					return false;
				}
			    line = "(" + comment_id + ")" + "  " +  text;
			    writer.println(line);
			    }
			writer.close();	
		}
		catch (Exception e) {
				e.printStackTrace();
			}
		return true;
	}
	
}
