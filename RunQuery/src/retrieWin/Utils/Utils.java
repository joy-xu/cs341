package retrieWin.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.thrift.protocol.TBinaryProtocol;

import edu.stanford.nlp.util.Pair;


import retrieWin.Indexer.Downloader;
import retrieWin.Indexer.ThriftReader;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Constants;
import retrieWin.SSF.arxivDocument;
import streamcorpus.OffsetType;
import streamcorpus.Rating;
import streamcorpus.Sentence;
import streamcorpus.StreamItem;
import streamcorpus.Token;

public class Utils {
	static public class TokenComparable implements Comparator<String>{
		 
		Map<String, Integer> _idf;
		public TokenComparable(Map<String, Integer> idf) {
			_idf = idf;
		}
	    @Override
	    public int compare(String token1, String token2) {
	        return (_idf.get(token1)<_idf.get(token2) ? -1 : (_idf.get(token1)==_idf.get(token2) ? 0 : 1));
	    }
	}
	
	public static List<String> getEquivalents(String expansions)
	{
		List<String> stopWords = Arrays.asList(new String[] {"a", "an", "and", "are", "as", "at", "be", "by", "for",
				"from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with"});
		List<String> equivalents = new ArrayList<String>();
		for(String entity:expansions.split("\\$")) {
			String[] terms = entity.split("[_.,()]");
			List<String> qTerms = new ArrayList<String>();
			for (int i = 0;i<terms.length;i++)
				if (terms[i].length() > 0)
					qTerms.add(terms[i]);
			
			int numTerms = qTerms.size();
			switch(numTerms) {
				case 1:
					equivalents.add(qTerms.get(0));
					break;
				case 2:
					equivalents.add(qTerms.get(0) + " " + qTerms.get(1));
					equivalents.add(qTerms.get(1) + " " + qTerms.get(0));
					equivalents.add(qTerms.get(0));
					equivalents.add(qTerms.get(1));
					break;
				default:
					for(int i = 0; i < numTerms; i++) {
						for(int j = i+1; j < numTerms; j++) {
							if(!stopWords.contains(qTerms.get(i)) && !stopWords.contains(qTerms.get(j)))
								equivalents.add(qTerms.get(i) + " " + qTerms.get(j));
						}
					}
			}
		}
		return equivalents;
	}
	

	@SuppressWarnings("unchecked")
	public static String getConcept(String str) {
		Map<String, Double> conceptRelevance = new HashMap<String, Double>();
		Map<String, Double> conceptMax = new HashMap<String, Double>();
		Map<String, Double> conceptTotal = new HashMap<String, Double>();
		Map<String, Integer> conceptCount = new HashMap<String, Integer>();
		Map<String, Integer> idf = new HashMap<String, Integer>();
		Map<String, List<String>> tokenKGrams = new HashMap<String, List<String>>();
		List<String> wordKGrams = new ArrayList<String>();
		String line = "", kgram = "", concept, word;
		String[] words;
		double prob, maxVal = 0.0,val = 0.0;
		String maxKey = "";
		int k = 3;//what kgrams to consider
		File f;
		Process p;
		
		int intersectCount, numKGrams, totalTokenGrams, count;
		List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
		
		try {
			//System.out.println("read in idf values");
			f = new File("data/idf.ser");
			if(!f.exists()) {
				System.out.println("Copying idf file");
				String s3PutIndex = String.format("s3cmd get %s data/idf.ser",Constants.s3ConceptsDirectory+"idf.ser");
				System.out.println(s3PutIndex);
				p = Runtime.getRuntime().exec(s3PutIndex);
				p.waitFor();
				System.out.println("idf file copied");
			}
			FileInputStream fis = new FileInputStream("data/idf.ser");
	        ObjectInputStream ois = new ObjectInputStream(fis);
	        idf = (Map<String, Integer>) ois.readObject();
	        ois.close();
	        
	        //System.out.println("create k-grams for tokens");
	        totalTokenGrams = 0;
	        for(String token: tokens) {
	        	if(!idf.containsKey(token))
	        		idf.put(token, 1);
	        	//System.out.println(token + ":" + idf.get(token));
	        	if(!tokenKGrams.containsKey(token))
	        		tokenKGrams.put(token, new ArrayList<String>());
	        	List<String> temp = tokenKGrams.get(token);
	        	for(int i = 0; i < token.length() + 1 - k; i++) {
					kgram = token.substring(i, i+k);
					temp.add(kgram);
				}
	        	tokenKGrams.put(token, temp);
	        	totalTokenGrams += token.length() + 1 - k;
	        }
	        //System.out.println("order tokens in decreasing order of relevance");
	        Collections.sort(tokens, new TokenComparable(idf));
			
	        f = new File("data/conceptDict");
			if(!f.exists()) {
				System.out.println("Copying gcld concept file");
				String s3PutIndex = String.format("s3cmd get %s data/conceptDict",Constants.s3ConceptsDirectory+"conceptDict");
				System.out.println(s3PutIndex);
				p = Runtime.getRuntime().exec(s3PutIndex);
				p.waitFor();
				System.out.println("gcld concept file copied");
			}
			BufferedReader reader = new BufferedReader(new FileReader("data/conceptDict"));
			while((line = reader.readLine()) != null) {
				words = line.split("\t");
				if(words.length < 2)
					continue;
				concept = words[1].split(" ")[1].toLowerCase();
				prob = Double.parseDouble(words[1].split(" ")[0]);
				//create kgrams for words
				wordKGrams.clear();
				word = words[0].toLowerCase();
				numKGrams = 0;
				for(String wordKey: word.split(" ")) {
					for(int i = 0; i < wordKey.length() + 1 - k; i++) {
						kgram = wordKey.substring(i, i+k);
						wordKGrams.add(kgram);
						numKGrams++;
					}
				}
				if(numKGrams < 1)
					continue;
				
				//find intersection of kgrams, weight by idf value
				val = 0.0;
				count = numKGrams;
				for(String token: tokens) {
					wordKGrams.removeAll(tokenKGrams.get(token));
					intersectCount = numKGrams - wordKGrams.size();
					numKGrams = wordKGrams.size();
					val += (double)intersectCount/idf.get(token);
				}
				//consider only those above certain threshold
				if((double)(count - numKGrams)/totalTokenGrams < 0.2)
					continue;
				//if(concept.equals("seagram"))
					//System.out.println("$" + val);
				val = prob*val/(totalTokenGrams + numKGrams);
				if(!conceptMax.containsKey(concept)) {
					conceptMax.put(concept, 0.0);
					conceptTotal.put(concept, 0.0);
					conceptCount.put(concept, 0);
				}
				conceptMax.put(concept, Math.max(val, conceptMax.get(concept)));
				conceptTotal.put(concept, val + conceptTotal.get(concept));
				conceptCount.put(concept, 1 + conceptCount.get(concept));
			}
			reader.close();
				
			//System.out.println("compute relevance, use max score and give more weight to higher average score");
			maxVal = 0.0;
			for(String key: conceptMax.keySet()) {
				conceptRelevance.put(key, conceptMax.get(key));//*(conceptTotal.get(key)/conceptCount.get(key)));
				val = conceptRelevance.get(key);
				if(maxVal == 0.0) {
					maxKey = key;
					maxVal = val;
				}
				if((val > maxVal) || ((val == maxVal) && ((conceptTotal.get(key)/conceptCount.get(key)) > (conceptTotal.get(maxKey)/conceptCount.get(maxKey))))) {
					maxKey = key;
					maxVal = val;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return maxKey;
	}

	
	public static Map<Integer, Integer> getMapFromString(String str) {
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
	
	public static String getFromS3IfNotExists(String s3Directory, String localDirectory, String file) throws IOException, InterruptedException {
		File f = new File(localDirectory + file);
		Process p;
		if(!f.exists()) {
			System.out.println("Copying " + file);
			String s3PutIndex = String.format("s3cmd get %s %s", s3Directory+file, localDirectory+file);
			System.out.println(s3PutIndex);
			p = Runtime.getRuntime().exec(s3PutIndex);
			p.waitFor();
			System.out.println(file +  " copied");
		}
		return localDirectory + file;
	}
	
	private static int getTokenizedKGramMap(String str, Map<String, Map<String, Integer>> tokenKGrams, Map<String, Integer> idf, int k) {
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
	
	public static class conceptStringInfo implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		long start;
		int numKgrams;
		double prob;
		int conceptId;
	}


	//@SuppressWarnings("unchecked")
	public static String getCConcept(String str) {
		Map<String, Double> conceptRelevance = new HashMap<String, Double>();
		Map<String, Double> conceptMax = new HashMap<String, Double>();
		Map<String, Double> conceptTotal = new HashMap<String, Double>();
		Map<String, Integer> conceptCount = new HashMap<String, Integer>();
		Map<Integer, String> concepts = new HashMap<Integer, String>();
		Map<String, Integer> idf = new HashMap<String, Integer>();
		List<String> kgrams = new ArrayList<String>();
		Map<String, Map<String, Integer>> tokenKGrams = new HashMap<String, Map<String, Integer>>();
		Map<String, Map<Integer, Integer>> postingsList = new HashMap<String, Map<Integer,Integer>>();
        Map<Integer, Double> conceptStringScores = new HashMap<Integer, Double>();
        Map<String, Long> kgramLine = new HashMap<String, Long>();
		List<String> wordKGrams = new ArrayList<String>();
		List<conceptStringInfo> conceptString = new ArrayList<conceptStringInfo>();
		List<Long> conceptStringLine = new ArrayList<Long>();
		String line = "", kgram = "", concept, word;
		String[] words;
		double prob, maxVal = 0.0,val = 0.0;
		String maxKey = "";
		int k = 3;//what kgrams to consider
		File f;
		Process p;
		BufferedReader reader;
		
		Map<Integer, Integer> intersectCountMap = new HashMap<Integer, Integer>();
		
		int intersectCount, numKGrams, totalTokenGrams, count;
		
		try {
			System.out.println("read idf file");
			idf = (Map<String, Integer>) FileUtils.readFile(getFromS3IfNotExists(Constants.s3ConceptsDirectory, "data/", "idf.ser"));
	        
	        System.out.println("read kgrams file");
	        kgramLine = (Map<String, Long>) FileUtils.readFile(getFromS3IfNotExists(Constants.s3ConceptsDirectory, "data/", "kgramsPostingsLineBoundaries.ser"));
	        //System.out.println(kgramLine);
	        System.out.println("create k-grams for tokens");
	        List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
	        totalTokenGrams = getTokenizedKGramMap(str, tokenKGrams, idf, 3);
	        
	        System.out.println("order tokens in decreasing order of relevance");
	        Collections.sort(tokens, new TokenComparable(idf));
			
	        RandomAccessFile postingsFile = new RandomAccessFile(getFromS3IfNotExists(Constants.s3ConceptsDirectory, "data/", "kgramsPostings"), "r");
	        Map<String, Integer> numPreviousOccurences = new HashMap<String, Integer>();
			for(String token: tokens) {
	        	Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
	        	for(String kgramString: tokenKGrams.get(token).keySet()) {
	        		if(!numPreviousOccurences.containsKey(kgramString))
	        			numPreviousOccurences.put(kgramString, 0);
	        		
	        		if(!postingsList.containsKey(kgramString)) {
	        			if(kgramLine.get(kgramString) < 0)
	        				System.out.println(kgramLine.get(kgramString));
	        			postingsFile.seek(kgramLine.get(kgramString));
	        			postingsList.put(kgramString, getMapFromString(postingsFile.readLine()));
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
			
	        System.out.println("read concepts file");
	        concepts = (Map<Integer, String>) FileUtils.readFile(getFromS3IfNotExists(Constants.s3ConceptsDirectory, "data/", "concepts.ser"));
	        //System.out.println(concepts);
	        System.out.println("read concept string info");
	        reader = new BufferedReader(new FileReader("data/conceptStringInfo"));
	        while((line = reader.readLine()) != null) {
	        	conceptStringInfo obj = new conceptStringInfo();
	        	String[] split = line.split(",");
	        	obj.numKgrams = Integer.parseInt(split[0]);
	        	obj.start = Long.parseLong(split[1]);
	        	obj.prob = Double.parseDouble(split[2]);
	        	obj.conceptId = Integer.parseInt(split[3]);
	        	conceptString.add(obj);
	        }
	        //RandomAccessFile conceptFile = new RandomAccessFile(getFromS3IfNotExists(Constants.s3ConceptsDirectory, "data/", "conceptDict"), "r");
	        //System.out.println(conceptStringLine);
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
				//conceptRelevance.put(key, conceptMax.get(key));//*(conceptTotal.get(key)/conceptCount.get(key)));
				val = conceptMax.get(key);
				if(maxVal == 0.0) {
					maxKey = key;
					maxVal = val;
					//System.out.println(maxKey + ", " + maxVal);
				}
				if((val > maxVal) || ((val == maxVal) && ((conceptTotal.get(key)/conceptCount.get(key)) > (conceptTotal.get(maxKey)/conceptCount.get(maxKey))))) {
					System.out.println("^" + maxKey + ", " + maxVal);
					maxKey = key;
					maxVal = val;
					System.out.println("$" + maxKey + ", " + maxVal);
				}
			}
	        System.out.println(maxKey + ", " + maxVal);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return maxKey;
	}

	
	public static String bb_to_str(ByteBuffer buffer){
		String data = "";
		try {
			int old_position = buffer.position();
		    data = Charset.forName("UTF-8").newDecoder().decode(buffer).toString();
		    // reset buffer's position to its original so it is not altered:
		    buffer.position(old_position);  
		  }catch (Exception e){
		    e.printStackTrace();
		    return "";
		  }
		return data;
	}
	
	public static List<String> getSentences(StreamItem item) {
		List<String> tempSentences = new ArrayList<String>();
        List<String> allSentences = new ArrayList<String>();
        int index;
        String sent;
    	
        List<Sentence> s = item.body.sentences.get("lingpipe");
        for (int n = 0;n<s.size();n++)
    	{
    		List<Token> tList = s.get(n).getTokens();
    		StringBuilder sbuf = new StringBuilder();
    		
    		for (int t = 0;t<tList.size();t++)
    		{
    			
    			sbuf.append(tList.get(t).token);
    			sbuf.append(" ");
    		}
    		tempSentences.add(sbuf.toString());
    	}
    	
    	//tempSentences = Arrays.asList(" ACKNOWLEDGMENTS The author would like to thank H", "  Kamano , T",  "Penin and V.A.");
    	index = 0;
    	while(index < tempSentences.size()) {
    		sent = tempSentences.get(index).trim();
			index++;
			if(sent.isEmpty())
				continue;
			
			while(Pattern.matches(".*( |^)(([a-zA-Z]|Prof|prof) *\\.{0,1} *){1,3}", sent) && (index < tempSentences.size())) {
				sent += " " + tempSentences.get(index);
				index++;
				//System.out.println(sent + Pattern.matches(".* ([a-zA-Z] *\\.{0,1}){1,3}", sent));
			}
			
			if(sent.length() > 400) {
				for(String subSent: sent.split("\n|\\."))
					allSentences.add(subSent);
			}
			else {
				//System.out.println(sent);
				allSentences.add(sent.trim());
			}
    	}
    	return allSentences;
	}
	
	public static arxivDocument getArxivDoc(String docNo) {
		boolean inAckSec, inRefSec;
        Matcher matcher;
        NLPUtils coreNLP = new NLPUtils();
        arxivDocument doc = new arxivDocument();
		
        String[] a = docNo.split("__");
		String streamID = a[0];
		String localfilename = a[1];
		String[] b = localfilename.split("/");
		String fileName = b[b.length-1];
		String folder = a[2];
		
		try {
			StreamItem item = ThriftReader.GetFilteredStreamItems(folder, fileName, "tmp/", new HashSet<String>(Arrays.asList(streamID))).get(0);
	        
			for(String auth: bb_to_str(item.other_content.get("abstract").raw).split("Authors:|Categories:")[1].trim().split(",| and ")) {
				if(!auth.isEmpty())
					doc.addAuthor(auth.replaceAll("\n", " ").trim());
			}
			inAckSec = false;
			inRefSec = false;
			for(String sent: getSentences(item)) {
				if(sent.toLowerCase().contains("acknowledg") || sent.toLowerCase().contains("thank") || sent.toLowerCase().contains("grateful")) {
					inAckSec = true;
					if(sent.length() <= 20)
						continue;
				}
				else if(sent.toLowerCase().replaceAll(" ", "").contains("reference")) {
					inRefSec = true;
					if(sent.length() <= 20)
						continue;
				}
				if(inAckSec) {
					for(String per: coreNLP.getPersons(sent))
						doc.addAcknowledgement(per);
					inAckSec = false;
				}
				
				if(inRefSec || sent.startsWith("[")) {
					matcher = Pattern.compile("\\]").matcher(sent);
					if(!matcher.find()) {
						matcher = Pattern.compile("[a-zA-Z]").matcher(sent);
						if(!matcher.find())
							continue;
						else
							sent = sent.substring(Integer.valueOf(matcher.start()), sent.length()-1);
					}
					else if(matcher.start() == sent.length()-1)
						continue;
					else
						sent = sent.substring(Integer.valueOf(matcher.start()) + 1, sent.length()-1);
					
					List<String> reference = new ArrayList<String>();
					for(String auth: sent.split(",| and ")) {
						if(auth.isEmpty())
							continue;
						if(auth.length() < 20 && Pattern.matches("[a-zA-Z \\.]*[A-Z]\\.[a-zA-Z \\.]*", auth))
							reference.add(auth);
						else
							break;
					}
					if(!reference.isEmpty())
						doc.addReferences(reference);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return doc;
	}
	
	public static Pair<Long, Long> getByteOffset(String id, String slotValue, String workingDirectory) {
		long start = -1, end = -1;
		String tempVal = "";
		StreamItem item;
		
		String[] split = id.split("__");
		String streamId = split[0];
		String file = split[1];
		String folder = split[2];
		int sentIndex = Integer.parseInt(split[3]);
		System.out.println(sentIndex);
		System.out.println(id);
		System.out.println(streamId);
		System.out.println("Downloading file");
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new Pair<Long, Long>(start,end);
		int count = 0;
		try 
		{
			TBinaryProtocol protocol = ThriftReader.openBinaryProtocol(downloadedFile);
			while (true) 
	        {
	            item = new StreamItem(); 
	            try {
	            	item.read(protocol);  
	            }
	            catch (Exception transportException)
	            {
	            	break;
	            }
	            
            	if (!item.stream_id.equals(streamId))
            		continue;
            	System.out.println(item.stream_id);
            	
            	try {
	            	Sentence s = item.body.sentences.get("lingpipe").get(sentIndex);
	                
	            	start = end = -1;
	            	for (Token tok: s.getTokens())
	        		{
	            		if(start == -1 && slotValue.contains(tok.token)) {
	            			start = tok.offsets.get(OffsetType.BYTES).first;
	            			end = start + tok.offsets.get(OffsetType.BYTES).length;
	            			tempVal += tok.token + " ";
	            		}
	            		else if (start != -1 && slotValue.contains(tok.token)) {
	            			end += tok.offsets.get(OffsetType.BYTES).length;
	            			tempVal += tok.token + " ";
	            		}
	            		else if(!slotValue.contains(tok.token)) {
	            			if(tempVal.trim().equals(slotValue.trim()))
	            				break;
	            			else {
	            				start = -1;
	            				end = -1;
	            				tempVal = "";
	            			}
	            		}
	        		}
            	}
            	catch (Exception e) {
            		continue;
            	}
            	
            	if(end != -1)
            		break;
            }
			
			/*byte[] bytes = item.body.clean_visible.getBytes("UTF-8");
        	for(long i = start; i <= end; i++)
        		System.out.print((char)bytes[(int)i]);*/
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new Pair<Long, Long>(start,end);
	}
	
	public static synchronized Set<String> getManualAnnotationsForDocument(String docId, String workingDirectory)
	{
		Set<String> output = new HashSet<String>();
		StreamItem item;
		String[] split = docId.split("__");
		
		String streamId = split[0];
		String file = split[1];
		String folder = split[2];
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		if (downloadedFile == null)
			return output;
		try 
		{
			TBinaryProtocol protocol = ThriftReader.openBinaryProtocol(downloadedFile);
			while (true) 
	        {
	            item = new StreamItem(); 
	            try {
	            	item.read(protocol);  
	            }
	            catch (Exception transportException)
	            {
	            	break;
	            }
	            
            	if (!item.stream_id.equals(streamId))
            		continue;
            	
            	System.out.println(item.stream_id);
            	
            	try {
	                
	            	if (item.ratings == null){
	            		System.out.println("Ratings file is null");
	            		break;
	            	}
            		for (String a:item.ratings.keySet())
            		{
            			List<Rating> ratings = item.ratings.get(a);
            			if (ratings.isEmpty())
            				System.out.println("Map exists but Ratings list empty");
            			for (Rating r : ratings)
            			{
            				System.out.println("Rating is : " + r.relevance);
            				if (r.relevance == 1 || r.relevance == 2)
            				{
            					output.add(r.target.target_id);
            				}
            			}
            		}
	            	break;
	            }
            	catch (Exception e) {
            		continue;
            	}
            }
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		return output;
	}

}

	