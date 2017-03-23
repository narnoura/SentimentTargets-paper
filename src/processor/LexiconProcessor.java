/**
 * 
 */
package processor;

import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

import util.FileReader;
import util.Tokenizer;

/**
 * @author Narnoura
 * Reads sentiment lexicons and checks for sentiment of words
 */

public class LexiconProcessor {
	
	public HashMap<String,Double[]> Arsenl;
	public HashMap<String,String[]> Slsa;
	public HashMap<String,Integer> Sifaat;
	public HashMap<String,String[]> MPQA;
	public HashMap<String,String> lexicon_files;

	public LexiconProcessor() {
		lexicon_files = new HashMap<String, String>();
		//lexicon_files.put("Slsa", "../slsa2.txt");
		lexicon_files.put("Arsenl", "../lexicons/ArSenL/ArSenL_v1.0A.txt");
		lexicon_files.put("Sifaat", "../lexicons/ARABSENTI-LEXICON");
		lexicon_files.put("MPQA", "../lexicons/subjectivity_clues_hltemnlp05/subjclueslen1-HLTEMNLP05.tff");
		//this.ReadSlsa(lexicon_files.get("Slsa"));
		this.ReadArsenl(lexicon_files.get("Arsenl"));
		this.ReadSifaat(lexicon_files.get("Sifaat"));
		this.ReadMPQA(lexicon_files.get("MPQA"));
	}
	// Reads in Slsa lexicon and updates Slsa variable
	// Slsa is indexed by lemma:POS
	// Looks like this:
	// [lemma:POS] [pos,neg,neut,gloss]
	public void ReadSlsa(String file) {
		boolean skipempty = true;
		HashMap<String,String[]> lexicon = new HashMap<String,String[]>();
		List<String> lines = FileReader.ReadFile(file, "", skipempty);
		try {
			for (String line : lines) {
				if (line.startsWith("#")) {
					continue;
				}
				line = line.trim();
				String[] fields = line.split("\t");
				String lemma_pos = fields[0] + ":" + fields[1];
				String[] entries = new String[4];
				for (int i=2; i<fields.length; i++){
					entries[i-2] = fields[i];
				}
				lexicon.put(lemma_pos, entries);
			}
			}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.Slsa = lexicon;
	}
	// Prints Slsa lexicon for debugging
	public void PrintSlsa () {
		if (this.Slsa == null || this.Slsa.isEmpty()) {
			System.out.println("Empty lexicon!");
			System.exit(0);
		}
		else{
			System.out.println("LexiconProcessor::Printing Slsa\n");
			System.out.println("Lemma:POS Positive Negative Neutral Gloss\n");
			for (String key : Slsa.keySet()) {
				System.out.println(key + ":");
				String[] entries = Slsa.get(key);
				for (String e: entries) {
					System.out.println(e + " ");
				}
				System.out.println("\n");
			}
		}
	}
	public boolean HasSlsaKey (String lemma, String POS) {
		return(this.Slsa.containsKey(lemma + ":" + POS));
	}

	// POS should be : VERB, NOUN, ADJ, or ADV
	public Double SlsaPositiveScore (String lemma, String POS) {
		double pos = 0;
		String positive = "";
		try {
			positive = this.Slsa.get(lemma + ":" + POS)[0];
			if (positive == null) {
				return -1.0;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		pos = Double.parseDouble(positive);		
		return pos;
	}
	
	// Returns -1 if key is not available
	public Double SlsaNegativeScore (String lemma, String POS) {
		double neg = 0;
		String negative = "";
		try {
			negative = this.Slsa.get(lemma + ":" + POS)[1];
			if (negative == null) {
				return -1.0;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		neg = Double.parseDouble(negative);		
		return neg;
	}

	public Double SlsaAverageScore(String lemma, String POS) {
		if (!HasSlsaKey(lemma, POS)) {
			return -1.0;
		}
		double avg = 0;
		try {
			String key = lemma + ":" + POS;
			double pos = Double.parseDouble(this.Slsa.get(key)[0]);
			double neg = Double.parseDouble(this.Slsa.get(key)[1]);
			double neut = Double.parseDouble(this.Slsa.get(key)[2]);
			avg = (double) (pos + neg + neut) / 3;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return avg;
	}
	
	// Arsenl is indexed by lemma:POS
	// (POS currently is: n,v,a,r)
	// Automatically average the scores across
	// different lemma-POS pairs with different offsets
	// TODO: weighted average, or max, or sort file by pos
	// TODO: will probably need to debug.
	public void ReadArsenl (String file) {
		System.out.println("Reading Arsenl\n");
		boolean skipempty = true;
		HashMap<String,Double[]> lexicon = new HashMap<String,Double[]>();
		List<String> lines = FileReader.ReadFile(file, "", skipempty);
		try{
			Iterator<String> itr = lines.iterator();
			while (itr.hasNext()) {
				String line = (String) itr.next();
				if (line.startsWith("//")) {
					continue;
				}
				line = line.trim();
				String[] fields = line.split(";");
				String lemma = fields[0];
				String pos = fields[1];
				
				String temp = lemma;
				String temp_pos = pos;
				double avg_pos = 0, avg_neg = 0, avg_neut = 0;
				double count = 0;
				
				// *For adj experiment* : if pos not = adj, continue
				
				//while (itr.hasNext() &&  ((String) itr.next()).startsWith(lemma + ";")) {
				//while (temp.equals(lemma) &&
					//	temp_pos.equals(pos) && itr.hasNext()) {
				while (temp.equals(lemma) &&
							itr.hasNext()) {
					
					String next_line = (String) itr.next();
					next_line = next_line.trim();
					next_line = Tokenizer.RemoveExtraWhiteSpace(next_line);
					String[] next_fields = next_line.split(";");
					temp = next_fields[0];
					count +=1;
					// NOTE! must compare pos. they ignored it in the paper. so did Ramy I think.
					// try to re-print the lexicon and sort by pos.
					// Otherwise just average for all lemmas.
					// TODO Also: Try weighted average by confidence
					
					
					// *For adj experiment* : pos = fields[1]
					// if pos not = adj, continue
					
					avg_pos += Double.parseDouble(fields[2]);
					avg_neg += Double.parseDouble(fields[3]);
					avg_neut += (1 - Double.parseDouble(fields[2])
							- Double.parseDouble(fields[3]));
					fields = next_fields;
				}
				avg_pos = avg_pos / count;
				avg_neg = avg_neg / count;
				avg_neut = avg_neut / count;
				//String key = lemma + ":" + pos;
				String key = lemma;
				Double values[] = new Double[] {avg_pos,avg_neg,avg_neut};
				lexicon.put(key, values);
				// TODO key should only be lemma then. right now it's putting the 
				// first encountered pos in the key.
				//System.out.println("ArSenL: key:" + key + " " + 
					//avg_pos + " " + avg_neg + " " + avg_neut);
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.Arsenl = lexicon;
	}
	
	public boolean HasArsenlKey (String lemma) {
		//return(this.Arsenl.get(lemma + ":" + pos) != null);
		return(this.Arsenl.containsKey(lemma));
	}
	
	public Double ArsenlPositiveScore(String lemma) {
		if (!HasArsenlKey(lemma)) {
			return -1.0;
		}
		Double positive = 0.0;
		try {
			// positive = this.Arsenl.get(lemma + ":" + pos)[0];
			positive = this.Arsenl.get(lemma)[0];
		}
			catch (Exception e) {
			e.printStackTrace();
		}
			return positive;
	}
	
	public Double ArsenlNegativeScore(String lemma) {
		/*if (!HasArsenlKey(lemma, pos)) {
			return -1.0;
		}*/
		if (!HasArsenlKey(lemma)) {
		return -1.0;
		}
		Double negative = 0.0;
		try {
			// negative = this.Arsenl.get(lemma + ":" + pos)[1];
			negative = this.Arsenl.get(lemma)[1];
		}
			catch (Exception e) {
			e.printStackTrace();
		}
			return negative;
	}
	
	public Double ArsenlAverageScore (String lemma) {

		if (!HasArsenlKey(lemma)) {
			return -1.0;
		}
		Double average = 0.0;
		//String key = lemma + ":" + pos;
		String key = lemma;
		try {
			Double[] scores = this.Arsenl.get(key);
			average = (scores[0] + scores[1] + scores[2]) / 3;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return average;
	}
	
	public Double[] ArsenlScores (String lemma) {
		/*if (!HasArsenlKey(lemma, pos)) {
			return null;
		}
		else {
			return this.Arsenl.get(lemma + ":" + pos);
		}*/
		if (!HasArsenlKey(lemma)) {
		return null;
		}
		else {
			return this.Arsenl.get(lemma);
		}
	}
	
	public void PrintArsenl () { 
		if (this.Arsenl == null || this.Arsenl.isEmpty()) {
			System.out.println("Empty lexicon!");
			System.exit(0);
		}
		else{
			System.out.println("LexiconProcessor::Printing Arsenl\n");
			System.out.println("Lemma:POS Positive Negative Neutral\n");
			for (String key : Arsenl.keySet()) {
				System.out.println(key + ":");
				Double[] entries = Arsenl.get(key);
				for (Double e: entries) {
					System.out.println(e + " ");
				}
				System.out.println("\n");
			}
		}
		
	}
	
	
	// Abdul-Mageed & Diab manual lexicon
	// lexicon is indexed by lemma "buckwalter with diacritics" (WITHOUT _1)
	// value is 0 (neutral), 1 (positive), or 2 (negative)
	public void ReadSifaat(String file) {
		System.out.println("Reading Sifaat");
		HashMap<String,Integer> lexicon = new HashMap<String,Integer>();
		List<String> lines = FileReader.ReadUTF8File(file);
		try {
			for (String line: lines) {
				line = line.trim();
				String[] fields = line.split("\t");	
				// for undiactritized, try 4
				String lemma = fields[3];
				String polarity = fields[2];
				polarity = polarity.trim();
				lemma = lemma.trim();
				if (polarity.equals("") || lemma.equals("")) {
					continue;
				}
				//System.out.println("Sifaat lemma:" + lemma);
				//System.out.println("Sifaat sentiment:" + polarity);
				lexicon.put(lemma, Integer.parseInt(polarity));
				}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
		this.Sifaat = lexicon;
			
	}

	public boolean HasSifaatKey (String lemma) {
			/*String stripped = lemma;
			System.out.println("lemma:" + lemma);
			stripped = stripped.replaceAll("(\\_)(\\d)+\\z", "");
			System.out.println("stripped:" + stripped);*/
			
			return (this.Sifaat.containsKey(lemma));
	}
	
	public Integer GetSifaatSentiment (String lemma) {
		Integer sentiment = 0;
		/*String stripped = lemma;
		stripped = stripped.replaceAll("(\\_)(\\d)+\\z", "");*/
		if (!this.Sifaat.containsKey(lemma)) {
			return null;
		}
		else {
			sentiment = this.Sifaat.get(lemma);
		}
		
		return sentiment;
	}
	
	// MPQA Lexicon
	public void ReadMPQA(String file) {
		    System.out.println("Reading MPQA\n");
			boolean skipempty = true;
			HashMap<String,String[]> lexicon = new HashMap<String,String[]>();
			List<String> lines = FileReader.ReadFile(file, "", skipempty);
			try {
				for (String line : lines) {
					line = line.trim();
					String[] fields = line.split(" ");
					String word = fields[2];
					String subj = fields[0]; // weaksubj or strongsubj
					String pos = fields[3]; // adj, noun, verb, adverb, or anypos
					String stemmed = fields[4]; // y or n
					String pol = fields[5];
					String[] entries = new String[4];
					entries[0] = subj;
					entries[1] = pos;
					entries[2] = stemmed;
					entries[3] = pol;
					// System.out.println("\nWord:" + word);
					for (int i=0;i<entries.length;i++) {
						//System.out.println("Entry:" + entries[i]);
						String field = entries[i].split("=")[1];
						entries[i] = field;
					}
					String key = word.split("=")[1];
					lexicon.put(key, entries);
				}
				}
			catch (Exception e) {
				e.printStackTrace();
			}
			this.MPQA = lexicon;
		
	}
	
	public boolean HasMPQAKey (String word) {
		word = word.trim();
		return (this.MPQA.containsKey(word));
}
	
	// Returns strongsubj, weaksubj, or na
	public String GetMPQASubjectivity (String word) {
		String subj = "n";
		if (!HasMPQAKey(word)) {
			subj = "na";
		} else{
			subj = this.MPQA.get(word)[0];
		}
		return subj;
	}
	
	// Returns positive, negative, neutral, or na
	public String GetMPQAPriorPolarity (String word) {
		String pol = "na";
		if (!HasMPQAKey(word)) {
			pol = "na";
		} else{
			pol = this.MPQA.get(word)[3];
		}
		return pol;
	}
	
	
}



