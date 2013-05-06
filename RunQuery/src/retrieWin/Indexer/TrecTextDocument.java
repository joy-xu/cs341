package retrieWin.Indexer;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class TrecTextDocument {
	public final String docNumber;
	public final String text;
	public final String time;
	
	public TrecTextDocument(String dn, String txt, String tm)
	{
		docNumber = dn;
		text = tm;
		time = tm;
	}
	
	public void writeToFile(String folder,String file) throws Exception
	{
		BufferedWriter buf = new BufferedWriter(new FileWriter(folder+file));
		
			buf.write("<DOC>");
			buf.newLine();
			buf.write("<DOCNO>\n");
			buf.newLine();
			buf.write(docNumber);
			buf.newLine();
			buf.write("</DOCNO>");
			buf.newLine();
			buf.write("<TIME>\n");
			buf.newLine();
			buf.write(time);
			buf.newLine();
			buf.write("</TIME>\n");
			buf.newLine();
			buf.write("<TEXT>\n");
			buf.newLine();
			buf.write(text);
			buf.newLine();
			buf.write("</TEXT>\n");
			buf.newLine();
			buf.write("</DOC\n>");
			buf.newLine();
			buf.flush();
		
		buf.close();
	}
}
