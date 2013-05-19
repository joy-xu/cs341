package retrieWin.Indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fig.basic.LogInfo;

import retrieWin.Utils.FileUtils;

public class TrecTextDocument implements Serializable, Comparable<TrecTextDocument>{
	
	private static final long serialVersionUID = 1L;
	public final String docNumber;
	public final String text;
	public final String time;
	public final List<String> sentences;
	public TrecTextDocument(String dn, String txt, String tm, List<String> sentencesIn)
	{
		docNumber = dn;
		text = txt;
		time = tm;
		sentences = sentencesIn;
	}
	
	public static void serializeFile(Set<TrecTextDocument> results,String serializedFileLocation)
	{
		Map<String, TrecTextDocument> hmap = new HashMap<String,TrecTextDocument>();
		for (TrecTextDocument t:results)
		{
			String docNo = t.docNumber;
			hmap.put(docNo,t);
		}
		FileUtils.writeFile(hmap, serializedFileLocation);
	}
	
	public static void serializeFile(List<TrecTextDocument> results,String serializedFileLocation)
	{
		Map<String, TrecTextDocument> hmap = new HashMap<String,TrecTextDocument>();
		for (TrecTextDocument t:results)
		{
			String docNo = t.docNumber;
			hmap.put(docNo,t);
		}
		FileUtils.writeFile(hmap, serializedFileLocation);
	}
	
	@SuppressWarnings("unchecked")
	public static void serializeFile(Set<TrecTextDocument> results,String serializedFileLocation, Boolean append)
	{
		File f = new File(serializedFileLocation);
		
		if (!append || !f.exists())
		{
			serializeFile(results,serializedFileLocation);
		}
		else
		{
			Map<String,TrecTextDocument> storedFiles = (Map<String,TrecTextDocument>)FileUtils.readFile(serializedFileLocation);
			for (TrecTextDocument r:results)
			{
				String docNo = r.docNumber;
				if (!storedFiles.containsKey(docNo))
				{
					storedFiles.put(docNo, r);
				}
			}
			FileUtils.writeFile(storedFiles,serializedFileLocation);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public static void serializeFile(List<TrecTextDocument> results,String serializedFileLocation, Boolean append)
	{
		if (!append)
		{
			serializeFile(results,serializedFileLocation);
		}
		else
		{
			Map<String,TrecTextDocument> storedFiles = (Map<String,TrecTextDocument>)FileUtils.readFile(serializedFileLocation);
			for (TrecTextDocument r:results)
			{
				String docNo = r.docNumber;
				if (!storedFiles.containsKey(docNo))
				{
					storedFiles.put(docNo, r);
				}
			}
			FileUtils.writeFile(storedFiles,serializedFileLocation);
		}
	}
	
	
	public static List<TrecTextDocument> getFromStoredFile(Set<String> queryResults, String filteredFileName, List<String> documentTypes)
	{
		@SuppressWarnings("unchecked")
		Map<String,TrecTextDocument> storedFiles = (Map<String,TrecTextDocument>)FileUtils.readFile(filteredFileName);
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
		Set<String> queryResultsSet = new HashSet<String>(queryResults);
		
		for (String docNo:queryResultsSet)
		{
			if (storedFiles.containsKey(docNo)) {
				String documentType = docNo.split("__")[1].split("-")[0];
				if(documentTypes == null || documentTypes.contains(documentType)) {
					System.out.println("%%^^&&&&& "  + documentType);
					output.add(storedFiles.get(docNo));
				}
			}
			else
				System.out.println("File not found. You are definitely doing something wrong");
		}
		return output;
	}
	
	public void writeToFile(String filename,String workingDirectory)
	{
		try 
		{
			BufferedWriter buf = new BufferedWriter(new FileWriter(workingDirectory+filename,true));					
			buf.write("<DOC>");
			buf.newLine();
			buf.write("<DOCNO>");
			buf.newLine();
			buf.write(docNumber);
			buf.newLine();
			buf.write("</DOCNO>");
			buf.newLine();
			buf.write("<TIME>");
			buf.newLine();
			buf.write(time);
			buf.newLine();
			buf.write("</TIME>");
			buf.newLine();
			buf.write("<TEXT>");
			buf.newLine();
			buf.write(text);
			buf.newLine();
			buf.write("</TEXT>");
			buf.newLine();
			buf.write("</DOC>");
			buf.newLine();
			buf.flush();
			
			buf.close();
		}
		catch (Exception e)
		{e.printStackTrace();}
	}
	
	

	@Override
	public int compareTo(TrecTextDocument other) {
		
		return (this.docNumber.compareTo(other.docNumber));
		
	}
}
