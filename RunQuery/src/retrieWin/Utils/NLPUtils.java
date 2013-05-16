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
import retrieWin.SSF.SlotPattern;
import retrieWin.SSF.SlotPattern.Rule;

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
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

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
	
	public List<SlotPattern> findSlotPattern(String sentence, String entity1, String entity2) {
		List<SlotPattern> patterns = new ArrayList<SlotPattern>();
		
		try {
			if(sentence.length() > 400)
				return patterns;
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			Map<Integer, Set<Integer>> corefsEntity1 = getCorefs(document, entity1);
			Map<Integer, Set<Integer>> corefsEntity2 = getCorefs(document, entity2);
			
			int sentNum = 0;
			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				String sentenceFromMap = sentenceMap.toString().trim();
				//System.out.println(sentenceFromMap.length() + "##" + sentenceFromMap);
				if(sentenceFromMap.startsWith("Tags :") || sentenceFromMap.length() > 300)
					continue;
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				patterns.addAll(findRelation(graph, findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum)), findWordsInSemanticGraph(sentenceMap, entity2, corefsEntity2.get(sentNum))));
				sentNum++;
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
		System.out.println(entity + "," + words);
		return words;
	}
	
	//TODO - Improve if needed!
	public IndexedWord findWordsInSemanticGraphForSlotPattern(SemanticGraph graph, String pattern) {
		for(IndexedWord word: graph.vertexSet()) {
			if(pattern.compareToIgnoreCase(word.originalText()) == 0) {
				return word;
			}
		}
		return null;
	}

	public List<SlotPattern> findRelation(SemanticGraph graph, List<IndexedWord> words1, List<IndexedWord> words2) {
		List<SlotPattern> patterns = new ArrayList<SlotPattern>();
		List<IndexedWord> shortestPath = new ArrayList<IndexedWord>();
		SlotPattern pattern = new SlotPattern();
		Rule rule1 = new Rule();
		Rule rule2 = new Rule();
		boolean rulesCreated = false;
		IndexedWord word1 = null, wordN = null;
		
		for(IndexedWord w1: words1) {
			for(IndexedWord w2: words2) {
				if(w1 == w2)
					continue;
				List<IndexedWord> current = graph.getShortestUndirectedPathNodes(w1, w2);

				current.remove(w1); current.remove(w2);
				
				//Check if shortest path is through one of the entities, don't take it
				if(current.removeAll(words1) || current.removeAll(words2))
					continue;
				
				if(shortestPath.size() == 0 || shortestPath.size() > current.size()) {
					word1 = w1;
					wordN = w2;
					shortestPath = current;
				}
			}
		}

		
		if(shortestPath.isEmpty())
			return patterns;
		
		Set<IndexedWord> neighbours = getConjAndNeighbours(graph, shortestPath.get(0));
		if(!neighbours.containsAll(shortestPath))
			return patterns;
		
		for(IndexedWord word: neighbours) {
			if(!rulesCreated) {
				SemanticGraphEdge edge = graph.getEdge(word1, shortestPath.get(0));
				rule1.direction = (edge != null) ? EdgeDirection.In : EdgeDirection.Out;
				if(edge != null) 
					rule1.edgeType = edge.toString();
				else
					rule1.edgeType = graph.getEdge(shortestPath.get(0), word1).toString();
				
				edge = graph.getEdge(wordN, shortestPath.get(shortestPath.size()-1));
				rule2.direction = (edge != null) ? EdgeDirection.In : EdgeDirection.Out;
				if(edge != null) 
					rule2.edgeType = edge.toString();
				else
					rule2.edgeType = graph.getEdge(shortestPath.get(shortestPath.size()-1), wordN).toString();
				
				rulesCreated = true;
			}
			pattern = new SlotPattern();
			pattern.setPattern(word.originalText());
			pattern.setRules(Arrays.asList(rule1, rule2));
			patterns.add(pattern);
		}
		//System.out.println(patterns);
		return patterns;
	}
	
	private Set<IndexedWord> getConjAndNeighbours(SemanticGraph graph, IndexedWord word) {
		Set<IndexedWord> ans = new HashSet<IndexedWord>();
		List<IndexedWord> frontier = new ArrayList<IndexedWord>();
		IndexedWord current;
		ans.add(word);
		frontier.add(word);
		while(!frontier.isEmpty()) {
			current = frontier.remove(0);
			graph.getChildrenWithReln(current, GrammaticalRelation.valueOf("conj_and"));
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
	
	public List<String> findSlotValue(String sentence, String entity1, SlotPattern pattern, List<NERType> targetNERTypes) {
		List<String> values = new ArrayList<String>();
		//System.out.println(pattern);
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			Map<Integer, Set<Integer>> corefsEntity1 = getCorefs(document, entity1);
			
			int sentNum = 0;
			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				values.addAll(findValue(sentenceMap, findWordsInSemanticGraph(sentenceMap, entity1, corefsEntity1.get(sentNum)), pattern, targetNERTypes));
				sentNum++;
			}
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return values;
	}
	
	private Set<String> findValue(CoreMap sentence,
			List<IndexedWord> words1, SlotPattern pattern,
			List<NERType> targetNERTypes) {
		//System.out.println(pattern);
		//System.out.println(pattern.getRules());
		SemanticGraph graph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		Set<IndexedWord> ansSet = new HashSet<IndexedWord>();
		Set<IndexedWord> tempSet = new HashSet<IndexedWord>();
		Set<String> ans = new HashSet<String>();
		
		IndexedWord patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
		if(patternWord == null)
			return ans;
		Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
		
		//Checking rule1
		Set<IndexedWord> rule1Set = getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(0), graph);
		for(IndexedWord w1:words1) {
			if(rule1Set.contains(w1)) {
				tempSet.addAll(getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(1), graph));
			}
		}
		
		//Checking rule2
		Set<IndexedWord> rule2Set = getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(1), graph);
		for(IndexedWord w1:words1) {
			if(rule2Set.contains(w1)) {
				tempSet.addAll(getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(0), graph));
			}
		}
		
		for(IndexedWord w: tempSet) 
			ansSet.addAll(getConjAndNeighbours(graph, w));
		
		Map<String, String> nerMap = createNERMap(sentence);
		for(IndexedWord w: ansSet) {
			String phrase = findExpandedEntity(sentence, w.originalText());
			for(String tok: phrase.split(" "))
				if(targetNERTypes == null || targetNERTypes.contains(NERType.NONE) || targetNERTypes.contains(NERType.valueOf(nerMap.get(tok)))) {
					ans.add(phrase);
					break;
				}	
		}
		
		return ans;
	}
	
	private Set<IndexedWord> getWordsSatisfyingPattern(Set<IndexedWord> words, Rule rule, SemanticGraph graph) {
		//System.out.println(rule);
		Set<IndexedWord> ans = new HashSet<IndexedWord>();
		for(IndexedWord word: words)
			if(rule.direction == EdgeDirection.In)
				ans.addAll(graph.getParentsWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));
			else
				ans.addAll(graph.getChildrenWithReln(word, GrammaticalRelation.valueOf(rule.edgeType)));

		return ans;
	}

	private static String findExpandedEntity(CoreMap sentence, String str) {
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
		Tree node = root.getLeaves().get(index-1);
		Tree traverse = node;
	    for(int height = 0; height < 2; height++) {
	    	node = node.parent(root);
	    	if(node.value().equals("NP")) {
	    		break;
	    	}
	    }
	    return traverse.value().equals("NP") ? getText(traverse): getText(node);
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
		Set<String> persons = new HashSet<String>();
		String person = "";
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

	    for(CoreMap sent: sentences) {
	      for (CoreLabel token: sent.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String ner = token.get(NamedEntityTagAnnotation.class);  
	        if(NERType.valueOf(ner).equals(NERType.PERSON)) 
	        	person += word + " ";
	        else if(!person.isEmpty()) {
	        		persons.add(person.trim());
	        		person = "";
	        }
	      }
	    }
	    if(!person.isEmpty())
    		persons.add(person.trim());
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
                    for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                        clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                    clust2 = clust2.trim();
                    //don't need the self mention
                    if(clust.equals(clust2))
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
}
