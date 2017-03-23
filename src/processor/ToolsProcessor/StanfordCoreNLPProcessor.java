package processor.ToolsProcessor;



import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import data.Comment;
import data.DeftToken;
import data.Token;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.BeforeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap.Key;
import util.FileWriter;


public class StanfordCoreNLPProcessor {
	
	public String output_dir = "../stanford-output";
	public String input_dir;
	//public String prop = "tokenize, ssplit, pos, lemma, ner, parse, dcoref";
	public String prop = "tokenize, ssplit, pos, lemma, ner,depparse";
	public StanfordCoreNLP pipeline;

	public StanfordCoreNLPProcessor() {
		Properties props = new Properties();
		props.put("annotators", this.prop);
		//props.put("tokenize.options", "invertible=true");
		props.put("tokenize.options", "tokenizeNLs=false,untokenizable=allKeep,invertible=true");
		//props.put("tokenize.options", "invertible=true,tokenizeNLs=false");
		//props.put("tokenize.whitespace", true); // only tokenize on whitespace
		//props.put("ssplit.isOneSentence", true); // don't split comment into sentences
		pipeline = new StanfordCoreNLP(props);
	}
	
	public void AnnotateAndPrintText(String text, String prop) {
		
	}
	
	public void SetOutputDir(String output_dir) {
		this.output_dir = output_dir;
	}
	
	public void SetProperties(String prop) {
		this.prop = prop;
	}
	
	public Comment AnnotateComment(Comment c, String prop) {
		return c;
	}
	
	// Annotates text using properties and returns tokens
	public List<Token> AnnotateText(String text) {
		List<Token> stanford_tokens = new ArrayList<Token>();
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		//Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence: sentences){
			//System.out.println("Number of core labels in this sentence: " 
			//		+ sentence.get(TokensAnnotation.class).size());
			Tree tree = sentence.get(TreeAnnotation.class);
			SemanticGraph dependencies = 
				sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String word = token.getString(TextAnnotation.class);
				//Integer char_offset = token.get(BeginIndexAnnotation.class);
				String before = token.get(BeforeAnnotation.class);
				String original_word = token.get(OriginalTextAnnotation.class);
				//Integer char_offset = token.get(CharacterOffsetBeginAnnotation.class);
				String pos = token.getString(PartOfSpeechAnnotation.class);
				String ner = token.getString(NamedEntityTagAnnotation.class);
				String lemma = token.getString(LemmaAnnotation.class);
				int token_index = token.index();
				DeftToken dt = new DeftToken(word,"word");
				/*if (word.equals("#")) {
					continue;
				}*/
				dt.SetPOS(pos);
				dt.SetNER(ner);
				dt.SetText(word);
				dt.SetOriginal(original_word);
				dt.morph_features.put("lex", lemma);
				dt.SetParseTree(tree);
			    dt.SetDependencies(dependencies);
			    dt.SetSentenceIndex(token_index);
			    //token.index() to get token index in tree of this sentence?
			    // (will we need to return the sentence?)
			    // dependencies.get
				//dt.SetCoreferenceChain(graph);
				stanford_tokens.add(dt);
			}
		}
		return stanford_tokens;
	}
	
	/*public List<Comment> AnnotateComments(List<Comment> comments) {
		Properties props = new Properties();
		//props.setProperty("annotators", "tokenize, ssplit, ner, parse, dcoref");
		props.put("annotators", "tokenize,ssplit,pos, lemma, ner, parse, dcoref");
		props.put("tokenize.whitespace", true); // only tokenize on whitespace
		props.put("tokenize.options", "invertible=true");
		props.put("ssplit.isOneSentence", true); // don't split comment into sentences
		
		//props.put("ssplit.eolonly", true); // split only on end of line
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation document;
		for (Comment c: comments) {
			System.out.println("CoreNLP:doc_id:"+ c.comment_id_);
			document = new Annotation(c.raw_text_);
			// or, c.original_text and that way it returns the character offsets
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			System.out.println("Number of tokens:" + c.tokens_.size());
			
			int i=0;
			for (CoreMap sentence: sentences){
				//System.out.println("Number of core labels in this sentence: " 
					//	+ sentence.get(TokensAnnotation.class).size());
				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					String word = token.getString(TextAnnotation.class);
					Integer char_offset = token.get(BeginIndexAnnotation.class);
					String before = token.get(BeforeAnnotation.class);
					String original_word = token.get(OriginalTextAnnotation.class);
					//Integer char_offset = token.get(CharacterOffsetBeginAnnotation.class);
					String pos = token.getString(PartOfSpeechAnnotation.class);
					String ner = token.getString(NamedEntityTagAnnotation.class);
					String lemma = token.getString(LemmaAnnotation.class);
					if (word.equals("#")) {
						continue;
					}
					/*System.out.println("Token:"+word);
					System.out.println("Original word:"+original_word);
					System.out.println("Before annotation:"+before);
					System.out.println("Lemma:"+lemma);
					System.out.println("POS:"+pos);
					System.out.println("NER:"+ner);
					System.out.println("Number of core labels:" + i);*/
					//DeftToken t = (DeftToken) c.tokens_.get(i);
					//System.out.println("Corresponding token:" + t.text_);
					/*t.SetPOS(pos);
					t.SetNER(ner);
					t.SetText(word);
					t.SetCharOffset(char_offset);
					t.morph_features.put("lex", lemma);
					i+=1;*/
				//}
				/*Tree tree = sentence.get(TreeAnnotation.class);
				SemanticGraph dependencies = 
						sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				c.SetParseTree(tree);
				c.SetDependencies(dependencies);
			}
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			c.SetCoreferenceChain(graph);
		}
		return comments;
		
	}*/
	
	/*public void AnnotateAndPrintComments (List<Comment> comments, String prop) {
		Properties props = new Properties();
		props.setProperty("annotators", this.prop);
		props.put("tokenize.whitespace", true); // only tokenize on whitespace
		props.put("ssplit.isOneSentence", true); // don't split comment into sentences
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Annotation document;
		String file_path = FileWriter.Combine(output_dir, "stanford-out"); // can have duplicate ids
		StringBuilder builder = new StringBuilder();
		String out = "";
		for (Comment c: comments) {
			builder.append("DOC_ID\n");
			document = new Annotation(c.raw_text_);
			// or, c.original_text and that way it returns the character offsets
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			int i=0;
			for (CoreMap sentence: sentences){
				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					String word = token.getString(TextAnnotation.class);
					String pos = token.getString(PartOfSpeechAnnotation.class);
					String ner = token.getString(NamedEntityTagAnnotation.class);
					String lemma = token.getString(LemmaAnnotation.class);
					builder.append("TOKEN:\t" + "word:"+ word +"\t" + "lemma:" + lemma +"\t"
							+ "pos:" + pos + "\t" + "ner:" + ner + "\n") ;
					Token t = c.tokens_.get(i);
					/*t.SetPOS(pos);
					t.SetNER(ner);
					t.SetText(word);
					t.morph_features.put("lex", lemma);*/
					/*i+=1;
				}
				Tree tree = sentence.get(TreeAnnotation.class);
				builder.append("TREE:"+ tree.toString());
				SemanticGraph dependencies = 
						sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				builder.append("DEPENDENCIES:" + dependencies.toFormattedString());
				/*c.SetParseTree(tree);
				c.SetDependencies(dependencies);*/
			/*}
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			builder.append("COREFCHAIN:" + graph.toString());
			//c.SetCoreferenceChain(graph);
			builder.append("\n");
		}
		out = builder.toString();
		FileWriter.WriteFile(file_path, out);
	}*/
	
	public List<Comment> ReadAnnotatedComments(String path){
		return null;
		
	}
	
	// public static void main (String[] args)

}
