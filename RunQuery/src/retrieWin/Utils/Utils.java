package retrieWin.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
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
}
