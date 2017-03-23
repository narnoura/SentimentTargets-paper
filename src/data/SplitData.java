/**
 * 
 */
package data;

import main.Constants;

import java.util.List;

import processor.InputReader;
import processor.OutputWriter;
import util.FileWriter;

/**
 * @author Narnoura
 * Reads xml file and splits data into train, test and dev, writes to xml file
 * Run like this: Run/SplitData in=input_xml_file outputdir=output_directory
 * train=train_percentage test=test_percentage dev=dev_percentage
 */
public class SplitData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String input_xml_file = "";
		String output_dir = "";
		String train = "", test = "", dev = "";
		String input_encoding = "utf8ar";
		String output_encoding = "bw";
		boolean convertbw = false;
		
		try {
			 for (int i=0; i<args.length; i++) { 
				 String argument = args[i];	 
				 if(argument.startsWith(Constants.INPUT_FILE)) {
					input_xml_file = argument.substring(Constants.INPUT_FILE.length());
				 }
				 else if (argument.startsWith(Constants.OUTPUT_DIRECTORY)) {
					output_dir = argument.substring(Constants.OUTPUT_DIRECTORY.length());
				 }
				 else if (argument.startsWith(Constants.TRAIN_SIZE)) {
					train = argument.substring(Constants.TRAIN_SIZE.length());
				 } 
				 else if (argument.startsWith(Constants.DEV_SIZE)) {
					test = argument.substring(Constants.DEV_SIZE.length());
				 }
				 else if (argument.startsWith(Constants.TEST_SIZE)) {
						dev = argument.substring(Constants.TEST_SIZE.length());
				 }
			 }
		 }
		 catch(Exception e){
				System.out.println("No input arguments specified. Please specify an input file. Exiting \n");
				return;
			}
	     
		try {
		DataSplitter ds = new DataSplitter();
		List<Comment> comments = InputReader.ReadCommentsFromXML(input_xml_file, input_encoding, convertbw);
		ds.SplitComments(comments, Integer.parseInt(train), Integer.parseInt(test), Integer.parseInt(dev));
		List<Comment> train_set = ds.train_set;
		List<Comment> test_set = ds.test_set;
		List<Comment> dev_set = ds.dev_set;
	
		// Write to xml
		OutputWriter.WriteCommentsToXML(train_set, FileWriter.Combine(output_dir, "train.xml"), output_encoding);
		OutputWriter.WriteCommentsToXML(dev_set, FileWriter.Combine(output_dir, "dev.xml"), output_encoding);
		OutputWriter.WriteCommentsToXML(test_set, FileWriter.Combine(output_dir, "test.xml"), output_encoding);
		
		// Write to raw
		OutputWriter.WriteCommentsToRaw(train_set, FileWriter.Combine(output_dir, "train.raw"), output_encoding);
		OutputWriter.WriteCommentsToRaw(dev_set, FileWriter.Combine(output_dir, "dev.raw"), output_encoding);
		OutputWriter.WriteCommentsToRaw(test_set, FileWriter.Combine(output_dir, "test.raw"), output_encoding);
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
