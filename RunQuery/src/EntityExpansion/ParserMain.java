package EntityExpansion;
import java.util.*;
import java.io.*;
public class ParserMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		HTMLParser parser = new HTMLParser(args[0]);
		String outputFile = args[0] + ".expansion";
		List<String> anchors = parser.getOutGoingWikiAnchors(10);
		List<String> titles = parser.getOutGoingWikiPageTitles(10);
		
		System.out.println("Querying for " + args[0]);
		
		//System.out.println("Top Anchor Text: ");
		//for (int a = 0;a < anchors.size();a++)
			//System.out.println(anchors.get(a));
		
		//System.out.println("Top Titles: ");
		BufferedWriter buf = new BufferedWriter(new FileWriter(outputFile));
		for (int t = 0;t < titles.size();t++)
		{
			buf.write(titles.get(t));
			buf.newLine();
		}
		buf.close();
	}

}
