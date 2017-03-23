/**
 * 
 */
package models;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import data.Comment;
import data.Target;
import data.Token;
import util.FileWriter;

// TODO: import models.FeatureExtractor;? or extract + write together , or call 
// feature writer in feature extractor
// TODO: Change List<String> features to Set<String> features everywhere 

/**
 * @author Narnoura
 *
 */

public class FeatureWriter {

	/**
	 * Writes feature files for CRF (typically space separated
	 * or tab separated features)
	 */
	
	public String file_path;
	public String content;
	
	// e.g space or tabs
	public String separator;
	
	public FeatureWriter() {
		this.separator = " ";
		content = "";
	}

	public FeatureWriter(String file_path, String separator) {
		this.file_path = file_path;
		this.content = "";
		this.separator = separator;
	}

	public void SetFilePath (String file_path) {
		this.file_path = file_path;
		this.separator = " ";
	}
	
	public void SetSeparator(String separator) {
		this.separator = separator;
	}
	
	public void WriteSentence(List<List<String>> data) {
	
		for (List<String> token_features : data) {
			for (String f: token_features) {
				content += f;
				content += separator;
			}
			content += "\n";
		}
		content += "\n";
	}
	
	/*public void WriteLabels(List<String> labels) {
		for (String label: labels) {
			content += label;
			content += "\n";
		}
		
	}*/
	
	public void WriteNumeric(List<List<Double>> features) {
		
	}
	
	/*public void Next() {
		content += "\n";
	}*/
	
	public void Complete() {
		if (content.isEmpty()) {
			System.out.println("FeatureWriter: Empty Feature Content for this sentence."
					+ " Exiting\n");
			System.exit(0);
		}
		else {
			System.out.println("FeatureWriter: Finished writing features to file\n");
			FileWriter.WriteFile(file_path, content);
			content = "";
		}
	}
 }
