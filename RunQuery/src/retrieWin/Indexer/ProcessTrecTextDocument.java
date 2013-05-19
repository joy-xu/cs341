package retrieWin.Indexer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessTrecTextDocument {
	public static List<String> extractRelevantSentences(Collection<TrecTextDocument> documents, String entity1, String entity2) {
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
	
	public static List<String> extractRelevantSentences(Collection<TrecTextDocument> documents, String entity1) 
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
	
	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    String s = pattern.matcher(nfdNormalizedString).replaceAll("");
	    return s;
	}

	public static List<String> getCleanedSentences(List<String> sentences) {
		List<String> results = new ArrayList<String>();
		
		for(String sentence : sentences) {
			//System.out.println("Input :" + sentence);
			//Replacing accented a followed by two upper ascii characters with a '.
			sentence = sentence.replaceAll("â[^\\x00-\\x7f]{2}\\s?", "'");
			//cleaning out upper ascii characters
			sentence = sentence.replaceAll("[Ââ[^\\x00-\\x7f]]+", "");
			sentence = deAccent(sentence);
			//Remove things between {{ }}
			sentence = sentence.replaceAll("\\{\\s*\\{[^\\}]*\\}\\s*\\}","");
			// Handle links inside square brackets
			
			Pattern p = Pattern.compile("\\[\\s*\\[([^\\|\\]]*)\\|([^\\|\\]]*)\\]\\s*\\]");
			Matcher m = p.matcher(sentence);
			while(m.find())
			{
				
				String replacement = m.group(2);
				//System.out.println(replacement);
				sentence = m.replaceFirst(replacement);
				//System.out.println(sentence);
				m.reset(sentence);
			}
			
			sentence = sentence.replaceAll("\\[\\s*\\[", "");
			sentence = sentence.replaceAll("\\]\\s*\\]","");
					
			results.addAll(Arrays.asList(sentence.split("[|]+")));
			//System.out.println("Process : "+ sentence);
			//for(String s:))
			//	System.out.println("Output :" + s);
			//results.addAll();
		}
		return results;
	}
}
