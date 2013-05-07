import java.util.*;
import java.io.*;

import retrieWin.Utils.*;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import fig.basic.LogInfo;

public class QueryRetreiver {

	/**
	 * @param args
	 */
	public static List<String> executeQuery(String indexLoc, String query, int numResults, String first, String second, String workingDirectory) {
		Query q = new Query(indexLoc);
    	List<String> queryResults = q.queryIndex(query, numResults);
    	
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
	
	
	public static List<String> executeQuery(String indexLoc, String query, int numResults, String first, String workingDirectory) {
		Query q = new Query(indexLoc);
    	List<String> queryResults = q.queryIndex(query, numResults);
    	
    	System.out.println("Retreiving documents...");
    	ThriftReader tr = new ThriftReader(queryResults,workingDirectory);
    	List<String> sentences = new ArrayList<String>();
    	try {
			sentences = tr.getSentences(first, false, "results.txt");
		} 
    	catch (IOException e) {
    		LogInfo.logs(e.getMessage());
			e.printStackTrace();
		}
    	return sentences;
	}
	
	
	public static List<String> executeQueryWithLemma(StanfordCoreNLP processor, String indexLoc, String query, int numResults, String first, String workingDirectory) {
		Query q = new Query(indexLoc);
    	List<String> queryResults = q.queryIndex(query, numResults);
    	
    	System.out.println("Retreiving documents...");
    	ThriftReader tr = new ThriftReader(queryResults,workingDirectory);
    	List<String> sentences = new ArrayList<String>();
    	
		sentences = tr.getSentencesWithLemma(first,processor);
		
    	return sentences;
	}
	
}
