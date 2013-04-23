import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.thrift.ShortStack;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;


public class ExtractRelation {
	StanfordCoreNLP processor;
	
	public ExtractRelation() {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		processor = new StanfordCoreNLP(props, false);
	}
	
	public String findRelation(String sentence, String ent1, String ent2) {
		Annotation document = new Annotation(sentence);
		processor.annotate(document);
		CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
		SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		findRelation(graph, findWordsInSemanticGraph(graph, ent1), findWordsInSemanticGraph(graph, ent2));
		  
		return "";
	}
	
	public List<IndexedWord> findWordsInSemanticGraph(SemanticGraph graph, String entity) {
		List<String> entitySplits = Arrays.asList(entity.split(" "));
		List<IndexedWord> words = new ArrayList<IndexedWord>();
		for(IndexedWord word: graph.vertexSet()) {
			if(entitySplits.contains(word.originalText()))
				words.add(word);
		}
		
		return words;
	}
	
	public String findRelation(SemanticGraph graph, List<IndexedWord> words1, List<IndexedWord> words2) {
		List<IndexedWord> shortestPath = new ArrayList<IndexedWord>();
		for(IndexedWord w1: words1) {
			for(IndexedWord w2: words2) {
				List<IndexedWord> current = graph.getShortestUndirectedPathNodes(w1, w2);
				current.remove(w1); current.remove(w2);
				if(shortestPath.size() == 0 ||shortestPath.size() > current.size())
					shortestPath = current; 
			}
		}
		
		if(shortestPath.size() == 1) {
			System.out.println("Relation : " + shortestPath.get(0).originalText());
			return shortestPath.get(0).originalText();
		}
		else {
			System.out.println(shortestPath);
			return "";
		}
	}
}
