import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.io.IOException;
import util.CommandLineUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.util.HashSet;

public class HighRecallFilter {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		String entity  = args[2];
		
		String indexLoc = args[0];
		
		int numResults = Integer.MAX_VALUE;
		
    	String workingDirectory = args[1];
    	
    	List<String> output = new ArrayList<String>();
    	
    	if (workingDirectory.charAt(workingDirectory.length()-1) != '/')
    			workingDirectory = workingDirectory + "/";
    	
    	
    	String[] searchTerms = entity.split("\\$");
    	String entityName = searchTerms[0];
    	Query q = new Query(indexLoc);
    	for (String s:searchTerms)
    	{
    		System.out.println("Processing: " + entityName);
    		List<String> equivalents = new ArrayList<String>();
    		String[] terms = s.split("[_.,()]");
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
    		String queryString = QueryBuilder.buildOrQuery(equivalents);
    		
    		System.out.println("Querying Index for " + queryString + "...");
    		
    		List<String> queryResults = q.queryIndex(queryString);
    		output.addAll(new HashSet<String>(queryResults));
    		
		}
    	writeResultsToFile(workingDirectory,entityName,output);	
    	System.out.println("Done!");
    	return;
	}
	
	public static void writeResultsToFile(String dirname, String filename, List<String> queryResults) throws IOException
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
