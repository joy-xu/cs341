import java.util.*;
import java.io.*;

import util.*;
public class PhilSlot {

	static String indexLoc, first, second;
    static int withinWords;
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

                if (!options.containsKey("-numTerms"))
                        PrintUsageAndExit();
                if (Integer.parseInt(options.get("-numTerms")) != 2)
                        PrintUsageAndExit();

                if (!options.containsKey("-getSentences") && !options.containsKey("-getCompleteDocument"))
                        PrintUsageAndExit();

                first = null;
                second = null;
                if (!options.containsKey("-q1") || !options.containsKey("-q2"))
                        PrintUsageAndExit();
                first = options.get("-q1");
                second = options.get("-q2");
                withinWords = 1;
		if (options.containsKey("-withinWords"))
                        withinWords = Integer.parseInt(options.get("-withinWords"));	
	}

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		Map<String, String> options = new HashMap<String, String>();
		
		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		
		parseArgs(options);
		
		String queryString = null;
		queryString = "#od" + withinWords + "(#1(" + first + ") #1(" + second + "))";
		
		System.out.println("Querying Index for " + queryString + "...");
		Query q = new Query(indexLoc);
    		List<String> queryResults = q.queryIndex(queryString);
    	
    		System.out.println("Retreiving documents...");
    	
  	  	String workingDirectory = "";
    		if (options.containsKey("-downloadDirectory"))
	    	{
    			workingDirectory = options.get("-downloadDirectory");
    			if (workingDirectory == null)
    				workingDirectory = "";
    			if (workingDirectory.charAt(workingDirectory.length()-1) != '/')
    				workingDirectory = workingDirectory + "/";
    		}

	    	ThriftReader tr = new ThriftReader(queryResults,workingDirectory);
    	
    		boolean getSentences = false;
    		boolean getCompleteDocument = false;
    		boolean includeNER = false;
	    	boolean doBootstrap = false;
            boolean doPhilSlot= false;
    		String sentenceOutput = null;
	    	String documentOutput = null;
    	
	    	if (options.containsKey("-getSentences"))
    		{
    			getSentences = true;
	    		sentenceOutput = options.get("-getSentences");
	    	}
    	
	    	if (options.containsKey("-getCompleteDocument"))
    		{
    			getCompleteDocument = true;
	    		documentOutput = options.get("-getCompleteDocument");
    		}
	    	if (options.containsKey("-includeNER"))
    			includeNER = true;
    	
	    	if (options.containsKey("-bootstrap"))
    			doBootstrap = true;
    	
	    	if (options.containsKey("-philslot"))
    			doPhilSlot = true;
    	
	    	// Use return variable sentences for all the sentences
    		if (getSentences)
    		{
    			System.out.println("Printing sentences...");
    			List<String> sentences = new ArrayList<String>();
    			sentences = tr.getSentences(first,second,includeNER, sentenceOutput);
    			if (doBootstrap)
    			{
    				ExtractRelation er = new ExtractRelation();
    				er.findRelations(sentences, first, second);
    			}
                if (doPhilSlot) {
                	ExtractSlot es = new ExtractSlot();
                	es.findSlotVals(sentences, first, second, includeNER);
                }
    		}
    	
    		if (getCompleteDocument)
    		{
    			System.out.println("Printing complete document...");
	    		tr.getCompleteDocument(includeNER, documentOutput);
    		}
    		System.out.println("Done!");
    		return;
	}

	private static void PrintUsageAndExit()
	{
		System.out.println("QueryRetreiver Usage:");
		System.out.println("java -jar QueryRetreiver.jar");
		
		System.out.println("-indexLocation absolutePathNameOfIndexDirectory");
		System.out.println("-numTerms numberOfTermsInYourQuery(1 or 2, where each can be multiple words)");
		System.out.println("-q1 firstQueryTerm [-q2 secondQueryTerm]");
		System.out.println("-withinWords numberOfWordsWithinWhichTheQueryTermsShouldAppear");
		System.out.println("-downloadDirectory directoryForRetreivedDocuments");
		System.out.println("-getSentences outputFile (If you want to get concerned sentences only");
		System.out.println("-getCompleteDocument outputFile (If you want the entire document, with NER tags");
		System.out.println("At least one of the above two options must be enabled");
		System.out.println("-includeNER NER Tags will be printed if this argument is present");
		System.out.println("Example Usage:");
		System.out.println("java -jar QueryRetreiver.jar " +
				"-indexLocation /home/ubuntu/chunk -numTerms 2 -q1 EMET -q2 \"Clifford May\"" +
				" -withinWords 10 -getSentences retreivedSentences_Cliff -getCompleteDocument retreivedDoc_Cliff" +
				" -downloadDirectory /home/ubuntu/testing/");
		System.exit(0);
	}
}
