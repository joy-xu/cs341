
import java.util.*;
import java.io.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

import streamcorpus.*;

public class ThriftReader {

	public List<String> queryResults;
	public String workingDirectory;
	public Map<String,StreamItem> mapOfItems;
	public Map<String, List<String>> mapOfSentences;
	public Map<String, List<String>> mapOfTags;
	
	public ThriftReader(List<String> queryResult, String workingDirectoryInput)
	{
		try {
		queryResults = new ArrayList<String>();
		queryResults.addAll(queryResult);
		workingDirectory = workingDirectoryInput;
		mapOfItems = new HashMap<String, StreamItem>();
		mapOfSentences = new HashMap<String, List<String>>();
		mapOfTags = new HashMap<String, List<String>>();
		Map<String,Set<String>> folderToFiles = new HashMap<String,Set<String>>();
    	Map<String,Set<String>> fileToDocID = new HashMap<String,Set<String>>();
    	
    	for (int i = 0;i<queryResults.size();i++)
    	{
    		String fullName = queryResults.get(i);
    		
    		CorpusFileName cf = new CorpusFileName(fullName);
    		cf.downloadfile();
    		if (folderToFiles.containsKey(cf.folder))
    		{
    			folderToFiles.get(cf.folder).add(cf.filename);
    		}
    		else
    		{
    			Set<String> s = new HashSet<String>();
    			s.add(cf.filename);
    			folderToFiles.put(cf.folder, s);
    		}
    		
    		String absFileName = cf.folder + "_" + cf.filename;
    		if (fileToDocID.containsKey(absFileName))
    		{
    			fileToDocID.get(absFileName).add(cf.streamID);
    		}
    		else
    		{
    			Set<String> s = new HashSet<String>();
    			s.add(cf.streamID);
    			fileToDocID.put(absFileName,s);
    		}
    	}
		
    	    	
        Iterator<String> folderIT = folderToFiles.keySet().iterator();
    	while(folderIT.hasNext())
    	{
    		String currentFolder = folderIT.next();
    		Set<String> allFiles = folderToFiles.get(currentFolder);
    		Iterator<String> fileIT = allFiles.iterator();
    		while(fileIT.hasNext())
    		{
    			String currentFile = fileIT.next();
    			String absFileName = currentFolder + "_" + currentFile;
    			Set<String> streamIDs = fileToDocID.get(absFileName);
    			
    			String downloadDirectory = workingDirectory + currentFile;
            	String decryptedFile = downloadDirectory.substring(0,downloadDirectory.length()-4);
            	String fileInput = decryptedFile.substring(0, decryptedFile.length()-3);
    			/*
            	File f = new File(fileInput);
    			if (!f.exists())
    			{
    				String downloadURL = 
    	        			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
    	        			+ currentFolder + "/" + currentFile;
    	        	
    	        	Process p;
    	        	File downloadF = new File(downloadDirectory);
    	        	if (!downloadF.exists())
    	        	{
    	        		String downloadCommand = "/usr/local/bin/wget -O " + downloadDirectory + " " + downloadURL;
    	        		System.out.println(downloadCommand);
    	        		p = Runtime.getRuntime().exec(downloadCommand);
    	        		p.waitFor();
    	        	}
    	        	
    	        	File decryptF = new File(decryptedFile);
    	        	if (!decryptF.exists())
    	        	{
    	        		String decryptCommand = "/usr/local/bin/gpg -o " + decryptedFile + 
    	        							" -d " + downloadDirectory;
    	        	
    	        	
    	        	//System.out.println(decryptCommand);
    	        	p = Runtime.getRuntime().exec(decryptCommand);
    	        	
    	        	p.waitFor();
    	        	}
    	        	
    	        	String unxzCommand = "/usr/local/bin/unxz " + decryptedFile;
    	        	
    	        	//System.out.println(unxzCommand);
    	        	p = Runtime.getRuntime().exec(unxzCommand);
    	        	p.waitFor();
    	        	
    	        	String deleteCommand = "rm -f " + downloadDirectory;
    	        	p = Runtime.getRuntime().exec(deleteCommand);
    	        	p.waitFor();
    			}
    			*/
    			TTransport transport = new TIOStreamTransport(
                		new BufferedInputStream(
                				new FileInputStream(fileInput)));
                TBinaryProtocol protocol = new TBinaryProtocol(transport);
                transport.open();
                
                Set<String> added = new HashSet<String>();
                
                 
                while (true) 
                {
                    final StreamItem item = new StreamItem();
                    if (added.equals(streamIDs))
                     	break;
                    
                    item.read(protocol);
                    //out.write("counter = " + ++counter);
                  
                    
                    if (streamIDs.contains(item.stream_id))
                    {
                    	
                    	added.add(item.stream_id);
                    	
                    	mapOfItems.put(absFileName + "_" + item.stream_id, item);
                    	List<Sentence> s = item.body.sentences.get("lingpipe");
                        
                    	List<String> allSentences = new ArrayList<String>();
                    	List<String> allTags = new ArrayList<String>();
                    	for (int n = 0;n<s.size();n++)
                    	{
                    		List<Token> tList = s.get(n).getTokens();
                    		StringBuilder sbuf = new StringBuilder();
                    		StringBuilder nbuf = new StringBuilder();
                    		for (int t = 0;t<tList.size();t++)
                    		{
                    			
                    			sbuf.append(tList.get(t).token);
                    			sbuf.append(" ");
                    			nbuf.append(tList.get(t).entity_type);
                    			nbuf.append(" ");
                    		}
                    		allSentences.add(sbuf.toString());
                    		allTags.add(nbuf.toString());
                    	}
                        mapOfSentences.put(absFileName + "_" + item.stream_id,allSentences);
                        mapOfTags.put(absFileName + "_" + item.stream_id, allTags);
                    }
                }
    		}
    	}
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public List<String> getSentencesWithLemma(String first, StanfordCoreNLP processor)
	{
		Annotation doc = new Annotation(first);
		processor.annotate(doc);
		CoreMap s = doc.get(SentencesAnnotation.class).get(0);
		CoreLabel t = s.get(TokensAnnotation.class).get(0);
		String givenLemma = t.get(LemmaAnnotation.class);
		
		List<String> returnString = new ArrayList<String>();
		Iterator<String> it = mapOfSentences.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			List<String> currentAllSentences = mapOfSentences.get(absFileName);
						
			
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
				
				if (!ExtractRelation.isPureAscii(currentSentence))
					continue;
				
				if(currentSentence.length() > 400) {
	        		//System.out.println("Sentence too long.");
	        		continue;
	        	}
				
				System.out.println("Processing: " + currentSentence);
				Annotation document = new Annotation(currentSentence);
				processor.annotate(document);
				List<CoreMap> sentenceMap = document.get(SentencesAnnotation.class);
				for (int ss = 0;ss < sentenceMap.size();ss++)
				{
					boolean added = false;
					List<CoreLabel> allTokens = sentenceMap.get(ss).get(TokensAnnotation.class);
					for (int tt = 0;tt < allTokens.size();tt++)
					{
						String lemma = allTokens.get(tt).get(LemmaAnnotation.class);
						if (lemma.equalsIgnoreCase(givenLemma))
						{
							returnString.add(currentSentence);
							System.out.println("adding");
							added = true;
							break;
						}
					}
					if (added)
						break;
				}
			}
		}
				
		return returnString;
	}
	
	
	
	public List<String> getSentences(String first, Boolean includeNER, String outputFile) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		List<String> returnString = new ArrayList<String>();
		Iterator<String> it = mapOfSentences.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			List<String> currentAllSentences = mapOfSentences.get(absFileName);
			List<String> currentAllTags = mapOfTags.get(absFileName);
			
			buf.write("<DOC>");
			buf.newLine();
			buf.write("<DOCNO>");
			buf.newLine();
			buf.write(absFileName);
			buf.newLine();
			buf.write("</DOCNO>");
			buf.newLine();
			buf.write("<SENTENCES>");
			buf.newLine();
			
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
				if (currentSentence.toLowerCase().contains(first.toLowerCase()))
				{
					StringBuilder sbuf = new StringBuilder();
					String[] words = currentSentence.split(" ");
					String[] tags = currentAllTags.get(j).split(" ");
					for (int w = 0;w<words.length;w++)
					{
						sbuf.append(words[w]);
						if (includeNER)
						{
							sbuf.append("__");
							sbuf.append(tags[w]);
						}
						sbuf.append(" ");
					}
					returnString.add(sbuf.toString());
					buf.write(sbuf.toString());
					buf.newLine();
				}
			}
			buf.write("</SENTENCES>");
			buf.newLine();
			buf.write("</DOC>");
			buf.newLine();
		}
		buf.flush();
		buf.close();
		return returnString;
	}
	
	public List<String> getSentences(String first, String second, boolean includeNER, String outputFile) throws IOException
	{
		List<String> returnString = new ArrayList<String>();
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		//System.out.println(mapOfSentences.keySet().size());
		buf.write("<DOCS>");
		Iterator<String> it = mapOfSentences.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			List<String> currentAllSentences = mapOfSentences.get(absFileName);
			List<String> currentAllTags = mapOfTags.get(absFileName);
			
			
			buf.write("<DOC>");
			buf.newLine();
			buf.write("<DOCNO>");
			buf.newLine();
			buf.write(absFileName);
			buf.newLine();
			buf.write("</DOCNO>");
			buf.newLine();
			
			buf.write("<SENTENCES>");
			buf.newLine();
			
			boolean done = true;
			String output = "";
			int firstPos = -1;
			int secondPos = -1;
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
				String currentTags = currentAllTags.get(j);
				
				firstPos = currentSentence.toLowerCase().indexOf(first.toLowerCase());
				secondPos = currentSentence.toLowerCase().indexOf(second.toLowerCase());
				if (!done && firstPos != 1 && firstPos < secondPos)
				{
					output = "";
					done = true;
				}
				if (!done)
				{
					if (secondPos != -1)
					{
						StringBuilder sbuf = new StringBuilder();
						String[] allWords = currentSentence.split(" ");
						String[] allTags =  currentTags.split(" ");
						for (int w = 0;w<allWords.length;w++)
						{
							sbuf.append(allWords[w]);
							if (includeNER)
							{
								sbuf.append("__");
								sbuf.append(allTags[w]);
							}
							sbuf.append(" ");
						}
						output = output + "." + sbuf.toString();
						returnString.add(output);
						buf.write("<SENTENCE>");
						buf.write(output);
						buf.write("</SENTENCE>");
						buf.newLine();						
					}
					output = "";
					done = true;
					continue;
				}
				else
				{
					if (firstPos == -1)
						continue;
					else
					{
						StringBuilder sbuf = new StringBuilder();
						String[] allWords = currentSentence.split(" ");
						String[] allTags =  currentTags.split(" ");
						for (int w = 0;w<allWords.length;w++)
						{
							sbuf.append(allWords[w]);
							if (includeNER)
							{
								sbuf.append("__");
								sbuf.append(allTags[w]);
							}
							sbuf.append(" ");
						}
					
						if (firstPos != -1 && secondPos != -1)
						{
							output = sbuf.toString();
							returnString.add(output);
							buf.write("<SENTENCE>");
							buf.write(output);
							buf.write("</SENTENCE>");
							buf.newLine();
							output = "";
							done = true;
						}
						else if (firstPos != -1 && secondPos == -1)
						{
							output = sbuf.toString();
							done = false;
						}
					}
				}
					
			}
			buf.write("</SENTENCES>");
			buf.newLine();
			buf.write("</DOC>");
			buf.newLine();
		}
		buf.write("</DOCS>");
		buf.flush();
		buf.close();
		return returnString;
	}

	public void getCompleteDocument(Boolean includeNER, String outputFile) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		buf.write("<DOCS>");
		Iterator<String> it = mapOfItems.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			StreamItem currentItem = mapOfItems.get(absFileName);
			if (currentItem == null || currentItem.body == null || currentItem.body.clean_visible == null)
				continue;
			
			buf.write("<DOC>");
			buf.newLine();
			buf.write("<DOCNO>");
			buf.newLine();
			buf.write(absFileName);
			buf.newLine();
			buf.write("</DOCNO>");
			buf.newLine();
			buf.write("<Clean Visible>");
			buf.newLine();
			buf.write(currentItem.body.clean_visible);
			buf.newLine();
			buf.write("</Clean Visible>");
			buf.newLine();
			buf.write("<SENTENCES>");
			buf.newLine();
			
			Map<String,List<Sentence>> mm = currentItem.body.sentences;
            
            Iterator<String> jt = mm.keySet().iterator();
            
            while(jt.hasNext())
            {
            	String key = jt.next();
            	buf.write(key + ":");
            	buf.newLine();
            	List<Sentence> listS = mm.get(key);
            	
            	for (int i = 0;i<listS.size();i++)
            	{
            		Sentence current = listS.get(i);
            		List<Token> t = current.getTokens();
            		StringBuffer allTokens = new StringBuffer();
            		for (int j = 0; j < t.size();j++)
            		{
            			Token currentToken = t.get(j);
            			String word = currentToken.token;
            			EntityType ent = currentToken.entity_type;
            			int mention_id = currentToken.mention_id;
            			int equiv_id  = currentToken.equiv_id;
            			if (includeNER)
            				allTokens.append(word + "__" + ent + " ");
            			else
            				allTokens.append(word + " ");
            		}
            		buf.write("<SENTENCE>");
            		buf.write(allTokens.toString());
            		buf.newLine();
            		buf.write("</SENTENCE>");
            	}
            }
			buf.write("</SENTENCES>");
			buf.newLine();
			buf.write("</DOC>");
			buf.newLine();
		}
		buf.write("</DOCS>");
		buf.flush();
		buf.close();	
	}
}