package retrieWin.Indexer;

import java.util.ArrayList;
import java.util.List;

public class ProcessTrecTextDocument {
	public static List<String> extractRelevantSentences(List<TrecTextDocument> documents, String entity1, String entity2) {
		List<String> returnString = new ArrayList<String>();
		for (TrecTextDocument t:documents)
		{
			List<String> currentAllSentences = t.sentences;
			boolean done = true;
			String output = "";
			int firstPos = -1;
			int secondPos = -1;
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
			
				
				firstPos = currentSentence.toLowerCase().indexOf(entity1.toLowerCase());
				secondPos = currentSentence.toLowerCase().indexOf(entity2.toLowerCase());
				if (!done && firstPos != 1 && firstPos < secondPos)
				{
					output = "";
					done = true;
				}
				if (!done)
				{
					if (secondPos != -1)
					{
						output = output + "." + currentSentence;
						returnString.add(output);						
					}
					if (firstPos > secondPos)
					{
						output = currentSentence;
						done = false;
					}
					else
					{
						output = "";
						done = true;
					}
				}
				else
				{
					if (firstPos == -1)
						continue;
					else
					{
						output = currentSentence;
						if (secondPos != -1)
						{
							returnString.add(output);
							output = "";
							done = true;
						}
						else
						{
							done = false;
						}
					}
				}
					
			}
		}
		return returnString;
	}
	
	public static List<String> extractRelevantSentences(List<TrecTextDocument> documents, String entity1) 
	{
		List<String> returnString = new ArrayList<String>();
		for (TrecTextDocument t:documents)
		{
			List<String> currentAllSentences = t.sentences;
			for (int j = 0;j<currentAllSentences.size();j++)
			{
				String currentSentence = currentAllSentences.get(j);
				if (currentSentence.toLowerCase().contains(entity1.toLowerCase()))
				{
					returnString.add(currentSentence);
				}
			}
		}
		return returnString;
	}
}
