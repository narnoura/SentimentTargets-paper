/**
 * 
 */
package util;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author Narnoura
 * Utilities for writing to file.
 */
public class FileWriter {

	public static boolean WriteFile(String file_path, String content){
		try{
			java.io.FileWriter fw = new java.io.FileWriter(file_path);
			PrintWriter pw = new PrintWriter(fw);
			pw.write(content);
			pw.close();
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean WriteEfficientFile(String file_path, String content){
		try{
		    java.io.FileWriter fw = new java.io.FileWriter(file_path);
		    Writer out = new BufferedWriter(fw);
			out.write(content);
			out.close();
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	// more efficient
	public static void WriteToFile(List<String> lines, String output_path) {
			String out = "";
			try {
				Writer outw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(output_path), "UTF-8"));
				for (String line : lines) {
					outw.write(line + "\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	/*public static boolean WriteUTF8File (String file_path, String content) {
		try{
			PrintWriter pw = new PrintWriter(file_path, "UTF-8");
			pw.write(content);
			pw.close();
			return true;
		} catch(Exception e){
			System.out.println(e.getMessage());
			return false;
		}
		
	}*/
	
	public static void WriteUTF8File (String file_path, String content) {
		try {
		Writer out = new BufferedWriter(new OutputStreamWriter (
				new FileOutputStream (file_path), "UTF-8"));
		out.write(content);
		out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public static boolean WriteXMLFile (Document document, String file_path,
			String encoding) {
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
		    if (encoding == "utf8" || encoding == "utf8ar") {
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			}
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty
		        ("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(document, "UTF-8"); 
			StreamResult result = new StreamResult(new File(file_path));
			transformer.transform(source, result);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
		return true;
	}
	
	public static String Combine (String path1, String path2) {
		File file1 = new File(path1);
		File file2 = new File(file1, path2);
		return file2.getPath();
	}
	
}
