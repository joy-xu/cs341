package retrieWin.PatternBuilder;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;

import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.Utils.FileUtils;

import edu.stanford.nlp.stats.IntCounter;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class ContactMeetPlaceTime implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;

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
		gatherResults();
	
		LogInfo.end_track();
	}
	
	public void gatherResults() {
		
		
		NLPUtils nlp = new NLPUtils();
		List<String> relations = new ArrayList<String>();
		IntCounter<SlotPattern> patternWeights = new IntCounter<SlotPattern>();
		Map<SlotPattern,List<String>> allPatterns = new HashMap<SlotPattern,List<String>>();
		relations.add("prep_at");
		//	relations.add("prep_in");
		List<String> folders = new ArrayList<String>();
		/*
		for (int i = 0;i<24;i++)
		{
			String folderName = String.format("2012-07-25-%02d", i);
			folders.add(folderName);
		}
		*/
		folders.add("2012-07-25-00");
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
		for (Entity e:entities)
		{
			String query = QueryBuilder.buildOrQuery(e.getExpansions());
			if (!AllTrecDocs.containsKey(query))
				continue;
			List<TrecTextDocument> trecDocs = AllTrecDocs.get(query);
			
			Set<String> uniqueSentences = new HashSet<String>();
			Map<String, Set<String>> expansionToSentences = new HashMap<String,Set<String>>();
			if (!trecDocs.isEmpty())
			{
				for(String expansion:e.getExpansions()) {
					
					expansionToSentences.put(expansion, new HashSet<String>());
					List<String> sentences  = ProcessTrecTextDocument.extractRelevantSentences(trecDocs, expansion);
					for (String sentence:sentences)
					{
						if (uniqueSentences.contains(sentence))
							continue;
						uniqueSentences.add(sentence);
						Set<String> currentExpansionSet = expansionToSentences.get(expansion);
						currentExpansionSet.add(sentence);
						expansionToSentences.put(expansion,currentExpansionSet);
						
					}
				}
				System.out.println("Query: " + query + " Size of results: " + uniqueSentences.size());
				for (String expansion:expansionToSentences.keySet())
				{
					//System.out.println("Expansion: " + expansion);
					Set<String> currentExpansionSet = expansionToSentences.get(expansion);
					for (String sentence:currentExpansionSet)
					{
						
						Map<SlotPattern,List<String>> patterns = nlp.findSlotPatternGivenEntityAndRelation(sentence, expansion, relations);
						for (SlotPattern pattern:patterns.keySet())
						{
							patternWeights.incrementCount(pattern,patterns.get(pattern).size());
							if (allPatterns.containsKey(pattern))
							{
								List<String> existing = allPatterns.get(pattern);
								existing.addAll(patterns.get(pattern));
								allPatterns.put(pattern,existing);
							}
							else
							{
								allPatterns.put(pattern, patterns.get(pattern));
							}
						}
						
					}
				}
			}
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
}

