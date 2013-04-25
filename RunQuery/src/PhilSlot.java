import java.util.*;
import java.io.*;

import edu.stanford.nlp.stats.IntCounter;

import util.*;
public class PhilSlot {

	static String indexLoc, entity, slot, workingDirectory = "", sentenceOutput;
	static List<String> patterns = new ArrayList<String>();
    static int withinWords;
    static boolean getSentences = false, getCompleteDocument = false, includeNER = false;
    static boolean doBootstrap= false, doPhilSlot= false;
	
	/**
	 * @param args
	 */

	public static void parseArgs(Map<String, String> options) {
		System.out.println("Bootstrap Options");
        for (Map.Entry<String, String> entry: options.entrySet()) {
        	System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue());
        }

        if (!options.containsKey("-indexLocation"))
        	PrintUsageAndExit();
        indexLoc = options.get("-indexLocation");

        if (!options.containsKey("-entity"))
        	PrintUsageAndExit();
        entity = options.get("-entity");
                
        if (!options.containsKey("-slot"))
            PrintUsageAndExit();
        slot = options.get("-slot");
                
        if (!options.containsKey("-pattern"))
            patterns = Patterns.getPatterns(slot);
        else
            patterns.add(options.get("-pattern"));
        
        if (!options.containsKey("-getSentences"))
            PrintUsageAndExit();
        if (options.containsKey("-getSentences"))
		{
			getSentences = true;
    		sentenceOutput = options.get("-getSentences");
    	}
        
        withinWords = 1;
		if (options.containsKey("-withinWords"))
            withinWords = Integer.parseInt(options.get("-withinWords"));	
	
    	if (options.containsKey("-includeNER"))
			includeNER = true;
	
    	if (options.containsKey("-bootstrap"))
			doBootstrap = true;
	
    	if (options.containsKey("-philslot"))
			doPhilSlot = true;
    	
    	if (options.containsKey("-downloadDirectory"))
    	{
			workingDirectory = options.get("-downloadDirectory");
			if (workingDirectory == null)
				workingDirectory = "";
			if (workingDirectory.charAt(workingDirectory.length()-1) != '/')
				workingDirectory = workingDirectory + "/";
		}
	}

	public static void main(String[] args) throws IOException{
		Map<String, String> options = new HashMap<String, String>();
		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		parseArgs(options);
		
		IntCounter<String> slotValCounter = new IntCounter<String>();
		
		for(String pattern: patterns) {
			String queryString = null;
			queryString = "#od" + withinWords + "(#1(" + entity + ") #1(" + pattern + "))";
			
			System.out.println("Querying Index for " + queryString + "...");
			Query q = new Query(indexLoc);
	    	List<String> queryResults = q.queryIndex(queryString);
	    	
	    	System.out.println("Retreiving documents...");
	    	
	  	    ThriftReader tr = new ThriftReader(queryResults,workingDirectory);
	    	
	    	// Use return variable sentences for all the sentences
	    	if (getSentences)
	    	{
	    		System.out.println("Printing sentences...");
	    		List<String> sentences = new ArrayList<String>();
	    		sentences = tr.getSentences(entity,pattern,includeNER, sentenceOutput);
	   			if(sentences.isEmpty()) {
	    			System.out.println("No matches found...");
	    			return;
	    		}
	    				
	    		if (doPhilSlot) {
	               	ExtractSlot es = new ExtractSlot();
	               	es.findSlotVals(sentences, entity, pattern, includeNER, slotValCounter);
	            }
	    	}
		}
		
    	System.out.println("Done!");
        System.out.println(slotValCounter);
    	return;
	}

	private static void PrintUsageAndExit()
	{
		System.out.println("QueryRetreiver Usage:");
		System.out.println("java -jar QueryRetreiver.jar");
		
		System.out.println("-indexLocation absolutePathNameOfIndexDirectory");
		System.out.println("-numTerms numberOfTermsInYourQuery(1 or 2, where each can be multiple words)");
		System.out.println("-entity firstQueryTerm [-pattern secondQueryTerm]");
		System.out.println("-withinWords numberOfWordsWithinWhichTheQueryTermsShouldAppear");
		System.out.println("-downloadDirectory directoryForRetreivedDocuments");
		System.out.println("-getSentences outputFile (If you want to get concerned sentences only");
		System.out.println("-getCompleteDocument outputFile (If you want the entire document, with NER tags");
		System.out.println("At least one of the above two options must be enabled");
		System.out.println("-includeNER NER Tags will be printed if this argument is present");
		System.out.println("Example Usage:");
		System.out.println("java -jar QueryRetreiver.jar " +
				"-indexLocation /home/ubuntu/chunk -numTerms 2 -entity EMET -pattern \"Clifford May\"" +
				" -withinWords 10 -getSentences retreivedSentences_Cliff -getCompleteDocument retreivedDoc_Cliff" +
				" -downloadDirectory /home/ubuntu/testing/");
		System.exit(0);
	}
}
