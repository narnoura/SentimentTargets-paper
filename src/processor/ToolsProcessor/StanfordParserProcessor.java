package processor.ToolsProcessor;

import java.io.PrintWriter;
import java.util.*;

import org.w3c.dom.Document;

import util.FileReader;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.international.arabic.ArabicTreebankLanguagePack;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import data.Comment;
import data.Token;
import data.Target;

// Note: If we are parsing tokenized comments (with no gold), we won't have the targets
// Unless we store the tokenized comments also in the comments
// But we have the ids, so we can match the tokenized to the not tokenized comments
// But not the targets
// Would have to tokenize the targets separately?
// To run this class, must add your Stanford parser jar files to classpath

public class StanfordParserProcessor {
	
	// Parser model (.gz)
	// For Arabic, currently only utf8 input accepted. BW parsing models not available.
	public String model_;
	
	// Token_type specifies the tokenization scheme of the input text 
	// Can be word, ATB, or D3 (MyD3)
	public String token_type_;
	
	LexicalizedParser lp;
	
	public StanfordParserProcessor(String model, String token_type) {
		this.model_ = model;
		this.token_type_ = token_type;
		try {
			lp = LexicalizedParser.loadModel(this.model_);
		}
		catch (Exception e) {
			System.out.println("Unable to load parser model \n");
			e.printStackTrace();
		}
		// for long sentences, see how to put memory to 2000M
		// wtf is it not finding edu anymore?
	}
	
	// Parses a sentence or comment represented by tokens
	// Currently doesn't return the typed dependencies, but we can also do that I guess
	public Tree Parse (String[] tokens) {
		Tree parse = null;
		System.out.println("Parsing new comment \n");
		try{
			List<CoreLabel> label_list = Sentence.toCoreLabelList(tokens);
			parse = lp.apply(label_list);
			parse.pennPrint();
			//parse.pennPrint (printwriter) or
			// TreePrint tp = new TreePrint(Tree t, String id, PrintWriter pw         )
			// tp.printTree(parse)
			System.out.println();
			}
		catch (Exception e) {
			e.printStackTrace();
		}
		return parse;
	}
	
	// Parses a single Comment, returns a tree 
	// get the tokens from tokenizing the comment, create a list that has the text tokens, and run the parser
	// on the tokens
	// keeps the id of the comment!
	public Tree ParseComment (Comment comment) {
		Tree parse_tree = null; 
		try {
			// It will just split it into ATB tokens
			List<Token> tokens = comment.Tokenize(this.token_type_);
			String[] words = new String[tokens.size()];
			for (int i=0;i<tokens.size();i++) {
				words[i] = tokens.get(i).text_;
				words[i] = words[i].replaceAll("\\+", "");
			}
			List<CoreLabel> label_list = Sentence.toCoreLabelList(words);
			parse_tree = lp.apply(label_list);		
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
		return parse_tree;	
	}
	
	// Parses a single Comment, returns a tree 
	// get the tokens from tokenizing the comment, create a list that has the text tokens, and run the parser
	// on the tokens
	// Ignores Al+ and effectively feeds only ATB tokens
	// So if you give it an ATB tokenized comment it will also work
	//
	// keeps the id of the comment!
	public Tree ParseD3Comment (Comment comment) {
		Tree parse_tree = null;
		try {
			List<Token> tokens = comment.tokens_;
			String[] words = new String[tokens.size()];
			int node = 0;
			for (int i=0;i<tokens.size();i++) {
				Token t = tokens.get(i);
				if (t.text_.equals("Al+")) {
					continue;
				}
				words[node] = t.text_;
				words[node] = words[node].replaceAll("\\+", "");
				node +=1;
				}
			List<CoreLabel> label_list = Sentence.toCoreLabelList(words);
			parse_tree = lp.apply(label_list);		
			}
			catch (Exception e) {
				e.printStackTrace();
			}	
		
		return parse_tree;
	}

	// Parses a list of Comments, returns a list of trees (hopefully in same order)
	// If file_path is not empty, prints the trees to the output dir as an xml file
	// get the tokens from tokenizing the comment, create a list that has the text tokens, and run the parser
	// on the tokens
	// keeps the id of the comment!
	//
	// For coreference: can try it with penn tree instead of xml, might be faster
	public List<Tree> ParseD3Comments (List<Comment> comments, String file_path) {
		List<Tree> parse_trees = new ArrayList<Tree>();
		TreebankLanguagePack tlp = new ArabicTreebankLanguagePack();
		try {
			PrintWriter pw = new PrintWriter(file_path);
			TreePrint tp = new TreePrint("xmlTree,", "xml", tlp);
			// TreePrint tp = new TreePrint("penn", "xml", tlp);
			tp.printHeader(pw, "UTF8");
			for (Comment c: comments) {
				System.out.println("Parsing new comment \n");
					// String id = c.comment_id_;
					// String text = c.GetText();
					List<Token> tokens = c.tokens_;
					c.SetTokens(tokens);
					String[] words = new String[c.tokens_.size()];
					int node = 0;
					for (int i=0;i<tokens.size();i++) {
						if (tokens.get(i).text_.equals("Al+")) {
							continue;
						}
						words[node] = tokens.get(i).text_;
						if (words[node].startsWith("+") || words[node].endsWith("+")
								&& !(words[node].equals("+")) && !(words[node].equals(" + "))
								&& !(words[node].equals(" +")) && !(words[node].equals("+ "))) { 
							words[node] = words[node].replaceAll("\\+", "");
							}
						node +=1;
					}
					List<CoreLabel> label_list = Sentence.toCoreLabelList(words);
					Tree parse = lp.apply(label_list);
					tp.printTree(parse,c.comment_id_,pw);
					parse_trees.add(parse);
					
				}
			// TODO: issue: why isn't it printing the last 's' and the footer?
			tp.printFooter(pw);
			
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		return parse_trees;	
	}
	
	
	// Parses a list of Comments (can be D3 tokenized), returns a list of trees (hopefully in same order)
		// If file_path is not empty, prints the trees to the output dir as an xml file
		// get the tokens from tokenizing the comment, create a list that has the text tokens, and run the parser
		// on the tokens
		// keeps the id of the comment!
		//
		// For coreference: can try it with penn tree instead of xml, might be faster
		public List<Tree> ParseComments (List<Comment> comments, String file_path) {
			List<Tree> parse_trees = new ArrayList<Tree>();
			TreebankLanguagePack tlp = new ArabicTreebankLanguagePack();
			try {
				PrintWriter pw = new PrintWriter(file_path);
				TreePrint tp = new TreePrint("xmlTree,", "xml", tlp);
				// TreePrint tp = new TreePrint("penn", "xml", tlp);
				tp.printHeader(pw, "UTF8");
				for (Comment c: comments) {
					System.out.println("Parsing new comment \n");
						// String id = c.comment_id_;
						// String text = c.GetText();
						List<Token> tokens = c.tokens_;
						c.SetTokens(tokens);
						String[] words = new String[c.tokens_.size()];
						for (int i=0;i<tokens.size();i++) {
							words[i] = tokens.get(i).text_;
							if (words[i].startsWith("+") || words[i].endsWith("+")
									&& !(words[i].equals("+")) && !(words[i].equals(" + "))
									&& !(words[i].equals(" +")) && !(words[i].equals("+ ")))
								{ words[i] = words[i].replaceAll("\\+", ""); }
						}
						List<CoreLabel> label_list = Sentence.toCoreLabelList(words);
						Tree parse = lp.apply(label_list);
						tp.printTree(parse,c.comment_id_,pw);
						parse_trees.add(parse);
						
					}
				// TODO: issue: why isn't it printing the last 's' and the footer?
				tp.printFooter(pw);
				
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			return parse_trees;	
		}
	
	// Reads parse trees from an xml file
	// use tree or xml?
	/*public List<Comment> ReadParseTrees (String file_path) {
		List<Comment> comments = new ArrayList<Comment>();
		Document xml_doc = FileReader.ReadXMLFile(file_path, "");
		
	
		xml_doc.getDocumentElement().normalize();
	}
	*/
}
