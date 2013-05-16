package retrieWin.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import retrieWin.Indexer.ThriftReader;
import retrieWin.SSF.Constants;
import retrieWin.SSF.arxivDocument;
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
	
	private static List<String> getSentences(StreamItem item) {
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
	
	public static arxivDocument getArxivDocs(String docNo) {
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
}
