package retrieWin.SSF;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrieWin.Querying.ExecuteQuery;
import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.SlotName;
import retrieWin.Indexer.TrecTextDocument;

public class Entity  implements Serializable {
	private static final long serialVersionUID = 1L;
	EntityType entityType;
	String name;
	List<String> expansions, disambiguations;
	Map<SlotName, List<String>> slotValues;
	
	private void disambiguate(Map<TrecTextDocument, Double> results) {
		int maxScore = disambiguations.size();
		for(TrecTextDocument doc: results.keySet()) {
			int score = 0;
			for(String simString: disambiguations) {
				if(doc.text.toLowerCase().contains(simString.toLowerCase()))
					score += 1;
			}
			results.put(doc, (double)score/maxScore);
		}
	}
	
	public Map<TrecTextDocument, Double> getRelevantDocuments(String indexLocation, String workingDirectory) {
		ExecuteQuery queryExecuter =  new ExecuteQuery(indexLocation);
		Map<TrecTextDocument, Double> results = new HashMap<TrecTextDocument, Double>();
		String query;
		
		for(String expansion: expansions) {
			query = QueryBuilder.buildOrderedQuery(expansion, 10);
			for(TrecTextDocument doc: queryExecuter.executeQuery(query, Integer.MAX_VALUE, workingDirectory)) {
				results.put(doc, 0.0);
			}
		}
		
		disambiguate(results);
		return results;
	}
}
