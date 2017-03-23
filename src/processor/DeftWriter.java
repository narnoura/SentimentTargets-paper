package processor;

import data.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.FileWriter;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DeftWriter {
	/*
	 * Writes output to CSV file or best.xml file
	 */
	
	public List<Comment> output_comments;
	public String output_path;
	public DeftWriter() {
	}
	public DeftWriter(List<Comment> output_comments) {
		this.output_comments = output_comments;
	}
	public void WriteToCSVFile(String output_path) {
		if (this.output_comments.isEmpty()) {
			return;
		}
		String csv_output = "";
		for (Comment c: this.output_comments) {
			for (Target t: c.targets_) {
				DeftTarget dt = (DeftTarget) t;
				String target_erem_id  = dt.ere_id;
				String target_length = dt.length;
				String target_offset = dt.offset;
				// These are the targets we couldn't find an offset for
				if (target_offset == null) {
					continue;
				}
				String target_text = dt.text_;
				String source_erem_id = dt.source_ere_id;
				String source_length = dt.source_length;
				String source_offset = dt.source_offset;
				String source_text = dt.source;
				String sentiment_polarity = dt.sentiment_;
				if (sentiment_polarity.equals("positive")) {
					sentiment_polarity = "pos";
				}
				if (sentiment_polarity.equals("negative")) {
					sentiment_polarity = "neg";
				}
				csv_output += target_erem_id + "," + target_length 
						+ "," + target_offset + "," + target_text + "," + source_erem_id
						+ "," + source_length + "," + source_offset + "," + source_text
						+ "," + sentiment_polarity + "\n";
			
			}
		}
		FileWriter.WriteFile(output_path, csv_output);
	}
	
	// need to update this file to print non-entity targets
	public void WriteToCSVFileWithOffsets(String annotation_id, String output_path) {
		if (this.output_comments.isEmpty()) {
			return;
		}
		
		String offsets="";
		Integer off0 = 0;
		Integer off1 = 0;
		boolean has_multiple_annotation = annotation_id.startsWith("ENG_DF");
		
		if (has_multiple_annotation) {
		System.out.println("annotation id:"+annotation_id);
		String[] fields = annotation_id.split("_");
		offsets = fields[5]; // e.g ENG_DF_00183_2015_0408_F0000009B_offsets
		String[] pair = offsets.split("-");
		off0 = Integer.parseInt(pair[0]);
		off1 = Integer.parseInt(pair[1]);
		System.out.println("off0:"+off0);
		System.out.println("off1:"+off1);
		}
		String csv_output = "";
		
		for (Comment c: this.output_comments) {
			// First print targets
			for (Target t: c.targets_) {
				DeftTarget dt = (DeftTarget) t;
				String target_erem_id  = dt.ere_id;
				String target_length = dt.length;
				String target_offset = dt.offset;
				// These are the targets we couldn't find an offset for
				if (target_offset == null 
					|| (has_multiple_annotation &&
							(Integer.parseInt(dt.offset) < off0 
							|| Integer.parseInt(dt.offset) > off1))) {
					continue;
				}
				String target_text = dt.text_;
				target_text = target_text.replaceAll(",","");
				String source_erem_id = dt.source_ere_id;
				String source_length = dt.source_length;
				String source_offset = dt.source_offset;
				String source_text = dt.source;
				String sentiment_polarity = dt.sentiment_;
				if (sentiment_polarity.equals("positive")) {
					sentiment_polarity = "pos";
				}
				if (sentiment_polarity.equals("negative")) {
					sentiment_polarity = "neg";
				}
				// exclude sources not in this segment, make them null
				if ((has_multiple_annotation && 
						Integer.parseInt(source_offset) < off0 
						|| Integer.parseInt(source_offset) > off1)) {
							source_erem_id = "";
							source_offset = "";
							source_text = "";
							source_length = "";
					//continue;
						}
				csv_output += target_erem_id + "," + target_length 
						+ "," + target_offset + "," + target_text + "," + source_erem_id
						+ "," + source_length + "," + source_offset + "," + source_text
						+ "," + sentiment_polarity + "\n";
			
			}
			
			// Then print all other entities that are not targets
		/*	if (!c.entities_.isEmpty()) {
				System.out.println("Annotation id:" + annotation_id + "has nonempty entities!");
				for (Entity e: c.entities_) {
					if (has_multiple_annotation && Integer.parseInt(e.offset) >= off0 && Integer.parseInt(e.offset) <=off1
						&& e.sentiment.equals("none")) {
						csv_output += e.ere_id + "," + e.length
								+ "," + e.offset + "," + e.text + "," + ""
								+ "," + "" + "," + "" + "," + ""
								+ "," + "none" + "\n";
						} else if (!has_multiple_annotation && e.sentiment.equals("none")) {
							csv_output += e.ere_id + "," + e.length
									+ "," + e.offset + "," + e.text + "," + ""
									+ "," + "" + "," + "" + "," + ""
									+ "," + "none" + "\n";
						}
				}
			} else {
				System.out.println("Empty entities for annotation id:" + annotation_id);
			}*/
			
		}
		FileWriter.WriteFile(output_path, csv_output);
		
	}
	
}
