
import java.util.*;
import java.io.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

import streamcorpus.*;

public class FilteredFileBuilder {


	public static void createFiles(Set<String> queryResult, String workingDirectoryInput, String writeDirectory)
	{
		try {
		List<String> queryResults = new ArrayList<String>();
		queryResults.addAll(queryResult);
		String workingDirectory = workingDirectoryInput;
		
		Map<String,Set<String>> filesToStreamIDs = new HashMap<String,Set<String>>();
    	
    	
    	for (int i = 0;i<queryResults.size();i++)
    	{
    		String fullName = queryResults.get(i);
    		
    		CorpusFileName cf = new CorpusFileName(fullName);
    		cf.setlocalfilename(workingDirectory+cf.filename);
    		cf.downloadfile();
    		//String localFile = workingDirectory + cf.localfilename;
    		if (filesToStreamIDs.containsKey(cf.localfilename))
    			filesToStreamIDs.get(cf.localfilename).add(cf.streamID);
    		else
    		{
    			Set<String> s = new HashSet<String>();
    			s.add(cf.streamID);
    			filesToStreamIDs.put(cf.localfilename, s);
    		} 		
    	}
		
    	    	
        Iterator<String> fileIT = filesToStreamIDs.keySet().iterator();
    	while(fileIT.hasNext())
    	{
    			String currentFile = fileIT.next();
    			
    			Set<String> streamIDs = filesToStreamIDs.get(currentFile);
    			
    			String downloadDirectory = currentFile;
            	String decryptedFile = downloadDirectory.substring(0,downloadDirectory.length()-4);
            	String fileInput = decryptedFile.substring(0, decryptedFile.length()-3);
    			
    			TTransport transport = new TIOStreamTransport(
                		new BufferedInputStream(
                				new FileInputStream(fileInput)));
                TBinaryProtocol protocol = new TBinaryProtocol(transport);
                transport.open();
                 
                Set<String> added = new HashSet<String>();
                String[] all = currentFile.split("/");
                String absFileName = all[all.length-1];
                BufferedWriter buf = new BufferedWriter(new FileWriter(writeDirectory+absFileName));
                 
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
                    	buf.write("<DOC>");
            			buf.newLine();
            	
            			buf.write("<DOCNO>");
            			buf.newLine();
            			buf.write(item.stream_id+"_"+absFileName+"_" +item.stream_time.zulu_timestamp);
            			buf.newLine();
            			buf.write("</DOCNO>");
            			buf.newLine();
            			buf.write("<TIME>");
            			buf.newLine();
            			buf.write(Double.toString(item.stream_time.epoch_ticks));
            			buf.newLine();
            			buf.write("</TIME>");
            			buf.newLine();
            			buf.write("<TEXT>");
            			buf.newLine();
            			buf.write(item.body.clean_visible);
            			buf.newLine();
            			buf.write("</TEXT>");
            			buf.newLine();
                    	buf.write("</DOC>");
                    	buf.newLine();
                    	buf.flush();
                    }
                }
    		}
    	
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	

}

