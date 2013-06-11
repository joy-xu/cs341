package retrieWin.SSF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.relation.RoleUnresolved;

import org.w3c.dom.EntityReference;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.exec.Execution;

import retrieWin.SSF.Constants.NERType;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;

public class RunTests implements Runnable {
	public static StanfordCoreNLP processor;
	Concept conceptExtractor;
	private NLPUtils coreNLP;
	
	public RunTests() {
		coreNLP = new NLPUtils();
		conceptExtractor = new Concept();
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner, dcoref");
		processor = new StanfordCoreNLP(props, false);
	}
	
	public static void main(String[] args) {
		Execution.run(args, "Main", new RunTests());
	}
	
	@Override
	public void run() {
		File file = new File(Constants.entitiesSerilizedFile);
		List<Entity> entities = null;
		List<Slot> slots = null;
		if(file.exists()) {
			entities = (List<Entity>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		file = new File(Constants.slotsSerializedFile);
		if(file.exists()){
			slots = (List<Slot>)FileUtils.readFile(file.getAbsolutePath().toString());
		}
		readAndRunTests(entities, slots);
	}
	
	public void readAndRunTests(List<Entity> entities, List<Slot> slots) {
		generateFiles(entities);
		LogInfo.logs("Running tests");
	}
	
	public void generateFiles(List<Entity> entities) {
		File file = new File("test/info");
		if(!file.exists()) {
			file.mkdirs();
		}
		
		file = new File("test/sentences");
		if(!file.exists()) {
			file.mkdirs();
		}
		
		for(Entity e:entities) {
			if(e.getTargetID().contains("en.wikipedia.org")) {
				Process p;
				
				try {
					String getCommand = String.format("python scripts/get_infobox.py %s", e.getName());
					//System.out.println(getCommand);
					p = Runtime.getRuntime().exec(getCommand);	
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					StringBuffer buffer = new StringBuffer();
					while ((line = input.readLine()) != null) 
					{
						buffer.append(line + "\n");
					}
					BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("test/info/%s.txt", e.getName())));
					writer.write(buffer.toString());
					writer.close();
					
					getCommand = String.format("python scripts/get_sentences_from_wiki.py %s", e.getName());
					//System.out.println(getCommand);
					p = Runtime.getRuntime().exec(getCommand);	
					input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					buffer = new StringBuffer();
					int count = 0;
					while ((line = input.readLine()) != null) 
					{
						if(count>3)
							buffer.append(line + "\n");
						count++;
					}
					writer = new BufferedWriter(new FileWriter(String.format("test/sentences/%s.txt", e.getName())));
					writer.write(buffer.toString());
					writer.close();
				} 
				catch (IOException ex) {
				
					ex.printStackTrace();
				}
			}
		}
		/**/
	}
	
	public static Map<String, Set<String>> getRelevantSentences(String expansion, String entityName) {
		Map<String, Set<String>> relevantSentences = new HashMap<String,Set<String>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(String.format("test/sentences/%s.txt", entityName)));
			String line;

			Set<String> id = new HashSet<String>();
			id.add("streamID__news__folderName__11");
			while((line = reader.readLine()) != null) {
				line = line.trim();
				//sentMapping.put(line, id);
				for(String split:expansion.split(" ")) {
					if(line.toLowerCase().contains(split.toLowerCase())) {
						//Map<String, Set<String>> sentMapping = new HashMap<String, Set<String>>();
						relevantSentences.put(line, id);
					}
				}
				//System.out.println(line);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return relevantSentences;
	}
}