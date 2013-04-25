import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Patterns {
	static Map<String,List<String>> patterns = new HashMap<String, List<String>>();
	static boolean initialized = false;
	
	Patterns() {
		setPatterns();
	}
	
	static void setPatterns() {
		patterns.put("founderof", Arrays.asList("founder", "ceo", "architect"));
		
		initialized = true;
	}
	public static void addPattern(String slot, String value) {
		patterns.get(slot).add(value);
	}
	
	public static List<String> getPatterns(String slot) {
		if(!initialized)
			setPatterns();
		return patterns.get(slot);
	}
}
