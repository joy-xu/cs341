package retrieWin.Indexer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.FileUtils;
public class TrecTextDocument implements Serializable{
	
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
	
	public static void serializeFile(List<TrecTextDocument> results,String serializedFileLocation)
	{
		FileUtils.writeFile(results, serializedFileLocation);
	}
	
	public static List<TrecTextDocument> getFromStoredFile(List<String> queryResults, String filteredFileName)
	{
		@SuppressWarnings("unchecked")
		List<TrecTextDocument> storedFiles = (List<TrecTextDocument>)FileUtils.readFile(filteredFileName);
		List<TrecTextDocument> output = new ArrayList<TrecTextDocument>();
		Set<String> queryResultsSet = new HashSet<String>(queryResults);
		
		for (TrecTextDocument candidate:storedFiles)
		{
			if (queryResultsSet.contains(candidate.docNumber))
				output.add(candidate);
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
		{}
	}
}
