package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

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
	
	public static String findExpandedEntity(CoreMap sentence, IndexedWord word) {
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
