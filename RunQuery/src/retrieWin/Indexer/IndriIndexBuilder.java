package retrieWin.Indexer;
import java.io.File;

import lemurproject.indri.IndexEnvironment;

public class IndriIndexBuilder {
	public static void buildIndex(String indexLocation, String corpusLocation) {
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
}
