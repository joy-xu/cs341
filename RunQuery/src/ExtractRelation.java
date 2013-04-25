import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;
import java.util.LinkedList;

import java.util.HashMap;

import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.stats.IntCounter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import util.Pair;

public class ExtractRelation {
	StanfordCoreNLP processor;

	public static IntCounter<String> relationCounter = new IntCounter<String>();
	public static IntCounter<Pair<String,String>> entityPairCounter = new IntCounter<Pair<String,String>>();

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
	
	public HashMap<String, Double> findRelations(List<String> sentences, String ent1, String ent2){
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
		    IntCounter<String> relationCounter = new IntCounter<String>();
            for(int sentenceCounter = 0; sentenceCounter < sentences.size(); sentenceCounter++) {
            	String sentence = sentences.get(sentenceCounter);
            	System.out.println("\n\n" + sentence);
            	
            	if(sentence.length() > 500) {
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
            double total = relationCounter.totalCount();
            HashMap<String, Double> normalizedCounts = new HashMap<String, Double>();
            for(String key:relationCounter.keySet())
            	normalizedCounts.put(key, relationCounter.getCount(key) / total);
            return normalizedCounts;
        /*}
        catch(Exception ex) {
        	
        }*/
	}
	
	public void findEntityPairs(List<String> queryOutput, String term)
	{
		
		
		for (int i = 0;i<queryOutput.size();i++)
		{
			String sentence = queryOutput.get(i);
			System.out.println("\n\n" + sentence);
        	
        	if(sentence.length() > 400) {
        		System.out.println("Sentence too long.");
        		continue;
        	}	
        	
        	if(!isPureAscii(sentence)) {
        		System.out.println("Contains non-ascii characters. Aborting.");
        		continue;
        	}
			Pair<String, String> relations = findEntities(sentence, term);
			
			entityPairCounter.incrementCount(relations);
			
		}
		
		//System.out.println(entityPairCounter.toString());
	}
	
	
	public Pair<String,String> findEntities(String sentence, String term)
	{
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
			
			SemanticGraph basicgraph = sentenceMap.get(BasicDependenciesAnnotation.class);
			SemanticGraph ccgraph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
			
			IndexedWord head = findHeadNode(basicgraph, term);
			
			List<IndexedWord> children = ccgraph.getChildList(head);
			Pair<IndexedWord, IndexedWord> entityRoots = findEntityPairs(ccgraph, head, children);
			String first = findChildPhrase(ccgraph,entityRoots.getFirst());
			String second = findChildPhrase(ccgraph,entityRoots.getSecond());
			return new Pair<String,String>(first,second);
		}
		catch(Exception ex) {
			
		}
		return new Pair<String,String>("","");
	}
	
	public String findChildPhrase(SemanticGraph graph, IndexedWord root)
	{
		Set<IndexedWord> children = new HashSet<IndexedWord>();
		
		Queue<IndexedWord> parents = new LinkedList<IndexedWord>();
		parents.add(root);
		
		while(!parents.isEmpty())
		{
			IndexedWord current = parents.poll();
			List<IndexedWord> allChildren = graph.getChildList(current);
			for (int i =0;i<allChildren.size();i++)
			{
				parents.add(allChildren.get(i));
				children.add(allChildren.get(i));
			}
		}
	
		List<IndexedWord> childList = new ArrayList<IndexedWord>(children);
		Collections.sort(childList);
		
		StringBuilder sbuf = new StringBuilder();
		for (int i = 0; i < childList.size();i++)
		{
			sbuf.append(childList.get(i).originalText());
			sbuf.append(" ");
		}
		return sbuf.toString();
	}
	
	public Pair<IndexedWord,IndexedWord> findEntityPairs(SemanticGraph graph, IndexedWord head, List<IndexedWord> children)
	{
		Collections.sort(children);
		for (int i = 0;i<children.size();i++)
		{
			IndexedWord first = children.get(i);
			for (int j = i+1;j<children.size();j++)
			{
				IndexedWord second = children.get(j);
				if ((first.sentIndex() - head.sentIndex())*(second.sentIndex()-head.sentIndex()) >= 0)
					continue;
				
				SemanticGraphEdge firstEdge = graph.getEdge(first, head);
				SemanticGraphEdge secondEdge = graph.getEdge(second,head);
				
				String firstRelation = firstEdge.getRelation().toString();
				String secondRelation = secondEdge.getRelation().toString();
				
				if ((firstRelation.equals("prep_at") && secondRelation.equals("nsubj"))
					|| (firstRelation.equals("nsubj") && secondRelation.equals("prep_at")))
					return new Pair<IndexedWord,IndexedWord>(first,second);
				
			}
		}
		return new Pair<IndexedWord, IndexedWord>(new IndexedWord(), new IndexedWord());
	}
	
	public IndexedWord findHeadNodeForPhrase(String term)
	{
		Annotation document = new Annotation(term);
		processor.annotate(document);
		CoreMap sentenceMap = document.get(SentencesAnnotation.class).get(0);
		SemanticGraph g = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
		return g.getFirstRoot();
	}
	
	
	public IndexedWord findHeadNode(SemanticGraph graph, String term)
	{
		List<String> entitySplits = Arrays.asList(term.split(" "));
		List<IndexedWord> allWords = new ArrayList<IndexedWord>();
		
		for(IndexedWord word: graph.vertexSet()) {
			if(StringUtils.containsIgnoreCase(entitySplits, word.originalText()))
				allWords.add(word);
		}
		
		
		for (IndexedWord word: allWords)
		{
			List<IndexedWord> allParents = graph.getParentList(word);
			boolean isHead = true;
			for (IndexedWord parent:allParents)
			{
				if (allWords.contains(parent))
				{
					isHead = false;
					break;
				}
			}
			if (isHead)
			{
				return word;
			}
		}
		IndexedWord head = findHeadNodeForPhrase(term);
		for(IndexedWord word: graph.vertexSet()) {
			if(head.originalText().equals(word.originalText()))
				return word;
		}
		return new IndexedWord();
	}
	
	public List<String> findRelation(String sentence, String ent1, String ent2) {
		try {
			Annotation document = new Annotation(sentence);
			processor.annotate(document);
			List<String> relations = new ArrayList<String>();
			for(CoreMap sentenceMap : document.get(SentencesAnnotation.class)) {
				SemanticGraph graph = sentenceMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
				relations.addAll(findRelation(graph, findWordsInSemanticGraph(graph, ent1), findWordsInSemanticGraph(graph, ent2)));
			}
			return relations;
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
				//List<SemanticGraphEdge> edges = graph.getShortestUndirectedPathEdges(w1, w2);
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
			//System.out.println("Not returning anything from findRelation - " + shortestPath);
			for(IndexedWord w:shortestPath)
				relationsFound.add(w.originalText());
			return relationsFound;
		}
	}
}
