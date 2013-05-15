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
			String ret = "[";
			ret += "edgeType = " + edgeType + ", ";
			ret += "direction = " + direction + "]";
			return ret;
		}
		
		@Override
		public boolean equals(Object obj) {
		       if (this == obj)
		           return true;
		       if (obj == null)
		           return false;
		       if (getClass() != obj.getClass())
		           return false;
		       final Rule other = (Rule) obj;
		       if (this.direction == other.direction && this.edgeType.equals(other.edgeType))
		           return true;
		       return false;
		   }
		
		@Override
		public int hashCode() {
			return edgeType.hashCode() + direction.toString().hashCode();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
	       if (this == obj)
	           return true;
	       if (obj == null)
	           return false;
	       if (getClass() != obj.getClass())
	           return false;
	       final SlotPattern other = (SlotPattern) obj;
	       if (this.rules.size() != other.rules.size() || !other.pattern.toLowerCase().equals(this.pattern.toLowerCase()))
	           return false;
	       if((this.rules.get(0).equals(other.rules.get(0)) && this.rules.get(1).equals(other.rules.get(1))) ||
	    		   (this.rules.get(1).equals(other.rules.get(0)) && this.rules.get(0).equals(other.rules.get(1))))
	    		   return true;
	       return false;
	   }
	
	@Override
	public String toString() {
		String ret = "[";
		ret += "pattern = " + pattern + ", ";
		ret += " confidenceScore = " + confidenceScore + ", ";
		ret += "rules = {" + rules.toString();
		ret += "}]";
		return ret;
	}
	
	@Override
	public int hashCode() {
		int hashCode = this.pattern.toLowerCase().hashCode();
		for(Rule rule:this.rules) {
			hashCode += rule.hashCode();
		}
		return hashCode;
	}

	public List<Rule> getRules() {
		return rules;
	}
	
	public Rule getRules(int ruleNumber) {
		if(ruleNumber < rules.size())
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
