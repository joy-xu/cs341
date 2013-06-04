package retrieWin.SSF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import retrieWin.PatternBuilder.QueryFactory;
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
	
	static double disambiguationThreshold = 0.0;
	
	public Entity(String targetID, String name, EntityType type, String group, List<String> expansions, List<String> disambiguations) {
		this.targetID = targetID;
		this.name = name;
		this.entityType = type;
		this.group = group;
		this.disambiguations = disambiguations;
		this.expansions = expansions;
		this.slotValues = new HashMap<SlotName, List<String>>();
	}
	
	private List<TrecTextDocument> disambiguate(List<TrecTextDocument> results) {
		List<TrecTextDocument> filtered = new ArrayList<TrecTextDocument>();
		int maxScore = getDisambiguations().size();
		for(TrecTextDocument doc: results) {
			//System.out.println(doc.text);
			int score = 0;
			for(String simString: getDisambiguations()) {
				if(doc.text.toLowerCase().contains(simString.toLowerCase()))
					score += 1;
			}
			//remove documents which most likely don't belong to this entity
			if((double)score/maxScore >= disambiguationThreshold)
				filtered.add(doc);
			//else
				//System.out.println("This doc failed the test :(");
		}
		return filtered;
	}
	
	public List<String> updateSlot(Slot slot, List<String> candidates) {
		if(this.slotValues == null)
			this.slotValues = new HashMap<SlotName, List<String>>();
		List<String> added = new ArrayList<String>();
		if(!slotValues.containsKey(slot.getName()))
				slotValues.put(slot.getName(), new ArrayList<String>());
		for(String candidate: candidates) {
			//get normalized concept for the slot candidate
			if(slotValues.get(slot.getName()).contains(candidate))
				continue;
			else {
				List<String> values = slotValues.get(slot.getName());
				values.add(candidate);
				slotValues.put(slot.getName(), values);
				added.add(candidate);
			}
		}
		return added;
	}
	
	public List<TrecTextDocument> getRelevantDocuments(String timestamp, String workingDirectory, List<Entity> entities) {
		String query = QueryBuilder.buildOrQuery(getExpansions());
		List<TrecTextDocument> docs = QueryFactory.DoQuery(Arrays.asList(timestamp), Arrays.asList(query), workingDirectory, entities, null).get(query);
		//return docs;
		System.out.println("Originally had " + docs.size() + " docs");
		return disambiguate(docs);
	}
	
	public List<TrecTextDocument> getRelevantDocuments(String timestamp, String workingDirectory, List<Entity> entities, ExecuteQuery eq) {
		String query = QueryBuilder.buildOrQuery(getExpansions());
		List<TrecTextDocument> docs = QueryFactory.DoQuery(Arrays.asList(timestamp), Arrays.asList(query), workingDirectory, entities, eq).get(query);
		//return docs;
		System.out.println("Originally had " + docs.size() + " docs");
		return disambiguate(docs);
	}

	public EntityType getEntityType() {
		return entityType;
	}

	void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	public String getTargetID() {
		return targetID;
	}

	void setTargetID(String targetID) {
		this.targetID = targetID;    
	}

	public String getGroup() {
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

	public List<String> getDisambiguations() {
		return disambiguations;
	}

	void setDisambiguations(List<String> disambiguations) {
		this.disambiguations = disambiguations;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}
}
