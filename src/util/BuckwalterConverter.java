/**
 * 
 */
package util;

/**
 * @author Narnoura
 * Methods for converting between Arabic utf8, Buckwalter, and safe buckwalter.
 * From Ramy Eskander's Transliterator.java and StringProcessor.java code
 *
 */
public class BuckwalterConverter {

	public static String ConvertToBuckwalter(String word) {
		word = word.replaceAll("\u0621", "'"); //\u0621 : ARABIC LETTER HAMZA
		word = word.replaceAll("\u0622", "|"); //\u0622 : ARABIC LETTER ALEF WITH MADDA ABOVE
		word = word.replaceAll("\u0623", ">"); //\u0623 : ARABIC LETTER ALEF WITH HAMZA ABOVE
		word = word.replaceAll("\u0624", "&"); //\u0624 : ARABIC LETTER WAW WITH HAMZA ABOVE
		word = word.replaceAll("\u0625", "<"); //\u0625 : ARABIC LETTER ALEF WITH HAMZA BELOW
		word = word.replaceAll("\u0626", "}"); //\u0626 : ARABIC LETTER YEH WITH HAMZA ABOVE
		word = word.replaceAll("\u0627", "A"); //\u0627 : ARABIC LETTER ALEF
		word = word.replaceAll("\u0628", "b"); //\u0628 : ARABIC LETTER BEH
		word = word.replaceAll("\u0629", "p"); //\u0629 : ARABIC LETTER TEH MARBUTA
		word = word.replaceAll("\u062A", "t"); //\u062A : ARABIC LETTER TEH
		word = word.replaceAll("\u062B", "v"); //\u062B : ARABIC LETTER THEH
		word = word.replaceAll("\u062C", "j"); //\u062C : ARABIC LETTER JEEM
		word = word.replaceAll("\u062D", "H"); //\u062D : ARABIC LETTER HAH
		word = word.replaceAll("\u062E", "x"); //\u062E : ARABIC LETTER KHAH
		word = word.replaceAll("\u062F", "d"); //\u062F : ARABIC LETTER DAL
		word = word.replaceAll("\u0630", "*"); //\u0630 : ARABIC LETTER THAL
		word = word.replaceAll("\u0631", "r"); //\u0631 : ARABIC LETTER REH
		word = word.replaceAll("\u0632", "z"); //\u0632 : ARABIC LETTER ZAIN
		word = word.replaceAll("\u0633", "s"); //\u0633 : ARABIC LETTER SEEN
		word = word.replaceAll("\u0634", "\\$"); //\u0634 : ARABIC LETTER SHEEN
		word = word.replaceAll("\u0635", "S"); //\u0635 : ARABIC LETTER SAD
		word = word.replaceAll("\u0636", "D"); //\u0636 : ARABIC LETTER DAD
		word = word.replaceAll("\u0637", "T"); //\u0637 : ARABIC LETTER TAH
		word = word.replaceAll("\u0638", "Z"); //\u0638 : ARABIC LETTER ZAH
		word = word.replaceAll("\u0639", "E"); //\u0639 : ARABIC LETTER AIN
		word = word.replaceAll("\u063A", "g"); //\u063A : ARABIC LETTER GHAIN		
		word = word.replaceAll("\u0640", "_"); //\u0640 : ARABIC TATWEEL
		word = word.replaceAll("\u0641", "f"); //\u0641 : ARABIC LETTER FEH
		word = word.replaceAll("\u0642", "q"); //\u0642 : ARABIC LETTER QAF
		word = word.replaceAll("\u0643", "k"); //\u0643 : ARABIC LETTER KAF
		word = word.replaceAll("\u06A9", "k"); //\u0643 : ARABIC LETTER KAF
		word = word.replaceAll("\u0644", "l"); //\u0644 : ARABIC LETTER LAM
		word = word.replaceAll("\u0645", "m"); //\u0645 : ARABIC LETTER MEEM
		word = word.replaceAll("\u0646", "n"); //\u0646 : ARABIC LETTER NOON
		word = word.replaceAll("\u0647", "h"); //\u0647 : ARABIC LETTER HEH
		word = word.replaceAll("\u0648", "w"); //\u0648 : ARABIC LETTER WAW
		word = word.replaceAll("\u0649", "Y"); //\u0649 : ARABIC LETTER ALEF MAKSURA
		word = word.replaceAll("\u06CC", "Y"); //\u06CC : ARABIC LETTER FARSI YEH
		word = word.replaceAll("\u064A", "y"); //\u064A : ARABIC LETTER YEH
		word = word.replaceAll("\u06D1", "y"); //\u06D1 : ARABIC LETTER YEH WITH THREE DOTS BELOW
		word = word.replaceAll("\u064B", "F"); //\u064B : ARABIC FATHATAN
		word = word.replaceAll("\u064C", "N"); //\u064C : ARABIC DAMMATAN
		word = word.replaceAll("\u064D", "K"); //\u064D : ARABIC KASRATAN
		word = word.replaceAll("\u064E", "a"); //\u064E : ARABIC FATHA
		word = word.replaceAll("\u064F", "u"); //\u064F : ARABIC DAMMA
		word = word.replaceAll("\u0650", "i"); //\u0650 : ARABIC KASRA
		word = word.replaceAll("\u0651", "~"); //\u0651 : ARABIC SHADDA
		word = word.replaceAll("\u0652", "o"); //\u0652 : ARABIC SUKUN		
		word = word.replaceAll("\u0670", "`"); //\u0670 : ARABIC LETTER SUPERSCRIPT ALEF
		word = word.replaceAll("\u0671", "{"); //\u0671 : ARABIC LETTER ALEF WASLA
		word = word.replaceAll("\u067E", "P"); //\u067E : ARABIC LETTER PEH
		word = word.replaceAll("\u0686", "J"); //\u0686 : ARABIC LETTER TCHEH
		word = word.replaceAll("\u06A4", "V"); //\u06A4 : ARABIC LETTER VEH
		word = word.replaceAll("\u06AF", "G"); //\u06AF : ARABIC LETTER GAF
		word = word.replaceAll("\u0698", "R"); //\u0698 : ARABIC LETTER JEH (no more in Buckwalter system)
		//Not in Buckwalter system \u0679 : ARABIC LETTER TTEH
		//Not in Buckwalter system \u0688 : ARABIC LETTER DDAL
		//Not in Buckwalter system \u06A9 : ARABIC LETTER KEHEH
		//Not in Buckwalter system \u0691 : ARABIC LETTER RREH
		//Not in Buckwalter system \u06BA : ARABIC LETTER NOON GHUNNA
		//Not in Buckwalter system \u06BE : ARABIC LETTER HEH DOACHASHMEE
		//Not in Buckwalter system \u06C1 : ARABIC LETTER HEH GOAL
		//Not in Buckwalter system \u06D2 : ARABIC LETTER YEH BARREE
		word = word.replaceAll("\u060C", ","); //\u060C : ARABIC COMMA
		word = word.replaceAll("\u061B", ";"); //\u061B : ARABIC SEMICOLON
		word = word.replaceAll("\u061F", "?"); //\u061F : ARABIC QUESTION MARK
		//Not significant for morphological analysis
		word = word.replaceAll("\u0640", ""); //\u0640 : ARABIC TATWEEL
		//Not suitable for morphological analysis : remove all vowels/diacritics, i.e. undo the job !
		word = word.replaceAll("[FNKaui~o]", "");
		//TODO : how to handle ARABIC LETTER SUPERSCRIPT ALEF and ARABIC LETTER ALEF WASLA ?		
		//word = word.replaceAll("[`\\{]", ""); //strip them for now
		return word;
	}
	
	public static String ConvertToUTF8 (String word) {	
		word = word.replaceAll("'", "\u0621"); //\u0621 : ARABIC LETTER HAMZA
		word = word.replaceAll("\\|", "\u0622"); //\u0622 : ARABIC LETTER ALEF WITH MADDA ABOVE
		word = word.replaceAll(">", "\u0623"); //\u0623 : ARABIC LETTER ALEF WITH HAMZA ABOVE
		word = word.replaceAll("&", "\u0624"); //\u0624 : ARABIC LETTER WAW WITH HAMZA ABOVE
		word = word.replaceAll("<", "\u0625"); //\u0625 : ARABIC LETTER ALEF WITH HAMZA BELOW
		word = word.replaceAll("}", "\u0626"); //\u0626 : ARABIC LETTER YEH WITH HAMZA ABOVE
		word = word.replaceAll("A", "\u0627"); //\u0627 : ARABIC LETTER ALEF
		word = word.replaceAll("b", "\u0628"); //\u0628 : ARABIC LETTER BEH
		word = word.replaceAll("p", "\u0629"); //\u0629 : ARABIC LETTER TEH MARBUTA
		word = word.replaceAll("t", "\u062A"); //\u062A : ARABIC LETTER TEH
		word = word.replaceAll("v", "\u062B"); //\u062B : ARABIC LETTER THEH
		word = word.replaceAll("j", "\u062C"); //\u062C : ARABIC LETTER JEEM
		word = word.replaceAll("H", "\u062D"); //\u062D : ARABIC LETTER HAH
		word = word.replaceAll("x", "\u062E"); //\u062E : ARABIC LETTER KHAH
		word = word.replaceAll("d", "\u062F"); //\u062F : ARABIC LETTER DAL
		word = word.replaceAll("\\*", "\u0630"); //\u0630 : ARABIC LETTER THAL
		word = word.replaceAll("r", "\u0631"); //\u0631 : ARABIC LETTER REH
		word = word.replaceAll("z", "\u0632"); //\u0632 : ARABIC LETTER ZAIN
		word = word.replaceAll("s", "\u0633" ); //\u0633 : ARABIC LETTER SEEN
		word = word.replaceAll("\\$", "\u0634"); //\u0634 : ARABIC LETTER SHEEN
		word = word.replaceAll("S", "\u0635"); //\u0635 : ARABIC LETTER SAD
		word = word.replaceAll("D", "\u0636"); //\u0636 : ARABIC LETTER DAD
		word = word.replaceAll("T", "\u0637"); //\u0637 : ARABIC LETTER TAH
		word = word.replaceAll("Z", "\u0638"); //\u0638 : ARABIC LETTER ZAH
		word = word.replaceAll("E", "\u0639"); //\u0639 : ARABIC LETTER AIN
		word = word.replaceAll("g", "\u063A"); //\u063A : ARABIC LETTER GHAIN
		word = word.replaceAll("_", "\u0640"); //\u0640 : ARABIC TATWEEL
		word = word.replaceAll("f", "\u0641"); //\u0641 : ARABIC LETTER FEH
		word = word.replaceAll("q", "\u0642"); //\u0642 : ARABIC LETTER QAF
		word = word.replaceAll("k", "\u0643"); //\u0643 : ARABIC LETTER KAF
		word = word.replaceAll("l", "\u0644"); //\u0644 : ARABIC LETTER LAM
		word = word.replaceAll("m", "\u0645"); //\u0645 : ARABIC LETTER MEEM
		word = word.replaceAll("n", "\u0646"); //\u0646 : ARABIC LETTER NOON
		word = word.replaceAll("h", "\u0647"); //\u0647 : ARABIC LETTER HEH
		word = word.replaceAll("w", "\u0648"); //\u0648 : ARABIC LETTER WAW
		word = word.replaceAll("Y", "\u0649"); //\u0649 : ARABIC LETTER ALEF MAKSURA
		word = word.replaceAll("y", "\u064A"); //\u064A : ARABIC LETTER YEH
		word = word.replaceAll("F", "\u064B"); //\u064B : ARABIC FATHATAN
		word = word.replaceAll("N", "\u064C"); //\u064C : ARABIC DAMMATAN
		word = word.replaceAll("K", "\u064D"); //\u064D : ARABIC KASRATAN
		word = word.replaceAll("a", "\u064E"); //\u064E : ARABIC FATHA
		word = word.replaceAll("u", "\u064F"); //\u064F : ARABIC DAMMA
		word = word.replaceAll("i", "\u0650"); //\u0650 : ARABIC KASRA
		word = word.replaceAll("~", "\u0651"); //\u0651 : ARABIC SHADDA
		word = word.replaceAll("o", "\u0652"); //\u0652 : ARABIC SUKUN
		word = word.replaceAll("`", "\u0670"); //\u0670 : ARABIC LETTER SUPERSCRIPT ALEF
		word = word.replaceAll("\\{", "\u0671"); //\u0671 : ARABIC LETTER ALEF WASLA
		word = word.replaceAll("P", "\u067E"); //\u067E : ARABIC LETTER PEH
		word = word.replaceAll("J", "\u0686"); //\u0686 : ARABIC LETTER TCHEH
		word = word.replaceAll("V", "\u06A4"); //\u06A4 : ARABIC LETTER VEH
		word = word.replaceAll("G", "\u06AF"); //\u06AF : ARABIC LETTER GAF
		word = word.replaceAll("R", "\u0698"); //\u0698 : ARABIC LETTER JEH (no more in Buckwalter system)
		//Not in Buckwalter system \u0679 : ARABIC LETTER TTEH
		//Not in Buckwalter system \u0688 : ARABIC LETTER DDAL
		//Not in Buckwalter system \u06A9 : ARABIC LETTER KEHEH
		//Not in Buckwalter system \u0691 : ARABIC LETTER RREH
		//Not in Buckwalter system \u06BA : ARABIC LETTER NOON GHUNNA
		//Not in Buckwalter system \u06BE : ARABIC LETTER HEH DOACHASHMEE
		//Not in Buckwalter system \u06C1 : ARABIC LETTER HEH GOAL
		//Not in Buckwalter system \u06D2 : ARABIC LETTER YEH BARREE
		word = word.replaceAll(",", "\u060C" ); //\u060C : ARABIC COMMA
		word = word.replaceAll(";", "\u061B"); //\u061B : ARABIC SEMICOLON
		word = word.replaceAll("\\?", "\u061F"); //\u061F : ARABIC QUESTION MARK
		return word;
	}
	
	public static String ConvertBuckwalterToSafe(String word){
		return word.replace("'", "C").
						replace("|", "M").
						replace("}", "Q").
						replace("*", "V").
						replace("$", "c").
						replace("{", "L").
						replace("`", "e").
						replace("~", "X").
						replace(">", "O").
						replace("&", "W").
						replace("<", "I");
	}
	
	public static String ConvertSafeToBuckwalter(String word){
		return word.replace("C", "'").
						replace("M", "|").
						replace("Q", "}").
						replace("V", "*").
						replace("c", "$").
						replace("L", "{").
						replace("e", "`").
						replace("X", "~").
						replace("O", ">").
						replace("W", "&").
						replace("I", "<");
	}

	public static String IgnoreUnsafeBuckwalterforMada(String word) {
		word = word.replaceAll("\\'", "");
		word = word.replaceAll("\\}", "");
		word = word.replaceAll("\\{", "");
		return word;
	}
	
}
