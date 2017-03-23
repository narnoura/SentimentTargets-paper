package processor.ToolsProcessor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import data.Comment;
import data.Token;

import java.io.IOException;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import util.Tokenizer;

public class StanfordPOSTagger {
	
	public static String model = "../models/english-bidirectional-distsim.tagger";

	public StanfordPOSTagger() {
		// TODO Auto-generated constructor stub
	}
	
	public static List<Comment> TagComments (List<Comment> feature_comments) {
		MaxentTagger tagger =
				new MaxentTagger(model);
		for (Comment c: feature_comments) {
			if (c.tokens_.size()==0){
			System.out.println("Stanford tagger:Removing empty comment");
			continue;
		    }
			c = TagComment(c,tagger);
			
		}
		return feature_comments;
	}
	
	// Can also have a lang_dir and store the tags there (either Stanford or Sadegh's)
	// (faster)
	public static Comment TagComment (Comment c, MaxentTagger tagger) {
		String text = c.raw_text_;
		text = text.replaceAll("_", "<PUNC-US>");
		text = Tokenizer.RemoveExtraWhiteSpace(text);
		String tagged_text = tagger.tagTokenizedString(text);
		String[] tags = tagged_text.split(" ");
	
		//System.out.println("Length of tokens:"+c.tokens_.size());
		//System.out.println("Length of tags:"+tags.length);
		if (c.tokens_.size() != tags.length) {
			System.out.println("Length of tokens:"+c.tokens_.size());
			System.out.println("Length of tags:"+tags.length);
			System.exit(0);
		}
		for (Token t: c.tokens_) {
			String word, tag = "NN";
			int i = c.tokens_.indexOf(t);
			tag = tags[i].split("_")[1];
			//word,tag = tags.get(i).split("_");
			t.SetPOS(tag);
			/*if (t.text_.equals("_")) {
				System.out.println("Found underscore word: " + t.text_ + " tag:" + tag);
			}*/
		}
		
		return c;
	}
}
