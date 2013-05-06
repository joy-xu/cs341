package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;

import retrieWin.SSF.Constants.EdgeDirection;

public class SlotPattern  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double confidenceScore;
	List<Rule> rules;
	String pattern;
	
	public class Rule implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		EdgeDirection direction;
		String edgeType;
	}
}
