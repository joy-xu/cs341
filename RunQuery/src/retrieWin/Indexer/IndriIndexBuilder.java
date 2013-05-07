package retrieWin.Indexer;
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
			index.create(indexLocation);
			index.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
