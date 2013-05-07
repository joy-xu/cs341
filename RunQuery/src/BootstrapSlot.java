import java.util.*;
import java.io.*;

import retrieWin.Utils.*;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class BootstrapSlot{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		Map<String, String> options = new HashMap<String, String>();
		
		
		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		
		/*System.out.println("Bootstrap Options");
		for (Map.Entry<String, String> entry: options.entrySet()) {
			System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue());
		}*/
		
		if (!options.containsKey("-indexLocation"))
			PrintUsageAndExit();
		
		String indexLoc = options.get("-indexLocation");
		
		if (!options.containsKey("-getSentences") && !options.containsKey("-getCompleteDocument"))
			PrintUsageAndExit();
		
		int withinWords = 1;
		if (options.containsKey("-withinWords")) {
			withinWords = Integer.parseInt(options.get("-withinWords"));
		}
		
		int numResults = Integer.MAX_VALUE;
		if (options.containsKey("-numResults")) {
			numResults = Integer.parseInt(options.get("-numResults"));
		}
    	
    	String workingDirectory = "";
    	if (options.containsKey("-downloadDirectory")) {
    		workingDirectory = options.get("-downloadDirectory");
    		if (workingDirectory == null)
    			workingDirectory = "";
    		if (workingDirectory.charAt(workingDirectory.length()-1) != '/')
    			workingDirectory = workingDirectory + "/";
    	}
    	    	
    	boolean getSentences = false;
    	boolean getCompleteDocument = false;
    	boolean includeNER = false;
    	boolean doBootstrap = false;
    	String sentenceOutput = null;
    	String documentOutput = null;
    	String bootstrapInput = null;
    	
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
    	
    	if (options.containsKey("-bootstrap")) {
    		doBootstrap = true;
    		bootstrapInput = options.get("-bootstrap");
    	}
    	
    	Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP processor = new StanfordCoreNLP(props, false);
			
    	List<String> bootstrapList = getBootstrapInput(bootstrapInput);

    	// Use return variable sentences for all the sentences
    	
    	if (getSentences) {
    		Query q = new Query(indexLoc);
    		for(String term:bootstrapList) {
	    		
	    		String queryString = QueryBuilder.buildSingleTermQuery(term);
	    		
	    		System.out.println("Querying Index for " + queryString + "...");
	    		
	    		ExtractRelation er = new ExtractRelation(processor);
	    		List<String> queryOutput = QueryRetreiver.executeQueryWithLemma(processor, indexLoc, queryString, numResults, term, workingDirectory);
	    		
	    		//To Do: Add the extraction function
	    		er.findEntityPairs(queryOutput, term);
	    		System.out.println(ExtractRelation.entityPairCounter);
    		}
    		
    		System.out.println(ExtractRelation.entityPairCounter);
		}

    	System.out.println("Done!");
    	return;
	}
	
	private static List<String> getBootstrapInput(String fileName) {
		List<String> bootstrapList = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			try {
				while((line=reader.readLine())!=null) {
					bootstrapList.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bootstrapList;
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