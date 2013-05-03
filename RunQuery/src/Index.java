import lemurproject.indri.IndexEnvironment;


public class Index {
	public static void buildIndex(String indexLocation, String corpusLocation) {
		IndexEnvironment index = new IndexEnvironment();
		try {
			String[] fields = {"Time", "Text"};
			index.setDocumentRoot(corpusLocation);
			index.setStemmer("krovetz");
			index.setIndexedFields(fields);
			index.setMemory(1000000);
			index.create(indexLocation);
			index.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
