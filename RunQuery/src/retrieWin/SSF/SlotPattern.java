package retrieWin.SSF;

import java.io.Serializable;
import java.util.List;

import retrieWin.SSF.Constants.EdgeDirection;

public class SlotPattern  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double confidenceScore;
	private List<Rule> rules;
	private String pattern;
	
	public static class Rule implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public EdgeDirection direction;
		public String edgeType;
		
		@Override
		public String toString() {
			String ret = "";
			ret += "edgeType = " + edgeType + "\n";
			ret += "direction = " + direction + "\n";
			return ret;
		}
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += "pattern = " + pattern + "\n";
		ret += "confidenceScore = " + confidenceScore + "\n";
		ret += "rules = " + rules.toString() + "\n";
		return ret;
	}

	public List<Rule> getRules() {
		return rules;
	}
	
	public Rule getRules(int ruleNumber) {
		if(rules.size() < ruleNumber)
			return rules.get(ruleNumber);
		return null;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}
	
	public void adRule(Rule rule) {
		rules.add(rule);
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public double getConfidenceScore() {
		return confidenceScore;
	}

	public void setConfidenceScore(double confidenceScore) {
		this.confidenceScore = confidenceScore;
	}
}
