package retrieWin.Indexer;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import retrieWin.SSF.*;
import retrieWin.Querying.*;
public class Indexer {

	public static void writeIndexToS3fs(String baseFolder,String indexLocation,String trecTextSerializedFile)
	{
		try{
			
			String s3directory = Constants.s3directory+baseFolder;
			Process p;
			if(indexLocation!=null) {
				String s3PutIndex = String.format("s3cmd put -r %s %s",indexLocation,s3directory+"index/");
				System.out.println(s3PutIndex);
				p = Runtime.getRuntime().exec(s3PutIndex);
				p.waitFor();
			}
			if(trecTextSerializedFile!=null) {
				String s3PutFiltered = String.format("s3cmd put %s %s",trecTextSerializedFile,s3directory);
				System.out.println(s3PutFiltered);
				
				p = Runtime.getRuntime().exec(s3PutFiltered);
				p.waitFor();
			}
		
		}
		catch (Exception e)
		{
			System.out.println("Writing to S3 failed");
			e.printStackTrace();
		}
	}
	
	public static void readIndexFromS3fs(String baseFolder, String indexLocation, String trecTextFileLocation)
	{
		try{

		String s3directory = Constants.s3directory+baseFolder;
		String s3Index = s3directory + "index/";
		String s3getIndex = String.format("s3cmd get -r %s* %s",s3Index,indexLocation);
		System.out.println(s3getIndex);
		Process p;
		p = Runtime.getRuntime().exec(s3getIndex);
		p.waitFor();
		
		
		String s3File = s3directory + "filteredSerialized.ser";
		String s3getFiltered = String.format("s3cmd get %s %s",s3File,trecTextFileLocation);
		System.out.println(s3getFiltered);
		p = Runtime.getRuntime().exec(s3getFiltered);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Reading from S3 failed");
			e.printStackTrace();
		}	
	}
	
	public static Boolean checkS3Existence(String folder, String file)
	{
		try{
		Process p;
		String s3cmdls = "s3cmd ls " + folder;
		System.out.println(s3cmdls);
		p = Runtime.getRuntime().exec(s3cmdls);
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = input.readLine()) != null) 
		{
		    String[] components = line.split("\\t");
		    String nameOnly = components[components.length-1];
		    String[] pathComponents = nameOnly.split("/");
		    String relName = pathComponents[pathComponents.length-1];
		    System.out.println(relName);
		    if (relName.equals(file))
		    	return true;
		}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static Boolean VerifyIndexExistence(String timestamp)
	{
		String[] splits = timestamp.split("-");
		String currentFolder = Constants.s3directory;

		for (int i = 0;i<4;i++)
		{
			if (!checkS3Existence(currentFolder,splits[i]))
				return false;
			currentFolder = currentFolder + splits[i] + "/";
		}
		return checkS3Existence(currentFolder,"index");
	}
	
	public static void createIndex(String timestamp,String baseFolder, String tmpdirLocation, String filteredIndexLocation, String serializedFileLocation,
			List<Entity> allEntities)
	{
		Boolean doesIndexExist = VerifyIndexExistence(timestamp);
		if (!doesIndexExist)
		{
			Indexer.createIndexHelper(timestamp, tmpdirLocation, filteredIndexLocation, serializedFileLocation, allEntities); 
			writeIndexToS3fs(baseFolder,filteredIndexLocation,serializedFileLocation);
		}	
		else
			readIndexFromS3fs(baseFolder,filteredIndexLocation,serializedFileLocation);
	} 
	
	public static void createUnfilteredIndex(List<String> downloadHours, String downloadDirectory, String saveFilesLocation, String indexLocation) {
		
		for(String downloadHour: downloadHours) {
			if(!downloadHour.endsWith("/"))
				downloadHour += "/";
			System.out.println(downloadHour);
			String folderName = downloadHour.replace("-", "/");
			String downloadFolder = downloadDirectory + folderName, saveFolder = saveFilesLocation + folderName;
			File baseDir = new File(downloadFolder);
			if (!baseDir.exists())
				baseDir.mkdirs();
			baseDir = new File(saveFolder);
			if (!baseDir.exists())
				baseDir.mkdirs();
			System.out.println(downloadHour);
			ThriftReader.GetFolder(downloadHour, downloadFolder, saveFolder);
			
			try {
				Process p;
				String deleteCommand = "sudo rm -rf " + downloadFolder;
				p = Runtime.getRuntime().exec(deleteCommand);
				p.waitFor();
			}
			catch (Exception exc)
			{
				System.out.println("Failed to delete temporary files");
			}
		}
		IndriIndexBuilder.buildIndex(indexLocation, saveFilesLocation);
	}

	public static void createIndexHelper(String folder, String tmpdirLocation, String filteredIndexLocation, String serializedFileLocation,
			List<Entity> allEntities)
	{
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
		
		//--------------------------------------------------------------
		ExecuteQuery queryExecutor = new ExecuteQuery(indexFolder);
		
		ExecutorService e = Executors.newFixedThreadPool(1);
		Set<TrecTextDocument> allResults = new HashSet<TrecTextDocument>();
		
		for (Entity entity:allEntities)
		{
			e.execute(new parallelQuerier(entity,queryExecutor,filesLocation,filteredFilesLocation, serializedFileLocation, allResults));
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
		
		Set<TrecTextDocument> output;
		Entity entity;
		ExecuteQuery queryExecutor;
		String filesLocation;
		String filteredFilesLocation;
		String serializedFileLocation;
		public parallelQuerier(Entity ent, ExecuteQuery eq, String loc,String filteredLoc, String serializedFileLoc, Set<TrecTextDocument> In)
		{
			output = In;
			entity = ent;
			queryExecutor = eq;
			filesLocation = loc;
			filteredFilesLocation = filteredLoc;
			serializedFileLocation = serializedFileLoc;
		}
		
		private synchronized void addToList(List<TrecTextDocument> results)
		{
			output.addAll(results);
		}
		/*
		private synchronized void writeResultsToFile(List<TrecTextDocument> results)
		{
			
			ThriftReader.WriteTrecTextDocumentToFile(results, "filtered", filteredFilesLocation);
			TrecTextDocument.serializeFile(results, serializedFileLocation, true);
		}
		*/
		public void run()
		{
			String query = QueryBuilder.buildOrQuery(entity.getExpansions());
			//System.out.println("Querying for: " + query);
			List<TrecTextDocument> queryResults = queryExecutor.executeQuery(query, Integer.MAX_VALUE, filesLocation);
			System.out.println("Query Results for: " + query + " : " + queryResults.size());
			//writeResultsToFile(queryResults);
			addToList(queryResults);
		}
	}
}
