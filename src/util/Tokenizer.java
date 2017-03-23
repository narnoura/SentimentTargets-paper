
package util;

import data.Token;
import main.Constants;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * @author Narnoura
 * Probably should be called 'StringProcessor'.
 Takes care of tokenization and string processing options.
 *
 */

public class Tokenizer {
	
    // Tokenize into string array
	// Splits text by space separator
	public static String[] StringTokenize(String text) {
		 // Todo remove whitespace tokens
		 String [] words = text.split(" ");
		 return words;
		}
	
	// Tokenize into tokens
	// Splits text by space separator, splits punctuations,
	// ignores empty spaces
	public static List<Token> SimpleTokenize(String text, String token_type) {
		
			List<Token> tokens = new ArrayList<Token>();
			text = RemoveExtraWhiteSpace(text);
			String[] words = text.split(" ");
			for (int i = 0; i< words.length; i++) {
				if (!words[i].equals("") && !words[i].equals(" ")) {
				Token t = new Token(words[i], token_type);
				tokens.add(t);
				}
			}
			return tokens;
	}
	public static String RemoveExtraWhiteSpace(String text) {
		return text.replaceAll("(\\s+)", " ");
	}
	
	public static String RemoveNewLines(String text) {
		return text.replaceAll("\\n", "");
	}

	
	// Splits basic punctuations, digits, removes tatweel, romanizes all numericals. Assumes
	// input is in BW.
	public static String ProcessLikeMadamira (String text) {
		text = text.replaceAll("_", "");

		// Separate digits from words
		Pattern regex = Pattern.compile("([^\\d+\\s+\\.])(\\d+)");
		Matcher regexMatcher = regex.matcher(text);
		if (regexMatcher.find()) {
			text = regexMatcher.replaceAll(regexMatcher.group(1) + " " 
					+ regexMatcher.group(2));
		}
		Pattern regex2 = Pattern.compile("(\\d+)([^\\d+\\s+\\.])");
		Matcher regexMatcher2 = regex2.matcher(text);
		if (regexMatcher2.find()) {
			text = regexMatcher2.replaceAll(regexMatcher2.group(1) + " " 
					+ regexMatcher2.group(2));
		}
		Pattern regex3 = Pattern.compile("(\\D+)(\\.)");
		Matcher regexMatcher3 = regex3.matcher(text);
		if (regexMatcher3.find()) {
			text = regexMatcher2.replaceAll(regexMatcher3.group(1) +
					" " + regexMatcher3.group(2));
			text = text.replaceAll("\\.", " \\. ");
		}
		text = text.replaceAll("\\!", " \\! ");
		text = text.replaceAll("\\?", " \\? ");
		text = text.replaceAll("\\,", " \\, ");
		text = text.replaceAll("\\(", " \\( ");
		text = text.replaceAll("\\)", " \\) ");
		text = text.replaceAll("\\;", " \\; ");
		text = text.replaceAll("\\-", " \\- ");
		text = text.replaceAll("\\/", " \\/ ");
		
		text = text.replaceAll("\u0660", "0");
		text = text.replaceAll("\u0661", "1");
		text = text.replaceAll("\u0662", "2");
		text = text.replaceAll("\u0663", "3");
		text = text.replaceAll("\u0664", "4");
		text = text.replaceAll("\u0665", "5");
		text = text.replaceAll("\u0666", "6");
		text = text.replaceAll("\u0667", "7");
		text = text.replaceAll("\u0668", "8");
		text = text.replaceAll("\u0669", "9");
	
		
		text = RemoveExtraWhiteSpace(text);
		text = text.trim();
		return text;
	}
	
	// Splits basic punctuations, digits, removes quote and post tags, lowercases
	public static String ProcessEnglishDeft (String text) {
		
		// Separates quotes and post tags
		text = text.replaceAll("<quote>", " <quote> ");
		text = text.replaceAll("<quote ", " <quote ");
		text = text.replaceAll("</quote>", " </quote> ");
		text = text.replaceAll("<post", " <post ");
		text = text.replaceAll("<post", " <post ");
		text = text.replaceAll("</post>", " </post> ");
		
		// Separate original author for quotes
		text = text.replaceAll("\\\"\\>", "\\\"\\> "); //"orig_author="abcd">The
		text = text.replaceAll("</a>", " </a> ");
		text = RemoveExtraWhiteSpace(text);
		//text = text.toLowerCase();
		text = text.trim();
		return text;
	}
	
	public static String RemovePunctuationFromEndAndBeginning (String text) {
		
		text = text.replaceAll("[\\.\\!\\?\\,\\;\\-]+\\z", "");
		text = text.replaceAll("//A[\\.\\!\\?\\,\\;\\-]+", "");
		return text;
	}
	
	public static String RemoveArabicDiacritics(String string){
		return string.replaceAll("[aiuFKN~o`]", "");
	}
	
	// Normalizes Y/y for ya, </> for Alef, and p/h for ta marbuta
	public static String ArabicNormalize(String string){
		return string.replace("Y", "y").
					replace("<", "A").
					replace(">", "A").
					replace("|", "A").
					replace("p", "h");
	} 
	
	public static String ProcessATBBraces (String string) {
		string = string.replaceAll("-RRB-",")");
		string = string.replaceAll("-LRB", "(");
		// maybe
		string = string.replaceAll("@@", "");
		return string;
		
	}
	
	// Given input POS in Arsenl format (a,n,v,r)  
	//
	// Particles: return STOP
	public static String ResolvePOS (String pos) {
			switch (pos) {
			
			// Arsenl
			case "n": return "NOUN";
			case "v": return "VERB";
			case "r": return "ADV";
			case "a" : return "ADJ";
			
			// Madamira
			
			case "noun": return "NOUN";
			case "noun_num": return "NOUN";
			case "noun_quant": return "NOUN";
			case "noun_prop": return "NOUN";
			
			case "verb": return "VERB";
			case "verb_pseudo": return "VERB";
			case "adv": return "ADV";
			case "adv_interrog": return "ADV";
			case "adv_rel": return "ADV";
			
			case "adj" : return "ADJ";
			case "adj_comp" : return "ADJ";
			case "adj_num" : return "ADJ";
			
			case "pron": return "NOUN";
			case "pron_dem": return "NOUN";
			case "pron_exclam": return "NOUN";
			case "pron_interrog": return "NOUN";
			case "pron_rel": return "NOUN";
			case "abbrev": return "NOUN";
			
			case "part": return "STOP";
			case "part_dem": return "STOP";
			case "part_focus": return "STOP";
			case "part_det": return "STOP";
			case "part_fut": return "STOP";
			case "part_interrog": return "STOP";
			case "part_neg": return "NEG";
			case "part_restrict" : return "STOP";
			case "part_verb": return "STOP";
			case "part_voc": return "STOP";
			case "prep": return "STOP";
			case "punc": return "STOP";
			case "conj" : return "STOP";
			case "conj_sub" : return "STOP";
			case "interj" : return "STOP";
			case "digit" : return "STOP";
			case "latin" : return "NOUN";
				
			default:{ 
				System.out.println("Invalid input part of speech. "
						+ "Pls specify a POS tag in Madamira or Arsenl format "
						+ " \n");
				return null;
			}
		}


}
}

