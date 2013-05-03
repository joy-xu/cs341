import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;


public class FindSentences {
	AbstractSequenceClassifier<CoreLabel> classifier;
	public FindSentences() {
		String serializedClassifier = "lib/english.all.3class.distsim.crf.ser.gz";
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
		getEntityExpansions("/home/aju/Stanford/cs341/cs341/RunQuery/src/seedSet/expansions.txt");
		return newSentence;
	}
	
	
	public HashMap<String, HashSet<String>> getEntityExpansions(String fileName) {
		BufferedReader reader;
		HashMap<String, HashSet<String>> ret = new HashMap<String, HashSet<String>>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
		
		
			String line = "";

			while((line=reader.readLine()) != null) {
				String splits[] = line.split("##");
				String expansions[] = splits[0].split("$");
				ret.put(splits[0], new HashSet<String>(Arrays.asList(expansions)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
}
