package retrieWin.PatternBuilder;


import java.io.File;

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
		relations.add("prep_at");
		
		for(Entity e:entities) {
			if (e.getEntityType()!=EntityType.PER)
				continue;
			System.out.println("Processing Entity: " + e.getName());
			String query = QueryBuilder.buildOrQuery(e.getExpansions());
            //System.out.println("Querying for: " + query); =
			List<String> folders = new ArrayList<String>();
			List<String> queries = new ArrayList<String>();
			folders.add("2012-05-05-05");
			queries.add(query);
			Set<TrecTextDocument> trecDocs = QueryFactory.DoQuery(folders, queries, workingDirectory, entities, null);
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
				
				for (String expansion:expansionToSentences.keySet())
				{
					System.out.println("Expansion: " + expansion);
					Set<String> currentExpansionSet = expansionToSentences.get(expansion);
					for (String sentence:currentExpansionSet)
					{
						System.out.println("Full sentence: " + sentence);
						List<SlotPattern> patterns = nlp.findSlotPatternGivenEntityAndRelation(sentence, expansion, relations);
						for (SlotPattern pattern:patterns)
						{
							patternWeights.incrementCount(pattern);
						}
						
					}
				}
			}
		}
		for (SlotPattern pattern:patternWeights.keySet())
		{
			System.out.println(pattern.toString());
			System.out.println(patternWeights.getCountAsString(pattern));
		}
	}
	
	@SuppressWarnings("unchecked")
	public void readEntities() {
		File file = new File(Constants.entitiesSerilizedFile);
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
	}
}

