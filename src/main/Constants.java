package main;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;


public class Constants {

	// Command line options
	public static String INPUT_FILE = "in=";
	public static String TEST_FILE = "testfile=";
	public static String OUTPUT_DIRECTORY = "outputdir=";
	public static String INPUT_DIRECTORY = "inputdir=";
	public static String LANGUAGE_FILES_DIRECTORY = "langfiles=";
	public static String RUN_OPTION = "runopt=";
	public static String MODEL_OPTION = "modelopt=";
	public static String MODEL_FILE = "modelfile=";
	public static String INPUT_ENCODING = "inputenc=";
	public static String LEXICON_FILE = "lexicon="; 
	public static String USE_PARSE = "-useparse";
	public static String EVAL_MATCH = "eval=";
	public static String TOK_OPTION = "tokopt=";
	public static String CLUSTER_FILE = "clusterfile="; 

	public static String TRAIN_SIZE = "train=";
	public static String DEV_SIZE = "dev=";
	public static String TEST_SIZE = "test=";
	
	public static String BINARY_FEATURES = "binfeat=";
	public static String CONT_FEATURES = "contfeat=";
	
	public static String BOOST_TRAINING = "-boosttrain";
	public static String ALL_SIGNIFICANCE = "-allsig";
	public static String PREDICT_MENTIONS = "-predictmentions";


	// Model types
	// maybe don't really need this
	public static String ALL_NP = "all_np_baseline";
	
	// Processing options
	public static String PUNC_REGEX 
	= "[\\{\\}\\<\\>\\|\\&\\$\\'\\`\\~\\*\\-\\=\\\"\\_\\:\\#\\@\\!\\?\\^\\/\\(\\)\\[\\]\\%\\;\\\\\\+\\.\\,\\�\\�]+";
	//public static String ENGLISH_NUMBER_PUNC_REGEX = "^(["+ENGLISH_REGEX+"|"+NUMBER_REGEX+"|"+PUNC_REGEX+"]+)$";
	/*public static String SPACE_DELIMITER_IN = "spcdlm=";
	 */
	 
	// Default Files
	public static String MADAMIRA_UTF8_CONFIG = "../resources/Madamira-rawconfig.xml";
	public static String MADAMIRA_BW_CONFIG = "../resources/Madamira-rawconfig-bwout.xml";
	
	// Sentiment lists
	
	public static String[] BW_NEGATIONS = {"lm", "lA", "ln", "lys", "lYs", "mA", 
		"Edym", "gyr", "dwn", "bgyr", "blA", };
	public static Set<String> MY_BW_NEGATIONS = new HashSet<String>(Arrays.asList(BW_NEGATIONS));
	public static String[] COMMENT_SPLITTERS = {".", ",", "!", "?" };
	public static Set<String> MY_COMMENT_SPLITTERS = new HashSet<String>(Arrays.asList(COMMENT_SPLITTERS));
	
	/*
	public static String MLE_DB = "/resources/mle.db";
	public static String SLC_A_MODEL = "/resources/slc-A.model";
	public static String SLC_Y_MODEL = "/resources/slc-Y.model";
	public static String SLC_H_MODEL = "/resources/slc-H.model";
	public static String SLC_P_MODEL = "/resources/slc-P.model";
	
	public static String ENGLISH_REGEX = "[a-zA-Z]+";
	public static String NUMBER_REGEX = "\\d+";
	public static String PUNC_REGEX = "[\\{\\}\\<\\>\\|\\&\\$\\'\\`\\~\\*\\-\\=\\\"\\_\\:\\#\\@\\!\\?\\^\\/\\(\\)\\[\\]\\%\\;\\\\\\+\\.\\,\\�\\�]+";
	public static String ENGLISH_NUMBER_PUNC_REGEX = "^(["+ENGLISH_REGEX+"|"+NUMBER_REGEX+"|"+PUNC_REGEX+"]+)$";
	public static String SPACE_REGEX = "(.*\\s+.*)";

	public static String LATIN_MARKER = "@@LAT@@";
	public static String ANALYSIS_SPLIT_MARKER = "//";
	public static String EMPTY_MARKER = "##";
	public static String WORD_SPLIT_MARKER = "^^";
	public static String COUNT_MARKER = "%%";
	
	public static String YES = "yes";
	public static String NO = "no";
	public static String NONE_SPACE_DELIMITER="NONE";
	public static String TAB_SPACE_DELIMITER="TAB";*/
	
	
}

// fazlake
enum SENTIMENT {POSITIVE, NEGATIVE, UNDETERMINED};
enum LANGUAGE { UTF8_AR, BW, ROMAN};
