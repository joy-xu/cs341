import java.util.*;
import fig.basic.LogInfo;
import fig.exec.Execution;
import fig.basic.Option;

import java.io.*;

import retrieWin.Utils.*;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.Pair;



public class Bootstrap implements Runnable {
	
	@Option(gloss="index Location") public String indexLocation;
	@Option(gloss="within Words") public int withinWords;
	@Option(gloss="num Results") public int numResults;
	@Option(gloss="downloadDirectory") public String downloadDirectory;
	@Option(gloss="getsentences") public String getsentences;
	@Option(gloss="bootstrap") public String bootstrap;
	@Option(gloss="expansionFile") public String expansionFile;
	static String[] a;
	public static void main(String[] args) {
		Execution.run(args, "Main", new Bootstrap());
	}
	
	/**
	 * @param args
	 */
	@Override
	public void run(){
		// TODO Auto-generated method stub
		LogInfo.begin_track("Main");
    	
    	boolean findSentences = true;
    	    	
    	if(findSentences) {
			FindSentences fs = new FindSentences();
			fs.findAssociateOf(expansionFile, indexLocation, downloadDirectory);
			
			//Index.buildIndex("/home/aju/Stanford/cs341/data/new/", "/home/aju/Stanford/cs341/data/expanded/");
		}
    	else
    		runBootstrap();
    	System.out.println("Done!");
    	LogInfo.end_track();	
    	return;
	}
	
	private static List<Pair<String, String>> getBootstrapInput(String fileName) {
		List<Pair<String, String>> bootstrapList = new ArrayList<Pair<String, String>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			try {
				while((line=reader.readLine())!=null) {
					String[] splits = line.split("\t");
					bootstrapList.add(new Pair<String, String>(splits[0], splits[1]));
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

	private void runBootstrap() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP processor = new StanfordCoreNLP(props, false);
			
    	List<Pair<String, String>> bootstrapList = getBootstrapInput(bootstrap);

		System.out.println(downloadDirectory);
		Query q = new Query(indexLocation);
		HashMap<String, Double> totalNormalizedCounts = new HashMap<String, Double>(), minShare = new HashMap<String, Double>();
    	IntCounter<String> numAppearances = new IntCounter<String>();
 
    	for(Pair<String, String> pair:bootstrapList) {
	    	String first = pair.first();
	    	String second = pair.second();
	    		
	    	String queryString = QueryBuilder.buildOrderedQuery(first, second, withinWords);
	    		
	    	System.out.println("Querying Index for " + queryString + "...");
	
	    	ExtractRelation er = new ExtractRelation(processor);
	    	HashMap<String, Double> normalizedCounts = er.findRelations(QueryRetreiver.executeQuery(indexLocation, queryString, numResults, first, second, downloadDirectory), first, second);
	    	System.out.println(normalizedCounts);
	    	for(String key:normalizedCounts.keySet()) {
	    		numAppearances.incrementCount(key);
	    		if(totalNormalizedCounts.containsKey(key))
	    			totalNormalizedCounts.put(key, totalNormalizedCounts.get(key) + normalizedCounts.get(key));
	    		else
	    			totalNormalizedCounts.put(key, normalizedCounts.get(key));
	    		if(minShare.containsKey(key)) {
	    			if(minShare.get(key) > normalizedCounts.get(key)) {
	    				minShare.put(key, normalizedCounts.get(key));
	    			}
	    		}
	    		else
	    			minShare.put(key, normalizedCounts.get(key));
	    	}
    	}
		
		System.out.println("Relations found");
		try {
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
		for(String key:totalNormalizedCounts.keySet()) {
			totalNormalizedCounts.put(key, (totalNormalizedCounts.get(key) - minShare.get(key) ) * (numAppearances.getCount(key) -1) );
			//totalNormalizedCounts.put(key, (totalNormalizedCounts.get(key) ) *  numAppearances.getCount(key));
			if(totalNormalizedCounts.get(key) >0 ) {
				System.out.println(key + " : " + totalNormalizedCounts.get(key));
				writer.write(key + " : " + totalNormalizedCounts.get(key) + "\n");
			}
		}
		writer.close();
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
}
//Do ner tags for bootstrapping entities
//Better seed set.