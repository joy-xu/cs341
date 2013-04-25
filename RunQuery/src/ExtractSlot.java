import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
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
	String serializedClassifier;
	AbstractSequenceClassifier<CoreLabel> classifier;
	
	public ExtractSlot() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		processor = new StanfordCoreNLP(props, false);
		serializedClassifier = "lib/english.all.3class.distsim.crf.ser.gz";
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
	}
	
	public String createNERMap(String sentence, Map<String, String> nerMap, boolean notUseStanNER) {
		String separator = "__";
		if(!notUseStanNER) {
			separator = "/";
			sentence = classifier.classifyToString(sentence);
		}
		String[] words = sentence.split(" ");
		String newSentence = "";
			
		for(String word: words) {
			String[] wordNERPair = word.split(separator);
			newSentence += " " + wordNERPair[0];
			nerMap.put(wordNERPair[0], wordNERPair[1]);
		}
		return newSentence;
	}
	
	public void findSlotVals(List<String> sentences, String ent1, String ent2, boolean notUseStanNER, IntCounter<String> slotValCounter){
		  Map<String, String> nerMap = new HashMap<String,String>();
		  String sentence;
		  for(int sentenceCounter = 0; sentenceCounter < sentences.size(); sentenceCounter++) {
            	sentence = sentences.get(sentenceCounter);
            	nerMap.clear();
            	sentence = createNERMap(sentence, nerMap, notUseStanNER);
            	List<String> slotvals = findSlotVal(sentence, ent1, ent2, nerMap);
    			for(String slotVal:slotvals) {
    				slotValCounter.incrementCount(slotVal);
    			}
            }
    }
	
	public List<String> findSlotVal(String sentence, String ent1, String ent2, Map<String, String> nerMap) {
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
			SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
			return findSlotVal(graph, findWordsInSemanticGraph(graph, ent1), findWordsInSemanticGraph(graph, ent2), nerMap);
		}
		catch (NoSuchParseException e) {
			return Collections.<String>emptyList();
		}
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
			for(IndexedWord w2: entity) {
				ancestorDistance = graph.isAncestor(w1, w2);
				if ( ancestorDistance == 1 || ancestorDistance == 2) {
					children = graph.getChildList(w1);
					for(IndexedWord child: children) {
						String childText = child.originalText();
						if(nerMap.get(childText).startsWith("ORG"))
							slotValsFound.add(childText);
					}
				}
			}
		}
		
		return slotValsFound;
	}
}
