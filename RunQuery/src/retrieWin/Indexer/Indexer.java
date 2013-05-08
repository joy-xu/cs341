package retrieWin.Indexer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import retrieWin.SSF.*;
import retrieWin.Querying.*;
public class Indexer {

	public static void writeIndexToS3fs(String timestamp,String indexLocation,String trecTextSerializedFile)
	{
		try{
		String s3directory = Constants.s3directory+timestamp + "/";
		String s3MakeDirectory = String.format("sudo mkdir %s",s3directory);
		Process p;
		p = Runtime.getRuntime().exec(s3MakeDirectory);
		p.waitFor();
				
		String s3cmdCommand = String.format("sudo cp -r %s %s",indexLocation,s3directory);
		System.out.println(s3cmdCommand);
		
		p = Runtime.getRuntime().exec(s3cmdCommand);
		p.waitFor();
		
		String s3cmdSerializedFileCopyCommand = String.format("sudo cp %s %s",trecTextSerializedFile,s3directory);
		System.out.println(s3cmdSerializedFileCopyCommand);
		p = Runtime.getRuntime().exec(s3cmdSerializedFileCopyCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Writing to S3 failed");
			e.printStackTrace();
		}
	}
	
	public static void readIndexFromS3fs(String timestamp)
	{
		try{
		String s3directory = Constants.s3directory+timestamp + "/";
	
		String s3cmdCommand = String.format("sudo cp -r %s .",s3directory);
		System.out.println(s3cmdCommand);
		Process p;
		p = Runtime.getRuntime().exec(s3cmdCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Reading from S3 failed");
			e.printStackTrace();
		}	
	}
	
	public static Boolean VerifyIndexExistence(String timestamp)
	{
		String s3IndexFolder = Constants.s3directory + timestamp;
		File f = new File(s3IndexFolder);
		if (!f.exists())
			return false;
		
		File[] subdirectories = f.listFiles();
		Set<String> subdirectoryNames = new HashSet<String>();
		for (File subdir:subdirectories)
			subdirectoryNames.add(subdir.getName());
		return (subdirectoryNames.contains("index") && subdirectoryNames.contains("filteredSerialized.ser"));
	}
	
	public static void createIndex(String folder, String tmpdirLocation, String filteredIndexLocation, String serializedFileLocation,
			List<Entity> allEntities)
	{
		Boolean doesIndexExist = VerifyIndexExistence(folder);
		if (!doesIndexExist)
		{
			Indexer.createIndexHelper(folder, tmpdirLocation, filteredIndexLocation, serializedFileLocation, allEntities); 
			writeIndexToS3fs(folder,filteredIndexLocation,serializedFileLocation);
		}	
		else
			readIndexFromS3fs(folder);
	}
	
	public static void createIndexHelper(String folder, String tmpdirLocation, String filteredIndexLocation, String serializedFileLocation,
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
