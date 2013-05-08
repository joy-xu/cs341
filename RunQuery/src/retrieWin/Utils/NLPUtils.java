package retrieWin.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
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
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				patterns.addAll(findRelation(graph, findWordsInSemanticGraph(sentenceMap, entity1), findWordsInSemanticGraph(sentenceMap, entity2)));
			}
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		return patterns;
	}
	
	//TODO - Improve if needed!
	public List<IndexedWord> findWordsInSemanticGraph(CoreMap sentenceMap, String entity) {
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List<String> entitySplits = Arrays.asList(entity.split(" "));
		List<IndexedWord> words = new ArrayList<IndexedWord>();
		HashSet<String> expandedEntitySplits = new HashSet<String>();
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
				List<IndexedWord> current = graph.getShortestUndirectedPathNodes(w1, w2);
				current.remove(w1); current.remove(w2);
				word1 = w1;
				wordN = w2;
				if(shortestPath.size() == 0 || shortestPath.size() > current.size())
					shortestPath = current;
			}
		}
		//System.out.println(shortestPath);
		Set<IndexedWord> neighbours = getConjAndNeighbours(graph, shortestPath.get(0));
		if(!neighbours.containsAll(shortestPath) || shortestPath.size() == 0)
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
		
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);

			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				values.addAll(findValue(sentenceMap, findWordsInSemanticGraph(sentenceMap, entity1), pattern, targetNERTypes));
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
		SemanticGraph graph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		Set<IndexedWord> ansSet = new HashSet<IndexedWord>();
		Set<IndexedWord> tempSet = new HashSet<IndexedWord>();
		Set<String> ans = new HashSet<String>();
		IndexedWord patternWord = findWordsInSemanticGraphForSlotPattern(graph, pattern.getPattern());
		Set<IndexedWord> conjAndPatterns = getConjAndNeighbours(graph, patternWord);
		
		//Checking rule1
		Set<IndexedWord> rule1Set = getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(0));
		for(IndexedWord w1:words1) {
			if(rule1Set.contains(w1)) {
				tempSet.addAll(getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(1)));
			}
		}
		
		//Checking rule2
		Set<IndexedWord> rule2Set = getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(1));
		for(IndexedWord w1:words1) {
			if(rule2Set.contains(w1)) {
				tempSet.addAll(getWordsSatisfyingPattern(conjAndPatterns, pattern.getRules(0)));
			}
		}
		
		for(IndexedWord w: tempSet) 
			ansSet.addAll(getConjAndNeighbours(graph, w));
		
		
		for(IndexedWord w: ansSet) 
			ans.add(findExpandedEntity(sentence, w.originalText()));
		
		//restrict to targetNER types
		return ans;
	}
	
	private Set<IndexedWord> getWordsSatisfyingPattern(Set<IndexedWord> words, Rule rule) {
		return null;
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
