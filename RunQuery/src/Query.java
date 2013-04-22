import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.QueryRequest;
import lemurproject.indri.QueryResult;
import lemurproject.indri.QueryResults;

public class Query {
	
	public Query() {
		
	}
	
	public void queryIndex(String path, String query) {
		QueryEnvironment env = new QueryEnvironment();
		try {
			env.addIndex(path);
			QueryRequest queryRequest = new QueryRequest();
			queryRequest.query = query;
			queryRequest.resultsRequested = 10;
			QueryResults queryResults = env.runQuery(queryRequest);
			for(QueryResult result : queryResults.results) {
				System.out.println(result.documentName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new Query().queryIndex("/home/aju/cs341/data/index", "#od10(#1(Bill Gates) founded Microsoft)");
	}
}
