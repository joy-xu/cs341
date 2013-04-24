import java.util.ArrayList;
import java.util.List;

import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.QueryRequest;
import lemurproject.indri.QueryResult;
import lemurproject.indri.QueryResults;

public class Query {
	final String INDEX_LOCATION;
	QueryEnvironment env = new QueryEnvironment();
	public Query(String IndexLocation) {
		this.INDEX_LOCATION = IndexLocation;
		try {
			env.addIndex(INDEX_LOCATION);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<String> queryIndex(String query, int resultsRequested) {
		List<String> results = new ArrayList<String>();
		try {
			QueryRequest queryRequest = new QueryRequest();
			queryRequest.query = query;
			queryRequest.resultsRequested = resultsRequested;
			QueryResults queryResults = env.runQuery(queryRequest);
			for(QueryResult result : queryResults.results) {
				results.add(result.documentName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}
	
	public List<String> queryIndex(String query) {
		String abc = "ABCD";
		System.out.println(abc.toLowerCase());
		System.out.println(abc);
		ExtractRelation er = new ExtractRelation();
		er.findRelationsFromFile("/home/aju/cs341/data/expanded/retrieved.txt", "Bill Gates", "Microsoft");
		//er.findRelation("Bill Gates founded the billion dollar company Microsoft.", "Bill Gates", "Microsoft");
		return queryIndex(query, Integer.MAX_VALUE);
	}

	public static void main(String[] args) {
		System.out.println(new Query("/home/aju/cs341/data/index").queryIndex("#od10(#1(Bill Gates) founded Microsoft)"));
	}
}
