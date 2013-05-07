package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrieWin.SSF.Constants.EntityType;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;
import util.FileUtils;

public class SSF {
	List<Slot> slots;
	List<Entity> entities;
	
	public SSF() {
		initialize();
	}
	
	public void initialize() {
		
	}
	
	public void runSSF() {
	}
	
	public static void createSlotsFromFile(String inputFileName, String outputFileName) {
		try {
				List<Slot> slots = new ArrayList<Slot>();
				BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] vals = line.trim().split(",");
					Slot slot = new Slot();
					slot.setName(SlotName.valueOf(vals[0]));
					slot.setEntityType(EntityType.valueOf(vals[1]));
					slot.setThreshold(Double.parseDouble(vals[2]));
					slot.setApplyPatternAfterCoreference(Integer.parseInt(vals[3]) == 1 ? true : false);
					slot.setPatterns(new ArrayList<SlotPattern>());
					List<NERType> targetNERTypes = new ArrayList<NERType>();
					for(String ner: vals[4].split("\\$")) {
						targetNERTypes.add(NERType.valueOf(ner));
					}
					slot.setTargetNERTypes(targetNERTypes);
					
					if(slot.getEntityType() == EntityType.FAC)
						slot.setSourceNERTypes(Arrays.asList(NERType.ORGANIZATION, NERType.LOCATION));
					else if(slot.getEntityType() == EntityType.PER)
						slot.setSourceNERTypes(Arrays.asList(NERType.PERSON));
					else
						slot.setSourceNERTypes(Arrays.asList(NERType.ORGANIZATION));
					slots.add(slot);
				}
				reader.close();
				FileUtils.writeFile(slots, outputFileName);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		//createSlotsFromFile("data/Slots.csv", "data/SlotPatterns");
		//List<Slot> slots = (List<Slot>) FileUtils.readFile("data/SlotPatterns");
		//for(Slot slot: slots)
		//System.out.println(slot.getName() + "," + slot.getEntityType() + "," + slot.getSourceNERTypes() + "," + slot.getTargetNERTypes() + "," + slot.getThreshold());
		//System.out.println(slots);
		
		new SSF().runSSF();
	}
}
