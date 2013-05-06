
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Set;
import java.util.concurrent.Executors;

public class HighRecallFilter{
	public static void main(String[] args) throws IOException, InterruptedException{
		Set<String> output = new HashSet<String>();
		String entityFile = args[2];
		String indexLocation = args[1];
		String workingDirectory = args[0];
    	String filteredIndexFiles = args[3];
		if (workingDirectory.charAt(workingDirectory.length()-1) != '/')
			workingDirectory = workingDirectory + "/";
		
		String entity;
		BufferedReader buf = new BufferedReader(new FileReader(entityFile));
		ExecutorService e = Executors.newFixedThreadPool(16);
		while((entity=buf.readLine())!=null)
		{
			e.execute(new HRF(entity,indexLocation,output));
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
		System.out.println("Done querying!");
		writeResultsToFile(workingDirectory,"filtered",output);
		e = Executors.newFixedThreadPool(16);
		System.out.println("Output size: " + output.size());
		Set<String> downloadfiles = new HashSet<String>();
		for (String result:output)
    	{
    		CorpusFileName cf = new CorpusFileName(result);
    		cf.setlocalfilename(workingDirectory+cf.filename);
    		if (!downloadfiles.contains(cf.folder + "_" + cf.filename))
    		{
    			downloadfiles.add(cf.folder + "_" + cf.filename);
    			e.execute(cf);
    		}
    	}
		System.out.println("download file size: " + downloadfiles.size());
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
		System.out.println("Done downloading!");
		
		FilteredFileBuilder.createFiles(output, workingDirectory,filteredIndexFiles);
		
	}


public static class HRF implements Runnable{

	private Set<String> output;
	
	private String entity;
	private String indexLoc;
	public HRF(String entityInput, String indexLocation, Set<String> common)
	{
		output = common;
		entity = entityInput;
		indexLoc = indexLocation;
	}
	
	private synchronized void addtoSet(List<String> queryResults)
	{
		output.addAll(queryResults);
	}
	
	public static List<String> getEquivalents(String entity)
	{
		List<String> equivalents = new ArrayList<String>();
		String[] terms = entity.split("[_.,()]");
		List<String> qterms = new ArrayList<String>();
		for (int i = 0;i<terms.length;i++)
			if (terms[i].length() > 0)
				qterms.add(terms[i]);
		
		int numterms = qterms.size();
		if (numterms <= 2)
		{
			StringBuffer sbuf = new StringBuffer();
			for (String t:qterms)
			{
				sbuf.append(t);
				sbuf.append(" ");
			}
			sbuf.deleteCharAt(sbuf.length()-1);
			equivalents.add(sbuf.toString());
			if (numterms==2)
			{
    			sbuf = new StringBuffer();
    			for (int i = numterms-1;i>=0;i--)
    			{
    				sbuf.append(qterms.get(i));
    				sbuf.append(" ");
    			}
    			sbuf.deleteCharAt(sbuf.length()-1);
    			equivalents.add(sbuf.toString());
			}
		}
		else
		{
			for (int i = 0;i<numterms-1;i++)
			{   				
				StringBuffer sbuf = new StringBuffer();
				sbuf.append(qterms.get(i) + " ");
				sbuf.append(qterms.get(i+1));
				equivalents.add(sbuf.toString());
			}
		}	
		return equivalents;
	}
	public void run() 
	{    	
	    	String[] searchTerms = entity.split("\\$");
	    	//String entityName = searchTerms[0];
	    	Query q = new Query(indexLoc);
	    	for (String s:searchTerms)
	    	{
	    		//System.out.println("Processing: " + entityName);
	    		List<String> equivalents = getEquivalents(s);
	    		
	    		String queryString = QueryBuilder.buildOrQuery(equivalents);
	    		
	    		System.out.println("Querying Index for " + queryString + "...");
	    		
	    		List<String> queryResults = q.queryIndex(queryString);
	    		System.out.println("Adding to set for entity: " + entity);
	    		addtoSet(queryResults);
	    		
			}
	    	System.out.println("Done! for " + entity);
	    	return;
	}
}
    /*
	writeResultsToFile(workingDirectory,entity,output);	
    	/*
    	for (String result:output)
    	{
    		CorpusFileName cf = new CorpusFileName(result);
    		cf.setlocalfilename(workingDirectory+cf.filename);
    		cf.downloadfile();
    	}
    	
    	System.out.println("Done!");
    	return;
	}
	*/
	public static void writeResultsToFile(String dirname, String filename, Set<String> queryResults) throws IOException
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(dirname+filename, true));
		for (String s : queryResults)
		{
			buf.write(s);
			buf.newLine();
		}
		buf.flush();
		buf.close();
	}
	

}