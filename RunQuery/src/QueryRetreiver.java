import java.util.*;
import java.io.*;

import util.*;
public class QueryRetreiver {

	/**
	 * @param args
	 */
	public static List<String> executeQuery(String indexLoc, String query, String first, String second, String workingDirectory) {
		Query q = new Query(indexLoc);
    	List<String> queryResults = q.queryIndex(query);
    	
    	System.out.println("Retreiving documents...");
    	ThriftReader tr = new ThriftReader(queryResults,workingDirectory);
    	List<String> sentences = new ArrayList<String>();
    	try {
			sentences = tr.getSentences(first, second, false, "results.txt");
		} 
    	catch (IOException e) {
			e.printStackTrace();
		}
    	return sentences;
	}
}
