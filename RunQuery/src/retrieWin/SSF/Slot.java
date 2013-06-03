package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;
import retrieWin.SSF.SlotPattern.Rule;
import retrieWin.Utils.FileUtils;

public class Slot implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private EntityType entityType;
	private SlotName name;
	private double threshold;
	private List<SlotPattern> patterns;
	private List<NERType> sourceNERTypes;
	private List<NERType> targetNERTypes;
	private boolean applyPatternAfterCoreference;
	
	@Override
	public String toString() {
		String ans = "";
		ans += name + "\n";
		ans += entityType + "\n";
		ans += threshold + "\n";
		ans += sourceNERTypes + "\n";
		ans += targetNERTypes + "\n";
		ans += patterns + "\n";
	
		return ans;
	}
	
	public void addSlotPatterns(String fileName) {
		patterns = new ArrayList<SlotPattern>();
		SlotPattern pat;
		String line;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			while((line = reader.readLine()) != null) {
				if (line.isEmpty())	 continue;
				pat = new SlotPattern(line);
				patterns.add(pat);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		//System.out.println("Updated patterns to:" + patterns);
	}
	
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

	public SlotName getName() {
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

	public List<SlotPattern> getPatterns() {
		return patterns;
	}

	void setPatterns(List<SlotPattern> patterns) {
		this.patterns = patterns;
	}

	public List<NERType> getTargetNERTypes() {
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
