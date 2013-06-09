package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.Pair;

import retrieWin.Utils.FileUtils;
import retrieWin.Utils.Utils;
import retrieWin.Utils.Utils.TokenComparable;

public class Concept {
	String[] concepts = new String[numConcepts];
	Map<String, Integer> idf = new HashMap<String, Integer>();
	Map<String, Pair<Long, Integer>> kgramLine = new HashMap<String, Pair<Long, Integer>>();
	List<conceptStringInfo> conceptString = new ArrayList<conceptStringInfo>();
	List<String> kgrams = new ArrayList<String>();
	String[] kgramsPostings = new String[numKGrams];
	
	final String s3Directory = "s3://conceptsData/";
	final String localDirectory = "data/";
	final String idfFile = "idf.ser";
	final String kgramsFile = "kgrams";
	final String kgramsPostingsBoundariesFile = "kgramsInv.ser";
	final String kgramsPostingsFile = "kgramsPostings";
	final String conceptsFile = "concepts.ser";
	final String conceptsStringInfoFile = "conceptStringInfo";
	
	final static int numConcepts = 2211776;
	final static int numConceptStrings = 34754240;
	final static int numKGrams = 72199;
	
	public class conceptStringInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		//long start;
		short numKgrams;
		float prob;
		int conceptId;
	}
	
	@SuppressWarnings("unchecked")
	public Concept() {
		try {
			/*System.out.println("reading idf file");
			idf = (Map<String, Integer>) FileUtils.readFile(Utils.getFromS3IfNotExists(s3Directory, localDirectory, idfFile));
	        System.out.println("postings boundaries file");
	        kgramLine = (Map<String, Pair<Long, Integer>>) FileUtils.readFile(Utils.getFromS3IfNotExists(s3Directory, localDirectory, kgramsPostingsBoundariesFile));
	        System.out.println("reading concepts file");
	        concepts = (String[]) FileUtils.readFile(Utils.getFromS3IfNotExists(s3Directory, localDirectory, conceptsFile));
	        System.out.println("reading concept string info file");
	        BufferedReader reader = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists(s3Directory, localDirectory, conceptsStringInfoFile)));
	        String line;
	        int lineNum = 0;
	        while((line = reader.readLine()) != null) {
	        	//lineNum++;
	        	//if(lineNum % 1000000 == 0)
	        		//System.out.println(lineNum);
	        	conceptStringInfo obj = new conceptStringInfo();
	        	String[] split = line.split(",");
	        	obj.numKgrams = Short.parseShort(split[0]);
	        	//obj.start = Long.parseLong(split[1]);
	        	obj.prob = Float.parseFloat(split[1]);
	        	obj.conceptId = Integer.parseInt(split[2]);
	        	conceptString.add(obj);
	        }
	        reader.close();*/
	        /*System.out.println("reading kgrams file");
	        reader = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists(s3Directory, localDirectory, kgramsFile)));
	        while((line = reader.readLine()) != null) {
	        	kgrams.add(line);
	        }
	        reader.close();
	        System.out.println("reading kgrams postings file");
	        reader = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists(s3Directory, localDirectory, kgramsPostingsFile)));
	        lineNum = 0;
	        while((line = reader.readLine()) != null) {
	        	if(lineNum % 1000 == 0)
	        		System.out.println(lineNum);
	        	kgramsPostings[lineNum] = line;
	        	lineNum++;
	        }
	        reader.close();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int getTokenizedKGramMap(String str, Map<String, Map<String, Integer>> tokenKGrams, Map<String, Integer> idf, int k) {
		int totalTokenGrams = 0;
		String kgram;
        List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
        for(String token: tokens) {
        	if(!idf.containsKey(token))
        		idf.put(token, 1);
        	if(tokenKGrams.containsKey(token))
        		continue;
        	Map<String, Integer> temp = new HashMap<String, Integer>();
        	for(int i = 0; i < token.length() + 1 - k; i++) {
				kgram = token.substring(i, i+k);
				if(!temp.containsKey(kgram))
					temp.put(kgram, 1);
				else
					temp.put(kgram, temp.get(kgram) + 1);
			}
        	tokenKGrams.put(token, temp);
        	totalTokenGrams += token.length() + 1 - k;
        }
        return totalTokenGrams;
	}
	
	private Map<Integer, Integer> getMapFromString(String str) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		str = str.replaceAll("\\{", "");
		str = str.replaceAll("\\}", "");
		for(String token: str.split(",")) {
			String[] temp = token.trim().split("=");
			int key = Integer.parseInt(temp[0]);
			int value = Integer.parseInt(temp[1]);
			map.put(key, value);
		}
		return map;
	}

	public String getConcept(String str) {
		return str;
	}
	
	
	public String getCCConcept(String str) {
		Map<String, Double> conceptMax = new HashMap<String, Double>();
		Map<String, Double> conceptTotal = new HashMap<String, Double>();
		Map<String, Integer> conceptCount = new HashMap<String, Integer>();
		Map<String, Map<String, Integer>> tokenKGrams = new HashMap<String, Map<String, Integer>>();
		Map<String, Map<Integer, Integer>> postingsList = new HashMap<String, Map<Integer,Integer>>();
        Map<Integer, Double> conceptStringScores = new HashMap<Integer, Double>();
        Map<Integer, Integer> intersectCountMap = new HashMap<Integer, Integer>();
        String concept;
		double maxVal = 0.0,val = 0.0;
		String maxKey = "";
		int totalTokenGrams, intersectCount;//what kgrams to consider
		
		try {
			//System.out.println("create k-grams for tokens");
	        List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
	        totalTokenGrams = getTokenizedKGramMap(str, tokenKGrams, idf, 3);
	        
	        //System.out.println("Total number of KGrams = " + totalTokenGrams);
	        //System.out.println("order tokens in decreasing order of relevance");
	        Collections.sort(tokens, new TokenComparable(idf));
			
	        //System.out.println("Reading kgrams postings");
	        RandomAccessFile postingsFile = new RandomAccessFile(Utils.getFromS3IfNotExists(s3Directory, localDirectory, kgramsPostingsFile), "r");
	        Map<String, Integer> numPreviousOccurences = new HashMap<String, Integer>();
			for(String token: tokens) {
				System.out.println(token + " " + idf.get(token));
	        	Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
	        	for(String kgramString: tokenKGrams.get(token).keySet()) {
	        		//System.out.println(kgramString);
	        		if(!numPreviousOccurences.containsKey(kgramString))
	        			numPreviousOccurences.put(kgramString, 0);
	        		
	        		if(!postingsList.containsKey(kgramString)) {
	        			if(kgramLine.get(kgramString).first < 0)
	        				System.out.println(kgramLine.get(kgramString));
	        			//System.out.println(kgramLine.get(kgramString));
	        			byte[] b = new byte[kgramLine.get(kgramString).second-1];
	        			postingsFile.seek(kgramLine.get(kgramString).first);
	        			postingsFile.readFully(b);
	        			postingsList.put(kgramString, getMapFromString(new String(b)));
	        		}
	        		
	        		//System.out.println("Finding intersection");
	        		Map<Integer, Integer> currList = postingsList.get(kgramString);
	        		for(int key: currList.keySet()) {
	        			intersectCount = Math.min(currList.get(key) - numPreviousOccurences.get(kgramString), tokenKGrams.get(token).get(kgramString));
	        			if(!temp.containsKey(key))
	        				temp.put(key, intersectCount);
	        			else
	        				temp.put(key, temp.get(key) + intersectCount);
	        		}
	        		
	        		numPreviousOccurences.put(kgramString, numPreviousOccurences.get(kgramString) + 1);
	        	}
	        	
	        	//System.out.println("Updating concept scores");
	        	for(int key: temp.keySet()) {
	        		if(!intersectCountMap.containsKey(key))
	        			intersectCountMap.put(key, temp.get(key));
        			else
        				intersectCountMap.put(key, intersectCountMap.get(key) + temp.get(key));
	        		
	        		if(!conceptStringScores.containsKey(key))
	        			conceptStringScores.put(key, (double)temp.get(key)/idf.get(token));
	        		else
	        			conceptStringScores.put(key, conceptStringScores.get(key) + (double)temp.get(key)/idf.get(token));
	        	}
			}  	
			for(int key: intersectCountMap.keySet()) {
				if((double)intersectCountMap.get(key)/totalTokenGrams < 0.4)
					conceptStringScores.remove(key);
			}
			postingsFile.close();
	        
			BufferedWriter writer = new BufferedWriter(new FileWriter("blah"));
			//System.out.println("Finding most relevant concept");
			//System.out.println(conceptStringScores.keySet().size());
	        for(int key: conceptStringScores.keySet()) {
	        	concept = concepts[conceptString.get(key-1).conceptId];
	        	if(concept.equals("sildenafil"))
	        		writer.write(key);
	        	//System.out.println(concept);
	        	val = conceptString.get(key-1).prob*conceptStringScores.get(key)/(totalTokenGrams + conceptString.get(key-1).numKgrams);
				if(!conceptMax.containsKey(concept))
					conceptMax.put(concept, val);
				else if(conceptMax.get(concept) < val)
					conceptMax.put(concept, val);
				
				if(!conceptTotal.containsKey(concept))
					conceptTotal.put(concept, val);
				else
					conceptTotal.put(concept, conceptTotal.get(concept) + val);
				
				if(!conceptCount.containsKey(concept))
					conceptCount.put(concept, 1);
				else
					conceptCount.put(concept, conceptCount.get(concept) + 1);
			}
	        writer.close();
	        //System.out.println("Calculating max");
	        //System.out.println(conceptMax.keySet().size());
	        maxVal = 0.0;
			for(String key: conceptMax.keySet()) {
				val = Math.log(conceptCount.get(key))*conceptMax.get(key);
				if(key.equals("seagram"))
					System.out.println(key + "," + val + "," + (conceptTotal.get(key)/conceptCount.get(key)) + "," + conceptCount.get(key));
				if(maxVal == 0.0) {
					maxKey = key;
					maxVal = val;
				}
				if((val > maxVal) || ((val == maxVal) && ((conceptTotal.get(key)/conceptCount.get(key)) > (conceptTotal.get(maxKey)/conceptCount.get(maxKey))))) {
					maxKey = key;
					maxVal = val;
				}
			}
	        System.out.println(maxKey + ", " + maxVal + "," + (conceptTotal.get(maxKey)/conceptCount.get(maxKey)) + "," + conceptCount.get(maxKey));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return maxKey;
	}
/*
	public String getCConcept(String str) {
		Map<String, Double> conceptMax = new HashMap<String, Double>();
		Map<String, Double> conceptTotal = new HashMap<String, Double>();
		Map<String, Integer> conceptCount = new HashMap<String, Integer>();
		Map<String, Map<String, Integer>> tokenKGrams = new HashMap<String, Map<String, Integer>>();
		Map<String, Map<Integer, Integer>> postingsList = new HashMap<String, Map<Integer,Integer>>();
        Map<Integer, Double> conceptStringScores = new HashMap<Integer, Double>();
        Map<Integer, Integer> intersectCountMap = new HashMap<Integer, Integer>();
        String concept;
		double maxVal = 0.0,val = 0.0;
		String maxKey = "";
		int totalTokenGrams, intersectCount;//what kgrams to consider
		
		try {
			System.out.println("create k-grams for tokens");
	        List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
	        totalTokenGrams = getTokenizedKGramMap(str, tokenKGrams, idf, 3);
	        
	        System.out.println("order tokens in decreasing order of relevance");
	        Collections.sort(tokens, new TokenComparable(idf));
			
	        //RandomAccessFile postingsFile = new RandomAccessFile(Utils.getFromS3IfNotExists(s3Directory, localDirectory, kgramsPostingsFile), "r");
	        Map<String, Integer> numPreviousOccurences = new HashMap<String, Integer>();
			for(String token: tokens) {
	        	Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
	        	for(String kgramString: tokenKGrams.get(token).keySet()) {
	        		if(!numPreviousOccurences.containsKey(kgramString))
	        			numPreviousOccurences.put(kgramString, 0);
	        		
	        		if(!postingsList.containsKey(kgramString)) {
	        			if(kgramLine.get(kgramString) < 0)
	        				System.out.println(kgramLine.get(kgramString));
	        			//postingsFile.seek(kgramLine.get(kgramString));
	        			postingsList.put(kgramString, getMapFromString(kgramsPostings.get(kgrams.indexOf(kgramString))));
	        		}
	        		
	        		Map<Integer, Integer> currList = postingsList.get(kgramString);
	        		for(int key: currList.keySet()) {
	        			intersectCount = Math.min(currList.get(key) - numPreviousOccurences.get(kgramString), tokenKGrams.get(token).get(kgramString));
	        			if(!temp.containsKey(key))
	        				temp.put(key, intersectCount);
	        			else
	        				temp.put(key, temp.get(key) + intersectCount);
	        		}
	        		
	        		numPreviousOccurences.put(kgramString, numPreviousOccurences.get(kgramString) + 1);
	        	}
	        	
	        	for(int key: temp.keySet()) {
	        		if(!intersectCountMap.containsKey(key))
	        			intersectCountMap.put(key, temp.get(key));
        			else
        				intersectCountMap.put(key, intersectCountMap.get(key) + temp.get(key));
	        		
	        		if(!conceptStringScores.containsKey(key))
	        			conceptStringScores.put(key, (double)temp.get(key)/idf.get(token));
	        		else
	        			conceptStringScores.put(key, conceptStringScores.get(key) + (double)temp.get(key)/idf.get(token));
	        	}
			}  	
			for(int key: intersectCountMap.keySet()) {
				if((double)intersectCountMap.get(key)/totalTokenGrams < 0.2)
					conceptStringScores.remove(key);
			}
			//postingsFile.close();
	        
			System.out.println(conceptStringScores.keySet().size());
	        for(int key: conceptStringScores.keySet()) {
	        	concept = concepts.get(conceptString.get(key-1).conceptId);
	        	//System.out.println(concept);
	        	val = conceptString.get(key-1).prob*conceptStringScores.get(key)/(totalTokenGrams + conceptString.get(key-1).numKgrams);
				if(!conceptMax.containsKey(concept))
					conceptMax.put(concept, val);
				else if(conceptMax.get(concept) < val)
					conceptMax.put(concept, val);
				
				if(!conceptTotal.containsKey(concept))
					conceptTotal.put(concept, val);
				else
					conceptTotal.put(concept, conceptTotal.get(concept) + val);
				
				if(!conceptCount.containsKey(concept))
					conceptCount.put(concept, 1);
				else
					conceptCount.put(concept, conceptCount.get(concept) + 1);
			}
	        
	        System.out.println("Calculating max");
	        System.out.println(conceptMax.keySet().size());
	        maxVal = 0.0;
			for(String key: conceptMax.keySet()) {
				val = conceptMax.get(key);
				if(maxVal == 0.0) {
					maxKey = key;
					maxVal = val;
				}
				if((val > maxVal) || ((val == maxVal) && ((conceptTotal.get(key)/conceptCount.get(key)) > (conceptTotal.get(maxKey)/conceptCount.get(maxKey))))) {
					maxKey = key;
					maxVal = val;
				}
			}
	        System.out.println(maxKey + ", " + maxVal);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return maxKey;
	}
*/
}
