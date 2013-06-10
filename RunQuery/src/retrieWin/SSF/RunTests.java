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

import edu.stanford.nlp.parser.lexparser.NoSuchParseException;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.exec.Execution;

import retrieWin.SSF.Constants.NERType;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;

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
		//generateFiles(entities);
		LogInfo.logs("Running tests");
		runTests(entities, slots);
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
	
	public void runTests(List<Entity> entities, List<Slot> slots) {
		for(Entity entity:entities) {
			//if(entity.getTargetID().contains("en.wikipedia.org/wiki/Jim_Steyer")) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(String.format("test/sentences/%s.txt", entity.getName())));
					String line;
					List<String> sentences = new ArrayList<String>();
					while((line = reader.readLine()) != null) {
						line = line.trim();
						for(String expansion:entity.getExpansions()) {
							if(line.toLowerCase().contains(expansion.toLowerCase())) {
								sentences.add(line);
								break;
							}
						}
						//System.out.println(line);
					}
					
					for(Slot slot:slots) {
						Map<String, Double> values = null;
						if(slot.getName().equals(Constants.SlotName.Titles))
							values = findTitles(entity, slot, sentences, coreNLP, conceptExtractor);
						
						else if (slot.getName().equals(Constants.SlotName.Contact_Meet_PlaceTime))
							values = findContactMeetPlaceTime(entity,slot,sentences,coreNLP,conceptExtractor);
						else{
							for(String expansion: entity.getExpansions()) {	
								for(String sentence:sentences) {
									Annotation document = new Annotation(sentence);
									processor.annotate(document);
									values = coreNLP.findSlotValue(document, expansion, slot, false, "DEFAULTVAL");
									if(values != null && values.size() > 0) {
										for(String value: values.keySet())
											if(!NLPUtils.isEntitiesSame(expansion, value))
												LogInfo.logs(String.format("Entity    :%s\nExpansion :%s\nSentence :%s\nSlot      :%s\nValue     :%s\n\n",
													entity.getName(), expansion, sentence, slot.getName().toString(), value));
									}
								
								}
							}
						}
					}				
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
			//}
		}
	}
	
	public static Map<String, Double> findTitles(Entity entity, Slot slot, List<String> relevantSentences, NLPUtils coreNLP, Concept conceptExtractor) {
		Map<String, Double> candidates = new HashMap<String, Double>();
		for(String expansion: entity.getExpansions()) {
			for(String sentence: relevantSentences) {
				try {
					//check for any non-pronominal coreference
					for(String title: coreNLP.getCorefs(sentence, expansion)) {
						if(containsUppercaseToken(title) && !NLPUtils.isEntitiesSame(expansion, title)) {
							if(!candidates.containsKey(title)) {
								candidates.put(title, 1.0);
							}
							else {
								double score = candidates.get(title);
								candidates.put(title, score + 1);
							}
							LogInfo.logs(String.format("Entity    :%s\nExpansion :%s\nSentence :%s\nSlot      :%s\nValue     :%s\n\n",
									entity.getName(), expansion, sentence, slot.getName().toString(), title));
						}
					}
					//check for any compund nouns for this entity
					String nnTitle = coreNLP.getNNs(sentence, expansion);
					if(containsUppercaseToken(nnTitle) && !NLPUtils.isEntitiesSame(expansion, nnTitle)) {
						if(!candidates.containsKey(nnTitle)) {
							candidates.put(nnTitle, 1.0);
						}
						else {
							double score = candidates.get(nnTitle);
							candidates.put(nnTitle, score + 1);
						}
						LogInfo.logs(String.format("Entity    :%s\nExpansion :%s\nSentence :%s\nSlot      :%s\nValue     :%s\n\n",
								entity.getName(), expansion, sentence, slot.getName().toString(), nnTitle));
					}
				} catch(NoSuchParseException e) {
					e.printStackTrace();
					break;
				}
			}
		}
		return candidates;
	}

	private static Map<String, Double> findContactMeetPlaceTime(Entity entity, Slot slot, List<String> relevantSentences, NLPUtils coreNLP, Concept conceptExtractor) {
		Map<String, Double> candidates = new HashMap<String, Double>();
		for(String expansion: entity.getExpansions()) {
			for(String sentence: relevantSentences) {
				Map<String, Double> values= null;
				values = coreNLP.findPlaceTimeValue(sentence, expansion, slot, false);
				
				for(String str: values.keySet()) {
					if(!NLPUtils.isEntitiesSame(expansion, str)) {
						if(!candidates.containsKey(str)){
							candidates.put(str, 1.0);
						}
						else {
							double score = candidates.get(str);
							candidates.put(str, score + 1);
						}
						LogInfo.logs(String.format("Entity    :%s\nExpansion :%s\nSentence :%s\nSlot      :%s\nValue     :%s\n\n",
								entity.getName(), expansion, sentence, slot.getName().toString(), str));
					}
				}
			}
		}
		return candidates;

	}
	
	private static boolean containsUppercaseToken(String str) {
		for(String token: str.split(" ")) {
			if(token.isEmpty())
				continue;
			if(Character.isUpperCase(token.charAt(0)))
					return true;
		}
		return false;
	}
}
