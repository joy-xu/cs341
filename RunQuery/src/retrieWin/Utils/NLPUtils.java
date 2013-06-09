package retrieWin.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import retrieWin.SSF.Constants.EdgeDirection;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.PatternType;
import retrieWin.SSF.Constants.SlotName;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.Slot;
import retrieWin.SSF.SlotPattern;
import retrieWin.SSF.SlotPattern.Rule;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.regex.Pattern;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.DocIDAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;
import fig.basic.LogInfo;

public class NLPUtils {
	AbstractSequenceClassifier<CoreLabel> classifier;
	StanfordCoreNLP processor;
	public NLPUtils() {
		//String serializedClassifier = "lib/english.all.3class.distsim.crf.ser.gz";
		//classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner, dcoref");
		processor = new StanfordCoreNLP(props, false);
	}
	
	public Map<SlotPattern,List<String>> findEntitiesForFacilities(String sentence,String entity, List<String> edgeTypes)
	{
		Map<SlotPattern,List<String>> patterns = new HashMap<SlotPattern,List<String>>();
		try {
			if(sentence.length() > 400)
				return patterns;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			Map<Integer, Set<Integer>> corefsEntity = getCorefs(document, entity);
			
	
			//int sentNum = 0;
			List<CoreMap> allSentenceMaps = document.get(SentencesAnnotation.class);
			for(int sentNum = 0;sentNum < allSentenceMaps.size();sentNum++) {
				CoreMap sentenceMap = allSentenceMaps.get(sentNum);
				String sentenceFromMap = sentenceMap.toString().trim();

				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
				{
					continue;
				}
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				List<IndexedWord> words = findWordsInSemanticGraph(sentenceMap,entity,corefsEntity.get(sentNum));
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if (!words.isEmpty())
				{	
					Set<SemanticGraphEdge> allEdges = graph.getEdgeSet();
					String parentEdge = "";
					IndexedWord parent = new IndexedWord();
					Boolean found = false;
					for (SemanticGraphEdge edge:allEdges)
					{
						if (edgeTypes.contains(edge.toString()) && words.contains(edge.getDependent()))
						{
							parent = edge.getGovernor();
							parentEdge = edge.toString();
							found = true;
							break;
						}
					}
					if (found)
					{
						for (SemanticGraphEdge edge:allEdges)
						{
							if (edge.getGovernor().equals(parent) && !words.contains(edge.getDependent()))
							{
								SlotPattern pattern = new SlotPattern();
								Rule rule1 = new Rule();
								Rule rule2 = new Rule();
								rule1.direction = EdgeDirection.Out;
								rule2.direction = EdgeDirection.Out;
								rule1.edgeType = edge.toString();
								rule2.edgeType = parentEdge;
								pattern.setPattern(parent.lemma().toLowerCase().replaceAll("[^a-z]", ""));
								pattern.setRules(Arrays.asList(rule1, rule2));
								if (patterns.containsKey(pattern))
								{
									List<String> existing = patterns.get(pattern);
									existing.add(sentenceFromMap);
									patterns.put(pattern, existing);
								}
								else
								{
									List<String> newList = new ArrayList<String>();
									newList.add(sentenceFromMap);
									patterns.put(pattern, newList);
								}
							}
						}
					}
				}
			}
		}
		catch(Exception ex) 
		{
			System.out.println("Exception at : " + sentence);
			ex.printStackTrace();
		}
			
		return patterns;
	
	}
	
	public Map<SlotPattern,List<String>> findRelationToOrganization(String sentence,String entity)
	{
		Map<SlotPattern,List<String>> patterns = new HashMap<SlotPattern,List<String>>();
		try {
			if(sentence.length() > 400)
				return patterns;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			Map<Integer, Set<Integer>> corefsEntity = getCorefs(document, entity);
			
			
			List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
			for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
				CoreMap sentenceMap = allSentenceMap.get(sentNum);
				String sentenceFromMap = sentenceMap.toString().trim();

				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
				{
					continue;
				}
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				List<IndexedWord> words = findWordsInSemanticGraph(sentenceMap,entity,corefsEntity.get(sentNum));
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if (words.isEmpty())
				{
					continue;
				}
				List<String> candidates = new ArrayList<String>();
				Map<String,String> nermap = createNERMap(sentenceMap);
				for (String candidateWord:nermap.keySet())
				{
				//	System.out.println("word: " + candidateWord + " ner: " + nermap.get(candidateWord));
					if (nermap.get(candidateWord).equals("ORGANIZATION"))
					{
						candidates.add(candidateWord);
					}
				}
				Set<IndexedWord> allOrgWords = new HashSet<IndexedWord>();
				for (String candidate:candidates)
				{
					List<IndexedWord> orgWords = findWordsInSemanticGraph(sentenceMap,candidate,null);
					allOrgWords.addAll(orgWords);
				}
				List<IndexedWord> allOrgWordsList = new ArrayList<IndexedWord>(allOrgWords);
				List<SlotPattern> currentPatterns = findRelation(sentenceMap,words,allOrgWordsList);
				for (SlotPattern p :currentPatterns)
				{
					if (patterns.containsKey(p))
					{
						List<String> s = patterns.get(p);
						s.add(sentenceFromMap);
						patterns.put(p, s);
					}
					else
					{
						List<String> s = new ArrayList<String>();
						s.add(sentenceFromMap);
						patterns.put(p, s);
					}
				}		
			}
		}
		catch(Exception ex) 
		{
			System.out.println("Exception at : " + sentence);
			ex.printStackTrace();
		}
			
		return patterns;
	
	}
	
	public Map<SlotPattern,List<String>> findSlotPatternGivenEntityAndRelation(String sentence, String entity, List<String> edgeTypes)
	{		
		String deAccented = sentence;

		Map<SlotPattern,List<String>> patterns = new HashMap<SlotPattern,List<String>>();
		try {
			if(deAccented.length() > 400)
				return patterns;
			Annotation document = new Annotation(deAccented);
			processor.annotate(document);

			Map<Integer, Set<Integer>> corefsEntity = getCorefs(document, entity);
			
			
			List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
			for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
				CoreMap sentenceMap = allSentenceMap.get(sentNum);
				String sentenceFromMap = sentenceMap.toString().trim();
				
				
				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
				{
					continue;
				}
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);

				
				List<IndexedWord> words = findWordsInSemanticGraph(sentenceMap,entity,corefsEntity.get(sentNum));
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if (!words.isEmpty())
				{	
					/*
					System.out.println("Entity words: ");
					for (IndexedWord word:words)
						System.out.println(word.originalText());
					*/
					List<IndexedWord> placeTimeWords = new ArrayList<IndexedWord>();
					for (String edgeType:edgeTypes)
						placeTimeWords.addAll(findWordsInSemanticGraphByEdgeType(sentenceMap,edgeType));
					
					/*
					System.out.println("placeTimeWords: ");
					for (IndexedWord pt : placeTimeWords)
						System.out.println(pt.originalText());
					*/
					
					if (!words.isEmpty() && !placeTimeWords.isEmpty())
					{
						List<SlotPattern> currentPatterns = findRelation(sentenceMap, words, placeTimeWords);
						for (SlotPattern p :currentPatterns)
						{
							if (patterns.containsKey(p))
							{
								List<String> s = patterns.get(p);
								s.add(sentenceFromMap);
								patterns.put(p, s);
							}
							else
							{
								List<String> s = new ArrayList<String>();
								s.add(sentenceFromMap);
								patterns.put(p, s);
							}
						}
					}
				}
			}
		}
		catch(Exception ex) {
			//System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return patterns;
	}

	public List<SlotPattern> findSlotPattern(String sentence, String entity1, String entity2) {
		List<SlotPattern> patterns = new ArrayList<SlotPattern>();
		
		try {
			if(sentence.length() > 400)
				return patterns;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			Map<Integer, Set<Integer>> corefsEntity1 = getCorefs(document, entity1);
			Map<Integer, Set<Integer>> corefsEntity2 = getCorefs(document, entity2);
			
			List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
			for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
				CoreMap sentenceMap = allSentenceMap.get(sentNum);
				String sentenceFromMap = sentenceMap.toString().trim();
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
					continue;
				//SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				patterns.addAll(findRelation(sentenceMap, findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum)), findWordsInSemanticGraph(sentenceMap, entity2, corefsEntity2.get(sentNum))));
				patterns.addAll(findShortRelation(sentenceMap, findWordsInSemanticGraphSimple(sentenceMap, entity1), findWordsInSemanticGraphSimple(sentenceMap, entity2)));
			}
		}
		catch(Exception ex) {
			//System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return patterns;
	}
	
	//TODO - Improve if needed!
	public List<IndexedWord> findWordsInSemanticGraph(CoreMap sentenceMap, String entity, Set<Integer> corefs) {
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<String> entitySplits = Arrays.asList(entity.split(" "));
		List<IndexedWord> words = new ArrayList<IndexedWord>();
		HashSet<String> expandedEntitySplits = new HashSet<String>();
		
		//add corefs if they exist
		if(corefs != null) {
			for(int ind: corefs)
				words.add(graph.getNodeByIndex(ind));
		}
		
		for(String str:entitySplits) {
			String expansion = findExpandedEntity(sentenceMap, str);
			if(expansion!=null && !expansion.isEmpty())
				expandedEntitySplits.addAll(Arrays.asList(expansion.split(" ")));
		}
		for(IndexedWord word: graph.vertexSet()) {
			if(StringUtils.containsIgnoreCase(expandedEntitySplits, word.originalText())) {
				words.add(word);
			}
		}
		//System.out.println(entity + "," + words);
		return words;
	}
	
	public List<IndexedWord> findWordsInSemanticGraphSimple(CoreMap sentenceMap, String entity) {
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<String> entitySplits = Arrays.asList(entity.split(" "));
		List<IndexedWord> words = new ArrayList<IndexedWord>();

		for(IndexedWord word: graph.vertexSet()) {
			if(StringUtils.containsIgnoreCase(entitySplits, word.originalText())) {
				words.add(word);
			}
		}
		return words;
	}
	
	public List<IndexedWord> findWordsInSemanticGraphByEdgeType(CoreMap sentenceMap, String edgeType) {
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		Set<SemanticGraphEdge> allEdges = graph.getEdgeSet();
		List<IndexedWord> candidates = new ArrayList<IndexedWord>();
		for (SemanticGraphEdge edge:allEdges)
		{
			if (edge.toString().equals(edgeType))
				candidates.add(edge.getDependent());
		}
		return candidates;
	}
	
	//TODO - Improve if needed!
	public IndexedWord findWordsInSemanticGraphForSlotPattern(SemanticGraph graph, String pattern) {
		
		String[] split = pattern.split(" ");
		if(split.length == 0)
			return null;
		
		for(IndexedWord word: graph.vertexSet()) {
			if(split[0].compareToIgnoreCase(word.lemma().replaceAll("[^a-z]", "")) == 0) {
				boolean found = true;
				for(int i = 1; i < split.length; i++) {
					try {
						if(split[i].compareToIgnoreCase(graph.getNodeByIndex(word.index() + i).lemma().replaceAll("[^a-z]", "")) != 0) {
							found = false;
							break;
						}
					}
					catch (Exception e) {
						found = false;
						break;
					}
				}
				if(found)
					return word;
			}
		}
		return null;
	}
	
	//   -----------------------
	//   |         -----------  |
	//   V         V          | |
	//Microsoft founder Bill Gates
	List<SlotPattern> findShortRelation(CoreMap map, List<IndexedWord> words1, List<IndexedWord> words2) {
		SemanticGraph graph = map.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<SlotPattern> patterns = new ArrayList<SlotPattern>();
		
		for(IndexedWord w1: words1) {
			for(IndexedWord w2: words2) {
				List<IndexedWord> nodes1to2 = graph.getShortestDirectedPathNodes(w1, w2);
				List<IndexedWord> nodes2to1 = graph.getShortestDirectedPathNodes(w2, w1);

				if(nodes1to2 != null && nodes1to2.size() == 2) {
					//System.out.println("Two sized 1 to 2");
					//System.out.println(graph.getChildList(nodes1to2.get(0)));
					if(graph.getChildList(nodes1to2.get(0)).size() > 1) {
						for(IndexedWord w: graph.getChildList(nodes1to2.get(0))) {
							if(!words1.contains(w) && !words2.contains(w)) {
								String patternWord = w.lemma().toLowerCase().replaceAll("[^a-z]", "");
								if(!patternWord.isEmpty() ) {
									SlotPattern pattern = new SlotPattern();
									pattern.setPattern(patternWord);
		
									//pattern.setPattern(word.originalText());
									//System.out.println(word.originalText() + ":" + word.tag());
									pattern.setPatternType(PatternType.SourceInBetween);
									Rule rule1 = new Rule();
									//Out with respect to the entity
									rule1.direction = EdgeDirection.Out;
									rule1.edgeType = graph.getEdge(w1, w2).getRelation().getShortName();
									
									
									Rule rule2 = new Rule();
									//Out with respect to the entity
									rule2.direction = EdgeDirection.Out;
									rule2.edgeType = graph.getEdge(w1, w).getRelation().getShortName();
									
									pattern.setRules(Arrays.asList(rule1, rule2));
									patterns.add(pattern);
								}
								//System.out.println("Found " + pattern);
							}
						}
					}
					else{
						SlotPattern pattern = new SlotPattern();
						pattern.setPatternType(PatternType.WithoutPatternWord);
						Rule rule1 = new Rule();
						//Out with respect to the entity
						rule1.direction = EdgeDirection.Out;
						rule1.edgeType = graph.getEdge(w1, w2).getRelation().getShortName();
						
						pattern.setRules(Arrays.asList(rule1));
						patterns.add(pattern);
					}
				}
				
				else if(nodes2to1 != null && nodes2to1.size() == 2) {
					//System.out.println("Two sized 2 to 1");
					//System.out.println(graph.getChildList(nodes2to1.get(0)));
					if(graph.getChildList(nodes2to1.get(0)).size() > 1) {
						for(IndexedWord w: graph.getChildList(nodes2to1.get(0))) {
							if(!words1.contains(w) && !words2.contains(w)) {
								String patternWord = w.lemma().toLowerCase().replaceAll("[^a-z]", "");
								if(!patternWord.isEmpty() ) {
									SlotPattern pattern = new SlotPattern();
									
									pattern.setPattern(patternWord);
		
									//pattern.setPattern(word.originalText());
									//System.out.println(word.originalText() + ":" + word.tag());
									pattern.setPatternType(PatternType.TargetInBetween);
									Rule rule1 = new Rule();
									//Out with respect to the entity
									rule1.direction = EdgeDirection.In;
									rule1.edgeType = graph.getEdge(w2, w1).getRelation().getShortName();
									
									
									Rule rule2 = new Rule();
									//Out with respect to the entity
									rule2.direction = EdgeDirection.In;
									rule2.edgeType = graph.getEdge(w2, w).getRelation().getShortName();
									
									pattern.setRules(Arrays.asList(rule1, rule2));
									patterns.add(pattern);
								}
									
								//System.out.println(pattern.hashCode());
								//System.out.println(w1);
								//System.out.println(w2);
							}
						}
					}
					else {
						//System.out.println("Just one child.");
						SlotPattern pattern = new SlotPattern();
						pattern.setPatternType(PatternType.WithoutPatternWord);
						Rule rule1 = new Rule();
						//In with respect to the entity
						rule1.direction = EdgeDirection.In;
						rule1.edgeType = graph.getEdge(w2, w1).getRelation().getShortName();
						
						pattern.setRules(Arrays.asList(rule1));
						patterns.add(pattern);
						//System.out.println(pattern);
					}
				}
			}
		}
		if(patterns.size() > 1) {
			LogInfo.logs("_Sentence_ : " + map);
			LogInfo.logs("_Patterns_ : " + patterns);
		}
		return patterns;
	}
	
	public List<SlotPattern> findRelation(CoreMap map, List<IndexedWord> words1, List<IndexedWord> words2) {
		return findRelation(map, words1, words2, true);
	}

	public List<SlotPattern> findRelation(CoreMap map, List<IndexedWord> words1, List<IndexedWord> words2, boolean extractFullPattern) {
		SemanticGraph graph = map.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<SlotPattern> patterns = new ArrayList<SlotPattern>();
		List<IndexedWord> shortestPath = new ArrayList<IndexedWord>();
		SlotPattern pattern = new SlotPattern();
		Rule rule1 = new Rule();
		Rule rule2 = new Rule();
		boolean rulesCreated = false;
		IndexedWord word1 = null, wordN = null;
		
		//List<IndexedWord> shortestUncleanedPath = null;
		Set<String> personTokens = getPersonsAsTokens(map);
		
		//System.out.println("\nWords 1 " + words1);
		//System.out.println("Words 2 " + words2);
		boolean checkedOnce = false;
		for(IndexedWord w1: words1) {
			for(IndexedWord w2: words2) {
				if(w1 == w2)
					continue;
				try {
				List<IndexedWord> current = graph.getShortestUndirectedPathNodes(w1, w2);

				//LogInfo.logs("One option:" + current);
				current.remove(w1); current.remove(w2);
				
				List<IndexedWord> cleaned = new ArrayList<IndexedWord>();
				for(IndexedWord w:current) {
					if(!personTokens.contains(w.originalText()))
						cleaned.add(w);
				}
				
				//Check if shortest path is through one of the entities, don't take it
				if(cleaned.removeAll(words1) || cleaned.removeAll(words2))
					continue;
				
				if((!checkedOnce && shortestPath.size() == 0) || shortestPath.size() > current.size()) {
					word1 = w1;
					wordN = w2;
					checkedOnce = true;
					
					//shortestUncleanedPath = current;
					//System.out.println(current);
					//System.out.println(shortestUncleanedPath);
					//System.out.println(shortestPath);
					
					shortestPath = current;
					//System.out.println("Shortest path for " + w1.index() + " " + shortestPath.size());
				}
				}
				catch (Exception e)
				{
					System.out.println("Exception at : ");
					System.out.println(w1.originalText());
					System.out.println(w2.originalText());
				}
			}
		}
		
		

		//LogInfo.logs("Shortest dep path:" + shortestPath);
		
		if(shortestPath.isEmpty())
			return patterns;
		
		Set<IndexedWord> neighbours = getConjAndNeighbours(graph, shortestPath.get(0));
		if(!neighbours.containsAll(shortestPath))
			return patterns;
		//System.out.println("Neighbors " + neighbours);
		for(IndexedWord word: neighbours) {
			if(!rulesCreated) {
				SemanticGraphEdge edge = graph.getEdge(word1, shortestPath.get(0));
				rule1.direction = (edge != null) ? EdgeDirection.In : EdgeDirection.Out;
				if(edge != null) 
					rule1.edgeType = edge.toString();
				else {
					//System.out.println(shortestPath.get(0));
					//System.out.println(word1.index());
					//System.out.println(wordN.index());
					//System.out.println(graph.getEdge(shortestPath.get(0), word1));
					rule1.edgeType = graph.getEdge(shortestPath.get(0), word1).toString();
				}
				
				edge = graph.getEdge(wordN, shortestPath.get(shortestPath.size()-1));
				rule2.direction = (edge != null) ? EdgeDirection.In : EdgeDirection.Out;
				if(edge != null) 
					rule2.edgeType = edge.toString();
				else
					rule2.edgeType = graph.getEdge(shortestPath.get(shortestPath.size()-1), wordN).toString();
				
				rulesCreated = true;
			}
			pattern = new SlotPattern();

			pattern.setPattern(word.lemma().toLowerCase().replaceAll("[^a-z]", ""));

			//pattern.setPattern(word.originalText());
			//System.out.println(word.originalText() + ":" + word.tag());
			pattern.setPatternType(PatternType.WordInBetween);
			pattern.setRules(Arrays.asList(rule1, rule2));
			patterns.add(pattern);
		}
		//System.out.println(patterns);
		return patterns;
	}
	
	private Set<IndexedWord> getConjAndNeighbours(SemanticGraph graph, IndexedWord word) {
		//System.out.println("Inside conj_and_neighbors: " + word.originalText());
		Set<IndexedWord> ans = new HashSet<IndexedWord>();
		List<IndexedWord> frontier = new ArrayList<IndexedWord>();
		IndexedWord current;
		ans.add(word);
		frontier.add(word);
		while(!frontier.isEmpty()) {
			current = frontier.remove(0);
			//graph.getChildrenWithReln(current, GrammaticalRelation.valueOf("conj_and"));
			for(IndexedWord w: graph.getChildrenWithReln(current, GrammaticalRelation.valueOf("conj_and")))
				if(ans.contains(w))
					continue;
				else {
					frontier.add(w);
					ans.add(w);
				}
			for(IndexedWord w: graph.getParentsWithReln(current, GrammaticalRelation.valueOf("conj_and")))
				if(ans.contains(w))
					continue;
				else {
					frontier.add(w);
					ans.add(w);
				}
		}
		
		return ans;
	}
	
	public Map<String, Double> findPlaceTimeValue(String sentence, String entity1, Slot slot, boolean social) throws NoSuchParseException {
		Map<String, Double> candidates = new HashMap<String, Double>();
		System.out.println("Full sentence: " + sentence);
		if(sentence.length() > 400)
			return candidates;
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		//get coreferences for the entity
		Map<Integer, Set<Integer>> corefsEntity1 = getCorefs(document, entity1);
		
		List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
		for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
			CoreMap sentenceMap = allSentenceMap.get(sentNum);
			System.out.println("Processing sentence: " + sentenceMap.toString());
			Set<String> times = getTimeAsTokens(sentenceMap);
			Set<String> dates = getDateAsTokens(sentenceMap);
			//System.out.println(sentenceMap.toString());
			List<IndexedWord> foundWord = findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum));
			
			for(SlotPattern pattern: slot.getPatterns()) {
				//System.out.println(pattern);
					for(String ans: findValue(sentenceMap, findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum)), pattern, slot, social, null)) {
					//System.out.println(str);

					if(!ans.isEmpty()) {
						Pair<String,String> datetime = findNearestDateTime(sentenceMap.toString(), ans,dates,times);
						
						String str = "";
						for(String tok: ans.split(" ")) {
							if(!entity1.contains(tok))
								str += " " + tok;
						}
						str = str + " " + datetime.first;
						str = str + " " + datetime.second;
						str = str.trim();

						System.out.println(pattern + "|" + str);

						if(!candidates.containsKey(str))
							candidates.put(str, pattern.getConfidenceScore());
						else
							candidates.put(str, pattern.getConfidenceScore() + candidates.get(str));
					}
				}
			}
		}
		return candidates;
	}
	
	public Pair<String,String> findNearestDateTime(String sentence, String ans, Set<String> dates, Set<String> times)
	{
		String date = "";
		String time = "";
		int ansPos = sentence.indexOf(ans);
		int bestRange = Integer.MAX_VALUE;
		Pair<String,String> bestPair = new Pair<String,String>("","");
		for (String d:dates)
		{
			int datePos = sentence.indexOf(d);
			for (String t:times)
			{
				int timePos = sentence.indexOf(t);
				int minPos = (timePos <= datePos && timePos <= ansPos) ? timePos : (datePos <= ansPos ? datePos : ansPos);
				int maxPos = (timePos >= datePos && timePos >= ansPos) ? timePos : (datePos >= ansPos ? datePos : ansPos);
				int currentRange = maxPos - minPos;
				if (currentRange <= bestRange)
				{
					bestRange = currentRange;
					bestPair = new Pair<String,String>(d,t);
				}
			}
		}
		return bestPair;
	}

	
	public Map<String, Double> findSlotValue(Annotation document, String entity1, Slot slot, boolean social, String defaultVal) throws NoSuchParseException {

		Map<String, Double> candidates = new HashMap<String, Double>();
		try {
			//get coreferences for the entity
			Map<Integer, Set<Integer>> corefsEntity1 = getCorefs(document, entity1);
	
			List<CoreMap> allSentenceMap = document.get(SentencesAnnotation.class);
			for(int sentNum = 0;sentNum < allSentenceMap.size();sentNum++) {
				CoreMap sentenceMap = allSentenceMap.get(sentNum);

				for(SlotPattern pattern: slot.getPatterns()) {
					for(String ans: findValue(sentenceMap, findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum)), pattern, slot, social,defaultVal)) {
						System.out.println(ans + "|" + pattern);
						if(ans == null)
							continue;
						if(!ans.isEmpty()) {
							String str = "";
							for(String tok: ans.split(" ")) {
								if(!entity1.contains(tok))
									str += " " + tok;
							}
							str = str.trim();
							//Flag to check if we found a matching pattern already
							
							if(!str.isEmpty()) {

								if(!candidates.containsKey(str))
									candidates.put(str, pattern.getConfidenceScore());
								else
									candidates.put(str, pattern.getConfidenceScore() + candidates.get(str));
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			LogInfo.logs("Exception thrown. " + e);
			e.printStackTrace();
		}
		return candidates;
	}
	
	private Set<String> findValue(CoreMap sentence, List<IndexedWord> words1, SlotPattern pattern, Slot slot, boolean social, String defaultVal) {
		List<NERType> targetNERTypes = slot.getTargetNERTypes();
		SemanticGraph graph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		Set<IndexedWord> ansSet = new HashSet<IndexedWord>();
		Set<IndexedWord> tempSet = new HashSet<IndexedWord>();
		Set<String> ans = new HashSet<String>();
		IndexedWord patternWord = null;
		Set<Rule> matchedRules = new HashSet<Rule>();
		
		if(social || pattern.getPatternType().equals(Constants.PatternType.WithoutRules)) {
			patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
			if(patternWord == null)
				return ans;
			for(IndexedWord w: words1) {
				if(Math.abs(w.index() - patternWord.index()) < 5) {
					int i = Math.max(1, patternWord.index()-5);
					int count = 0;
					while(count < 20) {
						try {
							tempSet.add(graph.getNodeByIndex(i));
							i++;
							count++;
						}
						catch (Exception e){
							i++;
							count++;
							continue;
						}
					}
				}
			}
		}
		//for rules with no pattern word
		else if(pattern.getPatternType().equals(Constants.PatternType.WithoutPatternWord)) {
			tempSet = getWordsSatisfyingRule(new HashSet<IndexedWord>(words1), pattern.getRules(0), graph);
		}
		else if(pattern.getPatternType().equals(Constants.PatternType.TargetInBetween)) {
			patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
			if(patternWord == null)
				return ans;
			Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
			
			Set<IndexedWord> tempSet1 = getWordsSatisfyingRule(new HashSet<IndexedWord>(words1), pattern.getRules(0), graph);
			tempSet = getWordsSatisfyingRule(conjAndPatterns, pattern.getRules(1), graph);
			
			tempSet.retainAll(tempSet1);
		}
		else if(pattern.getPatternType().equals(Constants.PatternType.SourceInBetween)) {
			patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
			if(patternWord == null)
				return ans;
			Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
			
			//Checking rule2
			Set<IndexedWord> rule2Set = getWordsSatisfyingRule(new HashSet<IndexedWord>(words1), pattern.getRules(1), graph);
			for(IndexedWord w1:conjAndPatterns) {
				if(rule2Set.contains(w1)) {
					tempSet.addAll(getWordsSatisfyingRule(new HashSet<IndexedWord>(words1), pattern.getRules(0), graph));
				}
			}
		}
		else {//if(pattern.getPatternType().equals(Constants.PatternType.WordInBetween))
			patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
			if(patternWord == null)
				return ans;
			Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
			//System.out.println("pattern word is not null: " + patternWord.originalText());
			//for (IndexedWord w:conjAndPatterns)
			//{
			//	System.out.println(w.originalText());
			//}
			//Checking rule1
			//System.out.println("Rules:");
			//System.out.println(pattern.getRules(0).toString());
			//System.out.println(pattern.getRules(1).toString());
			Set<IndexedWord> rule1Set = getWordsSatisfyingRuleNew(conjAndPatterns, pattern.getRules(0), graph);
			//System.out.println("Rule1Set: ");
			//for (IndexedWord w:rule1Set)
			//{
			//	System.out.println(w.originalText());
			//}
			for(IndexedWord w1:words1) {
				if(rule1Set.contains(w1)) {
					if(tempSet.addAll(getWordsSatisfyingRuleNew(conjAndPatterns, pattern.getRules(1), graph)))
						matchedRules.add(pattern.getRules(1));
				}
			}
			
			//Checking rule2
			
			Set<IndexedWord> rule2Set = getWordsSatisfyingRuleNew(conjAndPatterns, pattern.getRules(1), graph);
			
			//System.out.println("Rule2Set: ");
			//for (IndexedWord w:rule2Set)
			//{
			//	System.out.println(w.originalText());
			//}
			
			for(IndexedWord w1:words1) {
				if(rule2Set.contains(w1)) {
					if(tempSet.addAll(getWordsSatisfyingRuleNew(conjAndPatterns, pattern.getRules(0), graph))) 
						matchedRules.add(pattern.getRules(0));
				}
			}
		}
		//System.out.println("tempset size: " + tempSet.size());
		
		for(IndexedWord w: tempSet)
		{
			ansSet.addAll(getConjAndNeighbours(graph, w));
		}
		
		if(slot.getName().equals(SlotName.CauseOfDeath)) {
			String phrase;
			for(Rule r: matchedRules) {
				if(r.edgeType.startsWith("prep")) {
					String[] split = r.edgeType.split("_");
					String w = split[split.length-1];
					String temp = "";
					phrase = findExpandedEntityForDeath(sentence, w);
					if(phrase == null)
						continue;
					for(String tok: phrase.split(" ")) 
						if(!tok.equals(patternWord.lemma()) && !Arrays.asList(split).contains(tok))
							temp += " " + tok;	
					
					if(!temp.trim().isEmpty())
						ans.add(temp.trim());
				}
				else {
					for(IndexedWord w: ansSet) {
						String temp = "";
						phrase = findExpandedEntity(sentence, w.originalText());
						if(phrase == null)
							continue;
						for(String tok: phrase.split(" ")) 
							if(!tok.equals(patternWord.lemma()))
								temp += " " + tok;	
						
						if(!temp.trim().isEmpty())
							ans.add(temp.trim());
					}
				}
			}
			return ans;
		}
		
		Map<String, String> nerMap = createNERMap(sentence);
		for(IndexedWord w: ansSet) {
			//System.out.println("word: " + w.originalText());
			String phrase = findExpandedEntity(sentence, w.originalText());
			//System.out.println("Phrase: " + phrase);
			String temp = "";
			for(String tok: phrase.split(" ")) {
				//System.out.println("Token: " + tok);
				//System.out.println("NER Types");
				//for (NERType n: targetNERTypes)
				//	System.out.println(n.toString());
				//System.out.println();
				if(targetNERTypes == null || targetNERTypes.contains(NERType.NONE) || targetNERTypes.contains(NERType.valueOf(nerMap.get(tok)))) {
					if(patternWord != null) {
						if(!tok.equals(patternWord.lemma()))
								temp += " " + tok;	
					}
					else 
						temp += " " + tok;	
				}
			}
			if(!temp.trim().isEmpty()) {
				ans.add(temp.trim());
				//System.out.println("ans: " + pattern);
			}
		}
		
		if(ans.isEmpty() && slot.getName().equals(SlotName.DateOfDeath))
			ans.add(defaultVal);
		return ans;
	}
	
	private Set<IndexedWord> getWordsSatisfyingRule(Set<IndexedWord> words, Rule rule, SemanticGraph graph) {
		//System.out.println(rule);
		Set<IndexedWord> ans = new HashSet<IndexedWord>();
		for(IndexedWord word: words)
			if(rule.direction == EdgeDirection.In)
				ans.addAll(graph.getParentsWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));
			else
				ans.addAll(graph.getChildrenWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));

		return ans;
	}
	
	private Set<IndexedWord> getWordsSatisfyingRuleNew(Set<IndexedWord> words, Rule rule, SemanticGraph graph) {
		//System.out.println(rule);
		Set<IndexedWord> ans = new HashSet<IndexedWord>();
		Set<IndexedWord> frontier = new HashSet<IndexedWord>(words);
		Set<IndexedWord> newFrontier = new HashSet<IndexedWord>();
		Set<IndexedWord> seen = new HashSet<IndexedWord>(words);
		
		while(!frontier.isEmpty()) {
			for(IndexedWord word: frontier)
				if(rule.direction == EdgeDirection.In)
					ans.addAll(graph.getParentsWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));
				else
					ans.addAll(graph.getChildrenWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));
			
			if(!ans.isEmpty())
				break;
			
			for(IndexedWord word: frontier) {
				newFrontier = new HashSet<IndexedWord>();
				newFrontier.addAll(graph.getParents(word));
				newFrontier.addAll(graph.getChildren(word));
				newFrontier.removeAll(seen);
				seen.addAll(newFrontier);
			}
			frontier = newFrontier;
		}
		
		return ans;
	}

	private static String findExpandedEntity(CoreMap sentence, String str) {
		//System.out.println("String is: " + str);
		IndexedWord word = null;
		for(IndexedWord w: sentence.get(CollapsedCCProcessedDependenciesAnnotation.class).vertexSet()) {
			if(w.originalText().compareToIgnoreCase(str) == 0) {
				word = w;
				break;
			}
		}
		if(word == null)
			return null;
		int index = word.index();
		Tree root = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		//root.pennPrint();
		Tree node = root.getLeaves().get(index-1);
		Tree traverse = node;
		Tree prev = node;
		boolean found = false;
	    int height = 0;
		while(node!=null) {
	    	//System.out.println("Before parenting");
	    	//System.out.println(node.value());
	    	//System.out.println("After parenting");
			prev = node;
			node = node.parent(root);
	    	//System.out.println(node.value());
	    	if(node.value().equals("NP")) {
	    		found = true;
	    	}
	    	if (height > 0 && !node.value().equals("NP"))
	    		break;
	    	height++;
	    }
	
	    return (found && !traverse.value().equals("NP")) ? getText(prev) : getText(traverse);
	    //return traverse.value().equals("NP") ? getText(traverse): getText(node);
	}
	
	private static String findExpandedEntityForDeath(CoreMap sentence, String str) {
		Tree root = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		Tree node = null;
		for(Tree n: root.getLeaves()) {
			if(n.toString().equals(str))
				node = n;
		}
		if(node == null)
			return null;
		while(node.parent(root) != null && node.getChildrenAsList().size() <= 1) {
			node = node.parent(root);
		}

	    return getText(node);
	}
	
	public Map<String, String> createNERMap(String sentence) {
		Map<String, String> nerMap = new HashMap<String, String>();
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

	    for(CoreMap sent: sentences) {
	      for (CoreLabel token: sent.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);   
	        nerMap.put(word, ner);
	      }
	    }
		return nerMap;
	}
	
	public Set<String> getPersons(String sentence) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		return getPersons(document.get(SentencesAnnotation.class));
	}
	
	public Set<String> getPersons(List<CoreMap> sentences) {
		Set<String> persons = new HashSet<String>();

		for(CoreMap sent: sentences) {
	      persons.addAll(getPersons(sent));
	    }
	    
	    return persons;
	}
	
	public Set<String> getPersons(CoreMap sentence) {
		Set<String> persons = new HashSet<String>();
		String person = "";
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);  
	        if(NERType.valueOf(ner).equals(NERType.PERSON)) 
	        	person += word + " ";
	        else if(!person.isEmpty()) {
	        		persons.add(person.trim());
	        		person = "";
	        }
	      }
        if(!person.isEmpty())
    		persons.add(person.trim());
        return persons;
	}
	
	public Set<String> getTimeAsTokens(CoreMap sentence)
	{
		Set<String> times = new HashSet<String>();
		String currentTime = "";
		
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);  
	        if(NERType.valueOf(ner).equals(NERType.TIME)) {
	        		currentTime = currentTime + " " + word;
	        }
	        else
	        {
	        	if (!currentTime.isEmpty())
	        	{
	        		times.add(currentTime.trim());
	        		currentTime = "";
	        	}
	        }
	    
	    }
        return times;
	}
	public Set<String> getDateAsTokens(CoreMap sentence)
	{
		Set<String> times = new HashSet<String>();
		String currentTime = "";
		
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);  
	        if(NERType.valueOf(ner).equals(NERType.DATE)) {
	        		currentTime = currentTime + " " + word;
	        }
	        else
	        {
	        	if (!currentTime.isEmpty())
	        	{
	        		times.add(currentTime.trim());
	        		currentTime = "";
	        	}
	        }
	    
	    }
        return times;
	}
	public Set<String> getPersonsAsTokens(CoreMap sentence) {
		Set<String> persons = new HashSet<String>();
		String person = "";
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);  
	        if(NERType.valueOf(ner).equals(NERType.PERSON)) {
	        		persons.add(word.trim());
	        }
	    }
        return persons;
	}
	
	public Map<String, String> createNERMap(CoreMap sentence) {
		Map<String, String> nerMap = new HashMap<String, String>();
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	      String word = token.get(TextAnnotation.class);
	      String ner = token.get(NamedEntityTagAnnotation.class);           
	      nerMap.put(word, ner);
	    }
	    return nerMap;
	}
	
	public Map<String, Set<String>> getCorefMap(String sentence) {
		Map<String, Set<String>> corefMap = new HashMap<String, Set<String>>();
		
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			
			Map<Integer, CorefChain> coref = document.get(CorefChainAnnotation.class);
			
			for(Map.Entry<Integer, CorefChain> entry : coref.entrySet()) {
	            CorefChain c = entry.getValue();
	            CorefMention cm = c.getRepresentativeMention();
	            String clust = "";
	            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
	            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
	                clust += tks.get(i).get(TextAnnotation.class) + " ";
	            clust = clust.trim();
	           
	            Set<String> mentions = new HashSet<String>();
	        	for(Set<CorefMention> s : c.getMentionMap().values()){
	            	for(CorefMention m: s) {
	            		String clust2 = "";
	            		if(m.mentionType != Dictionaries.MentionType.PRONOMINAL)
	            			continue;
	                    tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
	                    for(int i = m.startIndex-1; i < m.endIndex-1; i++)
	                        clust2 += tks.get(i).get(TextAnnotation.class) + " ";
	                    clust2 = clust2.trim();
	                    //don't need the self mention
	                    if(clust.equals(clust2))
	                        continue;
	                    mentions.add(clust2);
	                }
	            }
	        	if(!mentions.isEmpty())
	        		corefMap.put(clust, mentions);
	        }
			return corefMap;
		}
		catch (NoSuchParseException e) {
			return corefMap;
		}
	}

	public Map<Integer, Set<Integer>> getCorefs(Annotation document, String entity) {
		Map<Integer, Set<Integer>> ans = new HashMap<Integer, Set<Integer>>();
		Set<Integer> temp;
		
		try {
			Map<Integer, CorefChain> coref = document.get(CorefChainAnnotation.class);
			if(coref == null)
				return ans;
			
			for(Map.Entry<Integer, CorefChain> entry : coref.entrySet()) {
	            CorefChain c = entry.getValue();
	            CorefMention cm = c.getRepresentativeMention();
	            String clust = "";
	            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
	            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
	                clust += tks.get(i).get(TextAnnotation.class) + " ";
	            clust = clust.trim();
	            
	            boolean present = false;
	            for(String word: entity.split(" ")) {
	            	if(Arrays.asList(clust.split(" ")).contains(word)) {
	            		present = true;
	            		break;
	            	}
	            }
	            if(!present)
	            	continue;
	           
	            for(Set<CorefMention> s : c.getMentionMap().values()){
	            	for(CorefMention m: s) {
	            		if(m.mentionType != Dictionaries.MentionType.PRONOMINAL)
	            			continue;
	            		if(!ans.containsKey(m.sentNum-1))
	            				ans.put(m.sentNum - 1, new HashSet<Integer>());
	            		temp = ans.get(m.sentNum-1);
	            		temp.add(m.startIndex);
	            		ans.put(m.sentNum-1, temp);
	                }
	            }
	        }
			return ans;
		}
		catch (NoSuchParseException e) {
			return ans;
		}
	}

	public String getNNs(String sentence, String entity) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		String ans = "";
		
		for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
			if(findExpandedEntity(sentenceMap, entity) == null)
				continue;
			for(String token: findExpandedEntity(sentenceMap, entity).split(" "))
				if(!entity.contains(token))
					ans += token + " ";
		}
		return ans.trim();
	}
	
	public boolean containsTokens(String s2, String s1) {
		for(String token: s1.split(" ")) {
			if(!s2.contains(token))
				return false;
		}
		return true;
	}
	
	public Set<String> getCorefs(String sentence, String head) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		Set<String> corefs = new HashSet<String>();
		
		Map<Integer, CorefChain> coref = document.get(CorefChainAnnotation.class);
		
		for(Map.Entry<Integer, CorefChain> entry : coref.entrySet()) {
            CorefChain c = entry.getValue();
            //this is because it prints out a lot of self references which aren't that useful
            if(c.getMentionMap().size() <= 1)
                continue;
            
            CorefMention cm = c.getRepresentativeMention();
            String clust = "";
            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
           
            for(Set<CorefMention> s : c.getMentionMap().values()){
            	for(CorefMention m: s) {
            		String clust2 = "";
                    tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                    boolean flag = false;
                    for(int i = m.startIndex-1; i < m.endIndex-1; i++) {
                    	if(head.toLowerCase().contains(tks.get(i).get(TextAnnotation.class).toLowerCase()))
                    		flag = true;
                        clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                    }
                    clust2 = clust2.trim();
                    //don't need the self mention
                    if(clust.equals(clust2) || flag)
                        continue;
                    if(containsTokens(clust, head) && !m.mentionType.equals(MentionType.valueOf("PRONOMINAL")))
                    	corefs.add(clust2);
                }
            }
        }
		return corefs;
	}
	
	public List<SemanticGraph> getCollapsedCCSemanticGraphs(String sentence) {
		List<SemanticGraph> lst = new ArrayList<SemanticGraph>();
		
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		
		for(CoreMap map:document.get(SentencesAnnotation.class)) {
			lst.add(map.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
		}
		
		return lst;
	}
	
	public List<SemanticGraph> getBasicSemanticGraphs(String sentence) {
		List<SemanticGraph> lst = new ArrayList<SemanticGraph>();
		
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		
		for(CoreMap map:document.get(SentencesAnnotation.class)) {
			lst.add(map.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class));
		}
		
		return lst;
	}
	
	public List<Tree> getTreeRoots(String sentence) {
		List<Tree> lst = new ArrayList<Tree>();
		
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		
		for(CoreMap map:document.get(SentencesAnnotation.class)) {
			lst.add(map.get(TreeCoreAnnotations.TreeAnnotation.class));
		}
		
		return lst;
	}
	
	public static String getText(Tree tree) {
    	StringBuilder b = new StringBuilder();
    	for(Tree leaf:tree.getLeaves()) {
    		b.append(leaf.value() + " ");
    	}
    	return b.toString().trim();
    }

	//Handle Path passing through entities
	public IntCounter<SlotPattern> extractPERRelation(String sentence, String entity) {
		// TODO Auto-generated method stub
		//sentence = deAccent(sentence);
		//System.out.println(sentence);
		IntCounter<SlotPattern> patterns = new IntCounter<SlotPattern>();
		try {
			if(sentence.length() > 400)
				return patterns;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			//LogInfo.begin_track("Current entity: " + entity);
			for(CoreMap map:document.get(SentencesAnnotation.class)) {				
				Set<String> persons = getPersons(map);

				if(persons.size() > 1 && persons.contains(entity)) {
					LogInfo.logs("Entity :" + entity);
					LogInfo.logs("Sentence :" + map);
					LogInfo.logs("People :" + persons);	
					//SemanticGraph graph = map.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
					
					for(String person:persons) {
						if(!person.equals(entity)) {
							List<IndexedWord> indexWords1 = findWordsInSemanticGraph(map, person, null);
							List<IndexedWord> indexWords2 = findWordsInSemanticGraph(map, entity, null);
							
							LogInfo.begin_track("Finding relation: " + entity + " # " + person);
							List<SlotPattern> patts = findRelation(map, indexWords1, indexWords2);
							for(SlotPattern pattern: patts)
								patterns.incrementCount(pattern);
							
							LogInfo.logs("Found patterns" + patts);
							LogInfo.end_track();
							
						}
					}

				}
			}
			//LogInfo.end_track();
		}
		catch(Exception ex){
			System.out.println("Exception");
			ex.printStackTrace();
		}
		System.out.println("Returning " + patterns);
		return patterns;
	}
	
	public Map<Pair<String,String>,List<String>> getTwoSidesForPatternWord(String sentence, SlotPattern pattern)
	{
		Map<Pair<String,String>,List<String>> result = new HashMap<Pair<String,String>,List<String>>();
		try {
			if(sentence.length() > 400)
				return result;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				
				String sentenceFromMap = sentenceMap.toString().trim();
				
				
				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
				{
					continue;
				}
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);

				
				IndexedWord patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if (patternWord == null)
					return result;
				
				Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
				Set<IndexedWord> rule1SetTemp = getWordsSatisfyingRule(conjAndPatterns, pattern.getRules(0), graph);
				Set<IndexedWord> rule2SetTemp = getWordsSatisfyingRule(conjAndPatterns, pattern.getRules(1), graph);
				
				// Remove pronouns
				Set<IndexedWord> rule1Set = new HashSet<IndexedWord>();
				for (IndexedWord w:rule1SetTemp)
				{
					if (w.tag().equals("PRP") || w.tag().equals("PRP$") || w.tag().equals("WP") || w.tag().equals("WP$"))
						continue;
					rule1Set.add(w);
				}
				
				Set<IndexedWord> rule2Set = new HashSet<IndexedWord>();
				for (IndexedWord w:rule2SetTemp)
				{
					if (w.tag().equals("PRP") || w.tag().equals("PRP$") || w.tag().equals("WP") || w.tag().equals("WP$"))
						continue;
					rule2Set.add(w);
				}
				for (IndexedWord r1:rule1Set)
				{
					String phrase1 = findExpandedEntity(sentenceMap,r1.originalText());
					for (IndexedWord r2:rule2Set)
					{
						String phrase2 = findExpandedEntity(sentenceMap, r2.originalText());
						Pair<String,String> p = new Pair<String,String>(phrase1,phrase2);
						if (result.containsKey(p))
						{
							List<String> existing = result.get(p);
							existing.add(sentenceFromMap);
							result.put(p, existing);
						}
						else
						{
							List<String> newList = new ArrayList<String>();
							newList.add(sentenceFromMap);
							result.put(p, newList);
						}
					}
				}
			}
		}
		catch(Exception ex) {
			//System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return result;
	}
}
