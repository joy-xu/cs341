package retrieWin.PatternBuilder;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;


import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.SSF.SlotPattern.Rule;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.PriorityQueue;
import retrieWin.Utils.Utils;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.Utils.FileUtils;

import edu.stanford.nlp.stats.IntCounter;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class ContactMeetPlaceTime implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="output File") public String outputFile;
	List<Entity> entities;
	public static void main(String[] args) {
		Execution.run(args, "Main", new ContactMeetPlaceTime());
	}
	
	public ContactMeetPlaceTime(){
		readEntities();
	}

	@Override
	public void run() {
		LogInfo.begin_track("run()");
		//runBootstrap();
		if (!workingDirectory.endsWith("/"))
			workingDirectory = workingDirectory + "/";
		//gatherResults();
		List<String> filename_nonSchools = new ArrayList<String>();
		List<String> filenames = new ArrayList<String>();
		
		filenames.add("output_20120721");
		filenames.add("outputDay2_20120722");
		
		
		filename_nonSchools.add("output_nonSchools");
		filename_nonSchools.add("output_nonSchools_20120721");
		
		aggregateResults(filenames,filename_nonSchools);
		//writeHumanReadableResults(filename);
		LogInfo.end_track();
	}
	
	
	public void aggregateResults(List<String> filenames, List<String> nonSchoolFilenames)
	{
		Map<SlotPattern,Set<String>> allPatterns = new HashMap<SlotPattern,Set<String>>();
		for (String filename:filenames)
		{
			@SuppressWarnings("unchecked")
			Map<SlotPattern,Set<String>> currentPattern = (Map<SlotPattern,Set<String>>)FileUtils.readFile(filename);
			for (SlotPattern p:currentPattern.keySet())
			{
				if (allPatterns.containsKey(p))
				{
					Set<String> existing = allPatterns.get(p);
					existing.addAll(currentPattern.get(p));
					allPatterns.put(p, existing);
				}
				else
				{
					allPatterns.put(p, currentPattern.get(p));
				}
			}
		}
		Map<SlotPattern,Set<String>> allPatternsNonSchools = new HashMap<SlotPattern,Set<String>>();
		for (String filename:nonSchoolFilenames)
		{
			@SuppressWarnings("unchecked")
			Map<SlotPattern,Set<String>> currentPattern = (Map<SlotPattern,Set<String>>)FileUtils.readFile(filename);
			for (SlotPattern p:currentPattern.keySet())
			{
				if (allPatternsNonSchools.containsKey(p))
				{
					Set<String> existing = allPatternsNonSchools.get(p);
					existing.addAll(currentPattern.get(p));
					allPatternsNonSchools.put(p, existing);
				}
				else
				{
					allPatternsNonSchools.put(p, currentPattern.get(p));
				}
			}
		}
		double totalCount = 0;
		Map<SlotPattern,Double> patternCounts = new HashMap<SlotPattern,Double>();
		for (SlotPattern pattern:allPatterns.keySet())
		{
			totalCount += allPatterns.get(pattern).size();
			if (patternCounts.containsKey(pattern))
			{
				patternCounts.put(pattern, patternCounts.get(pattern)+allPatterns.get(pattern).size());
			}
			else
			{
				patternCounts.put(pattern, (double)allPatterns.get(pattern).size());
			}
		}
		
		// Add 1.5 times the count of non schools
		for (SlotPattern pattern:allPatternsNonSchools.keySet())
		{
			totalCount += 1.5*allPatternsNonSchools.get(pattern).size();
			if (patternCounts.containsKey(pattern))
			{
				patternCounts.put(pattern, patternCounts.get(pattern)+(1.5*allPatternsNonSchools.get(pattern).size()));
			}
			else
			{
				patternCounts.put(pattern, 1.5*allPatternsNonSchools.get(pattern).size());
			}
		}
		Map<SlotPattern,Double> patternWeights = new HashMap<SlotPattern,Double>();
		PriorityQueue<SlotPattern> pq = new PriorityQueue<SlotPattern>();
		for (SlotPattern p: patternCounts.keySet())
		{
			double confidence = patternCounts.get(p)/totalCount;
			patternWeights.put(p,confidence);
			pq.add(p,confidence);
		}
		try
		{
			BufferedWriter buf = new BufferedWriter(new FileWriter(outputFile));
			while(!pq.isEmpty())
			{
				SlotPattern p = pq.next();
				double confidence = patternWeights.get(p);
				p.setConfidenceScore(confidence);
				buf.write(p.toString());
				buf.newLine();
			}
			buf.flush();
			buf.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	public void writeHumanReadableResults(List<String> filenames)
	{
		Map<SlotPattern,Set<String>> allPatterns = new HashMap<SlotPattern,Set<String>>();
		for (String filename:filenames)
		{
			@SuppressWarnings("unchecked")
			Map<SlotPattern,Set<String>> currentPattern = (Map<SlotPattern,Set<String>>)FileUtils.readFile(filename);
			for (SlotPattern p:currentPattern.keySet())
			{
				if (allPatterns.containsKey(p))
				{
					Set<String> existing = allPatterns.get(p);
					existing.addAll(currentPattern.get(p));
					allPatterns.put(p, existing);
				}
				else
				{
					allPatterns.put(p, currentPattern.get(p));
				}
			}
		}
		try
		{
			BufferedWriter buf = new BufferedWriter(new FileWriter(outputFile));
			for (SlotPattern p:allPatterns.keySet())
			{
				buf.write("pattern$" + p.getPattern()+"\n");
				List<Rule> rules = p.getRules();
				buf.write("rule1$" + rules.get(0).edgeType + "$" + rules.get(0).direction + "\n");
				buf.write("rule2$" + rules.get(1).edgeType + "$" + rules.get(1).direction + "\n");
				Set<String> sentences = allPatterns.get(p);
				for (String sentence:sentences)
				{
					buf.write(sentence + "\n");
				}
				buf.write("\n");
			}
			buf.flush();
			buf.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void gatherResults() {
		
		
		NLPUtils nlp = new NLPUtils();
		List<String> relations = new ArrayList<String>();
		IntCounter<SlotPattern> patternWeights = new IntCounter<SlotPattern>();
		Map<SlotPattern,Set<String>> allPatterns = new HashMap<SlotPattern,Set<String>>();
		relations.add("prep_at");
		//relations.add("prep_in");
		List<String> folders = new ArrayList<String>();
		
		for (int j = 23;j<24;j++)
		{
		for (int i = 0;i<24;i++)
		{
			String folderName = String.format("2012-07-%02d-%02d", j,i);
			folders.add(folderName);
		}
		}
		
		//folders.add("2012-07-25-01");
		Map<Entity,String> entityToQueries = new HashMap<Entity,String>();
		List<String> queries = new ArrayList<String>();
		
		for(Entity e:entities) {
			if (e.getEntityType()!=EntityType.PER)
				continue;
			String query = QueryBuilder.buildOrQuery(e.getExpansions());
			entityToQueries.put(e, query);
			queries.add(query);
		}
			
		Map<String,List<TrecTextDocument>> AllTrecDocs = QueryFactory.DoQuery(folders, queries, workingDirectory, entities);
		/*
		for (String q:AllTrecDocs.keySet())
		{
			System.out.println("query: " + q + " Number of results is: " + AllTrecDocs.get(q).size());
		}
		*/
		int numthreads = 16;
		ExecutorService exc = Executors.newFixedThreadPool(numthreads);
		for (Entity e:entities)
		{
			exc.execute(new ParallelNLPParsers(e, AllTrecDocs,nlp,relations, allPatterns));
		}
		exc.shutdown();
		while(true)
		{
			try 
			{
				if (exc.awaitTermination(1, TimeUnit.MINUTES))
						break;
				System.out.println("Waiting in NLPParser");
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
				System.out.println("Waiting in NLPParser - Thread interrupted");
			}
		}
		
		for (SlotPattern pattern:allPatterns.keySet())
		{
			patternWeights.incrementCount(pattern,allPatterns.get(pattern).size());
		}
		
		for (SlotPattern pattern:patternWeights.keySet())
		{
			System.out.println(pattern.toString());
			System.out.println("Sentences: ");
			for (String s:allPatterns.get(pattern))
			{
				System.out.println(s);
			}
			System.out.println("Count: ");
			System.out.println(patternWeights.getCountAsString(pattern));
		}
		
		
		FileUtils.writeFile(allPatterns, outputFile);
		try{
			
			String s3directory = "s3://contactmeetentity/";
			Process p;
	
			String s3PutIndex = String.format("s3cmd put %s %s",outputFile,s3directory);
			System.out.println(s3PutIndex);
			p = Runtime.getRuntime().exec(s3PutIndex);
			p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Writing to S3 failed");
			e.printStackTrace();
		}
		
		
	}
	public List<String> getDisambiguations(String entity) {
		String baseFolder = "data/entities_expanded/";
		List<String> disambiguations = new ArrayList<String>();
		
		try {
			File file = new File(baseFolder + entity + ".expansion");
			System.out.println(file.getAbsolutePath());
			if(file.exists()) {
				System.out.println("file exists");
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = reader.readLine()) != null) {
					disambiguations.add(line.trim());
				}
				reader.close();
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return disambiguations;
	}
	@SuppressWarnings("unchecked")
	public void readEntities() {
		File file = new File(Constants.entitiesSerilizedFile);
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		else {
			String fileName = "data/entities.csv";
			entities = new ArrayList<Entity>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] splits = line.split("\",\"");
					EntityType type = splits[1].replace("\"", "").equals("PER") ? EntityType.PER : (splits[1].equals("ORG") ? EntityType.ORG : EntityType.FAC);
					String name = splits[0].replace("\"", "").replace("http://en.wikipedia.org/wiki/", "").replace("https://twitter.com/", "");
					Entity entity = new Entity(splits[0].replace("\"", ""), name, type, splits[2].replace("\"", ""),
							Utils.getEquivalents(splits[3].replace("\"", "")), getDisambiguations(name));
					entities.add(entity);
				}
				reader.close();
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
			FileUtils.writeFile(entities, Constants.entitiesSerilizedFile);
		}
		/* for(Entity e:entities) {
			System.out.println(e.getName());
			System.out.println(e.getTargetID());
			System.out.println(e.getGroup());
			System.out.println(e.getEntityType());
			System.out.println(e.getExpansions());
			System.out.println(e.getDisambiguations());
		} */
	}
	
	
	private static class ParallelNLPParsers implements Runnable{
		Map<SlotPattern,Set<String>> allPatterns;
		List<TrecTextDocument> trecDocs = new ArrayList<TrecTextDocument>();
		Entity ent;
		NLPUtils nlp;
		List<String> relations;
		public ParallelNLPParsers(Entity e, Map<String,List<TrecTextDocument>> queryResults, NLPUtils nlpin, 
				List<String> relIn, Map<SlotPattern,Set<String>> out)
		{
			allPatterns = out;
			ent = e;
			nlp = nlpin;
			relations = relIn;
			String query = QueryBuilder.buildOrQuery(e.getExpansions());
			if (queryResults.containsKey(query))
				trecDocs = queryResults.get(query);
		}
		
		private synchronized void addToPatternMap(Map<SlotPattern,List<String>> patterns)
		{
			for (SlotPattern pattern:patterns.keySet())
			{
				if (allPatterns.containsKey(pattern))
				{
					Set<String> existing = allPatterns.get(pattern);
					existing.addAll(patterns.get(pattern));
					allPatterns.put(pattern,existing);
				}
				else
				{
					allPatterns.put(pattern, new HashSet<String>(patterns.get(pattern)));
				}
			}
		}
		
		public void run()
		{
			Set<String> uniqueSentences = new HashSet<String>();
			Map<String, Set<String>> expansionToSentences = new HashMap<String,Set<String>>();
			if (!trecDocs.isEmpty())
			{
				for(String expansion:ent.getExpansions()) 
				{	
					expansionToSentences.put(expansion, new HashSet<String>());
					List<String> sentences  = ProcessTrecTextDocument.getCleanedSentences(ProcessTrecTextDocument.extractRelevantSentences(trecDocs, expansion));
					for (String sentence:sentences)
					{
						if (uniqueSentences.contains(sentence))
							continue;
						//if (sentence.contains(" at "))
							//System.out.println("sentence: " + sentence);
						uniqueSentences.add(sentence);
						Set<String> currentExpansionSet = expansionToSentences.get(expansion);
						currentExpansionSet.add(sentence);
						expansionToSentences.put(expansion,currentExpansionSet);
						
					}
				}
				//System.out.println("Query: " + ent.getExpansions().get(0) + " Size of results: " + uniqueSentences.size());
				for (String expansion:expansionToSentences.keySet())
				{
					//System.out.println("Expansion: " + expansion);
					Set<String> currentExpansionSet = expansionToSentences.get(expansion);
					for (String sentence:currentExpansionSet)
					{
						Map<SlotPattern,List<String>> patterns = nlp.findSlotPatternGivenEntityAndRelation(sentence, expansion, relations);
						addToPatternMap(patterns);	
					}
				}
			}
		}
	}
}

