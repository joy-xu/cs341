package retrieWin.Indexer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import retrieWin.SSF.*;
import retrieWin.Querying.*;
public class Indexer {

	public static void createIndex(String folder, String indexFolder, String filesLocation, String filteredIndexLocation, String filteredFilesLocation,
			List<retrieWin.SSF.Entity> allEntities)
	{
		ThriftReader.GetFolder(folder, filesLocation);
		IndriIndexBuilder.buildIndex(indexFolder, filesLocation);
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
			List<TrecTextDocument> queryResults = queryExecutor.executeQuery(query, Integer.MAX_VALUE, filesLocation);
			addToList(queryResults);
		}
	}
}
