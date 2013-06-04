package retrieWin.PatternBuilder;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import retrieWin.Indexer.Indexer;
import retrieWin.Indexer.TrecTextDocument;

import retrieWin.Querying.ExecuteQuery;

import retrieWin.SSF.Entity;

public class QueryFactory {
	
	public static Map<String,List<TrecTextDocument>> DoQuery(List<String> folders, List<String> queries,
								String workingDirectory, List<Entity> entities, ExecuteQuery eq)
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
			Callable<Map<String,List<TrecTextDocument>>> c = new ParallelIndexAndQueryFactory(folder,queries,workingDirectory,entities, eq);
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
	
	
	public static class ParallelIndexAndQueryFactory implements Callable<Map<String,List<TrecTextDocument>>>
	{
		
		String folder;
		List<String> queries;
		String workingDirectory;
		List<Entity> entities;
		ExecuteQuery eq;
		public ParallelIndexAndQueryFactory(String folderIn,List<String> queriesIn,
									String workingDir,List<Entity> entitiesIn, ExecuteQuery eqIn)
		{
			folder = folderIn;
			queries = queriesIn;
			workingDirectory = workingDir;
			entities = entitiesIn;
			eq = eqIn;
		}
		
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
			if (eq == null)
			{
				Indexer.createIndex(folder,baseFolder, tempDirectory, indexLocation, trecTextSerializedFile, entities);
				eq = new ExecuteQuery(indexLocation,trecTextSerializedFile);
			}
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
			//eq.emptyData();
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
