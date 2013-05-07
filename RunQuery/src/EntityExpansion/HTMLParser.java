package EntityExpansion;
import java.util.*;
import java.util.regex.*;
import java.util.regex.Pattern;
import java.io.*;

public class HTMLParser {

	private List<String> sentences = new ArrayList<String>();
	HTMLParser(String filename) throws IOException
	{
		BufferedReader buf = new BufferedReader(new FileReader(filename));
		String line;
		while((line = buf.readLine())!= null)
		{
			sentences.add(line);
		}
		buf.close();
	}
	
	public List<String> getOutGoingWikiAnchors(int number)
	{
		List<String> output = new ArrayList<String>();
		Map<String,Integer> counts = new HashMap<String,Integer>();
		Pattern p = Pattern.compile("<a href=\"/wiki/[^>]*>([^<]*)</a>");
		for (int i = 0;i<sentences.size();i++)
		{
			String currentSentence = sentences.get(i);
			if (currentSentence.length() < 4)
				continue;
			if (!currentSentence.substring(0, 3).equals("<p>") && !currentSentence.substring(0,4).equals("<li>"))
				continue;
			
			Matcher m = p.matcher(currentSentence);
			while(m.find())
			{
				String currentAnchor = m.group(1);
				if (counts.containsKey(currentAnchor))
					counts.put(currentAnchor, counts.get(currentAnchor)+1);
				else
					counts.put(currentAnchor,1);
			}
		}
		retrieWin.Utils.PriorityQueue<String> pq = new retrieWin.Utils.PriorityQueue<String>();
		
		Iterator<String> it = counts.keySet().iterator();
		while(it.hasNext())
		{
			String currentAnchor = it.next();
			pq.add(currentAnchor,counts.get(currentAnchor));
		}
		
		int i = 0;
		while(i < number && pq.hasNext())
		{
			String added = pq.next();
			
			output.add(added);
			i++;
		}
		return output;
	}
	
	
	public List<String> getOutGoingWikiPageTitles(int number)
	{
		List<String> output = new ArrayList<String>();
		Pattern p = Pattern.compile("<a href=\"/wiki/[\\S]* title=\"([^\"]*)\">");
		Map<String,Integer> counts = new HashMap<String,Integer>();
		for (int i = 0;i<sentences.size();i++)
		{
			String currentSentence = sentences.get(i);
			if (currentSentence.length() < 4)
				continue;
			if (!currentSentence.substring(0, 3).equals("<p>") && !currentSentence.substring(0,4).equals("<li>"))
				continue;
			
			Matcher m = p.matcher(currentSentence);
			while(m.find())
			{
				String currentAnchor = m.group(1);
				if (counts.containsKey(currentAnchor))
					counts.put(currentAnchor, counts.get(currentAnchor)+1);
				else
					counts.put(currentAnchor,1);
			}
		}
		retrieWin.Utils.PriorityQueue<String> pq = new retrieWin.Utils.PriorityQueue<String>();
		
		Iterator<String> it = counts.keySet().iterator();
		while(it.hasNext())
		{
			String currentAnchor = it.next();
			pq.add(currentAnchor,counts.get(currentAnchor));
		}
		
		int i = 0;
		while(i < number && pq.hasNext())
		{
			String added = pq.next();
			
			output.add(added);
			i++;
		}
		return output;
	}
}
