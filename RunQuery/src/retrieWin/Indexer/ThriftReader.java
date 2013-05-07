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


import streamcorpus.StreamItem;

public class ThriftReader {
	
	private static TrecTextDocument populateTrecTextDocument(StreamItem item, String filename)
	{
		String docNumber = item.stream_id + "_" + filename + "_" + item.stream_time.zulu_timestamp;
		return new TrecTextDocument(docNumber,item.body.clean_visible,Double.toString(item.stream_time.epoch_ticks));		
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
		
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new ArrayList<TrecTextDocument>();
		
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
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
            	if (item.body == null || item.body.clean_visible == null)
            		continue;
            	output.add(populateTrecTextDocument(item,file));
            }
        }
		} catch (Exception e)
		{}
		
		return output;
	}
	
	public static List<TrecTextDocument> GetAllFiles(String folder, String file, String workingDirectory)
	{
		
		String downloadedFile = Downloader.downloadfile(folder, file, workingDirectory);
		
		if (downloadedFile == null) return new ArrayList<TrecTextDocument>();
		
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
		try 
		{
			TBinaryProtocol protocol = openBinaryProtocol(downloadedFile);
			while (true) 
	        {
	            final StreamItem item = new StreamItem(); 
	            item.read(protocol);  
	            
	            if (item.body == null || item.body.clean_visible == null)
	            	continue;
	            output.add(populateTrecTextDocument(item,file));
	            
	        }
		} 
		catch (Exception e)
			{
				System.out.println("Thrift Error");
			}
		
		return output;
	}
	
	public static void WriteTrecTextDocumentToFile(List<TrecTextDocument> documentList,String filename,String downloadDirectory)
	{
		for (TrecTextDocument t:documentList)
		{
			t.writeToFile(filename,downloadDirectory);
		}			
	}
	
	public static void GetFolder(String folder, String downloadDirectory)
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
			    filenames.add(line);
			}
			
			ExecutorService e = Executors.newFixedThreadPool(16);
			
			for (String file:filenames)
			{
				e.execute(new parallelWriter(folder,file,downloadDirectory));
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
			return;
		}
	}
	
	public static class parallelWriter implements Runnable
	{
		String folder, file, downloadDirectory;
		public parallelWriter(String folderIn, String fileIn, String dwndIn)
		{
			folder = folderIn;
			file = fileIn;
			downloadDirectory = dwndIn;
		}
		public void run()
		{
			List<TrecTextDocument> documentList = GetAllFiles(folder,file,downloadDirectory);
			WriteTrecTextDocumentToFile(documentList,file.substring(0,file.length()-10),downloadDirectory);
		}
	}
}
