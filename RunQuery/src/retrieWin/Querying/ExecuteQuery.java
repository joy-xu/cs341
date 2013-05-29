package retrieWin.Querying;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import retrieWin.Indexer.Downloader;
import retrieWin.Indexer.ThriftReader;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.Utils.FileUtils;
import retrieWin.Utils.NLPUtils;

import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.QueryRequest;
import lemurproject.indri.QueryResult;
import lemurproject.indri.QueryResults;

public class ExecuteQuery {
	
	final String INDEX_LOCATION;
	Map<String,TrecTextDocument> storedFiles = new HashMap<String,TrecTextDocument>();
			
	QueryEnvironment env = new QueryEnvironment();
	public ExecuteQuery(String IndexLocation) {
		this.INDEX_LOCATION = IndexLocation;
		try {
			env.addIndex(INDEX_LOCATION);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public ExecuteQuery(String IndexLocation,String filteredFilesLocation) {
		storedFiles = (Map<String,TrecTextDocument>)FileUtils.readFile(filteredFilesLocation);
		this.INDEX_LOCATION = IndexLocation;
		try {
			env.addIndex(INDEX_LOCATION);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void emptyData()
	{
		storedFiles = null;
	}
	
	public Set<String> queryIndex(String query, int resultsRequested) {
		Set<String> results = new HashSet<String>();
		try {
			QueryRequest queryRequest = new QueryRequest();
			queryRequest.query = query;
			queryRequest.resultsRequested = resultsRequested;
			QueryResults queryResults = env.runQuery(queryRequest);
			for(QueryResult result : queryResults.results) {
				results.add(result.documentName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	public Set<String> queryIndex(String query) {
		return queryIndex(query, Integer.MAX_VALUE);
	}

	public List<TrecTextDocument> getFromStoredFile(Set<String> queryResults)
	{
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
		for (String docNo:queryResults)
		{
			if (storedFiles.containsKey(docNo))
			{
				
				output.add(new TrecTextDocument(storedFiles.get(docNo)));
			}
			else
				System.out.println("File not found. You are definitely doing something wrong");
		}
		return output;
	}
	
	public List<TrecTextDocument> executeQueryFromStoredFile(String query, int numResults)
	{
		Set<String> queryResults = queryIndex(query, numResults);
		return getFromStoredFile(queryResults);
	}
	
	public List<TrecTextDocument> executeQuery(String query, int numResults, String workingDirectory) {
    	Set<String> queryResults = queryIndex(query, numResults);
    	System.out.println("Results for query: " + query + ": " + queryResults.size());
  
    	Map<String, Set<String>> fileMap = new HashMap<String, Set<String>>();
    	Map<String, Set<String>> streamIDMap = new HashMap<String, Set<String>>();
    	
    	for(String result:queryResults) { 
    		//System.out.println(result);
    		String[] a = result.split("__");
			String streamID = a[0];
			String localfilename = a[1];
			String[] b = localfilename.split("/");
			String filename = b[b.length-1];
		
			
			String folder = a[2];
			
			if(!fileMap.keySet().contains(folder)) {
				fileMap.put(folder, new HashSet<String>());
			}
			Set<String> set = fileMap.get(folder);
			set.add(filename);
			fileMap.put(folder, set);
			
			if(!streamIDMap.keySet().contains(filename)) {
				streamIDMap.put(filename, new HashSet<String>());
			}
			set = streamIDMap.get(filename);
			set.add(streamID);
			streamIDMap.put(filename, set);
    	}
    	List<TrecTextDocument> result = new ArrayList<TrecTextDocument>();
    	int numthreads = 16;
		ExecutorService exc = Executors.newFixedThreadPool(numthreads);
    	for(String folder:fileMap.keySet()) {
    		for(String file:fileMap.get(folder)) {
    			exc.execute(new Downloader(folder,file,workingDirectory));
    			//result.addAll(ThriftReader.GetFilteredFiles(folder,file,workingDirectory,streamIDMap.get(file)));
    		}
    	}
		
    	exc.shutdown();
		while(true)
		{
			try 
			{
				if (exc.awaitTermination(1, TimeUnit.MINUTES))
						break;
				System.out.println("Waiting in Downloader");
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
				System.out.println("Waiting in Downloader - Thread interrupted");
			}
		}
		
    	//List<TrecTextDocument> result = new ArrayList<TrecTextDocument>();
    	
    	numthreads = 16;
		exc = Executors.newFixedThreadPool(numthreads);
		
		List<Future<List<TrecTextDocument>>> futuresList = new ArrayList<Future<List<TrecTextDocument>>>();
    	for(String folder:fileMap.keySet()) {
    		for(String file:fileMap.get(folder)) {
    			Callable<List<TrecTextDocument>> c = new GetFilteredFilesParallel(folder,file,workingDirectory,streamIDMap.get(file));
				Future<List<TrecTextDocument>> s = exc.submit(c);
				futuresList.add(s);
    		}
    	}
    	
    	for (Future<List<TrecTextDocument>> f:futuresList)
		{
			List<TrecTextDocument> thisResult = new ArrayList<TrecTextDocument>();
			try
			{
				thisResult = f.get();
			}
			catch (Exception excep)
			{
				excep.printStackTrace();
			}
			result.addAll(thisResult);
		}
		exc.shutdown();
		
    	return result;
	}
	
	
private static class GetFilteredFilesParallel implements Callable<List<TrecTextDocument>>{
		
		String folder;
		String file;
		String wd;
		Set<String> streamIDs;
		
		public GetFilteredFilesParallel(String fldIn, String fIn, String wdIn, Set<String> streamIdIn)
		{
			folder = fldIn;
			file = fIn;
			wd = wdIn;
			streamIDs = streamIdIn;
		}
		
		@Override
		public List<TrecTextDocument> call() throws Exception {

			return ThriftReader.GetFilteredFiles(folder, file, wd, streamIDs);
		}
	}

}
