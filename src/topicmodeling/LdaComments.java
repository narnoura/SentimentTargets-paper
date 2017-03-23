/**
 * 
 */
package topicmodeling;


import data.Comment;
import data.Token;

import util.FileWriter;
import util.Tokenizer;

import com.aliasi.cluster.LatentDirichletAllocation;
//import com.aliasi.cluster.LdaReportingHandler; 
import com.aliasi.tokenizer.*;
import com.aliasi.symbol.*;
import com.aliasi.util.ObjectToCounterMap;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.HashMap;

/**
 * @author noura Runs LDA on a corpus of comments Uses the LingPipe API
 */
public class LdaComments {

	public int maxWordsPerTopic;
	public int maxTopicsPerDoc;
	public SymbolTable symbolTable;
	public LatentDirichletAllocation.GibbsSample sample;
	public HashMap<Integer, HashMap<String, Double>> topics;
	public HashMap<Integer, HashMap<Integer, Double>> documents;
	public static HashSet<String> stopwords = ArabicStopWords.ReadStopWords();
	
	public LdaComments() {
		symbolTable = new MapSymbolTable();
		//maxWordsPerTopic = 200;
		//maxTopicsPerDoc = 10;
		//maxWordsPerTopic = 50;
		maxWordsPerTopic = 5;
		maxTopicsPerDoc = 3;
		//maxTopicsPerDoc = 5;
		topics = new HashMap<Integer, HashMap<String, Double>>();
		documents = new HashMap<Integer, HashMap<Integer, Double>>();
	}

	public void SetSample(LatentDirichletAllocation.GibbsSample sample) {
		this.sample = sample;
	}
	
	public void SetTopics (HashMap<Integer, HashMap<String, Double>> topics) {
		this.topics = topics;
	}
	
	public void SetDocuments (HashMap<Integer, HashMap<Integer, Double>> documents) {
		this.documents = documents;
	}

	// Get map of topics to words in corpus
	public HashMap<Integer, HashMap<String, Double>> CorpusTopics(LatentDirichletAllocation.GibbsSample sample) {
		if (symbolTable.numSymbols() == 0) {
			System.out.println("Warning: Empty symbol table. Pls run LDA first \n");
			return null;
		}
		System.out.println("Getting topics");
		HashMap<Integer, HashMap<String, Double>> corpusTopics = new HashMap<Integer, HashMap<String, Double>>();
		int numTopics = sample.numTopics();
		int numWords = sample.numWords();
		System.out.println("Number of topics: " + numTopics);
		System.out.println("Number of words: " + numWords);

		for (int topic = 0; topic < numTopics; ++topic) {
			// int topicCount = sample.topicCount(topic);
			ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
			for (int word = 0; word < numWords; ++word) {
				counter.set(new Integer(word), sample.topicWordCount(topic, word));
			}
			
			List<Integer> topWords = counter.keysOrderedByCountList();
			HashMap<String, Double> wordProbs = new HashMap<String, Double>();
			for (int rank = 0; rank < maxWordsPerTopic && rank < topWords.size(); ++rank) {
				int wordId = topWords.get(rank);
				String word = symbolTable.idToSymbol(wordId);
				// int wordCount = sample.wordCount(wordId);
				// int topicWordCount = sample.topicWordCount(topic,wordId);
				double topicWordProb = sample.topicWordProb(topic, wordId);
				// double z = binomialz(topicWordCount, topicCount, wordCount,
				// numTokens);
				wordProbs.put(word, topicWordProb);
			}
			
			corpusTopics.put(topic, wordProbs);
		}
		System.out.println("Finished getting topics");
		return corpusTopics;
	}

	// Get map of documents to topics in corpus
	public HashMap<Integer, HashMap<Integer, Double>> CorpusDocuments(LatentDirichletAllocation.GibbsSample sample) {

		if (symbolTable.numSymbols() == 0) {
			System.out.println("Warning: Empty symbol table. Pls run LDA first \n");
			return null;
		}
		System.out.println("Getting documents");
		HashMap<Integer, HashMap<Integer, Double>> corpusDocuments = new HashMap<Integer, HashMap<Integer, Double>>();
		int numDocs = sample.numDocuments();
		int numTopics = sample.numTopics();
		int numWords = sample.numWords();
		
		System.out.println("Number of documents: " + numDocs);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("Number of words: " + numWords);

		for (int doc = 0; doc < numDocs; ++doc) {
			int docCount = 0;
			for (int topic = 0; topic < numTopics; ++topic) {
				docCount += sample.documentTopicCount(doc, topic);
			}
			ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
			for (int topic = 0; topic < numTopics; ++topic) {
				counter.set(new Integer(topic), sample.documentTopicCount(doc, topic));
			}
			List<Integer> topTopics = counter.keysOrderedByCountList();
			HashMap<Integer, Double> topicProbs = new HashMap<Integer, Double>();
			for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerDoc; ++rank) {
				int topic = topTopics.get(rank);
				int docTopicCount = sample.documentTopicCount(doc, topic);
				double docTopicPrior = sample.documentTopicPrior();
				double docTopicProb = (docTopicCount + docTopicPrior) / (docCount + numTopics * docTopicPrior);
				topicProbs.put(topic, docTopicProb);
			}
			corpusDocuments.put(doc, topicProbs);
		}
		System.out.println("Finished getting documents");

		return corpusDocuments;
	}

	public LatentDirichletAllocation.GibbsSample runLDA(List<Comment> comments) throws Exception {
		if (comments.isEmpty()) {
			System.out.println("Comments are empty. Exiting LDA");
			return null;
		}
		System.out.println("Running LDA");

		int minTokenCount = 5;
		short numTopics = 20;
		double topicPrior = 0.1;
		double wordPrior = 0.01;
		int burninEpochs = 0;
		int sampleLag = 1;
		int numSamples = 1000;
		long randomSeed = 6474835;

		System.out.println("Reading corpus");
		CharSequence[] commentDocs = readCorpus(comments);
		System.out.println("Read corpus");
		System.out.println("Tokenizing documents");
		int[][] docTokens = LatentDirichletAllocation.tokenizeDocuments(commentDocs, LDA_TOKENIZER_FACTORY, symbolTable,
				minTokenCount);
		System.out.println("Tokenized documents");
		System.out.println("Running Gibbs sampling");
		LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation.gibbsSampler(docTokens, numTopics,
				topicPrior, wordPrior, burninEpochs, sampleLag, numSamples, new Random(randomSeed), null);
		System.out.println("Finished Gibbs sampling");

		// int maxWordsPerTopic = 200;
		// int maxTopicsPerDoc = 10;
		// boolean reportTokens = true;
		// handler.fullReport(sample,maxWordsPerTopic,maxTopicsPerDoc,reportTokens);
		return sample;
	}

	// Create static readCorpus function to read in CharSequence from set of
	// comments.
	// Uses lemmas from comment tokens, skipping clitics
	public CharSequence[] readCorpus(List<Comment> corpus_comments) throws IOException {
		List<CharSequence> articleTextList = new ArrayList<CharSequence>();
		// StringBuilder docBuf = new StringBuilder();
		for (Comment c : corpus_comments) {
			String text = "";
			for (Token t : c.tokens_) {
				if (t.morph_features.containsKey("lex") && (t.clitic.equals("word") || t.clitic.equals("none"))) {
					text += t.morph_features.get("lex") + " ";
					// docBuf.append(t.morph_features.get("lex"));
				} else if ((t.clitic.equals("word") || t.clitic.equals("none"))) {
					text += t.text_ + " ";
				}
				// will ignore tokenized clitics
			}
			text = Tokenizer.RemoveExtraWhiteSpace(text);
			articleTextList.add(text);
			// docBuf.append(text);
		}

		CharSequence[] articleTexts = articleTextList.<CharSequence> toArray(new CharSequence[articleTextList.size()]);

		return articleTexts;
	}
	
	/* TODO: function to read Wiki corpus */

	public void WriteModelToFile() {
		String my_file = "train.topicmodel";
	}
	
	public void Print() {
		System.out.println("######## Printing topics ########");
		if (topics.isEmpty() || documents.isEmpty()) {
			return;
		}
		for (Integer topic : topics.keySet()) {
			HashMap<String, Double> words = topics.get(topic);
			System.out.println("Topic " + topic);
			for (String word: words.keySet()) {
				System.out.println(word + "\t" + words.get(word));
			}
		}
		System.out.println("\n######## Printing documents ########");
		for (Integer doc : documents.keySet()) {
			HashMap<Integer, Double> topics = documents.get(doc);
			System.out.println("Document " + doc);
			for (Integer topic: topics.keySet()) {
				System.out.println(topic + "\t" + topics.get(topic));
			}
		}
		
	}
	
	public void PrintToFiles() {
		String topic_out = "";
		String doc_out = "";
		String topic_file = "topic-keys.model";
		String doc_file = "doc-topics.model";
		System.out.println("Printing topics to file");
		if (topics.isEmpty() || documents.isEmpty()) {
			return;
		}
		for (Integer topic : topics.keySet()) {
			HashMap<String, Double> words = topics.get(topic);
			topic_out += "Topic " + topic + "\n";
			for (String word: words.keySet()) {
				topic_out += word + "\t" + words.get(word);
				topic_out += "\n";
			}
		}
		FileWriter.WriteFile(topic_file, topic_out);
		System.out.println("Printing documents to file");
		for (Integer doc : documents.keySet()) {
			HashMap<Integer, Double> topics = documents.get(doc);
			doc_out += "Document " + doc + "\n";
			for (Integer topic: topics.keySet()) {
				doc_out += topic + "\t" + topics.get(topic);
				doc_out += "\n";
			}
		}
		FileWriter.WriteFile(doc_file, doc_out);
	}
	
	static final TokenizerFactory LdaTokenizerFactory() {
		TokenizerFactory factory = BW_TOKENIZER_FACTORY;
		factory = new StopTokenizerFactory(factory, stopwords);
		// can add Arabic stopwords
		return factory;
	}

	static final TokenizerFactory BW_TOKENIZER_FACTORY
	// letter or digit or hyphen (\x2D) or buckwalter character
	= new RegExTokenizerFactory("[\\x2D\\_\\'\\`\\|\\>\\&\\<\\}\\*\\$\\{\\~\\+a-zA-Z0-9]+");
	// TODO want digits and punc letters allowed but not as single digits. or see how to adjust the tokenize function
	
	static final TokenizerFactory LDA_TOKENIZER_FACTORY = LdaTokenizerFactory();

	// CommentTokenizerFactory
	/*
	 * static final TokenizerFactory wormbaseTokenizerFactory() {
	 * TokenizerFactory factory = BASE_TOKENIZER_FACTORY; factory = new
	 * NonAlphaStopTokenizerFactory(factory); factory = new
	 * LowerCaseTokenizerFactory(factory); factory = new
	 * EnglishStopTokenizerFactory(factory); factory = new
	 * StopTokenizerFactory(factory,STOPWORD_SET); factory = new
	 * StemTokenizerFactory(factory); return factory; }
	 */

}
