package retrieWin.Indexer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
public class TrecTextDocument {
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
