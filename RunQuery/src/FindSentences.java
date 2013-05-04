import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import util.NLPUtils;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import fig.basic.LogInfo;


public class FindSentences {
	NLPUtils utils = new NLPUtils();
	
	public void findAssociateOf(String entityFile, String indexLoc, String workingDirectory) {
		int numResults = 1000;
		HashMap<String, HashSet<String>> expansions = getEntityExpansions(entityFile);
		for(String entity:expansions.keySet()) {
			LogInfo.logs("Expanding " + entity);
			for(String expansion:expansions.get(entity)) {
				LogInfo.logs("\t Querying " + expansion);	
				String queryString = QueryBuilder.buildSingleTermQuery(expansion);
				List<String> lst = QueryRetreiver.executeQuery(indexLoc, queryString, numResults, expansion, workingDirectory);
				for(String s:lst)
					LogInfo.logs("\t\t" + s);
			}
		}
	}
	
	public HashMap<String, HashSet<String>> getEntityExpansions(String fileName) {
		BufferedReader reader;
		HashMap<String, HashSet<String>> ret = new HashMap<String, HashSet<String>>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
		
		
			String line = "";

			while((line=reader.readLine()) != null) {
				String splits[] = line.split("##");
				System.out.println(splits[1].split("\\$").length);
				String expansions[] = splits[1].split("\\$");
				ret.put(splits[0], new HashSet<String>(Arrays.asList(expansions)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
}
