package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.SlotName;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Entity  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	EntityType entityType;
	String name;
	Set<String> expansions, disambiguations;
	Map<SlotName, List<String>> slotValues;
	
	public void getRelevantDocuments(String indexLocation) {
		throw new NotImplementedException();
	}
}
