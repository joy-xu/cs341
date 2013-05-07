package retrieWin.Indexer;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import retrieWin.SSF.Constants;
import lemurproject.indri.IndexEnvironment;

public class IndriIndexBuilder {
	public static void buildIndexFromJavaAPI(String indexLocation, String corpusLocation) {
		IndexEnvironment index = new IndexEnvironment();
		try {
			String[] fields = {"TIME", "TEXT"};

			index.setDocumentRoot(corpusLocation);


			index.setStoreDocs(false);
			index.setStemmer("krovetz");
			index.setIndexedFields(fields);
			index.setNumericField("TIME", true,"DateFieldAnnotator");
			index.setMemory(1000000);
			index.setNormalization(true);
			index.create(indexLocation);
			File dir = new File(corpusLocation);
			if(dir.exists()) {
				for(File file:dir.listFiles()) {
					//System.out.println(file.getAbsolutePath());
					index.addFile(file.getAbsolutePath(), "trectext");
				}
			}
			
			index.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void buildIndex(String indexLocation, String corpusLocation)
	{
		Process p;
		
		String buildIndexCommand = String.format("IndriBuildIndex %s -corpus.path=%s -index=%s -memory=%s", Constants.indriBuildIndexParamFile,
					corpusLocation, indexLocation, "5000m");
		try {
			p = Runtime.getRuntime().exec(buildIndexCommand);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while((line = input.readLine())!=null)
				System.out.println(line);
		}
		catch (Exception e)
		{
			System.out.println("Index building failed");
			e.printStackTrace();
		}
	}
}
