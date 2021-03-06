import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import retrieWin.Utils.CommandLineUtils;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class ExtractArguments {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
Map<String, String> options = new HashMap<String, String>();
		
		
		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		
		System.out.println("Bootstrap (ExtractArguments Options");
		for (Map.Entry<String, String> entry: options.entrySet()) {
			System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue());
		}
		
		if (!options.containsKey("-indexLocation"))
			PrintUsageAndExit();
		
		String indexLoc = options.get("-indexLocation");
		
		if (!options.containsKey("-getSentences") && !options.containsKey("-getCompleteDocument"))
			PrintUsageAndExit();
		
		int withinWords = 1;
		if (options.containsKey("-withinWords")) {
			withinWords = Integer.parseInt(options.get("-withinWords"));
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
    	
    	Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP processor = new StanfordCoreNLP(props, false);
    	
    	List<Pair<String, String>> bootstrapList = new ArrayList<Pair<String, String>>();
    	bootstrapList.add(new Pair<String, String>("Bill Gates", "Microsoft"));
    	bootstrapList.add(new Pair<String, String>("Steve Jobs", "Apple"));
    	bootstrapList.add(new Pair<String, String>("Larry Page", "Google"));
    	bootstrapList.add(new Pair<String, String>("Sergey Brin", "Google"));
    	bootstrapList.add(new Pair<String, String>("Narayana Murthy", "Infosys"));
    	bootstrapList.add(new Pair<String, String>("Hugh Hefner", "Playboy"));
    	bootstrapList.add(new Pair<String, String>("Marcus Goldman", "Goldman Sachs"));
    	bootstrapList.add(new Pair<String, String>("Samuel Sachs", "Goldman Sachs"));
    	bootstrapList.add(new Pair<String, String>("James Simons", "Renaissance Technologies"));
    	bootstrapList.add(new Pair<String, String>("Kenneth Griffin", "Citadel LLC"));
    	bootstrapList.add(new Pair<String, String>("Emanuel Lehman", "Lehman Brothers"));
    	bootstrapList.add(new Pair<String, String>("Henry Lehman", "Lehman Brothers"));
    	// Use return variable sentences for all the sentences
    	
    	if (getSentences) {
    		Query q = new Query(indexLoc);
    		for(Pair<String, String> pair:bootstrapList) {
	    		String first = pair.first();
	    		String second = pair.second();
	    		String queryString = null;
	    		if (second != null)
	    			queryString = "#uw" + withinWords + "(#1(" + first + ") #1(" + second + "))";
	    		else
	    			queryString = "#1(" + first + ")";
	    		
	    		System.out.println("Querying Index for " + queryString + "...");
	    		
	    		ExtractRelation er = new ExtractRelation(processor);
	    		er.findRelations(QueryRetreiver.executeQuery(indexLoc, queryString, 1000, first, second, workingDirectory), first, second);
    	   	}
    		System.out.println(ExtractRelation.relationCounter);
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
