package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrieWin.SSF.Constants.EntityType;
import util.FileUtils;
import util.Utils;
import retrieWin.SSF.Constants.NERType;
import retrieWin.SSF.Constants.SlotName;

public class SSF {
	final String slotsSerializedFile = "data/slots.ser", entitiesSerilizedFile = "data/entities.ser";
	List<Slot> slots;
	List<Entity> entities;
	
	public SSF() {
		initialize();
	}
	
	public void initialize() {
		readEntities();
		readSlots();
	}
	
	@SuppressWarnings("unchecked")
	public void readEntities() {
		File file = new File(entitiesSerilizedFile);
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		else {
			String fileName = "data/entities.csv";
			entities = new ArrayList<Entity>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = "";
				while((line = reader.readLine()) != null) {
					String[] splits = line.split("\",\"");
					EntityType type = splits[1].replace("\"", "").equals("PER") ? EntityType.PER : (splits[1].equals("ORG") ? EntityType.ORG : EntityType.FAC);
					String name = splits[0].replace("\"", "").replace("http://en.wikipedia.org/wiki/", "").replace("https://twitter.com/", "");
					Entity entity = new Entity(splits[0].replace("\"", ""), name, type, splits[2].replace("\"", ""),
							Utils.getEquivalents(splits[3].replace("\"", "")), getDisambiguations(name));
					entities.add(entity);
				}
				reader.close();
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
		for(Entity e:entities) {
			System.out.println(e.getName());
			System.out.println(e.getTargetID());
			System.out.println(e.getGroup());
			System.out.println(e.getEntityType());
			System.out.println(e.getExpansions());
			System.out.println(e.getDisambiguations());
		}
		
		FileUtils.writeFile(entities, entitiesSerilizedFile);
	}
	
	public List<String> getDisambiguations(String entity) {
		String baseFolder = "data/entities_expanded/";
		List<String> disambiguations = new ArrayList<String>();
		
		try {
			File file = new File(baseFolder + entity + ".expansion");
			System.out.println(file.getAbsolutePath());
			if(file.exists()) {
				System.out.println("file exists");
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = reader.readLine()) != null) {
					disambiguations.add(line.trim());
				}
				reader.close();
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return disambiguations;
	}
	
	@SuppressWarnings("unchecked")
	public void readSlots() {
		File file = new File(slotsSerializedFile);
		if(file.exists()) {
			slots = (List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		else {
			String inputFileName = "data/Slots.csv";
	
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
					FileUtils.writeFile(slots, slotsSerializedFile);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		for(Slot slot: slots)
			System.out.println(slot.getName() + "," + slot.getEntityType() + "," + slot.getSourceNERTypes() + "," + slot.getTargetNERTypes() + "," + slot.getThreshold());
		//System.out.println(slots);
	}
	
	public void runSSF() {
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
