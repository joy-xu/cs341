package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;

public class Slot implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private EntityType entityType;
	private SlotName name;
	private double threshold;
	private List<SlotPattern> patterns;
	private List<NERType> sourceNERTypes;
	private List<NERType> targetNERTypes;
	private boolean applyPatternAfterCoreference;
	
	public List<String> normalize(List<String> vals) {
		throw new NoSuchElementException();
	}
	public List<String> extractSlotVals(Entity ent, Map<TrecTextDocument, Double> docs) {
		throw new NoSuchElementException();
	}

	EntityType getEntityType() {
		return entityType;
	}

	void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	SlotName getName() {
		return name;
	}

	void setName(SlotName name) {
		this.name = name;
	}

	double getThreshold() {
		return threshold;
	}

	void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	List<SlotPattern> getPatterns() {
		return patterns;
	}

	void setPatterns(List<SlotPattern> patterns) {
		this.patterns = patterns;
	}

	List<NERType> getTargetNERTypes() {
		return targetNERTypes;
	}

	void setTargetNERTypes(List<NERType> targetNERTypes) {
		this.targetNERTypes = targetNERTypes;
	}

	List<NERType> getSourceNERTypes() {
		return sourceNERTypes;
	}

	void setSourceNERTypes(List<NERType> sourceNERTypes) {
		this.sourceNERTypes = sourceNERTypes;
	}

	boolean isApplyPatternAfterCoreference() {
		return applyPatternAfterCoreference;
	}

	void setApplyPatternAfterCoreference(boolean applyPatternAfterCoreference) {
		this.applyPatternAfterCoreference = applyPatternAfterCoreference;
	}
}
