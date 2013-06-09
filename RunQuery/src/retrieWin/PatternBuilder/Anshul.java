package retrieWin.PatternBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

import retrieWin.Indexer.Downloader;
import retrieWin.Indexer.ThriftReader;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Concept;
import retrieWin.SSF.Constants;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SSF;
import retrieWin.SSF.Slot;
import retrieWin.SSF.arxivDocument;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;
import streamcorpus.Sentence;
import streamcorpus.StreamItem;
import streamcorpus.Token;

public class Anshul {

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
	
	public static class conceptStringInfo implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//long start;
		short numKgrams;
		float prob;
		int conceptId;
	}
	
	public static String getConcept(String str) {
		Map<String, Double> conceptRelevance = new HashMap<String, Double>();
		Map<String, Double> conceptMax = new HashMap<String, Double>();
		Map<String, Double> conceptTotal = new HashMap<String, Double>();
		Map<String, Integer> conceptCount = new HashMap<String, Integer>();
		Map<String, Integer> idf = new HashMap<String, Integer>();
		Map<String, List<String>> tokenKGrams = new HashMap<String, List<String>>();
		//Map<String, Map<String, Double>> conceptDict = new HashMap<String, Map<String, Double>>();
		List<String> wordKGrams = new ArrayList<String>();
		String line = "", kgram = "", concept, word;
		String[] words;
		double maxVal = 0.0,val = 0.0;
		float prob;
		String maxKey = "";
		int k = 3;
		
		int intersectCount, numKGrams, totalTokenGrams, count;
		int lineNum;
		List<String> tokens = Arrays.asList(str.toLowerCase().split(" "));
		
		Map<String, Map<Integer, Integer>> postingsMap = new HashMap<String, Map<Integer,Integer>>();
		for(int x = 1; x <= 1; x++) {
		postingsMap.clear();
		int numFiles = 17;
		Map<String, Long> kgramLine = new HashMap<String, Long>();
		Map<String, Pair<Long, Integer>> kgramInv = new HashMap<String, Pair<Long,Integer>>();
		List<String> kgrams = new ArrayList<String>();
		try {
			System.out.println("postings boundaries file");
	        kgramLine = (Map<String, Long>) FileUtils.readFile(Utils.getFromS3IfNotExists("s3://conceptsData/", "data/", "kgramsPostingsLineBoundaries.ser"));
	        long start, size;
	        
	        System.out.println("reading kgrams file");
	        BufferedReader reader = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists("s3://conceptsData/", "data/", "kgrams")));
	        BufferedReader reader1 = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists("s3://conceptsData/", "data/", "kgramsPostingsLineBoundaries")));
	        BufferedWriter writer1 = new BufferedWriter(new FileWriter("data/blah1"));
	        start = 0;
	        lineNum = 1;
	        String line1;
	        while((line = reader.readLine()) != null) {
	        	line1 = reader1.readLine();
	        	size = kgramLine.get(line);
	        	//writer1.write(start + " " + (size-start) + "\n");
	        	start = size;
	            lineNum++;
		    	kgramInv.put(line, new Pair<Long, Integer>(Long.parseLong(line1.split(" ")[0]),Integer.parseInt(line1.split(" ")[1])));
		    }
	        reader.close();
	        //writer1.close();
			
	        for(String kg: kgramInv.keySet()) {
	        	writer1.write(kg + " " + kgramInv.get(kg).first + " " + kgramInv.get(kg).second + "\n");
	        }
	        writer1.close();
	        FileOutputStream fout = new FileOutputStream("data/kgramsInv.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(kgramInv);
			oos.close();
	        
			/*System.out.println("reading kgrams postings file");
	        BufferedReader reader = new BufferedReader(new FileReader(Utils.getFromS3IfNotExists("s3://conceptsData/", "data/", "kgramsPostings")));
	        BufferedWriter writer1 = new BufferedWriter(new FileWriter("data/blah2"));
	        lineNum = 0;
	        long size = 0;
	        while((line = reader.readLine()) != null) {
	        	if(lineNum % 1000 == 0)
	        		System.out.println(lineNum);
	        	size += line.length() + 1;
	        	writer1.write(size + "\n");
	        	lineNum++;
	        }
	        reader.close();
	        writer1.close();*/
			/*Map<String, Integer> bound = new HashMap<String, Integer>();
			FileInputStream fis = new FileInputStream("data/kgrams_bound");
	        ObjectInputStream ois = new ObjectInputStream(fis);
	        bound = (Map<String, Integer>) ois.readObject();
	        ois.close();
	        
	        RandomAccessFile postingsFile = new RandomAccessFile("data/kgramsPostings", "r");
	        postingsFile.seek(bound.get("'',"));
	        System.out.println(postingsFile.readLine());*/
			/*Map<String, Long> bound = new HashMap<String, Long>();
			List<conceptStringInfo> z = new ArrayList<conceptStringInfo>(); 
			Map<String, Integer> concepts = new HashMap<String, Integer>();
			//Map<Integer, String> conceptsinv = new HashMap<Integer, String>();
			String[] conceptsinv = new String[2211776];
			BufferedWriter writer1 = new BufferedWriter(new FileWriter("data/conceptStringInfo"));
			
			BufferedReader reader = new BufferedReader(new FileReader("data/conceptDict"));
			//BufferedReader reader1 = new BufferedReader(new FileReader("data/kgrams"));
			long start = 0;
			lineNum = 0;
			count = 0;
			int id = 0;
			while((line = reader.readLine()) != null) {
				//kgram = reader1.readLine();
				words = line.split("\t");
				concept = words[1].split(" ")[1].toLowerCase();
				if(concept.length() > count) {
					count = concept.length();
					System.out.println(count);
				}
				prob = Float.parseFloat(words[1].split(" ")[0]);
				word = words[0].toLowerCase();
				numKGrams = 0;
				for(String wordKey: word.split(" ")) 
					numKGrams += Math.max(0, wordKey.length() + 1 - k);
				
				lineNum++;
				if(lineNum % 1000000 == 0)
					System.out.println(lineNum);
				//conceptStringInfo blah = new conceptStringInfo();
				//blah.numKgrams = numKGrams;
				//blah.start = start;
				//blah.prob = prob;
				if(!concepts.containsKey(concept)) {
					concepts.put(concept, id);
					conceptsinv[id] = concept;
					id++;
				}
				//blah.conceptId = concepts.get(concept);
				writer1.write(numKGrams + "," + prob + "," + concepts.get(concept) + "\n");
				//z.add(blah);
				//bound.put(kgram, start);
				start += (line.length() + 1);
			}
			reader.close();
			writer1.close();
			System.out.println(id);
			FileOutputStream fout = new FileOutputStream("data/concepts.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(conceptsinv);
			oos.close();*/
			/*FileOutputStream fout = new FileOutputStream("data/conceptStringInfo.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);   
			oos.writeObject(z);
			oos.close();*/
			/*BufferedReader[] postings = new BufferedReader[numFiles];
			String[] postingsLine = new String[numFiles];
			BufferedReader[] kgrams = new BufferedReader[numFiles];
			String[] kgramsLine = new String[numFiles];
			for(int y = 1; y <= numFiles; y++) {
				postings[y-1] = new BufferedReader(new FileReader("data/concepts/kgramPostings" + y));
				postingsLine[y-1] = postings[y-1].readLine();
				kgrams[y-1] = new BufferedReader(new FileReader("data/concepts/kgrams" + y));
				kgramsLine[y-1] = kgrams[y-1].readLine();
			}

			BufferedWriter writer1 = new BufferedWriter(new FileWriter("data/kgramsPostings"));
			BufferedWriter writer2 = new BufferedWriter(new FileWriter("data/kgrams"));
			List<Integer> minIndices = new ArrayList<Integer>();
			String minKgram;
			lineNum = 0;
			do{
				lineNum++;
				if(lineNum % 1000 == 0) {
					System.out.println(lineNum);
					System.out.println(minIndices);
				}
				minIndices.clear();
				minKgram = null;
				for(int i = 0; i < numFiles; i++) {
					if(kgramsLine[i] != null) {
						if(minKgram == null) {
							minKgram = kgramsLine[i];
							minIndices.add(i);
						}
						else if (minKgram.compareTo(kgramsLine[i]) == 0) {
							minIndices.add(i);
						}
						else if (minKgram.compareTo(kgramsLine[i]) > 0) {
							minIndices.clear();
							minKgram = kgramsLine[i];
							minIndices.add(i);
						}
					}
				}
				Map<Integer, Integer> map = new HashMap<Integer, Integer>();
				for(int index: minIndices) {
					Map<Integer, Integer> temp = getMapFromString(postingsLine[index]);
					for(int key: temp.keySet()) {
						if(!map.containsKey(key))
							map.put(key, temp.get(key));
						else
							map.put(key, map.get(key) + temp.get(key));
					}
					kgramsLine[index] = kgrams[index].readLine();
					postingsLine[index] = postings[index].readLine();
				}
				writer1.write(map + "\n");
				writer2.write(minKgram + "\n");
			}while(!minIndices.isEmpty());
			writer1.close();
			writer2.close();*/
			/*BufferedReader reader = new BufferedReader(new FileReader("data/conceptDict"));
			lineNum = 0;
			while((line = reader.readLine()) != null) {
				lineNum++;
				if(lineNum <= 32000000)
					continue;
				words = line.split("\t");
				if(words.length < 2)
					continue;
				//create kgrams for words
				word = words[0].toLowerCase();
				for(String wordKey: word.split(" ")) {
					for(int i = 0; i < wordKey.length() + 1 - k; i++) {
						kgram = wordKey.substring(i, i+k);
						if(!postingsMap.containsKey(kgram))
							postingsMap.put(kgram, new HashMap<Integer, Integer>());
						Map<Integer, Integer> temp = postingsMap.get(kgram);
						count = 0;
						if(temp.containsKey(lineNum))
							count = temp.get(lineNum);
						temp.put(lineNum, count+1);
						postingsMap.put(kgram, temp);
					}
				}
				
			}
			if(true) {
				System.out.println(lineNum);
				BufferedWriter writer1 = new BufferedWriter(new FileWriter("data/concepts/kgramPostings17"));
				BufferedWriter writer2 = new BufferedWriter(new FileWriter("data/concepts/kgrams17"));
				List<String> tempList = new ArrayList<String>(postingsMap.keySet());
				Collections.sort(tempList);
		        for(String key: tempList) {
		        	writer1.write(postingsMap.get(key) + "\n");// + " " + conceptCount.get(key) + "\n");
		        	writer2.write(key + "\n");
		        }
		        writer1.close();
		        writer2.close();
		        break;
			}
			reader.close();*/
			//System.out.println("read in idf values");
			/*FileInputStream fis = new FileInputStream("data/idf.ser");
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
			
			BufferedReader reader = new BufferedReader(new FileReader("data/conceptDict"));
			//lineNum = 0;
			while((line = reader.readLine()) != null) {
				//lineNum++;
				//if(lineNum % 10000000 == 0)
					//System.out.println(lineNum);
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
			}*/
			//System.out.println(maxKey + " " + maxVal);
			
			//printing map to file
			/*BufferedWriter writer = new BufferedWriter(new FileWriter("data/conceptMatchOutput"));
	        for(String key: conceptRelevance.keySet()) 
	        	writer.write(key + " " + conceptRelevance.get(key) + "\n");// + " " + conceptCount.get(key) + "\n");
	        writer.close();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		return maxKey;
	}
	
	static class ValueComparator implements Comparator<String> {

	    Map<String, Double> base;
	    public ValueComparator(Map<String, Double> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	    }
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
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		//Concept concept = new Concept();
		long startTime = System.nanoTime();
		//getConcept("blah");
		//System.out.println(getConcept("Billy gates"));
		SSF ssf = new SSF();
		for(Entity entity: ssf.getEntities()) {
			if(!entity.getName().equals("Annie_Laurie_Gaylor"))
				continue;
			for(Slot slot: ssf.getSlots()) {
				if(!slot.getName().equals(Constants.SlotName.AwardsWon))
					continue;
				Annotation document = new Annotation("When asked why a Wisconsin group is concerned about a Nativity Scene in Athens , Texas , Annie Laurie Gaylor said she received a complaint from one of the groups members who lives in Athens .");
				ssf.processor.annotate(document);
				for(String exp: entity.getExpansions()) {
					System.out.println(exp);
					System.out.println(ssf.getCoreNLP().findSlotValue(document, exp, slot, false, null));
				}
			}
		}
		//System.out.println(Utils.getByteOffset("1324308300-9d5f56237cc8395411069bdf18888f6b__news-269-91294453ea1cfade403ac432d1418052-ce9557b63d5603ce5e494052fb0ef513.sc.xz.gpg__2011-12-19-15/__840", "Benjamin Netanyahu", "tmp/"));
		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime) + " ns"); 
	}
}
