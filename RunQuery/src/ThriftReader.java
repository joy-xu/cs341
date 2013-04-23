
import java.util.*;
import java.io.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

import streamcorpus.*;
public class ThriftReader {

	public List<String> queryResults;
	public String workingDirectory;
	public Map<String,StreamItem> mapOfItems;
	public Map<String, List<Token>> mapOfTokens;
	
	public ThriftReader(List<String> queryResult, String workingDirectoryInput)
	{
		try {
		queryResults = new ArrayList<String>();
		queryResults.addAll(queryResult);
		workingDirectory = workingDirectoryInput;
		mapOfItems = new HashMap<String, StreamItem>();
		mapOfTokens = new HashMap<String, List<Token>>();
		Map<String,Set<String>> folderToFiles = new HashMap<String,Set<String>>();
    	Map<String,Set<String>> fileToDocID = new HashMap<String,Set<String>>();
    	
    	for (int i = 0;i<queryResults.size();i++)
    	{
    		String fullName = queryResults.get(i);
    		
    		String[] a = fullName.split("_");
    		String docName = a[0];
    		String localFileName = a[1];
    		String[] b = localFileName.split("/");
    		String s3fileName = b[b.length-1];
    		
    		String timeStamp = a[2];
    		String timeTokens[] = timeStamp.split("T");
    		String folder = timeTokens[0] + "-" + timeTokens[1].substring(0,2);
    		
    		if (folderToFiles.containsKey(folder))
    		{
    			folderToFiles.get(folder).add(s3fileName);
    		}
    		else
    		{
    			Set<String> s = new HashSet<String>();
    			s.add(s3fileName);
    			folderToFiles.put(folder, s);
    		}
    		
    		String absFileName = folder + "_" + s3fileName;
    		if (fileToDocID.containsKey(absFileName))
    		{
    			fileToDocID.get(absFileName).add(docName);
    		}
    		else
    		{
    			Set<String> s = new HashSet<String>();
    			s.add(docName);
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
    	        		String downloadCommand = "wget -O " + downloadDirectory + " " + downloadURL;
    	        		System.out.println(downloadCommand);
    	        		p = Runtime.getRuntime().exec(downloadCommand);
    	        		p.waitFor();
    	        	}
    	        	
    	        	 
    	        	String decryptCommand = "gpg -o " + decryptedFile + 
    	        							" -d " + downloadDirectory;
    	        	
    	        	
    	        	System.out.println(decryptCommand);
    	        	p = Runtime.getRuntime().exec(decryptCommand);
    	        	
    	        	p.waitFor();
    	        	
    	        	String unxzCommand = "unxz " + decryptedFile;
    	        	
    	        	System.out.println(unxzCommand);
    	        	p = Runtime.getRuntime().exec(unxzCommand);
    	        	p.waitFor();
    			}
    			
    			TTransport transport = new TIOStreamTransport(
                		new BufferedInputStream(
                				new FileInputStream(fileInput)));
                TBinaryProtocol protocol = new TBinaryProtocol(transport);
                transport.open();
                
                Set<String> added = new HashSet<String>();
                while (true) 
                {
                    final StreamItem item = new StreamItem();
                    item.read(protocol);
                    //out.write("counter = " + ++counter);
                    if (added.equals(streamIDs))
                    	break;
                    if (streamIDs.contains(item.stream_id))
                    {
                    	added.add(item.stream_id);
                    	mapOfItems.put(absFileName + "_" + item.stream_id, item);
                    	List<Sentence> s = item.body.sentences.get("lingpipe");
                        List<Token> listOfTokens = new ArrayList<Token>();
                    	for (int n = 0;n<s.size();n++)
                    	{
                    		List<Token> t = s.get(n).getTokens();
                    		listOfTokens.addAll(t);
                    	}
                        mapOfTokens.put(absFileName + "_" + item.stream_id, listOfTokens);
                    }
                }
    		}
    	}
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
	public void getSentences(String first, String outputFile) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		
		Iterator<String> it = mapOfItems.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			StreamItem currentItem = mapOfItems.get(absFileName);
			if (currentItem == null || currentItem.body == null)
				continue;
			String clean_visible = currentItem.body.clean_visible;
			if (clean_visible == null)
				continue;
			String[] sentences = clean_visible.split(".");
			
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
			
			for (int j = 0;j<sentences.length;j++)
			{
				String currentSentence = sentences[j];
				
				if (currentSentence.indexOf(first) != -1)
				{
					buf.write(currentSentence);
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
	}
	
	public void getSentences(String first, String second, String outputFile) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		
		Iterator<String> it = mapOfItems.keySet().iterator();
		while(it.hasNext())
		{
			String absFileName = it.next();
			StreamItem currentItem = mapOfItems.get(absFileName);
			if (currentItem == null || currentItem.body == null)
				continue;
			String clean_visible = currentItem.body.clean_visible;
			if (clean_visible == null)
				continue;
			String[] sentences = clean_visible.split(".");
			
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
			for (int j = 0;j<sentences.length;j++)
			{
				String currentSentence = sentences[j];
				if (done)
				{
					firstPos = currentSentence.indexOf(first);
				}
				secondPos = currentSentence.indexOf(second);
				if (!done)
				{
					if (secondPos != -1)
					{
						output = output + "." + currentSentence;
						buf.write(output);
						buf.newLine();
						
					}
					output = "";
					done = true;
					continue;
				}
				else
				{
					if (firstPos != -1 && secondPos != -1)
					{
						output = currentSentence;
						buf.write(output);
						buf.newLine();
						output = "";
						done = true;
					}
					else if (firstPos != -1 && secondPos == -1)
					{
						output = currentSentence;
						done = false;
					}
					else
						continue;
				}
					
			}
			buf.write("</SENTENCES>");
			buf.newLine();
			buf.write("</DOC>");
			buf.newLine();
		}
		buf.flush();
		buf.close();
	}

	public void getCompleteDocument(String outputFile) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory + outputFile));
		
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
            			allTokens.append(word + "__" + ent + " ");
            			
            		}
            		buf.write(allTokens.toString());
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
	}
}