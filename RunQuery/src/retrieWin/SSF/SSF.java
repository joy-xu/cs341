package retrieWin.SSF;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import fig.basic.*;
import edu.stanford.nlp.util.Pair;
import fig.exec.Execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrieWin.Indexer.Indexer;
import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.ThriftReader;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.Indexer.Indexer.parallelQuerier;
import retrieWin.Querying.ExecuteQuery;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;
import retrieWin.SSF.SlotPattern.Rule;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;

public class SSF implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="download Hour") public String downloadHour;
	@Option(gloss="index Location") public String indexLocation;
	@Option(gloss="index Location") public String saveAsDirectory;
	private List<Slot> slots;
	private List<Entity> entities;
	private NLPUtils coreNLP;
	Concept conceptExtractor;
	public static StanfordCoreNLP processor;
	public SSF() {
		initialize();
	}
	
	public void initialize() {
		readEntities();
		readSlots();
		
		setCoreNLP(new NLPUtils());
		conceptExtractor = new Concept();
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner, dcoref");
		processor = new StanfordCoreNLP(props, false);
		
	}
	
	@SuppressWarnings("unchecked")
	public void readEntities() {

		File file = new File(Constants.entitiesSerilizedFile);
		if(file.exists()) {
			setEntities((List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString()));
			System.out.println("Read data for " + getEntities().size() + " entities from serialized file");
		}
		else {
			
			// Read from expansions file
			System.out.println("entities file does not exist");
			Map<String,Set<String>> namesToExpansions = new HashMap<String,Set<String>>();
			String expFileName = "data/entities_expansions";
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
			setEntities(new ArrayList<Entity>());
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] splits = line.split("\",\"");
					EntityType type = splits[1].replace("\"", "").equals("PER") ? EntityType.PER : (splits[1].equals("ORG") ? EntityType.ORG : EntityType.FAC);
					String name = splits[0].replace("\"", "").replace("http://en.wikipedia.org/wiki/", "").replace("https://twitter.com/", "");
					List<String> equivalents = new ArrayList<String>(namesToExpansions.get(name));
					Entity entity = new Entity(splits[0].replace("\"", ""), name, type, splits[2].replace("\"", ""),
							equivalents, getDisambiguations(name));
					getEntities().add(entity);
				}
				reader.close();	
				FileUtils.writeFile(getEntities(), Constants.entitiesSerilizedFile);
				System.out.println("New entities file created");
			}
			catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(ex.getMessage());
			}
			FileUtils.writeFile(entities, Constants.entitiesSerilizedFile);
			/*for(Entity en:entities) {
				System.out.println(en.getTargetID() + ":" + en.getDisambiguations());
			}*/
		}
	}
	
	public List<String> getDisambiguations(String entity) {
		String baseFolder = "data/entities_expanded_updated/";
		List<String> disambiguations = new ArrayList<String>();
		
		try {
			File file = new File(baseFolder + entity);
			if(file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = reader.readLine()) != null) {
					disambiguations.add(line.trim());
				}
				reader.close();
			}
			else 
				System.out.println("disambiguation file for " + entity + "does not exist");	
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return disambiguations;
	}
	
	@SuppressWarnings("unchecked")
	public void readSlots() {
		File file = new File(Constants.slotsSerializedFile);
		if(file.exists()) {
			setSlots((List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString()));
		}
		else {
			try {
			createSlots();
			setSlots((List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString()));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean containsUppercaseToken(String str) {
		for(String token: str.split(" ")) {
			if(token.isEmpty())
				continue;
			if(Character.isUpperCase(token.charAt(0)))
					return true;
		}
		return false;
	}
	
	public static Map<String, Pair<Set<String>, Double>> findTitles(Entity entity, Slot slot, Map<String, Map<String, Set<String>>> relevantSentences, NLPUtils coreNLP, Concept conceptExtractor) {
		Map<String, Pair<Set<String>, Double>> candidates = new HashMap<String, Pair<Set<String>, Double>>();
		for(String expansion: entity.getExpansions()) {
			List<String> expansionTokens = Arrays.asList(expansion.split(" "));
			for(String sent: relevantSentences.get(expansion).keySet()) {
				System.out.println("Processing for titles: " + sent);
				Annotation document = new Annotation(sent);
				processor.annotate(document);
				Map<Integer, Set<Integer>> corefsEntity1 = coreNLP.getCorefs(document, expansion);
				List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
				for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
					CoreMap sentenceMap = allSentenceMap.get(sentNum);
					String sentence = sentenceMap.toString();
					List<IndexedWord> entityWords = coreNLP.findWordsInSemanticGraph(sentenceMap, expansion, corefsEntity1.get(sentNum));
					boolean flag = coreNLP.doesExpandedEntityMatch(entity, entityWords);
					
					if(!flag)
						continue;
					
					try {
						//check for any non-pronominal coreference
						for(String tit: coreNLP.getCorefs(sentence, expansion)) {
							Map<String, String> nerMap = coreNLP.createNERMap(sentenceMap);
							String title = "";
							for(String tok: tit.split(" ")) {
								if(nerMap.containsKey(tok))
									if(nerMap.get(tok).equals(NERType.O))
										title += "tok" + " ";
							}
							title.trim();
							if(containsUppercaseToken(title)) {
								String[] titleTokens = title.split(" ");
								boolean dontAdd = false;
								for (String t:titleTokens)
								{
									System.out.println("Entity expansion is: " + expansion);
									System.out.println("Title token is: " + t);
								
									if (expansionTokens.contains(t))
										dontAdd = true;
								}
								if(!dontAdd) {
									if(!candidates.containsKey(title)) {
										Set<String> documents = new HashSet<String>(relevantSentences.get(expansion).get(sentence));
										candidates.put(title, new Pair<Set<String>, Double>(documents, (double)relevantSentences.get(expansion).get(sentence).size()));
									}
									else {
										Pair<Set<String>, Double> setAndScore = candidates.get(title);
										for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
											setAndScore.first().add(sentenceID);
										}
										setAndScore.setSecond(setAndScore.second() + relevantSentences.get(expansion).get(sentence).size());
										candidates.put(title, setAndScore);
									}
								}
							}
						}

					/*
					//check for any compund nouns for this entity
					String nnTitle = coreNLP.getNNs(sentence, expansion);
					if(containsUppercaseToken(nnTitle)) {
						String[] titleTokens = nnTitle.split(" ");
						boolean dontAdd = false;
						for (String t:titleTokens)
						{
							System.out.println("Entity expansion is: " + expansion);
							System.out.println("Title token is: " + t);
						
							if (expansionTokens.contains(t))
								dontAdd = true;
						}
						if (!dontAdd)
						{
					*/
						
						//check for any compound nouns for this entity
						String nnTit = coreNLP.getNNs(sentence, expansion);
						Map<String, String> nerMap = coreNLP.createNERMap(sentenceMap);
						String nnTitle = "";
						for(String tok: nnTit.split(" ")) {
							if(nerMap.containsKey(tok))
								if(nerMap.get(tok).equals(NERType.O))
									nnTitle += "tok" + " ";
						}
						nnTitle.trim();
						if(containsUppercaseToken(nnTitle)) {

							if(!candidates.containsKey(nnTitle)) {
								Set<String> documents = new HashSet<String>(relevantSentences.get(expansion).get(sentence));
								candidates.put(nnTitle, new Pair<Set<String>, Double>(documents, 1.0));
							}
							else {
								Pair<Set<String>, Double> setAndScore = candidates.get(nnTitle);
								for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
									setAndScore.first().add(sentenceID);
								}
								setAndScore.setSecond(setAndScore.second() + 1);

								candidates.put(nnTitle, setAndScore);

							}
						}
					} catch(NoSuchParseException e) {
						e.printStackTrace();
						break;
					}
				}
			}
		}
		return candidates;
	}

	private static Map<String, Pair<Set<String>, Double>> findContactMeetPlaceTime(Entity entity, Slot slot, Map<String, Map<String, Set<String>>> relevantSentences, NLPUtils coreNLP, Concept conceptExtractor) {
		Map<String, Pair<Set<String>, Double>> candidates = new HashMap<String, Pair<Set<String>, Double>>();
		for(String expansion: entity.getExpansions()) {
			for(String sentence: relevantSentences.get(expansion).keySet()) {
				//System.out.println(relevantSentences.get(expansion).get(sentence) + ":" + sentence);
				//System.out.println("Now processing: " + sentence);
				try {
					String docType = "social";
					String docId="";
					int sentIndex = 0;
					for(String id: relevantSentences.get(expansion).get(sentence)) {
						String temp = id.substring(0, id.lastIndexOf("__"));
						if(id.contains("news")) {
							docType = "news";
							break;
						}
						else if(id.contains("arxiv")) {
							docType = "arxiv";
							docId = temp;
							break;
						}
					}
					//for each sentence, find possible slot values and add to candidate list
					//arxiv documents
					if(docType.equals("arxiv")) {
						continue;
					}
					//social documents
						
					else{
						Map<String, Double> values= null;
						if(docType.equals("social")) {
							
							values = coreNLP.findPlaceTimeValue(sentence, expansion, slot, entity, false);
						}
						//News documents	
						else {
							
							values = coreNLP.findPlaceTimeValue(sentence, expansion, slot, entity, false);
						}
						for(String str: values.keySet()) {
							if(!candidates.containsKey(str)){
								//candidates.put(str, values.get(str));
								Set<String> documents = new HashSet<String>(relevantSentences.get(expansion).get(sentence));
								//for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
								//	documents.add(sentenceID);
								//}
								candidates.put(str, new Pair<Set<String>, Double>(documents, values.get(str) * relevantSentences.get(expansion).get(sentence).size()));
							}
							else {
								Pair<Set<String>, Double> setAndScore = candidates.get(str);
								for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
									setAndScore.first().add(sentenceID);
								}
								setAndScore.setSecond(setAndScore.second() + values.get(str) * relevantSentences.get(expansion).get(sentence).size());
								candidates.put(str, setAndScore);
							}
						}
					}
				} catch(NoSuchParseException e) {
					e.printStackTrace();
					break;
				}
			}
		}
		return candidates;

	}
	
	public Map<String, Pair<Set<String>, Double>> findCandidates(Entity entity, Slot slot, Map<String, Map<String, Set<String>>> relevantSentences, NLPUtils coreNLP, Concept conceptExtractor) {
		if(slot.getName().equals(Constants.SlotName.Titles))
			return findTitles(entity, slot, relevantSentences, coreNLP, conceptExtractor);
		
		if (slot.getName().equals(Constants.SlotName.Contact_Meet_PlaceTime))
			return findContactMeetPlaceTime(entity,slot,relevantSentences,coreNLP,conceptExtractor);
		
		Map<String, Pair<Set<String>, Double>> candidates = new HashMap<String, Pair<Set<String>, Double>>();
		String defaultVal = "";
		
		for(String expansion: entity.getExpansions()) {
			for(String sentence: relevantSentences.get(expansion).keySet()) {
				System.out.println("Entity: " + entity.getName() + " Slot: " + slot.getName() + " Full sentence: " + sentence);
				Annotation document = new Annotation(sentence);
				processor.annotate(document);
				//String streamID = null, folderName = null;
				//System.out.println(relevantSentences.get(expansion).get(sentence) + ":" + sentence);
				//String[] split = relevantSentences.get(expansion).get(sentence).split("__");
				//defaultVal = split[split.length-1];
				String docType = "social";
				String docId="";
				//int sentIndex = 0;
				for(String id: relevantSentences.get(expansion).get(sentence)) {
					//System.out.println("ID from findCandidates:" + id);
					String temp = id.substring(0, id.lastIndexOf("__"));
					defaultVal = temp.substring(temp.lastIndexOf("__") + 2).replace("/","");
					//System.out.println(temp);
					//System.out.println(defaultVal);
					//System.out.println(sentence);
					String streamID = null, folderID = null;
					streamID = id.substring(0, id.indexOf("__"));
					String tempFolderID = id.substring(0, id.lastIndexOf("__"));
					folderID = tempFolderID.substring(tempFolderID.lastIndexOf("__") + 2).replace("/","");
					
					//System.out.println(id + "\n");
					//System.out.println(streamID);
					//System.out.println(folderID + "\n\n");
					
					if(id.contains("news")) {
						docType = "news";
						break;
					}
					else if(id.contains("arxiv")) {
						docType = "arxiv";
						docId = temp;
						break;
					}
				}
				
				try {
					//for each sentence, find possible slot values and add to candidate list
					//arxiv documents
					//System.out.println("Now processing: " + sentence);
					if(docType.equals("arxiv")) {
						if(!slot.getName().equals(Constants.SlotName.AssociateOf) || !entity.getEntityType().equals(Constants.EntityType.PER))
							continue;
						arxivDocument arxivDoc = new arxivDocument(docId, workingDirectory);
						List<String> arxivCandidates = new ArrayList<String>();
						if(arxivDoc.getAuthors().contains(expansion)) {
							for(String author: arxivDoc.getAuthors()) 
								if(!author.equals(expansion))
									arxivCandidates.add(author);
									//candidates.put(author, 1.0);
							for(String ack: arxivDoc.getAcknowledgements())
								arxivCandidates.add(ack);
								//candidates.put(ack, 1.0);
						}
						else if(arxivDoc.getAcknowledgements().contains(expansion)) {
							for(String author: arxivDoc.getAuthors())
								arxivCandidates.add(author);
								//candidates.put(author, 1.0);
						}
						
						for(List<String> reference: arxivDoc.getReferences()) {
							if(reference.contains(expansion)) {
								for(String author: reference) 
									if(!author.equals(expansion))
										arxivCandidates.add(author);
										//candidates.put(author, 1.0);
							}
						}
						for(String arxivCandidate: arxivCandidates) {
							if(!NLPUtils.isEntitiesSame(entity.getName().replace("_", " "), arxivCandidate) && !arxivCandidate.equals(entity.getName().replace("_", " "))) {
								if(!candidates.containsKey(arxivCandidate)) {
									Set<String> documents = new HashSet<String>(relevantSentences.get(expansion).get(sentence));
									candidates.put(arxivCandidate, new Pair<Set<String>, Double>(documents, (double)documents.size()));
								}
								else {
									Pair<Set<String>, Double> setAndScore = candidates.get(arxivCandidate);
									for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
										setAndScore.first().add(sentenceID);
									}
									setAndScore.setSecond(setAndScore.second() + relevantSentences.get(expansion).get(sentence).size());
									candidates.put(arxivCandidate, setAndScore);
								}
							}
						}
					}
					else {
						//social documents
						Map<String, Double> values = null;
						if(docType.equals("social")) {
							values = coreNLP.findSlotValue(document, expansion, slot, (!slot.getTargetNERTypes().contains(NERType.NONE)) ? true : false, defaultVal, entity);
						}
						//news documents
						else {
							values = coreNLP.findSlotValue(document, expansion, slot, false, defaultVal, entity);
						}
						for(String str: values.keySet()) {
							if(!NLPUtils.isEntitiesSame(entity.getName().replace("_", " "), str) && !str.equals(entity.getName().replace("_", " "))) {
								if(!candidates.containsKey(str)){
									Set<String> documents = new HashSet<String>();
									for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
										documents.add(sentenceID);
									}
									LogInfo.logs(String.format("Entity    :%s\nExpansion :%s\nSentence :%s\nSlot      :%s\nValue     :%s\n\n",
															entity.getName(), expansion, sentence, slot.getName().toString(), str));
									candidates.put(str, new Pair<Set<String>, Double>(documents, values.get(str) * relevantSentences.get(expansion).get(sentence).size()));
								}
								else {
									Pair<Set<String>, Double> setAndScore = candidates.get(str);
									for(String sentenceID:relevantSentences.get(expansion).get(sentence)) {
										setAndScore.first().add(sentenceID);
									}
									setAndScore.setSecond(setAndScore.second() + values.get(str) * relevantSentences.get(expansion).get(sentence).size());
									candidates.put(str, setAndScore);
								}
							}
						}
					}
				} catch(NoSuchParseException e) {
					e.printStackTrace();
					break;
				}
			}
		}
		return candidates;
	}
	
	public void runSSF(String timestamp) {
		// create index for the current hour
		System.out.println("Creating index...");
		String[] splits = timestamp.split("-");
		String currentFolder = workingDirectory;
		for (int i = 0;i<4;i++)
		{
			currentFolder = currentFolder + splits[i] + "/";
			File f = new File(currentFolder);
			if (!f.exists())
				f.mkdir();
		}
		String year = splits[0],month = splits[1], day = splits[2], hour = splits[3];
		
		String baseFolder = String.format("%s/%s/%s/%s/",year,month,day,hour);
		String indexLocation = workingDirectory + baseFolder + "index/";
		String tempDirectory = workingDirectory + baseFolder + "temp/";
		String trecTextSerializedFile = workingDirectory + baseFolder + "filteredSerialized.ser";
		// if the directory does not exist, create it
		File baseDir = new File(baseFolder);
		if (!baseDir.exists())
			baseDir.mkdirs();

		Indexer.createIndex(timestamp,baseFolder, tempDirectory, indexLocation, trecTextSerializedFile, getEntities());
		
		ExecuteQuery eq = new ExecuteQuery(indexLocation,trecTextSerializedFile);		
		// read in existing information for entities 
		System.out.println("Reading entities...");
		readEntities();
		

		
		// read in slot information 
		System.out.println("Reading slots...");
		readSlots();
		//System.out.println(slots);
		
		// for each entity, for each slot, for each entity expansion
		System.out.println("Finding slot values...");
		
		ExecutorService e = Executors.newFixedThreadPool(8);
		OutputWriter writer = new OutputWriter(timestamp + ".txt");
		
		for(Entity entity: getEntities()) {
			e.execute(new FillSlotForEntity(entity,timestamp,getEntities(),getCoreNLP(),conceptExtractor,workingDirectory, getSlots(), eq, writer));
		}
		e.shutdown();
		while(true)
		{
			try {
				if (e.awaitTermination(1, TimeUnit.MINUTES))
					break;
				System.out.println("Waiting");
			}
			catch(InterruptedException ie){
				System.out.println("Waiting - Thread interrupted");
			}
		}
		writer.Close();
	}
	
private class FillSlotForEntity implements Runnable{
		
		Entity entity;
		String timestamp;
		List<Entity> entities;
		NLPUtils nlp;
		Concept conceptExtractor;
		List<Slot> allSlots;
		String workingDirectory;
		ExecuteQuery eq;
		OutputWriter writer;
		public FillSlotForEntity(Entity en, String tm, List<Entity> listEntities, NLPUtils nlpIn, Concept cin, String wd, List<Slot> slotInput
				, ExecuteQuery eqIn, OutputWriter writerIn)
		{
			entity = en;
			timestamp = tm;
			entities = listEntities;
			nlp = nlpIn;
			conceptExtractor = cin;
			allSlots = slotInput;
			workingDirectory = wd;
			eq = eqIn;
			writer = writerIn;
		}
		
		@Override
		public void run(){

			//System.out.println("Finding slot values for entity " + entity.getName());
			//get all relevant documents for the entity

			List<TrecTextDocument> docs = entity.getRelevantDocuments(timestamp, workingDirectory, entities, eq);
			//System.out.println("Retrieved " + docs.size() + " relevant documents for entity: " + entity.getName());
			if(docs.isEmpty())
				return;
			
			//get relevant sentences for each expansion, ensure no sentence retrieved twice
			Map<String, Map<String, Set<String>>> relevantSentences = new HashMap<String, Map<String, Set<String>>>();
			Set<String> retrievedSentences = new HashSet<String>();
			for(String expansion: entity.getExpansions()) {
				Map<String, Set<String>> sentences = new HashMap<String, Set<String>>();
				boolean phraseQuery;
				if (entity.getEntityType().equals(Constants.EntityType.PER))
					phraseQuery = false;
				else
					phraseQuery = true;
				if (entity.getName().equals("Fargo_Moorhead_Derby_Girls"))
					phraseQuery = true;
				
				Map<String, Set<String>> returnedSet = ProcessTrecTextDocument.extractRelevantSentencesWithDocID(docs, expansion, phraseQuery);
				for(String sent: returnedSet.keySet()) 
					if(!retrievedSentences.contains(sent)) {
							sentences.put(sent, returnedSet.get(sent));
							retrievedSentences.add(sent);
					}
				relevantSentences.put(expansion, sentences);
			}
			//System.out.println("Number of relevant sentences: " + retrievedSentences.size() + " for entity: " + entity.getName());
			
			//iterate to fill slots
			Map<Slot, List<String>> slotToValues = new HashMap<Slot,List<String>>();
			for(Slot slot: allSlots) {
				//compute only for relevant slots for this entity

				if(!slot.getEntityType().equals(entity.getEntityType()))
						continue;

				

				boolean filter = (slot.getName().equals(Constants.SlotName.Contact_Meet_Entity) || 
						slot.getName().equals(Constants.SlotName.Contact_Meet_PlaceTime) || 
						(slot.getName().equals(Constants.SlotName.Affiliate)));
				//if (!filter)
					//	continue;
				/*boolean aff_per = (slot.getName().equals(Constants.SlotName.Affiliate) && slot.getEntityType().equals(Constants.EntityType.PER));
				if (!aff_per)
>>>>>>> c72ad48beb9e693828889b151b7deeff536d37eb
					{
						continue;
					}
				 */
				//TODO: remove this, computing only one slot right now
				if(slot.getPatterns() !=null && slot.getPatterns().isEmpty())
					continue;
				//System.out.println("In slot " + slot.getName());
				//System.out.println(slot);
				//System.out.println("Finding value for " + slot.getName() + " for entity: " + entity.getName());
				//for each expansion, slot pattern, get all possible candidates
				Map<String, Pair<Set<String>, Double>> candidates = findCandidates(entity, slot, relevantSentences, nlp, conceptExtractor);
				Map<String, Pair<Set<String>, Double>> normalizedMappings = new HashMap<String, Pair<Set<String>, Double>>();
				//remove candidates below the threshold value
				for(String slotValue:candidates.keySet()) {
					System.out.println("slotvalue found was: " + slotValue);
					String concept = conceptExtractor.getConcept(slotValue);
					System.out.println("Concept: " + concept);
					if(!normalizedMappings.containsKey(concept))
						normalizedMappings.put(concept, new Pair<Set<String>, Double>(new HashSet<String>(), 0.0));
					Pair<Set<String>, Double> pair = normalizedMappings.get(concept);
					pair.first().add(slotValue);
					pair.setSecond(pair.second() + candidates.get(slotValue).second());
					
				}
				List<String> finalCandidateList = new ArrayList<String>();
				for(String key: normalizedMappings.keySet()) 
					if(candidates.get(key).second() > slot.getThreshold()) 
						finalCandidateList.add(key);
				
				for(String finalCandidateNormalized:finalCandidateList) {
					for(String finalCandidateUnNormalized:normalizedMappings.get(finalCandidateNormalized).first()) {
						for(String id:candidates.get(finalCandidateUnNormalized).first()) {
							String streamID = null, folderID = null;
							streamID = id.substring(0, id.indexOf("__"));
							String tempFolderID = id.substring(0, id.lastIndexOf("__"));
							folderID = tempFolderID.substring(tempFolderID.lastIndexOf("__") + 2).replace("/","");
							Pair<Long, Long> offset = Utils.getByteOffset(id, finalCandidateUnNormalized, workingDirectory);
							writer.Write(streamID, entity.getTargetID(), (double)500, folderID, slot.getName().toString(), finalCandidateNormalized, offset.first(), offset.second());
						}
					}
				}
				//updating slots
				slotToValues.put(slot, finalCandidateList);
				//System.out.println(entity.getName() + "," + slot.getName() + ":" + entity.updateSlot(slot, finalCandidateList));
			}
			
			// Auto populating subset slots
			for (Slot slot:allSlots)
			{
				if (slotToValues.containsKey(slot) && !slotToValues.get(slot).isEmpty())
				{
					List<Constants.SlotName> alsoPopulate = slot.getPopulateSet();
					if (alsoPopulate == null)
					{
						System.out.println("Also populate is null for slot: " + slot.getName().toString());
						continue;
					}
					for (Slot slot2 : allSlots)
					{
						if (alsoPopulate.contains(slot2.getName()) && slot2.getEntityType().equals(slot.getEntityType()))
						{
							for (String found : slotToValues.get(slot))
								if (!slotToValues.get(slot2).contains(found))
									slotToValues.get(slot2).add(found);
						}
					}
				}
			}
			for (Slot slot:allSlots)
				if (slotToValues.containsKey(slot))
					System.out.println(entity.getName() + "," + slot.getName() + ":" + entity.updateSlot(slot, slotToValues.get(slot)));
		}
	}
	
	
	void doTesting()
	{
		 SSF ssf = new SSF();
		 for(Slot slot: ssf.getSlots()) {

		 if(!slot.getName().equals(Constants.SlotName.Contact_Meet_PlaceTime))
		 continue;
		 System.out.println(ssf.getCoreNLP().findSlotValue(null, "", slot, false, null, null));

		 }
	}
	
	void buildLargeIndex() {
		List<String> downloadHours = new ArrayList<String>();
		for(int i = 19; i <= 19; i++) {
			for(int j=14; j < 16; j++) {
				String downloadHour = String.format("2011-12-%02d-%02d", i, j);
				downloadHours.add(downloadHour);
			}
		}
		Indexer.createUnfilteredIndex(downloadHours, workingDirectory, saveAsDirectory, indexLocation);
	}
	
	private void createSlots() throws IOException {
		//readSlots();
		List<Slot> slots = new ArrayList<Slot>();
		Slot slot;
		String filename;
		File file;
		
		//PER slots
		//Affiliate
		// Also populate - None
		List<Constants.SlotName> emptySlotNameSet = new ArrayList<Constants.SlotName>();
		slot = new Slot();
		slot.setName(SlotName.Affiliate);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//AssociateOf
		// Also Populate - None
		slot = new Slot();
		slot.setName(SlotName.AssociateOf);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.PERSON));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//Contact_Meet_PlaceTime
		// Also populate - None
		slot = new Slot();
		slot.setName(SlotName.Contact_Meet_PlaceTime);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//AwardsWon
		// Also populate - None
		slot = new Slot();
		slot.setName(SlotName.AwardsWon);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//DateOfDeath
		// Also populate - None
		slot = new Slot();
		slot.setName(SlotName.DateOfDeath);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.DATE, NERType.TIME));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//CauseOfDeath
		// Also populate - None
		slot = new Slot();
		slot.setName(SlotName.CauseOfDeath);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//Titles
		slot = new Slot();
		// Also populate - None
		slot.setName(SlotName.Titles);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//FounderOf
		// Also populate - Affiliate_PER
		List<Constants.SlotName> populateSet = new ArrayList<Constants.SlotName>();
		populateSet.add(Constants.SlotName.Affiliate);
		slot = new Slot();
		slot.setName(SlotName.FounderOf);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.ORGANIZATION));
		slot.addPopulateSets(populateSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//EmployeeOf
		// Also populate - Affiliate_Per
		slot = new Slot();
		slot.setName(SlotName.EmployeeOf);
		slot.setEntityType(EntityType.PER);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.ORGANIZATION));
		slot.addPopulateSets(populateSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//FAC Slots
		//Affiliate
		// Also populate - None, Need to add employee of patterns to this file (inverted)
		slot = new Slot();
		slot.setName(SlotName.Affiliate);
		slot.setEntityType(EntityType.FAC);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.PERSON,NERType.ORGANIZATION));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//Contact_Meet_Entity
		// Also populate - None
		slot = new Slot();
		slot.setName(SlotName.Contact_Meet_Entity);
		slot.setEntityType(EntityType.FAC);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.NONE));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);	
		
		//ORG Slots
		//Affiliate
		// Also populate - None, Need to add employee of (inverted)
		slot = new Slot();
		slot.setName(SlotName.Affiliate);
		slot.setEntityType(EntityType.ORG);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.PERSON,NERType.ORGANIZATION));
		slot.addPopulateSets(emptySlotNameSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//TopMembers
		//Also populate - Affiliate_ORF
		slot = new Slot();
		slot.setName(SlotName.TopMembers);
		slot.setEntityType(EntityType.ORG);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.PERSON));
		slot.addPopulateSets(populateSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//FoundedBy
		//Also populate - Affiliate_ORG
		populateSet.add(Constants.SlotName.TopMembers);
		slot = new Slot();
		slot.setName(SlotName.FoundedBy);
		slot.setEntityType(EntityType.ORG);
		slot.setThreshold(0.0);
		slot.setSourceNERTypes(null);
		slot.setTargetNERTypes(Arrays.asList(NERType.PERSON));
		slot.addPopulateSets(populateSet);
		filename = "data/slots/" + slot.getName().toString().toLowerCase() + "_" + slot.getEntityType().toString().toLowerCase();
		file = new File(filename);
		if(!file.exists()) 
			System.out.println("File for " + slot.getName() + " not found.");
		else
			slot.addSlotPatterns(filename);
		slots.add(slot);
		
		//System.out.println(slots);
		FileUtils.writeFile(slots, Constants.slotsSerializedFile);
	}
	
	public static void main(String[] args) throws IOException {
		if (!System.getenv().containsKey("LD_LIBRARY_PATH"))
		{
			System.out.println("Environment variable not set");
			return;
		}

		//System.out.println("Took "+(endTime - startTime) + " ns"); 
		//new SSF().createSlots();
		Execution.run(args, "Main", new SSF());
		//SSF s= new SSF();
	
		//new SSF().updateSlots();
		//Execution.run(args, "Main", new SSF());
		//SSF s= new SSF();
		//new SSF().doTesting();
		//Execution.run(args, "Main", new SSF());
	}

	@Override
	public void run() {
		boolean terminate = false;
		if(workingDirectory == null || workingDirectory.isEmpty()) {
			LogInfo.logs("Working directory cannot be empty. Set the -workingDirectory option.");
			terminate = true;
		}
		if(downloadHour == null || downloadHour.isEmpty()) {
			LogInfo.logs("Download hour cannot be empty. Set the -downloadHour option.");
			terminate = true;
		}
		if(terminate) {
			LogInfo.logs("Terminating execution!");
			return;
		}
		if(workingDirectory!=null && !workingDirectory.endsWith("/"))
			workingDirectory += "/";
		
		if(indexLocation!=null && !indexLocation.endsWith("/"))
			indexLocation += "/";
		
		if(saveAsDirectory !=null && !saveAsDirectory.endsWith("/"))
			saveAsDirectory += "/";
		
		LogInfo.begin_track("run()");
		LogInfo.logs(String.format("Download hour     : %s", downloadHour));
		LogInfo.logs(String.format("Working directory : %s", workingDirectory));
		
		
		List<String> folders = new ArrayList<String>();
		for(int d = 1; d <= 1; d++) {
			for(int i = 1 ;i <= 23; i++)
				folders.add(String.format("%04d-%02d-%02d-%02d", 2011,11,d,i));
		}
		
		for(String folderName:folders) {
			System.out.println("Running SSF for " + folderName);
			runSSF(folderName);
		}

		//buildLargeIndex();
		System.out.println("All Done!");
		
		LogInfo.end_track();
	}

	public List<Slot> getSlots() {
		return slots;
	}

	public void setSlots(List<Slot> slots) {
		/*
		for (Slot s:slots)
		{
			if (s.getName() == Constants.SlotName.Contact_Meet_Place_Time)
			{
				for (SlotPattern p:s.getPatterns())
					System.out.println(p.toString());
			}
		}
		*/
		this.slots = slots;
	}

	public NLPUtils getCoreNLP() {
		return coreNLP;
	}

	public void setCoreNLP(NLPUtils coreNLP) {
		this.coreNLP = coreNLP;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}
}
