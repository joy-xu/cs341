import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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


public class ExtractRelation {
	StanfordCoreNLP processor;
	public static IntCounter<String> relationCounter = new IntCounter<String>();
	
	public ExtractRelation(StanfordCoreNLP processor) {
		this.processor = processor;
	}
	
	public ExtractRelation() {
		
	}
	
	static CharsetEncoder asciiEncoder = 
		      Charset.forName("US-ASCII").newEncoder(); // or "ISO-8859-1" for ISO Latin 1
		  
		  public static boolean isPureAscii(String v) {
		    return asciiEncoder.canEncode(v);
		  }	
	
	public void findRelations(List<String> sentences, String ent1, String ent2){
		/*org.w3c.dom.Document document;
        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the    
            // XML file
            document = db.parse(fileName);

            NodeList sentenceList = document.getElementsByTagName("SENTENCE");
            System.out.println(String.format("%d sentences found matching '%s' and '%s'.", sentenceList.getLength(),
            								ent1, ent2));// item(0).getTextContent());*/
            for(int sentenceCounter = 0; sentenceCounter < sentences.size(); sentenceCounter++) {
            	String sentence = sentences.get(sentenceCounter);
            	System.out.println("\n\n" + sentence);
            	
            	if(sentence.length() > 400) {
            		System.out.println("Sentence too long.");
            		continue;
            	}	
            	
            	if(!isPureAscii(sentence)) {
            		System.out.println("Contains non-ascii characters. Aborting.");
            		continue;
            	}
    			List<String> relations = findRelation(sentence, ent1, ent2);
    			for(String relation:relations) {
    				relationCounter.incrementCount(relation.toLowerCase());
    			}
            }
        /*}
        catch(Exception ex) {
        	
        }*/
	}
	
	public List<String> findRelation(String sentence, String ent1, String ent2) {
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
			SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
			return findRelation(graph, findWordsInSemanticGraph(graph, ent1), findWordsInSemanticGraph(graph, ent2));
		}
		catch(Exception ex) {
			
		}
		return new ArrayList<String>();
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
	
	public List<String> findRelation(SemanticGraph graph, List<IndexedWord> words1, List<IndexedWord> words2) {
		List<String> relationsFound = new ArrayList<String>();
		List<IndexedWord> shortestPath = new ArrayList<IndexedWord>();
		for(IndexedWord w1: words1) {
			for(IndexedWord w2: words2) {
				List<IndexedWord> current = graph.getShortestUndirectedPathNodes(w1, w2);
				List<SemanticGraphEdge> edges = graph.getShortestUndirectedPathEdges(w1, w2);
				current.remove(w1); current.remove(w2);
				if(shortestPath.size() == 0 ||shortestPath.size() > current.size())
					shortestPath = current; 
			}
		}
		
		if(shortestPath.size() == 1 && !words1.contains(shortestPath.get(0))
				&& !words2.contains(shortestPath.get(0))) {
			relationsFound.add(shortestPath.get(0).originalText());
			for(SemanticGraphEdge edge:graph.getIncomingEdgesSorted(shortestPath.get(0))) {
				if(edge.getRelation().toString().equals("conj_and")&& !words1.contains(edge.getTarget())
						&& !words2.contains(edge.getTarget()))
					relationsFound.add(edge.getSource().originalText());
					
			}
			for(SemanticGraphEdge edge:graph.getOutEdgesSorted(shortestPath.get(0))) {
				if(edge.getRelation().toString().equals("conj_and") && !words1.contains(edge.getTarget())
						&& !words2.contains(edge.getTarget()))
					relationsFound.add(edge.getTarget().originalText());
					
			}
			System.out.println("Returning relations - " + relationsFound);
			return relationsFound;
		}
		else {
			System.out.println("Not returning anything from findRelation - " + shortestPath);
			return relationsFound;
		}
	}
}
