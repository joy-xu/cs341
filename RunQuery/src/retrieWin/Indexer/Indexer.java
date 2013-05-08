package retrieWin.Indexer;
import java.io.File;
import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import retrieWin.SSF.*;
import retrieWin.Querying.*;
public class Indexer {

	public static void createIndex(String folder, String tmpdirLocation, String filteredIndexLocation, String serializedFileLocation,
			List<Entity> allEntities)
	{
		System.out.println(folder);
		if (!folder.endsWith("/"))
			folder = folder + "/";
		String filesLocation = tmpdirLocation + "allFiles/";
		String folderToIndex = tmpdirLocation + "Unserialized/";
		String indexFolder = tmpdirLocation + "index/";
		String filteredFilesLocation = tmpdirLocation + "filteredFiles/";
		
		File tmpdir = new File(tmpdirLocation);
		File filesDir = new File(filesLocation);
		File indexDir = new File(indexFolder);
		File indexinFolder = new File(folderToIndex);
		File filteredFilesDir = new File(filteredFilesLocation);
		
		tmpdir.mkdirs();
		filesDir.mkdirs();
		indexDir.mkdirs();
		indexinFolder.mkdirs();
		filteredFilesDir.mkdirs();
		
		ThriftReader.GetFolder(folder, filesLocation, folderToIndex);
		
		IndriIndexBuilder.buildIndex(indexFolder, folderToIndex);
		ExecuteQuery queryExecutor = new ExecuteQuery(indexFolder);
		
		ExecutorService e = Executors.newFixedThreadPool(16);
		List<TrecTextDocument> allResults = new ArrayList<TrecTextDocument>();
		
		for (Entity entity:allEntities)
		{
			e.execute(new parallelQuerier(entity,queryExecutor,filesLocation,allResults));
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
		
		ThriftReader.WriteTrecTextDocumentToFile(allResults, "filtered", filteredFilesLocation);
		IndriIndexBuilder.buildIndex(filteredIndexLocation, filteredFilesLocation);
		TrecTextDocument.serializeFile(allResults,serializedFileLocation);
		/*
		try{
		Process p;
		String deleteCommand = "rm -rf " + tmpdirLocation;
		p = Runtime.getRuntime().exec(deleteCommand);
		p.waitFor();
		}
		catch (Exception exc)
		{
			System.out.println("Failed to delete temporary files");
		}
		*/
	}
	
	public static class parallelQuerier implements Runnable{
		List<TrecTextDocument> output;
		Entity entity;
		ExecuteQuery queryExecutor;
		String filesLocation;
		public parallelQuerier(Entity ent, ExecuteQuery eq, String loc,List<TrecTextDocument> in)
		{
			output = in;
			entity = ent;
			queryExecutor = eq;
			filesLocation = loc;
		}
		
		private synchronized void addToList(List<TrecTextDocument> results)
		{
			output.addAll(results);
		}
		
		public void run()
		{
			String query = QueryBuilder.buildOrQuery(entity.getExpansions());
			//System.out.println("Querying for: " + query);
			List<TrecTextDocument> queryResults = queryExecutor.executeQuery(query, Integer.MAX_VALUE, filesLocation);
			System.out.println("Query Results for: " + query + " : " + queryResults.size());
			addToList(queryResults);
		}
	}
}
