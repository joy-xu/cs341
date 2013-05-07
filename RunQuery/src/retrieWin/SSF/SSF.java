package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import retrieWin.SSF.Constants.EntityType;
import util.FileUtils;
import util.Utils;

public class SSF {
	List<Slot> slots;
	List<Entity> entities;
	
	public SSF() {
		initialize();
	}
	
	public void initialize() {
		readEntities();
	}
	
	public void readEntities() {
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
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		for(Entity e:entities) {
			System.out.println(e.getName());
			System.out.println(e.getTargetID());
			System.out.println(e.getGroup());
			System.out.println(e.getEntityType());
			System.out.println(e.getExpansions());
			System.out.println(e.getDisambiguations());
		}
		
		FileUtils.writeFile(entities, "data/entities.ser");
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
			}
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return disambiguations;
	}
	
	public void readSlots() {
		
	}
	
	public void runSSF() {
	}
	
	public static void main(String[] args) {
		new SSF().runSSF();
	}
}
