package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;

import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;

public class Slot implements Serializable {
	private static final long serialVersionUID = 1L;
	
	EntityType entityType;
	String name;
	double threshold;
	List<SlotPattern> patterns;
	List<NERType> sourceNERTypes, targetNERTypes;
	boolean applyPatternAfterCoreference;
	
	public List<String> extractSlotVals(Entity ent) {
		throw new NoSuchElementException();
	}
}
