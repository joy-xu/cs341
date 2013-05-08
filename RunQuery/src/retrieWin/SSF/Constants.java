package retrieWin.SSF;

import java.io.Serializable;

public class Constants  implements Serializable {
	private static final long serialVersionUID = 1L;

	public final static String slotsSerializedFile = "data/slots.ser", entitiesSerilizedFile = "data/entities.ser";
	public final static String indexLocation = "index";
	public final static String trecTextSerializedFile = "data/filteredSerialized.ser";
	public final static String indriBuildIndexParamFile = "data/buildIndexParams.xml";
	public final static String workingDirectory = "temp/";

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
		NONE
	}
	
	public enum SlotName{
		Founded_By,
		Founder_Of,
		Affiliate_Of,
		Cause_Of_Death,
		Date_Of_Death,
		Top_Founders,
		Contact_Meet_Place_Time,
		Employee_Of,
		Contact_Meet_Entity,
		Awards_Won,
		Titles
	}
	
	public enum EdgeDirection {
		In,
		Out
	}
}
