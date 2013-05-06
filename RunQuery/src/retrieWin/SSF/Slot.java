package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;
import util.*;

import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;

public class Slot implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	EntityType entityType;
	String name;
	double threshold;
	List<SlotPattern> patterns;
	List<NERType> sourceNERTypes, targetNERTypes;
	boolean applyPatternAfterCoreference;
}
