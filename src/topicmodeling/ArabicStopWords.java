package topicmodeling;

import java.util.HashSet;
import java.util.List;
import util.FileReader;

public class ArabicStopWords {
	
	public static String 
			stopword_file = "../Arabic-ATB-closed-class.list-Salloum-Habash.txt";
	
	public ArabicStopWords() {
		// TODO Auto-generated constructor stub
	}
	public static HashSet<String> ReadStopWords() {
		HashSet<String> stopwords = new HashSet<String>();
		List<String> lines =  FileReader.ReadUTF8File(stopword_file);
		for (String line: lines) {
			String[] fields = line.split("\t");
			String stopword = fields[2];
			stopwords.add(stopword);
		}
		return stopwords;
	}

}
