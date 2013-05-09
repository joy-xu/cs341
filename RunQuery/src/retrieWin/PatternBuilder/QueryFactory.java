package retrieWin.PatternBuilder;

import java.io.File;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrieWin.Indexer.Indexer;
import retrieWin.Indexer.TrecTextDocument;

import retrieWin.Querying.ExecuteQuery;

import retrieWin.SSF.Entity;

public class QueryFactory {
	
	public static Set<TrecTextDocument> DoQuery(List<String> folders, List<String> queries,
								String workingDirectory, List<Entity> entities)
	{
		int numthreads = folders.size()<8 ? folders.size():8;
		Set<TrecTextDocument> results = new HashSet<TrecTextDocument>();
		ExecutorService e = Executors.newFixedThreadPool(numthreads);
		
		for (String folder:folders)
		{
			e.execute(new ParallelIndexAndQueryFactory(folder,queries,results, workingDirectory, entities));
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
		return results;
	}
	
	public static class ParallelIndexAndQueryFactory implements Runnable
	{
		Set<TrecTextDocument> allResults;
		String folder;
		List<String> queries;
		String workingDirectory;
		List<Entity> entities;
		public ParallelIndexAndQueryFactory(String folderIn,List<String> queriesIn,Set<TrecTextDocument> output,
									String workingDir,List<Entity> entitiesIn)
		{
			folder = folderIn;
			queries = queriesIn;
			allResults = output;
			workingDirectory = workingDir;
			entities = entitiesIn;
		}
		
		public synchronized void addToList(Set<TrecTextDocument> current)
		{
			allResults.addAll(current);
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
			ExecuteQuery eq = new ExecuteQuery(indexLocation);
			
			int numthreads = queries.size() < 8 ? queries.size():8;
			ExecutorService e = Executors.newFixedThreadPool(numthreads);
			
			Set<TrecTextDocument> results = new HashSet<TrecTextDocument>();
			
			for (String query:queries)
			{
				e.execute(new ParallelQueryFactory(query,eq,trecTextSerializedFile,results));
			}
			e.shutdown();
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
			addToList(results);
		}
		
	}
	
	
	private static class ParallelQueryFactory implements Runnable{
		Set<TrecTextDocument> output;
		ExecuteQuery queryExecutor;	
		String query;
		String filteredFileLocation;
		public ParallelQueryFactory(String queryIn, ExecuteQuery eq, String trecTextSerializedFile,Set<TrecTextDocument> in)
		{
			output = in;
			queryExecutor = eq;
			filteredFileLocation = trecTextSerializedFile;
			query = queryIn;
		}
		
		private synchronized void addToList(List<TrecTextDocument> results)
		{
			output.addAll(results);
		}
		
		public void run()
		{
			
			List<TrecTextDocument> queryResults = queryExecutor.executeQueryFromStoredFile(query, Integer.MAX_VALUE, filteredFileLocation);
			System.out.println("Query Results for: " + query + " : " + queryResults.size());
			addToList(queryResults);
		}
	}
}
