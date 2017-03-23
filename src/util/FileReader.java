/**
 * 
 */
package util;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @author Narnoura
 * Utilities for reading files.
 */
public class FileReader {
	
	// Read lines from a raw file
	// Processes roman text. So if encoding is 'utf8ar', it will be converted to BW.
	public static List<String> ReadFile(String file_path, String encoding, boolean skipempty) {
		try{
			List<String> lines = new ArrayList<String>(); 
			InputStream is = new FileInputStream(file_path);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
	        String line;
	        while ((line = br.readLine()) != null){
	        	line = line.trim();
	        	// If input is in Arabic utf8, convert to BW
	        	//if( (!line.equals("")) && (!line.startsWith("//"))) {
	        	if( (!line.startsWith("//"))) {
	        	    if (encoding.equals("utf8ar") || encoding.equals("utf8")) {
	        	    	line = BuckwalterConverter.ConvertToBuckwalter(line);
	        	    }
	        	    if (!skipempty || !line.equals("")) {
	        		lines.add(line);
	        	    }
	        	}
	         }
            is.close();
            return lines;
		} catch(Exception e){
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	// Use for reading full DEFT file as is without skipping any whitespace or nonwhitespace character
	public static String ReadFileAsItIs(String file_path) {
		String original_text = "";
		try {
			Scanner s = new Scanner(new File(file_path));
			original_text = s.useDelimiter("\\Z").next();
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return original_text;
	}
	
	public static String ReadFileText(String file_path, String encoding, boolean skipempty) {
		System.out.println("Reading full file text!\n");
		try{
			String text = ""; 
			InputStream is = new FileInputStream(file_path);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
	        String line;
	        while ((line = br.readLine()) != null){
	        	text += line;
	         }
            is.close();
            return text;
		} catch(Exception e){
			System.out.println(e.getMessage());
			return null;
		}
	}
	 
	// Read lines from a utf8 file
	 public static List<String> ReadUTF8File(String filePath){
	        try{
	        	List<String> lines = new ArrayList<String>();
	            InputStream is = new FileInputStream(filePath);
	            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	            String line;
	            while ((line = br.readLine()) != null){
	            	line = line.trim();
	                if ((!line.equals("")) && (!line.startsWith("//"))) {
	                	lines.add(line);
	                }
	            }
	            is.close();
	            return lines;
	        }catch(Exception e){
	            System.out.println(e.getMessage());
	            return null;
	        }
	    }

	// Read lines from xml file
	public static Document ReadXMLFile(String file_path, String encoding) {
	  Document doc = null;
	  try {
		File xml_file = new File(file_path);
		DocumentBuilderFactory db_factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder d_builder = db_factory.newDocumentBuilder();
		if (encoding.equals("utf8") || encoding.equals("utf8ar")) {
			doc = d_builder.parse(new FileInputStream(xml_file), "UTF-8");
		}
		else {
		doc = d_builder.parse(xml_file);
		}
	  }
	  catch (Exception e) {
		  e.printStackTrace();
	  }
		return doc;
		
	}
	
}
