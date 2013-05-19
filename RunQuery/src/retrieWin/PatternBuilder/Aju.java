package retrieWin.PatternBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.Querying.ExecuteQuery;
import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;

import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class Aju implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="index Location") public String indexLocation;
	List<Entity> entities;
	public static void main(String[] args) {
		Execution.run(args, "Main", new Aju());
	}
	
	public Aju(){
		readEntities();
	}

	@Override
	public void run() {
		LogInfo.begin_track("run()");
		//runBootstrapForPair();
		runBootStrapforEntityAndNER();
	
		LogInfo.end_track();
	}
	
	public void runBootStrapforEntityAndNER() {
		ExecuteQuery eq = new ExecuteQuery(indexLocation);
		NLPUtils utils = new NLPUtils();
		//utils.extractPERRelation("The time has come to reassess to impact of former Presiding Justices Aharon Barak and Dorit Beinisch on Human Rights, the justice system, and the rule of law in the State of Israel.");
		List<String> folders = new ArrayList<String>();
		for(int i=0;i<24;i++)
			folders.add(String.format("%04d-%02d-%02d-%02d", 2012,6,1,i));
		
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
		
		for(Entity e:entities) {
			if(e.getEntityType()==EntityType.PER) {
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
						sentences  = ProcessTrecTextDocument.getCleanedSentences(sentences);
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
						//System.out.println("Expansion: " + expansion);
						Set<String> currentExpansionSet = expansionToSentences.get(expansion);
						for (String sentence:currentExpansionSet)
						{
							//System.out.println("Full sentence: " + sentence);
							utils.extractPERRelation(sentence, expansion);
							/*for (SlotPattern pattern:patterns.keySet())
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
							}*/
							
						}
					}
				}
			}
		}
		
		
	}
		
	public void runBootstrapForPair() {
		List<Pair<String,String>> bootstrapList = getBootstrapInput("src/seedSet/slot_Founded_by");
		NLPUtils utils = new NLPUtils();

		HashMap<SlotPattern, Double> weights = new HashMap<SlotPattern,Double>();
		for(Pair<String, String> pair:bootstrapList) {
			ExecuteQuery eq = new ExecuteQuery(indexLocation);
			LogInfo.logs("Start querying");
			List<TrecTextDocument> trecDocs = eq.executeQuery(QueryBuilder.buildUnorderedQuery(pair.first, pair.second, 10), 1000, workingDirectory);
			IntCounter<SlotPattern> individualWeights = new IntCounter<SlotPattern>();
			for(String str:ProcessTrecTextDocument.extractRelevantSentences(trecDocs, pair.first, pair.second)) {
				LogInfo.logs(str);
				List<SlotPattern> patterns = utils.findSlotPattern(str, pair.first, pair.second);
				for(SlotPattern pattern:patterns) {
					individualWeights.incrementCount(pattern);
				}
			}
			
			double total = individualWeights.totalCount();
			LogInfo.logs("\n\nIndividual counts for " + pair);
			for(SlotPattern pattern:individualWeights.keySet()) {
				LogInfo.logs(pattern + ":" + individualWeights.getCount(pattern));
				if(!weights.containsKey(pattern)) {
					weights.put(pattern, 0.0);
				}
				weights.put(pattern, weights.get(pattern) + individualWeights.getCount(pattern) / total);
			}
		}
		
		LogInfo.logs("\n\nDone finding patterns\n");
		for(SlotPattern pattern:weights.keySet()) {
			LogInfo.logs(pattern + ":" + weights.get(pattern));
		}
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
	
	@SuppressWarnings("unchecked")
	public void readEntities() {
		File file = new File(Constants.entitiesSerilizedFile);
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
	}
}
