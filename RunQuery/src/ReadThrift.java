
import java.io.*;
import java.util.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

import streamcorpus.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * User: jacek
 * Date: 3/8/13
 * Time: 3:38 PM
 */
public final class ReadThrift {
    public static void main(String[] args) {
        try {
        	String indexLocation = args[0];
        	String queryString = args[1];
        	
        	Query q = new Query(indexLocation);
        	List<String> queryResults = q.queryIndex(queryString);
        	
        	int documentCount = 0;
        	Map<String,Set<String>> folderToFiles = new HashMap<String,Set<String>>();
        	Map<String,Set<String>> fileToDocID = new HashMap<String,Set<String>>();
        	
        	String workingDirectory = args[2];
        	String fileOutput = args[3];
        	
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
        	
        	/*
        	String fileFolder = args[0];
        	
        	
        	String[] tokens = fileFolder.split("_");
        	String docName = tokens[0];
        	String fileName = tokens[1];
        	String fileTokens[] = fileName.split("/");
        	String realFileName = fileTokens[fileTokens.length-1];
        	String timeStamp = tokens[2];
        	String timeTokens[] = timeStamp.split("T");
        	String folder = timeTokens[0] + "-" + timeTokens[1].substring(0, 2);
        	
        	String downloadDirectory = workingDirectory + realFileName;
        	String decryptedFile = downloadDirectory.substring(0,downloadDirectory.length()-4);
        	
        	String fileInput = decryptedFile.substring(0, decryptedFile.length()-3);
            String fileOutput = fileInput.substring(0,fileInput.length()-3) + "_" + docName;
            */
        	File outf = new File(fileOutput);
            if (outf.exists())
             	outf.delete();
        	
            BufferedWriter out = new BufferedWriter(
            		new FileWriter(fileOutput));
            
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
        	        	System.out.print(p.getOutputStream().toString());
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
                        
                        if (item == null) 
                            break;
                        /*
                        if (item.body.clean_visible == null)
                        	continue;
                        */
                        if (streamIDs.contains(item.stream_id))
                        {
                        	documentCount++;
                        	System.out.println(documentCount);
                        	added.add(item.stream_id);         
	                        out.write("<DOC>");
	                        
	                        out.newLine();
	                        
	                        out.write("<DOCNO>" + item.source + "_" + absFileName + "_" + item.stream_id + "</DOCNO>");
	                        out.newLine();
	                        out.write("<TIME>" + item.stream_time.zulu_timestamp + "</TIME>");
	                        out.newLine();
	                        /*
	                        out.write("<TEXT>");
	                        out.newLine();
	                        out.write(item.body.clean_visible);
	                        out.newLine();
	                        out.write("</TEXT>");
	                        out.newLine();
	                        
	                        out.write("<TAG>");
	                        
	                        Map<String,Tagging> m = item.body.taggings;
	                        Iterator<String> it = m.keySet().iterator();
	                        while(it.hasNext())
	                        {
	                        	String key = it.next();
	                        	Tagging tag = m.get(key);
	                        	String str = tag.toString();
	                        	out.write(key + "\t:\t" + str);
	                        	out.newLine();
	                        }
	                        
	                        out.write("</TAG>");
	                        out.newLine();
	                        */
	                        out.write("<SENTENCE>");
	                        Map<String,List<Sentence>> mm = item.body.sentences;
	                        System.out.println(item.body.sentences.get(0));
	                        
	                        Iterator<String> it = mm.keySet().iterator();
	                        
	                        while(it.hasNext())
	                        {
	                        	String key = it.next();
	                        	out.write(key + ":");
	                        	out.newLine();
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
	                        			allTokens.append(word + "__" + ent + "__" + mention_id + "__" + equiv_id + " ");
	                        			
	                        		}
	                        		out.write(allTokens.toString());
	                        		out.newLine();
	                        	}
	                        }
	                        
	                        out.write("</SENTENCE>");
	                        
	                        out.newLine();
	                        
	                        
	                        
	                        out.write("</DOC>");
	                        out.newLine();
	                        
	                        out.flush();
                        }        
                    }
                    transport.close();
        		}
        	}
        	
        	/*
            File f = new File(fileInput);
           
            
            if (!f.exists())
            {
	        	String downloadURL = 
	        			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
	        			+ folder + "/" + realFileName;
	        	
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
	        	System.out.print(p.getOutputStream().toString());
	        	p.waitFor();
	        	
	        	String unxzCommand = "unxz " + decryptedFile;
	        	
	        	System.out.println(unxzCommand);
	        	p = Runtime.getRuntime().exec(unxzCommand);
	        	p.waitFor();
            }	
        	
            // File transport magically doesn't work
//            TTransport transport = new TFileTransport("test-data/john-smith-tagged-by-lingpipe-0.sc", true);
            TTransport transport = new TIOStreamTransport(
            		new BufferedInputStream(
            				new FileInputStream(fileInput)));
            TBinaryProtocol protocol = new TBinaryProtocol(transport);
            BufferedWriter out = new BufferedWriter(
            		new FileWriter(fileOutput));
            transport.open();
            int counter = 0;
            while (true) {
                final StreamItem item = new StreamItem();
                item.read(protocol);
                //out.write("counter = " + ++counter);
                
                if (item == null) 
                    break;
                
                if (!item.stream_id.equals(docName))
                	continue;
                
                if (item.body.clean_visible == null)
                {
                	counter++;
                	System.out.println(counter);
                	continue;
                }
                
                out.write("<DOC>");
                
                out.newLine();
                
                out.write("<DOCNO>" + item.source + "_" + counter + "_" + item.stream_id + "</DOCNO>");
                out.newLine();
                out.write("<TIME>" + item.stream_time.zulu_timestamp + "</TIME>");
                out.newLine();
                out.write("<TEXT>");
                out.newLine();
                out.write(item.body.clean_visible);
                out.newLine();
                out.write("</TEXT>");
                out.newLine();
                
                out.write("<TAG>");
                
                Map<String,Tagging> m = item.body.taggings;
                Iterator<String> it = m.keySet().iterator();
                while(it.hasNext())
                {
                	String key = it.next();
                	Tagging tag = m.get(key);
                	String str = tag.toString();
                	out.write(key + "\t:\t" + str);
                	out.newLine();
                }
                
                out.write("</TAG>");
                out.newLine();
                
                out.write("<SENTENCE>");
                Map<String,List<Sentence>> mm = item.body.sentences;
                
                it = mm.keySet().iterator();
                
                while(it.hasNext())
                {
                	String key = it.next();
                	out.write(key + ":");
                	out.newLine();
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
                			
                			allTokens.append(word + "__" + ent + " ");
                			
                		}
                		out.write(allTokens.toString());
                		out.newLine();
                	}
                }
                
                out.write("</SENTENCE>");
                
                out.newLine();
                
                
                
                out.write("</DOC>");
                out.newLine();
                
                out.flush();
                counter++;
                System.out.println(counter);
                
            }
            */
            out.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
