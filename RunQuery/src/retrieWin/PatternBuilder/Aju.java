package retrieWin.PatternBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.Querying.ExecuteQuery;
import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Entity;
import retrieWin.SSF.OutputWriter;
import retrieWin.SSF.Slot;
import retrieWin.SSF.SlotPattern;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class Aju implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="index Location") public String indexLocation;
	@Option(gloss="number of Results") public int numResults;
	List<Entity> entities;
	private List<Slot> slots;
	static StanfordCoreNLP processor;
	
	public static void main(String[] args) {
		//System.out.println("Co-founder".toLowerCase().replaceAll("[^a-z]", ""));
		Execution.run(args, "Main", new Aju());
	}
	
	public Aju(){
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner, dcoref");
		processor = new StanfordCoreNLP(props, false);
		readEntities();
		readSlots();
	}

	public void readSlots() {
		File file = new File(Constants.slotsSerializedFile);
		if(file.exists()) {
			slots = (List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
	}
	
	@Override
	public void run() {
		LogInfo.begin_track("run()");
		LogInfo.logs("Working directory  : " + workingDirectory);
		LogInfo.logs("Index location     : " + indexLocation);
		LogInfo.logs("Number of results  : " + numResults);
		
		extractSlotValue();
		
		//testFindSlotValue();
		
		//obj.findSlotPattern("Bill Gates' neighbor Steve Jobs complained that his dog was too loud.", "Bill Gates", "Steve Jobs");
		//obj.findSlotPattern("Oldest Oscar Winner Meryl Streep Adds Sense of History With Best Actress Oscar Scarlett Johansson Lands Hitchcock Movie", "Meryl Streep", "Oscar");
		//The movie showcases this enigmatic lady's personal demons, her struggle with dementia and her family relationships through Meryl Streep 's Oscar winning performance.
		
		//runBootstrapForPair();
		//runBootStrapforEntityAndNER();
	
		LogInfo.end_track();
	}
	
	private void testFindSlotValue() {
		NLPUtils obj = new NLPUtils();
		//obj.findSlotPattern("Bill Gates company Microsoft is the largest employer.", "Bill Gates", "Microsoft");
		
		//List<NERType> nerTags = new ArrayList<NERType>();
		//nerTags.add(NERType.);
		//Slot founded_by = null;

		OutputWriter writer = new OutputWriter("run.txt");
		for(Slot slot: slots) {
			if(slot.getName().equals(Constants.SlotName.FoundedBy)) {
				List<String> sentences = new ArrayList<String>();
				sentences.add("Seagram Company founder Bill Gates visited the Memorial Auditorium on last Monday.");
				
				for(String sentence:sentences) {
					Annotation document = new Annotation(sentence);
					processor.annotate(document);
					Map<String, Double> values = obj.findSlotValue(document, "Seagram Company", slot, false, "");
					LogInfo.logs("Sentence    : " + sentence);
					if(values != null && values.size() > 0) {
						for(String str:values.keySet()) {
							LogInfo.logs("Founded by  : " + str);
							writer.Write("1317997776-5197c3a02c98ab21e3a93eedca52599d", "Aharon_Barak", 666, "2010-02-04-05", slot.getName().toString(), str, (long)10, (long)15);
						} 
					}
					else {
						LogInfo.logs("Founded by  : NO RESULTS");
					}
				}
			}
			if(slot.getName().equals(Constants.SlotName.AwardsWon)) {
				List<String> sentences = new ArrayList<String>();
				sentences.add("Pulitzer prize winner Bill Gates visited the Memorial Auditorium on last Monday.");
				for(String sentence:sentences) {
					Annotation document = new Annotation(sentence);
					processor.annotate(document);
					Map<String, Double> values = obj.findSlotValue(document, "Bill Gates", slot, false, "");
					LogInfo.logs("Sentence    : " + sentence);
					if(values != null && values.size() > 0) {
						for(String str:values.keySet()) {
							
							LogInfo.logs("Award won   : " + str);
							writer.Write("1317997776-5197c3a02c98ab21e3a93eedca52599d", "Aharon_Barak", 666, "2010-02-04-05", slot.getName().toString(), str, (long)10, (long)15);
						} 
					}
					else {
						LogInfo.logs("Award won   : NO RESULTS");
					}
				}
			}
			if(slot.getName().equals(Constants.SlotName.FounderOf)) {
				List<String> sentences = new ArrayList<String>();
				sentences.add("Seagram Company founder Bill Gates visited the Memorial Auditorium on last Monday.");
				sentences.add("Seagram Company Ltd. co-founder Bill Gates worked with his friend Steve Jobs.");
				for(String sentence:sentences) {
					Annotation document = new Annotation(sentence);
					processor.annotate(document);
					Map<String, Double> values = obj.findSlotValue(document, "Bill Gates", slot, false, "");
					LogInfo.logs("Sentence    : " + sentence);
					if(values != null && values.size() > 0) {
						for(String str:values.keySet()) {
							LogInfo.logs("Founder of  : " + str);
							writer.Write("1317997776-5197c3a02c98ab21e3a93eedca52599d", "Aharon_Barak", 666, "2010-02-04-05", slot.getName().toString(), str, (long)10, (long)15);
						} 
					}
					else {
						LogInfo.logs("Founder of  : NO RESULTS");
					}
				}
			}
			if(slot.getName().equals(Constants.SlotName.Affiliate) && slot.getEntityType() == EntityType.PER) {
				List<String> sentences = new ArrayList<String>();
				sentences.add("Seagram founder Bill Gates worked with his friend Steve Jobs on last Monday.");
				for(String sentence:sentences) {
					Annotation document = new Annotation(sentence);
					processor.annotate(document);
					Map<String, Double> values = obj.findSlotValue(document, "Bill Gates", slot, false, "");
					LogInfo.logs("Sentence    : " + sentence);
					if(values != null && values.size() > 0) {
						for(String str:values.keySet()) {
							
							LogInfo.logs("Affiliate of: " + str);
							writer.Write("1317997776-5197c3a02c98ab21e3a93eedca52599d", "Aharon_Barak", 666, "2010-02-04-05", slot.getName().toString(), str, (long)10, (long)15);
						} 
					}
					else {
						LogInfo.logs("Affiliate of: NO RESULTS");
					}
				}
			}
		}
		writer.Close(); 
		
	}

	public void runBootStrapforEntityAndNER() {
		ExecuteQuery eq = new ExecuteQuery(indexLocation);
		NLPUtils utils = new NLPUtils();
		//utils.extractPERRelation("The time has come to reassess to impact of former Presiding Justices Aharon Barak and Dorit Beinisch on Human Rights, the justice system, and the rule of law in the State of Israel.");
		List<String> folders = new ArrayList<String>();
		for(int d = 1; d< 11; d++) {
			for(int i=0;i<24;i++)
				folders.add(String.format("%04d-%02d-%02d-%02d", 2012,6,d,i));
		}
		
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
		HashMap<SlotPattern, Double> weights = new HashMap<SlotPattern,Double>();
		IntCounter<SlotPattern> numAppearances = new IntCounter<SlotPattern>();
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

						List<String> cleanedSentences = ProcessTrecTextDocument.getCleanedSentences(ProcessTrecTextDocument.extractRelevantSentences(trecDocs, expansion));
						cleanedSentences  = ProcessTrecTextDocument.getCleanedSentences(cleanedSentences);
						for (String sentence:cleanedSentences)
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
							//List<SlotPattern> patterns = 
							numAppearances.addAll(utils.extractPERRelation(sentence, expansion));
							//System.out.println("--------------" + patterns);
							//for(SlotPattern pattern:patterns) {
							//	numAppearances.incrementCount(pattern);
							//}
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
		LogInfo.logs("\n\nDone finding patterns\n");
		double total = numAppearances.totalCount();
		for(SlotPattern key:numAppearances.keySet()) {
			weights.put(key, numAppearances.getCount(key) / total);
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
			
			for(SlotPattern key:weights.keySet()) {
				key.setConfidenceScore(numAppearances.getCount(key) / total);
				weights.put(key, numAppearances.getCount(key) / total);
				if(weights.get(key) > 0) {
					LogInfo.logs(key + " : " + weights.get(key));
					writer.write(key + " : " + weights.get(key) + "\n");
				}
			}
			writer.close();
		}
		catch(Exception ex) {
			LogInfo.logs(ex.getMessage());
		}
	}
		
	public void runBootstrapForPair() {
		List<Pair<String,String>> bootstrapList = getBootstrapInput("src/seedSet/slot_Founded_by");
		NLPUtils utils = new NLPUtils();

		HashMap<SlotPattern, Double> weights = new HashMap<SlotPattern,Double>(), minWeights = new HashMap<SlotPattern,Double>();
		IntCounter<SlotPattern> numAppearances = new IntCounter<SlotPattern>();
		for(Pair<String, String> pair:bootstrapList) {
			ExecuteQuery eq = new ExecuteQuery(indexLocation);
			LogInfo.logs("Start querying " + pair);
			List<TrecTextDocument> trecDocs = eq.executeQuery(QueryBuilder.buildUnorderedQuery(pair.first, pair.second, 10), numResults, workingDirectory);
			IntCounter<SlotPattern> individualWeights = new IntCounter<SlotPattern>();
			List<String> cleanedSentences = ProcessTrecTextDocument.getCleanedSentences(ProcessTrecTextDocument.extractRelevantSentences(trecDocs, pair.first, pair.second));
			for(String str:cleanedSentences) {
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
				double currentWt = individualWeights.getCount(pattern) / total;
				
				numAppearances.incrementCount(pattern);
				if(!weights.containsKey(pattern)) {
					weights.put(pattern, 0.0);
					minWeights.put(pattern, (double)Integer.MAX_VALUE);
				}
				weights.put(pattern, weights.get(pattern) + currentWt);
				if(minWeights.get(pattern) > currentWt)
					minWeights.put(pattern, currentWt);
			}
		}
		
		try {
			LogInfo.logs("\n\nDone finding patterns\n");
			BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
			//for(SlotPattern pattern:weights.keySet()) {
			//	LogInfo.logs(numAppearances.getCount(pattern) + ":" +  pattern + ":" + weights.get(pattern));
			//}

			for(SlotPattern key:weights.keySet()) {
				//double score = (weights.get(key) - minWeights.get(key) ) * (numAppearances.getCount(key) -1) ;
				double score = (weights.get(key) ) * (numAppearances.getCount(key)) ;
				key.setConfidenceScore(score);
				weights.put(key, score );
				//totalNormalizedCounts.put(key, (totalNormalizedCounts.get(key) ) *  numAppearances.getCount(key));
				if(weights.get(key) > 0) {
					LogInfo.logs(key );
					writer.write(key + "\n");
				}
			}
			writer.close();
		}
		catch(Exception ex) {
			LogInfo.logs(ex.getMessage());
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
	
	public void extractSlotValue() {
		ExecuteQuery eq = new ExecuteQuery(indexLocation);
		NLPUtils utils = new NLPUtils();
		//utils.extractPERRelation("The time has come to reassess to impact of former Presiding Justices Aharon Barak and Dorit Beinisch on Human Rights, the justice system, and the rule of law in the State of Israel.");
		List<String> folders = new ArrayList<String>();
		for(int d = 6; d <= 15; d++) {
			for(int i = 0; i < 24; i++)
				folders.add(String.format("%04d-%02d-%02d-%02d", 2012,4,d,i));
		}
		
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
		//HashMap<SlotPattern, Double> weights = new HashMap<SlotPattern,Double>();
		//IntCounter<SlotPattern> numAppearances = new IntCounter<SlotPattern>();
		NLPUtils obj = new NLPUtils();
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

						List<String> cleanedSentences = ProcessTrecTextDocument.getCleanedSentences(ProcessTrecTextDocument.extractRelevantSentences(trecDocs, expansion));
						//cleanedSentences  = ProcessTrecTextDocument.getCleanedSentences(cleanedSentences);
						for (String sentence:cleanedSentences)
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
						Set<String> currentExpansionSet = expansionToSentences.get(expansion);
						for (String sentence:currentExpansionSet) {
							if(sentence.contains(expansion)) {
								//LogInfo.logs(expansion + ":" + sentence);
								for(Slot slot: slots) {
									if(slot.getName().equals(Constants.SlotName.FoundedBy) && 
											e.getEntityType() == EntityType.ORG) {
										Annotation document = new Annotation(sentence);
										processor.annotate(document);
										Map<String, Double> values = obj.findSlotValue(document, expansion, slot, false, "");
										LogInfo.logs("Sentence   $$ " + sentence);
										if(values != null && values.size() > 0) {
											for(String str:values.keySet()) {
												LogInfo.logs("__Founded by '" + expansion +"': " + str);
											} 
										}
										else {
											LogInfo.logs("Founded by   '" + expansion +"': NO RESULTS");
										}
									}
									if(slot.getName().equals(Constants.SlotName.AwardsWon)  && 
											e.getEntityType() == EntityType.PER) {
										Annotation document = new Annotation(sentence);
										processor.annotate(document);
										Map<String, Double> values = obj.findSlotValue(document, expansion, slot, false, "");
										LogInfo.logs("Sentence   $$ " + sentence);
										if(values != null && values.size() > 0) {
											for(String str:values.keySet()) {
												
												LogInfo.logs("__Award won    '" + expansion +"': " + str);
											} 
										}
										else {
											LogInfo.logs("Award won    '" + expansion +"': NO RESULTS");
										}
									}
									if(slot.getName().equals(Constants.SlotName.FounderOf)  && 
											e.getEntityType() == EntityType.PER) {
										Annotation document = new Annotation(sentence);
										processor.annotate(document);
										Map<String, Double> values = obj.findSlotValue(document, expansion, slot, false, "");
										LogInfo.logs("Sentence   $$ " + sentence);
										if(values != null && values.size() > 0) {
											for(String str:values.keySet()) {
												LogInfo.logs("__Founder of   '" + expansion +"': " + str);
											} 
										}
										else {
											LogInfo.logs("Founder of   '" + expansion +"': NO RESULTS");
										}
									}
									if(slot.getName().equals(Constants.SlotName.Affiliate) &&
											slot.getEntityType() == EntityType.PER  && 
											e.getEntityType() == EntityType.PER) {
										Annotation document = new Annotation(sentence);
										processor.annotate(document);
										Map<String, Double> values = obj.findSlotValue(document, expansion, slot, false, "");
										LogInfo.logs("Sentence   $$ " + sentence);
										if(values != null && values.size() > 0) {
											for(String str:values.keySet()) {
												LogInfo.logs("__Affiliate of '" + expansion +"': " + str);
											} 
										}
										else {
											LogInfo.logs("Affiliate of '" + expansion +"': NO RESULTS");
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
