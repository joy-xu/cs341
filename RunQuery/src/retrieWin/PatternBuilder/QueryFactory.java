package retrieWin.PatternBuilder;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fig.basic.LogInfo;

import retrieWin.Indexer.Indexer;
import retrieWin.Indexer.TrecTextDocument;

import retrieWin.Querying.ExecuteQuery;

import retrieWin.SSF.Entity;

public class QueryFactory {
	
	public static Map<String,List<TrecTextDocument>> DoQuery(List<String> folders, List<String> queries,
								String workingDirectory, List<Entity> entities)
	{
		if (!System.getenv().containsKey("LD_LIBRARY_PATH"))
		{
			System.out.println("Environment variable not set");
			System.exit(0);
		}
		int numthreads = folders.size()<4 ? folders.size():4;
		System.out.println("Num thread is: " + numthreads);
		Map<String,List<TrecTextDocument>> results = new HashMap<String,List<TrecTextDocument>>();
		ExecutorService e = Executors.newFixedThreadPool(numthreads);
		List<Future<Map<String,List<TrecTextDocument>>>> futuresList = new ArrayList<Future<Map<String,List<TrecTextDocument>>>>();
		for (String folder:folders)
		{
			Callable<Map<String,List<TrecTextDocument>>> c = new ParallelIndexAndQueryFactory(folder,queries,workingDirectory,entities);
			Future<Map<String,List<TrecTextDocument>>> s = e.submit(c);
			futuresList.add(s);
		}
			
		for (Future<Map<String,List<TrecTextDocument>>> f:futuresList)
		{
			Map<String,List<TrecTextDocument>> thisResult = new HashMap<String,List<TrecTextDocument>>();
			try{
				thisResult = f.get();
			}
			catch (Exception excep)
			{
				excep.printStackTrace();
			}
			for (String query:thisResult.keySet())
			{
				if (results.containsKey(query))
				{
					List<TrecTextDocument> existing = results.get(query);
					existing.addAll(thisResult.get(query));
					results.put(query,existing);
				}
				else
				{
					results.put(query, thisResult.get(query));
				}
			}
		}
		e.shutdown();
		/*
			IndexAndQueryFactory p = new IndexAndQueryFactory(folder,queries,workingDirectory,entities);
			Map<String,List<TrecTextDocument>> current = p.getResults();
			for (String query:current.keySet())
			{
				if (results.containsKey(query))
				{
					List<TrecTextDocument> existing = results.get(query);
					existing.addAll(current.get(query));
					results.put(query,existing);
				}
				else
				{
					results.put(query,current.get(query));
				}
			}
			//e.execute(new ParallelIndexAndQueryFactory(folder,queries,results, workingDirectory, entities));
		}
		
		e.shutdown();
		while(true)
		{
			try {
				if (e.awaitTermination(1, TimeUnit.MINUTES))
					break;
				System.out.println("Waiting in QueryFactory");
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
				System.out.println("Waiting in QueryFactory - Thread interrupted");
			}
		}
		*/
		return results;
	}
	
	public static class IndexAndQueryFactory
	{
	
		String folder;
		List<String> queries;
		String workingDirectory;
		List<Entity> entities;

		public IndexAndQueryFactory(String folderIn,List<String> queriesIn,
									String workingDir,List<Entity> entitiesIn)
		{
			folder = folderIn;
			queries = queriesIn;
			workingDirectory = workingDir;
			entities = entitiesIn;
		}
		
		public Map<String,List<TrecTextDocument>> getResults() {
			// TODO Auto-generated method stub
			String[] splits = folder.split("-");
			String currentFolder = workingDirectory;
			for (int i = 0;i<4;i++)
			{
				currentFolder = currentFolder + splits[i] + "/";
				File f = new File(currentFolder);
				if (!f.exists())
					f.mkdir();
			}
			String year = splits[0],month = splits[1], day = splits[2], hour = splits[3];
			
			String baseFolder = String.format("%s/%s/%s/%s/",year,month,day,hour);
			String indexLocation = workingDirectory + baseFolder + "index/";
			String tempDirectory = workingDirectory + baseFolder + "temp/";
			String trecTextSerializedFile = workingDirectory + baseFolder + "filteredSerialized.ser";
			// if the directory does not exist, create it
			File baseDir = new File(baseFolder);
			if (!baseDir.exists())
				baseDir.mkdirs();
			Indexer.createIndex(folder,baseFolder, tempDirectory, indexLocation, trecTextSerializedFile, entities);
			ExecuteQuery eq = new ExecuteQuery(indexLocation,trecTextSerializedFile);
			
			int numthreads = queries.size() < 16 ? queries.size():16;
			ExecutorService e = Executors.newFixedThreadPool(numthreads);
			
			Map<String,List<TrecTextDocument>> results = new HashMap<String,List<TrecTextDocument>>();
			
			List<Future<Map<String,List<TrecTextDocument>>>> futuresList = new ArrayList<Future<Map<String,List<TrecTextDocument>>>>();
			for (String query:queries)
			{
				Callable<Map<String,List<TrecTextDocument>>> c = new ParallelQueryFactory(query,eq);
				Future<Map<String,List<TrecTextDocument>>> s = e.submit(c);
				futuresList.add(s);
			}
			for (Future<Map<String,List<TrecTextDocument>>> f:futuresList)
			{
				Map<String,List<TrecTextDocument>> thisResult = new HashMap<String,List<TrecTextDocument>>();
				try{
					thisResult = f.get();
				}
				catch (Exception excep)
				{
					excep.printStackTrace();
				}
				for (String query:thisResult.keySet())
				{
					if (results.containsKey(query))
					{
						List<TrecTextDocument> existing = results.get(query);
						existing.addAll(thisResult.get(query));
						results.put(query,existing);
					}
					else
					{
						results.put(query, thisResult.get(query));
					}
				}
			}
			e.shutdown();
			return results;
		}
	}	
	
	public static class ParallelIndexAndQueryFactory implements Callable<Map<String,List<TrecTextDocument>>>
	{
		
		String folder;
		List<String> queries;
		String workingDirectory;
		List<Entity> entities;

		public ParallelIndexAndQueryFactory(String folderIn,List<String> queriesIn,
									String workingDir,List<Entity> entitiesIn)
		{
			folder = folderIn;
			queries = queriesIn;
			workingDirectory = workingDir;
			entities = entitiesIn;
		}
		/*
		public synchronized void addMaptoMap(Map<String,List<TrecTextDocument>> current)
		{
			for (String query:current.keySet())
			{
				if (allResults.containsKey(query))
				{
					List<TrecTextDocument> present = allResults.get(query);
					present.addAll(current.get(query));
					allResults.put(query, present);
				}
				else
				{
					allResults.put(query, current.get(query));
				}
			}
		}
		
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String[] splits = folder.split("-");
			String currentFolder = workingDirectory;
			for (int i = 0;i<4;i++)
			{
				currentFolder = currentFolder + splits[i] + "/";
				File f = new File(currentFolder);
				if (!f.exists())
					f.mkdir();
			}
			String year = splits[0],month = splits[1], day = splits[2], hour = splits[3];
			
			String baseFolder = String.format("%s/%s/%s/%s/",year,month,day,hour);
			String indexLocation = workingDirectory + baseFolder + "index/";
			String tempDirectory = workingDirectory + baseFolder + "temp/";
			String trecTextSerializedFile = workingDirectory + baseFolder + "filteredSerialized.ser";
			// if the directory does not exist, create it
			File baseDir = new File(baseFolder);
			if (!baseDir.exists())
				baseDir.mkdirs();
			Indexer.createIndex(folder,baseFolder, tempDirectory, indexLocation, trecTextSerializedFile, entities);
			ExecuteQuery eq = new ExecuteQuery(indexLocation,trecTextSerializedFile);
			
			int numthreads = queries.size() < 16 ? queries.size():16;
			ExecutorService e = Executors.newFixedThreadPool(numthreads);
			
			Map<String,List<TrecTextDocument>> results = new HashMap<String,List<TrecTextDocument>>();
			List<Future<Map<String,List<TrecTextDocument>>>> futuresList = new ArrayList<Future<Map<String,List<TrecTextDocument>>>>();
			for (String query:queries)
			{
				Callable<Map<String,List<TrecTextDocument>>> c = new ParallelQueryFactory(query,eq);
				Future<Map<String,List<TrecTextDocument>>> s = e.submit(c);
				futuresList.add(s);
			}
			for (Future<Map<String,List<TrecTextDocument>>> f:futuresList)
			{
				Map<String,List<TrecTextDocument>> thisResult = new HashMap<String,List<TrecTextDocument>>();
				try{
					thisResult = f.get();
				}
				catch (Exception excep)
				{
					excep.printStackTrace();
				}
				for (String query:thisResult.keySet())
				{
					if (results.containsKey(query))
					{
						List<TrecTextDocument> existing = results.get(query);
						existing.addAll(thisResult.get(query));
						results.put(query,existing);
					}
					else
					{
						results.put(query, thisResult.get(query));
					}
				}
			}
			e.shutdown();
			/*
			while(true)
			{
				try {
					if (e.awaitTermination(1, TimeUnit.MINUTES))
						break;
					System.out.println("Waiting in ParallelQueryFactory");
				}
				catch(InterruptedException ie){
					ie.printStackTrace();
					System.out.println("Waiting in ParallelQueryFactory - Thread interrupted");
				}
			}
			addMaptoMap(results);
			
		}
		*/
		@Override
		public Map<String, List<TrecTextDocument>> call() throws Exception {
			// TODO Auto-generated method stub
			String[] splits = folder.split("-");
			String currentFolder = workingDirectory;
			for (int i = 0;i<4;i++)
			{
				currentFolder = currentFolder + splits[i] + "/";
				File f = new File(currentFolder);
				if (!f.exists())
					f.mkdir();
			}
			String year = splits[0],month = splits[1], day = splits[2], hour = splits[3];
			
			String baseFolder = String.format("%s/%s/%s/%s/",year,month,day,hour);
			String indexLocation = workingDirectory + baseFolder + "index/";
			String tempDirectory = workingDirectory + baseFolder + "temp/";
			String trecTextSerializedFile = workingDirectory + baseFolder + "filteredSerialized.ser";
			// if the directory does not exist, create it
			File baseDir = new File(baseFolder);
			if (!baseDir.exists())
				baseDir.mkdirs();
			Indexer.createIndex(folder,baseFolder, tempDirectory, indexLocation, trecTextSerializedFile, entities);
			ExecuteQuery eq = new ExecuteQuery(indexLocation,trecTextSerializedFile);
			
			int numthreads = queries.size() < 16 ? queries.size():16;
			System.out.println("Starting threads for folder: " + folder);
			ExecutorService e = Executors.newFixedThreadPool(numthreads);
			
			Map<String,List<TrecTextDocument>> results = new HashMap<String,List<TrecTextDocument>>();
			List<Future<Map<String,List<TrecTextDocument>>>> futuresList = new ArrayList<Future<Map<String,List<TrecTextDocument>>>>();
			for (String query:queries)
			{
				Callable<Map<String,List<TrecTextDocument>>> c = new ParallelQueryFactory(query,eq);
				Future<Map<String,List<TrecTextDocument>>> s = e.submit(c);
				futuresList.add(s);
			}
			for (Future<Map<String,List<TrecTextDocument>>> f:futuresList)
			{
				Map<String,List<TrecTextDocument>> thisResult = new HashMap<String,List<TrecTextDocument>>();
				try{
					thisResult = f.get();
				}
				catch (Exception excep)
				{
					excep.printStackTrace();
				}
				for (String query:thisResult.keySet())
				{
					if (results.containsKey(query))
					{
						List<TrecTextDocument> existing = results.get(query);
						existing.addAll(thisResult.get(query));
						results.put(query,existing);
					}
					else
					{
						results.put(query, thisResult.get(query));
					}
				}
			}
			System.out.println("Shutting down threads for folder: " + folder);
			eq.emptyData();
			//System.gc();
			e.shutdown();
			return results;
		}
		
	}
	
	
	private static class ParallelQueryFactory implements Callable<Map<String,List<TrecTextDocument>>>
	{
		ExecuteQuery queryExecutor;	
		String query;
		public ParallelQueryFactory(String queryIn, ExecuteQuery eq)
		{
			queryExecutor = eq;
			query = queryIn;
		}
		/*
		private synchronized void addToList(List<TrecTextDocument> results)
		{
			output.put(query,results);
		}
		
		public void run()
		{
			List<TrecTextDocument> queryResults = queryExecutor.executeQueryFromStoredFile(query, Integer.MAX_VALUE);
			//System.out.println("Query Results for: " + query + " : " + queryResults.size());
			addToList(queryResults);
		}
		*/

		@Override
		public Map<String, List<TrecTextDocument>> call() throws Exception {
			// TODO Auto-generated method stub
			List<TrecTextDocument> queryResults = queryExecutor.executeQueryFromStoredFile(query, Integer.MAX_VALUE);
			//System.out.println("Query Results for: " + query + " : " + queryResults.size());
			Map<String,List<TrecTextDocument>> output = new HashMap<String,List<TrecTextDocument>>();
			output.put(query,queryResults);
			return output;
		}
	}
}
