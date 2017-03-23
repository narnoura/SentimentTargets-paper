package processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.io.FileInputStream;
import java.io.InputStream;
 
import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.modules.cPickle;





public class LoadPickle {
	
	// This one actually doesn't use pickle, but classes.txt file which is output
	// from Google's word2vec code
	public HashMap<String, String> getSimpleWordToCluster(String file) {
		System.out.println("Reading classes file:"+file);
		boolean skipEmpty = true;
		HashMap<String, String> wordToCluster = new HashMap<String, String>();
		List<String> lines = util.FileReader.ReadFile(file, "english", skipEmpty);
		for (String line: lines) {
			String[] fields = line.split(" ");
			String word = fields[0];	
			String cluster = fields[1];
			wordToCluster.put(word, cluster);
		}
		return wordToCluster;
	}
	
	
	 public HashMap<String, String> getWordToClusterFileString(String file) {
		 	System.out.println("Reading pickle file:"+file);
	        HashMap<String, String> wordToCluster = new HashMap<String, String>();
	        File f = new File(file);
	        BufferedReader bufR;
	        StringBuilder strBuilder = new StringBuilder();
	        System.out.println("Creating buffered reader");
	        try {
	        	bufR = new BufferedReader(new FileReader(f));
	        	String aLine;
	        	while(null != (aLine = bufR.readLine())) {
	        		strBuilder.append(aLine).append("n");
	        	}
	        } catch (FileNotFoundException e) {
	        		e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        System.out.println("Creating pystring");
	        PyString pyStr = new PyString(strBuilder.toString());
	        System.out.println("Size of pystring:" + pyStr.__len__());
	        System.out.println("pystring:" + pyStr);
	        System.out.println("Creating pydictionary");
	        PyDictionary wordToClustersObj = new PyDictionary();
	        try {
	        wordToClustersObj = (PyDictionary) cPickle.loads(pyStr);
	        } catch(Exception e){
	        	e.printStackTrace();
	        }
	        System.out.println("Creating map");
	        ConcurrentMap<PyObject, PyObject> aMap = wordToClustersObj.getMap();
	        System.out.println("Filling map");
	        for (Map.Entry<PyObject, PyObject> entry : aMap.entrySet()) {
	            String word = entry.getKey().toString();
	            String clusterId = entry.getValue().toString();
	            //Integer cluster = Integer.parseInt(clusterId);
	           // PyList countryIdList = (PyList) entry.getValue();
	           /*PyObject clusterId = (PyObject) entry.getValue();
	            Integer cluster = (Integer) clusterId;
	            List<String> countryList = (List<String>) countryIdList.subList(0, countryIdList.size());
	            ArrayList<String> countryArrList = new ArrayList<String>(countryList);*/
	           wordToCluster.put(word, clusterId);
	        }
//	        System.out.println(idToCountries.toString());
	        return wordToCluster;
	    }

	 public HashMap<String, String> getWordToClusterFileStream(String file) {
		 	System.out.println("Reading pickle file:"+file);
	        HashMap<String, String> wordToCluster = new HashMap<String, String>();
	        File f = new File(file);
	        InputStream fs = null;
	        try {
	            fs = new FileInputStream(f);
	        } catch (FileNotFoundException e) {
	        	System.out.println("Couldn't read file stream");
	            e.printStackTrace();
	            return null;
	        }
	        System.out.println("Creating pyfile");
	        PyFile pyStr = new PyFile(fs);
	        PyDictionary wordToClustersObj = new PyDictionary();
	        System.out.println("Creating pydictionary");
	        try {
	        wordToClustersObj = (PyDictionary) cPickle.load(pyStr);
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        }
	        System.out.println("Creating map");
	        ConcurrentMap<PyObject, PyObject> aMap = wordToClustersObj.getMap();
	        System.out.println("Filling map");
	        for (Map.Entry<PyObject, PyObject> entry : aMap.entrySet()) {
	            String word = entry.getKey().toString();
	            String clusterId = entry.getValue().toString();
	            //Integer cluster = Integer.parseInt(clusterId);
	           // PyList countryIdList = (PyList) entry.getValue();
	           /*PyObject clusterId = (PyObject) entry.getValue();
	            Integer cluster = (Integer) clusterId;
	            List<String> countryList = (List<String>) countryIdList.subList(0, countryIdList.size());
	            ArrayList<String> countryArrList = new ArrayList<String>(countryList);*/
	           wordToCluster.put(word, clusterId);
	        }
//	        System.out.println(idToCountries.toString());
	        return wordToCluster;
	    }
}
