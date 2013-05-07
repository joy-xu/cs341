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
	private EntityType entityType;
	private String targetID;
	private String name;
	private String group;
	private List<String> expansions;
	private List<String> disambiguations;
	Map<SlotName, List<String>> slotValues;
	
	public Entity(String targetID, String name, EntityType type, String group, List<String> expansions, List<String> disambiguations) {
		this.targetID = targetID;
		this.name = name;
		this.entityType = type;
		this.group = group;
		this.disambiguations = disambiguations;
		this.expansions = expansions;
	}
	
	private void disambiguate(Map<TrecTextDocument, Double> results) {
		int maxScore = getDisambiguations().size();
		for(TrecTextDocument doc: results.keySet()) {
			int score = 0;
			for(String simString: getDisambiguations()) {
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
		
		for(String expansion: getExpansions()) {
			query = QueryBuilder.buildOrderedQuery(expansion, 10);
			for(TrecTextDocument doc: queryExecuter.executeQuery(query, Integer.MAX_VALUE, workingDirectory)) {
				results.put(doc, 0.0);
			}
		}
		
		disambiguate(results);
		return results;
	}

	EntityType getEntityType() {
		return entityType;
	}

	void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	String getTargetID() {
		return targetID;
	}

	void setTargetID(String targetID) {
		this.targetID = targetID;
	}

	String getGroup() {
		return group;
	}

	void setGroup(String group) {
		this.group = group;
	}

	public List<String> getExpansions() {
		return expansions;
	}

	void setExpansions(List<String> expansions) {
		this.expansions = expansions;
	}

	List<String> getDisambiguations() {
		return disambiguations;
	}

	void setDisambiguations(List<String> disambiguations) {
		this.disambiguations = disambiguations;
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}
}
