
public class QueryBuilder {
	public static String buildOrderedQuery(String first, String second, int withinWords) {
		String queryString = null;
		if (second != null)
			queryString = "#od" + withinWords + "(#1(" + first + ") #1(" + second + "))";
		else
			queryString = "#1(" + first + ")";
		return queryString;
	}
	
	public static String buildUnorderedQuery(String first, String second, int withinWords) {
		String queryString = null;
		if (second != null)
			queryString = "#uw" + withinWords + "(#1(" + first + ") #1(" + second + "))";
		else
			queryString = "#1(" + first + ")";
		return queryString;
	}
	
	public static String buildSingleTermQuery(String term)
	{
		term = term.replaceAll("[^A-Za-z0-9 ]", "");
		if(term.contains(",")) {
			return String.format("#%d(%s)", 2, term.replace(",", ""));
		}
		return "#1(" + term + ")";
	}
}
