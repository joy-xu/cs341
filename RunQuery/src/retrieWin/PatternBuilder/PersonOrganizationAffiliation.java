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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;

import retrieWin.Querying.ExecuteQuery;
import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.PriorityQueue;
import retrieWin.Utils.Utils;
import retrieWin.SSF.Constants.EdgeDirection;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.PatternType;
import retrieWin.SSF.SlotPattern.Rule;
import retrieWin.Utils.FileUtils;

import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class PersonOrganizationAffiliation implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="output file") public String outputFile;
	@Option(gloss="index location") public String indexLocation;
	List<Entity> entities;
	public static void main(String[] args) {
		Execution.run(args, "Main", new PersonOrganizationAffiliation());
	}
	
	public PersonOrganizationAffiliation(){
		readEntities();
	}

	@Override
	public void run() {
		LogInfo.begin_track("run()");
		//runBootstrap();
		if (!workingDirectory.endsWith("/"))
			workingDirectory = workingDirectory + "/";
		//gatherResults();
		
		List<String> filenames = new ArrayList<String>();
		filenames.add("perOrg_20120721");
		filenames.add("perOrg_20120722");
		filenames.add("perOrg_20120723");
		filenames.add("perOrg_20120725");
		//aggregateResults(filenames);
		//writeHumanReadableResults(filenames);
		List<SlotPattern> p = populateBootstrapSlotPatternsTopMembers();
		writeSlotPatternsToFile(outputFile, p);
		//doBootStrap();
		LogInfo.end_track();
	}
	
	
	public void writeSlotPatternsToFile(String filename, List<SlotPattern> patternsToWrite)
	{
		try{
			BufferedWriter wbuf = new BufferedWriter(new FileWriter(filename));
			for (SlotPattern p:patternsToWrite)
			{
				wbuf.write(p.toString());
				wbuf.newLine();
			}
			wbuf.close();
		}
		catch (Exception e)
		{
			System.out.println("Couldn't write pattern to file");
			e.printStackTrace();
		}
	}
	
	public List<SlotPattern> populateBootstrapSlotPatterns()
	{
		List<SlotPattern> result = new ArrayList<SlotPattern>();
		Rule r1 = new Rule();
		r1.edgeType = "nsubj";
		r1.direction = EdgeDirection.Out;
		Rule r2 = new Rule();
		r2.edgeType = "prep_at";
		r2.direction = EdgeDirection.Out;
		List<Rule> rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		
		// work, nsubj, prep-at
		SlotPattern p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setPattern("work");
		p.setRules(rules);
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// employ, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("employ");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// teacher, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("teacher");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// coach, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("coach");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// principal, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("principal");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// employee of, member of, affiliated to, works with, works at, plays for, 
		
		// play for
		r1 = new Rule();
		r1.edgeType = "nsubj";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep_for";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("play");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// affiliate, nsubjpass, prep_of
		r1 = new Rule();
		r1.edgeType = "nsubj";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep_of";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("affiliate");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		//manager of
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("manager");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		r1 = new Rule();
		r1.edgeType = "nsubjpass";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep_to";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		// affiliated to, nsubjpass, prep_to
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("affiliate");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// associated with
		r1 = new Rule();
		r1.edgeType = "nsubjpass";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep_with";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		// affiliated to, nsubjpass, prep_to
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("associate");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		return result;
	}
	
	public List<SlotPattern> populateBootstrapSlotPatternsTopMembers()
	{
		List<SlotPattern> result = new ArrayList<SlotPattern>();
		Rule r1 = new Rule();
		r1.edgeType = "nn";
		r1.direction = EdgeDirection.Out;
		Rule r2 = new Rule();
		r2.edgeType = "nn";
		r2.direction = EdgeDirection.Out;
		List<Rule> rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		
		// work, nsubj, prep-at
		SlotPattern p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setPattern("chairman");
		p.setRules(rules);
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		// employ, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("cto");
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		// teacher, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("ceo");
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		// coach, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("cfo");
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("officer");
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("president");
		p.setPatternType(PatternType.TargetInBetween);
		result.add(p);
		
		
		r1 = new Rule();
		r1.edgeType = "appos";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep_of";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setPattern("chairman");
		p.setRules(rules);
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// employ, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("cto");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// teacher, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("ceo");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		// coach, nsubj, prep_at
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("cfo");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("officer");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("president");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		
		r1 = new Rule();
		r1.edgeType = "poss";
		r1.direction = EdgeDirection.Out;
		r2 = new Rule();
		r2.edgeType = "prep-of";
		r2.direction = EdgeDirection.Out;
		rules = new ArrayList<Rule>();
		rules.add(r1);	rules.add(r2);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("vice-president");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		p = new SlotPattern();
		p.setConfidenceScore(1.0);
		p.setRules(rules);
		p.setPattern("vp");
		p.setPatternType(PatternType.WordInBetween);
		result.add(p);
		
		
		// employee of, member of, affiliated to, works with, works at, plays for, 
		
	
		
		return result;
	}
	
	public void doBootStrap()
	{
		List<SlotPattern> patterns = populateBootstrapSlotPatterns();
		Map<Pair<String,String>,Set<String>> results = new HashMap<Pair<String,String>,Set<String>>();
		NLPUtils nlp = new NLPUtils();
		ExecuteQuery eq = new ExecuteQuery(indexLocation);
		int numResults = 100;
		Map<SlotPattern,List<String>> patternToStringMap = new HashMap<SlotPattern,List<String>>();
		for (SlotPattern p:patterns)
		{
			System.out.println("Starting query for : " + p.getPattern());
			String query = String.format("#1(%s)", p.getPattern());
			System.out.println("Query String: " + query);
			List<TrecTextDocument> trecDocs = eq.executeQuery(query,numResults, workingDirectory);
			List<String> sentences = ProcessTrecTextDocument.getCleanedSentences(
					ProcessTrecTextDocument.extractRelevantSentences(trecDocs, p.getPattern()));
			patternToStringMap.put(p, sentences);
			System.out.println("Done querying for: " + p.getPattern());
		}
		
		//NLPUtils nlp = new NLPUtils();
		int totalCount = 0;
		Map<SlotPattern, Integer> patternCounts = new HashMap<SlotPattern, Integer>();
		for (SlotPattern p:patternToStringMap.keySet())
		{
			int counter = 0;
			int numthreads = 16;
			ExecutorService exc = Executors.newFixedThreadPool(numthreads);
			
			List<Future<Map<Pair<String,String>,List<String>>>> futuresList = new ArrayList<Future<Map<Pair<String,String>,List<String>>>>();
			
			
			for (String sentence:patternToStringMap.get(p))
			{
				Callable<Map<Pair<String,String>,List<String>>> c = new parallelBootstrapper(sentence,p,nlp);
				Future<Map<Pair<String,String>,List<String>>> s = exc.submit(c);
				futuresList.add(s);
			}
		
			for (Future<Map<Pair<String,String>,List<String>>> f:futuresList)
			{
				Map<Pair<String,String>,List<String>> thisResult = new HashMap<Pair<String,String>,List<String>>();
				try
				{
					thisResult = f.get();
				}
				catch (Exception excep)
				{
					excep.printStackTrace();
				}
				for (Pair<String,String> pr:thisResult.keySet())
				{
					counter += thisResult.get(pr).size();
					if (results.containsKey(pr))
					{
						Set<String> existing = results.get(pr);
						existing.addAll(thisResult.get(pr));
						results.put(pr,existing);
					}
					else
					{
						results.put(pr, new HashSet<String>(thisResult.get(pr)));
					}
				}
			}
			exc.shutdown();
			totalCount += counter;
			patternCounts.put(p, counter);
		}
		
		try{
			BufferedWriter wbuf = new BufferedWriter(new FileWriter("affiliate_per_org"));
			for (SlotPattern p:patternCounts.keySet())
			{
				p.setConfidenceScore(((double)patternCounts.get(p))/totalCount);
				wbuf.write(p.toString());
				wbuf.newLine();
			}
			wbuf.close();
		}
		catch (Exception e)
		{
			System.out.println("Could not write pattern to file");
			e.printStackTrace();
		}
		
		PriorityQueue<Pair<String,String>> resultsQueue = new PriorityQueue<Pair<String,String>>();
		for (Pair<String,String> pr:results.keySet())
		{
			resultsQueue.add(pr,(double)(results.get(pr).size()));
		}
		
		try
		{
			BufferedWriter buf = new BufferedWriter(new FileWriter(outputFile));
			while(!resultsQueue.isEmpty())
			{
				Pair<String,String> pr = resultsQueue.next();
				Set<String> sentences = results.get(pr);
				String pairLine = String.format("%s:%s:%d", pr.first,pr.second,sentences.size());
				buf.write(pairLine);
				buf.newLine();
				for (String s:sentences)
				{
					String sentenceLine = String.format("$%s", s);
					buf.write(sentenceLine);
					buf.newLine();
				}
				buf.newLine();
			}
			
			buf.close();
		}
		catch (Exception e)
		{
			System.out.println("Failed writing to file");
			e.printStackTrace();
		}
	}
	
private static class parallelBootstrapper implements Callable<Map<Pair<String,String>,List<String>>>{
		
		String sentence;
		SlotPattern pattern;
		
		NLPUtils nlp;
		public parallelBootstrapper(String sen, SlotPattern pin, NLPUtils nlpin)
		{
			nlp = nlpin;
			sentence = sen;
			pattern = pin;
		}
		
		@Override
		public Map<Pair<String,String>,List<String>> call() throws Exception {

			return nlp.getTwoSidesForPatternWord(sentence, pattern);
		}
	}


	public void aggregateResults(List<String> filenames)
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
		
		IntCounter<SlotPattern> patternWeights = new IntCounter<SlotPattern>();
		Map<SlotPattern,Set<String>> allPatterns = new HashMap<SlotPattern,Set<String>>();
		
		List<String> folders = new ArrayList<String>();
		
		for (int j = 25;j<26;j++)
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
			
		Map<String,List<TrecTextDocument>> AllTrecDocs = QueryFactory.DoQuery(folders, queries, workingDirectory, entities,null);
		/*
		for (String q:AllTrecDocs.keySet())
		{
			System.out.println("query: " + q + " Number of results is: " + AllTrecDocs.get(q).size());
		}
		*/
		int numthreads = 16;
		ExecutorService exc = Executors.newFixedThreadPool(numthreads);
		
		List<Future<Map<SlotPattern,Set<String>>>> futuresList = new ArrayList<Future<Map<SlotPattern,Set<String>>>>();
		for (Entity e:entities)
		{
			Callable<Map<SlotPattern,Set<String>>> c = new ParallelNLPParsers(e, AllTrecDocs,nlp);
			Future<Map<SlotPattern,Set<String>>> s = exc.submit(c);
			futuresList.add(s);
		}
		for (Future<Map<SlotPattern,Set<String>>> f:futuresList)
		{
			Map<SlotPattern,Set<String>> thisResult = new HashMap<SlotPattern,Set<String>>();
			try{
				thisResult = f.get();
			}
			catch (Exception excep)
			{
				excep.printStackTrace();
			}
			for (SlotPattern p:thisResult.keySet())
			{
				if (allPatterns.containsKey(p))
				{
					Set<String> existing = allPatterns.get(p);
					existing.addAll(thisResult.get(p));
					allPatterns.put(p,existing);
				}
				else
				{
					allPatterns.put(p, thisResult.get(p));
				}
			}
		}
		
		exc.shutdown();

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
		String baseFolder = "data/entities_expanded_new/";
		List<String> disambiguations = new ArrayList<String>();
		
		try {
			File file = new File(baseFolder + entity);
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
			// Read from expansions file
			Map<String,Set<String>> namesToExpansions = new HashMap<String,Set<String>>();
			String expFileName = "data/entities_expansions_new";
			try{
				BufferedReader buf = new BufferedReader(new FileReader(expFileName));
				String line;
				while((line = buf.readLine())!=null)
				{
					String[] tok = line.split(":");
					String name = tok[0];
					Set<String> expansions = new HashSet<String>();
					String[] dollahSep = tok[1].split("\\$");
					for (int j = 0;j<dollahSep.length;j++)
					{
						if (dollahSep[j].length() <= 0) continue;
						expansions.add(dollahSep[j].toLowerCase());
					}
					namesToExpansions.put(name,expansions);
				}
				buf.close();
			}
			catch (Exception e)
			{
				System.out.println("Entities expansion file not present");
				e.printStackTrace();
			}
			String fileName = "data/entities.csv";
			entities = new ArrayList<Entity>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] splits = line.split("\",\"");
					EntityType type = splits[1].replace("\"", "").equals("PER") ? EntityType.PER : (splits[1].equals("ORG") ? EntityType.ORG : EntityType.FAC);
					String name = splits[0].replace("\"", "").replace("http://en.wikipedia.org/wiki/", "").replace("https://twitter.com/", "");
					//List<String> equivalents = Utils.getEquivalents(splits[3].replace("\"", ""));
					List<String> equivalents = new ArrayList<String>(namesToExpansions.get(name));
					Entity entity = new Entity(splits[0].replace("\"", ""), name, type, splits[2].replace("\"", ""),
							equivalents, getDisambiguations(name));
					entities.add(entity);
				}
				reader.close();
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
			FileUtils.writeFile(entities, Constants.entitiesSerilizedFile);
		}
	}
	
	
	private static class ParallelNLPParsers implements Callable<Map<SlotPattern,Set<String>>>{
		
		List<TrecTextDocument> trecDocs = new ArrayList<TrecTextDocument>();
		Entity ent;
		NLPUtils nlp;
		
		public ParallelNLPParsers(Entity e, Map<String,List<TrecTextDocument>> queryResults, NLPUtils nlpin)
		{
			ent = e;
			nlp = nlpin;
			
			String query = QueryBuilder.buildOrQuery(e.getExpansions());
			if (queryResults.containsKey(query))
				trecDocs = queryResults.get(query);
		}
		
		private void addToPatternMap(Map<SlotPattern,Set<String>> allPatterns, Map<SlotPattern,List<String>> patterns)
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
		
		@Override
		public Map<SlotPattern,Set<String>> call() throws Exception {

			Set<String> uniqueSentences = new HashSet<String>();
			Map<String, Set<String>> expansionToSentences = new HashMap<String,Set<String>>();
			Map<SlotPattern,Set<String>> allPatterns = new HashMap<SlotPattern,Set<String>>();
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
					int numthreads = 16;
					ExecutorService exc = Executors.newFixedThreadPool(numthreads);
					
					List<Future<Map<SlotPattern,List<String>>>> futuresList = new ArrayList<Future<Map<SlotPattern,List<String>>>>();
					Map<SlotPattern,List<String>> thisThreadResults = new HashMap<SlotPattern,List<String>>();
					//System.out.println("Expansion: " + expansion);
					Set<String> currentExpansionSet = expansionToSentences.get(expansion);
					for (String sentence:currentExpansionSet)
					{
						Callable<Map<SlotPattern,List<String>>> c = new NLPSentenceParser(sentence,expansion,nlp);
						Future<Map<SlotPattern,List<String>>> s = exc.submit(c);
						futuresList.add(s);
					}
					for (Future<Map<SlotPattern,List<String>>> f:futuresList)
					{
						Map<SlotPattern,List<String>> thisResult = new HashMap<SlotPattern,List<String>>();
						try
						{
							thisResult = f.get();
						}
						catch (Exception excep)
						{
							excep.printStackTrace();
						}
						for (SlotPattern p:thisResult.keySet())
						{
							if (thisThreadResults.containsKey(p))
							{
								List<String> existing = thisThreadResults.get(p);
								existing.addAll(thisResult.get(p));
								thisThreadResults.put(p,existing);
							}
							else
							{
								thisThreadResults.put(p, thisResult.get(p));
							}
						}
					}
					exc.shutdown();
					addToPatternMap(allPatterns,thisThreadResults);
				}
			}
			return allPatterns;
		}
	}

	
private static class NLPSentenceParser implements Callable<Map<SlotPattern,List<String>>>{
		
		String sentence;
		String expansion;
		
		NLPUtils nlp;
		public NLPSentenceParser(String sen, String exp, NLPUtils nlpin)
		{
			nlp = nlpin;
			sentence = sen;
			expansion = exp;
		}
		
		@Override
		public Map<SlotPattern,List<String>> call() throws Exception {

			return nlp.findRelationToOrganization(sentence, expansion);
		}
	}
}



