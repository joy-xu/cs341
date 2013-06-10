package retrieWin.SSF;

import java.io.Serializable;

public class Constants  implements Serializable {
	private static final long serialVersionUID = 1L;

	public final static String slotsSerializedFile = "data/slots.ser", entitiesSerilizedFile = "data/entities.ser";
	
	
	public final static String indriBuildIndexParamFile = "data/buildIndexParams.xml";
	
	public final static String s3directory = "s3://hourlyindex/";
	public final static String s3directory_alternate = "s3://hourlyindexalternate/";
	public final static String s3ConceptsDirectory = "s3://conceptsData/";
	
	public enum PatternType {
		WordInBetween,
		SourceInBetween,
		TargetInBetween,
		WithoutPatternWord,
		WithoutRules
	}
	
	//public final static String defaultWorkingDirectory = "tmp/";
	
	public enum EntityType {
		PER,
		ORG,
		FAC
	}	

	public enum NERType {
		PERSON,
		LOCATION,
		ORGANIZATION,
		DATE,
		TIME,
		NONE,
		O,
		MONEY,
		SET,
		NUMBER,
		MISC,
		DURATION,
		ORDINAL,
		PERCENT
	}
	
	public enum SlotName{
		Affiliate,
		AssociateOf,
		Contact_Meet_PlaceTime,
		AwardsWon,
		DateOfDeath,
		CauseOfDeath,
		Titles,
		FounderOf,
		EmployeeOf,
		Contact_Meet_Entity,
		TopMembers,
		FoundedBy
	}
	
	public enum EdgeDirection {
		In,
		Out
	}
}