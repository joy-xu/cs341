import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;


public class ExtractSlot {
	StanfordCoreNLP processor;
	IntCounter<String> slotValCounter = new IntCounter<String>();
	
	public ExtractSlot() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		processor = new StanfordCoreNLP(props, false);
	}
	
	public String createNERMap(String sentence, Map<String, String> nerMap) {
		String[] words = sentence.split(" ");
		String newSentence = "";
		
		for(String word: words) {
			String[] wordNERPair = word.split("__");
			newSentence += " " + wordNERPair[0];
			nerMap.put(wordNERPair[0], wordNERPair[1]);
		}
		return newSentence;
	}
	
	public void findSlotVals(List<String> sentences, String ent1, String ent2){
		  Map<String, String> nerMap = new HashMap<String,String>();
		  for(int sentenceCounter = 0; sentenceCounter < sentences.size(); sentenceCounter++) {
            	String sentence = sentences.get(sentenceCounter);
            	nerMap.clear();
            	sentence = createNERMap(sentence, nerMap);
            	System.out.println(sentence);
    		    List<String> slotvals = findSlotVal(sentence, ent1, ent2, nerMap);
    			for(String slotVal:slotvals) {
    				slotValCounter.incrementCount(slotVal);
    			}
            }
          System.out.println(slotValCounter);
	}
	
	public List<String> findSlotVal(String sentence, String ent1, String ent2, Map<String, String> nerMap) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		return findSlotVal(graph, findWordsInSemanticGraph(graph, ent1), findWordsInSemanticGraph(graph, ent2), nerMap);
	}
	
	public List<IndexedWord> findWordsInSemanticGraph(SemanticGraph graph, String entity) {
		List<String> entitySplits = Arrays.asList(entity.split(" "));
		List<IndexedWord> words = new ArrayList<IndexedWord>();
		for(IndexedWord word: graph.vertexSet()) {
			if(StringUtils.containsIgnoreCase(entitySplits, word.originalText()))
				words.add(word);
		}
		
		return words;
	}
	
	public List<String> findSlotVal(SemanticGraph graph, List<IndexedWord> entity, List<IndexedWord> pattern, Map<String, String> nerMap) {
		List<String> slotValsFound = new ArrayList<String>();
		List<IndexedWord> children = new ArrayList<IndexedWord>();
		Set<IndexedWord> patternSet = new HashSet<IndexedWord>();
		
		int ancestorDistance;
		System.out.println(nerMap.toString());
		
		for(IndexedWord w1: pattern) {
			for(SemanticGraphEdge edge:graph.getIncomingEdgesSorted(w1)) {
				if(edge.getRelation().toString().equals("conj_and"))
					patternSet.add(edge.getSource());
			}
			for(SemanticGraphEdge edge:graph.getOutEdgesSorted(w1)) {
				if(edge.getRelation().toString().equals("conj_and"))
					patternSet.add(edge.getTarget());
			}
			patternSet.add(w1);
		}
		
		//if entity is ancestor of pattern, emit all children of pattern which are ORGs
		for(IndexedWord w1: patternSet) {
			System.out.println(w1.originalText());
			for(IndexedWord w2: entity) {
				ancestorDistance = graph.isAncestor(w1, w2);
				if ( ancestorDistance == 1 || ancestorDistance == 2) {
					children = graph.getChildList(w1);
					for(IndexedWord child: children) {
						String childText = child.originalText();
						System.out.println(nerMap.get(childText));
						if(nerMap.get(childText).equals("ORG"))
							slotValsFound.add(childText);
					}
				}
			}
		}
		
		return slotValsFound;
	}
}