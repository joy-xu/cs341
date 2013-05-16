package retrieWin.Indexer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;


import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;


import streamcorpus.Sentence;
import streamcorpus.StreamItem;
import streamcorpus.Token;

public class ThriftReader {
	
	private static TrecTextDocument populateTrecTextDocument(StreamItem item, String filename,String folder)
	{
		String docNumber = item.stream_id + "__" + filename + "__" + folder;
		List<Sentence> s = item.body.sentences.get("lingpipe");
        
    	List<String> allSentences = new ArrayList<String>();
    	
    	for (int n = 0;n<s.size();n++)
    	{
    		List<Token> tList = s.get(n).getTokens();
    		StringBuilder sbuf = new StringBuilder();
    		
    		for (int t = 0;t<tList.size();t++)
    		{
    			
    			sbuf.append(tList.get(t).token);
    			sbuf.append(" ");
    		}
    		allSentences.add(sbuf.toString());
    		
    	}
		return new TrecTextDocument(docNumber,item.body.clean_visible,Double.toString(item.stream_time.epoch_ticks),allSentences);		
	}
	
	private static TBinaryProtocol openBinaryProtocol(String inputFile) throws Exception
	{
			TTransport transport = new TIOStreamTransport(
	        		new BufferedInputStream(
	        				new FileInputStream(inputFile)));
	        TBinaryProtocol protocol = new TBinaryProtocol(transport);
	        transport.open();
	        return protocol;
	}
	
	public static List<TrecTextDocument> GetFilteredFiles(String folder, String file, String workingDirectory, Set<String> streamIDs)
	{
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
		
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new ArrayList<TrecTextDocument>();
		
		
		try {

		TBinaryProtocol protocol = openBinaryProtocol(downloadedFile);
        Set<String> added = new HashSet<String>();
       
        while (true) 
        {
            final StreamItem item = new StreamItem();
            if (added.equals(streamIDs))
             	break;
            
            item.read(protocol);
            
            if (streamIDs.contains(item.stream_id))
            {
            	added.add(item.stream_id);
            	if (item.body == null || item.body.clean_visible == null || 
            			item.body.sentences == null || !item.body.sentences.containsKey("lingpipe"))
            		continue;
            	output.add(populateTrecTextDocument(item,file,folder));
            }
        }
		} catch (Exception e)
		{e.printStackTrace();}
		
		return output;
	}
	
	public static List<StreamItem> GetFilteredStreamItems(String folder, String file, String workingDirectory, Set<String> streamIDs)
	{
		List<StreamItem> output = new ArrayList<StreamItem>();
		
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new ArrayList<StreamItem>();
		
		
		try {

		TBinaryProtocol protocol = openBinaryProtocol(downloadedFile);
        Set<String> added = new HashSet<String>();
       
        while (true) 
        {
            final StreamItem item = new StreamItem();
            if (added.equals(streamIDs))
             	break;
            
            item.read(protocol);
            
            if (streamIDs.contains(item.stream_id))
            {
            	added.add(item.stream_id);
            	if (item.body == null || item.body.clean_visible == null || 
            			item.body.sentences == null || !item.body.sentences.containsKey("lingpipe"))
            		continue;
            	output.add(item);
            }
        }
		} catch (Exception e)
		{e.printStackTrace();}
		
		return output;
	}
	
	public static Set<StreamItem> GetAllStreamItems(String downloadedFile)
	{
		
		//String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		Set<StreamItem> output = new HashSet<StreamItem>();
		try 
		{
			TBinaryProtocol protocol = openBinaryProtocol(downloadedFile);
			while (true) 
	        {
	            final StreamItem item = new StreamItem(); 
	            try {
	            item.read(protocol);  
	            }
	            catch (Exception transportException)
	            {
	            	break;
	            }
            	if (item.body == null || item.body.clean_visible == null || 
            			item.body.sentences == null || !item.body.sentences.containsKey("lingpipe"))
            	{
            		//System.out.println("null");
            		continue;
            	}
	            	
	            output.add(item);
	            
	        }
		} 
		catch (Exception e)
			{
				System.out.println("Thrift Error");
				e.printStackTrace();
			}
		
		return output;
	}
	
	public static Set<TrecTextDocument> GetAllFiles(String folder, String file, String workingDirectory)
	{
		
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new HashSet<TrecTextDocument>();
		
		Set<TrecTextDocument> output = new HashSet<TrecTextDocument>();
		try 
		{
			TBinaryProtocol protocol = openBinaryProtocol(downloadedFile);
			while (true) 
	        {
	            final StreamItem item = new StreamItem(); 
	            try {
	            item.read(protocol);  
	            }
	            catch (Exception transportException)
	            {
	            	break;
	            }
            	if (item.body == null || item.body.clean_visible == null || 
            			item.body.sentences == null || !item.body.sentences.containsKey("lingpipe"))
            	{
            		//System.out.println("null");
            		continue;
            	}
	            	
	            output.add(populateTrecTextDocument(item,file,folder));
	            
	        }
		} 
		catch (Exception e)
			{
				System.out.println("Thrift Error");
				e.printStackTrace();
			}
		
		return output;
	}
	
	public static void WriteTrecTextDocumentToFile(Set<TrecTextDocument> documentList,String filename,String downloadDirectory)
	{
		
		for (TrecTextDocument t:documentList)
		{
			t.writeToFile(filename,downloadDirectory);
		}			
	}
	
	public static void WriteTrecTextDocumentToFile(List<TrecTextDocument> documentList,String filename,String downloadDirectory)
	{
		
		for (TrecTextDocument t:documentList)
		{
			t.writeToFile(filename,downloadDirectory);
		}			
	}
	
	
	public static void GetFolder(String folder, String downloadDirectory, String folderToIndex)
	{
		List<String> filenames = new ArrayList<String>();
		try
		{
			Process p;
			String s3cmdCommand = "s3cmd ls s3://aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/" + folder;
			p = Runtime.getRuntime().exec(s3cmdCommand);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) 
			{
			    String[] components = line.split("\\t");
			    String nameOnly = components[components.length-1];
			    String[] pathComponents = nameOnly.split("/");
			    String relName = pathComponents[pathComponents.length-1];
			    if (relName.endsWith(".gpg"))
			    	filenames.add(relName);
			}
			
			ExecutorService e = Executors.newFixedThreadPool(16);
		
			for (String file:filenames)
			{
				e.execute(new parallelWriter(folder,file,downloadDirectory,folderToIndex));
			}
			e.shutdown();
			while(true)
			{
				try {
					if (e.awaitTermination(1, TimeUnit.MINUTES))
						break;
					System.out.println("Waiting");
				}
				catch(InterruptedException ie){
					System.out.println("Waiting - Thread interrupted");
				}
			}
			
		}
		catch (Exception e)
		{
			System.out.println(e);
			return;
		}
	}
	
	public static class parallelWriter implements Runnable
	{
		String folder, file, downloadDirectory, folderToIndex;
		public parallelWriter(String folderIn, String fileIn, String dwndIn, String folderToIndexIn)
		{
			folder = folderIn;
			file = fileIn;
			downloadDirectory = dwndIn;
			folderToIndex = folderToIndexIn;
		}
		public void run()
		{
			Set<TrecTextDocument> documentList = GetAllFiles(folder,file,downloadDirectory);
			WriteTrecTextDocumentToFile(documentList,file.substring(0,file.length()-10),folderToIndex);
		}
	}
}
