import java.util.List;
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
		return "#1(" + term + ")";
	}
	
	public static String buildOrQuery(List<String> equivalents)
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("<");
		for (String s:equivalents)
		{
			sbuf.append("#5(");
			sbuf.append(s);
			sbuf.append(")");
			sbuf.append(" ");
		}
		sbuf.deleteCharAt(sbuf.length()-1);
		sbuf.append(">");
		return sbuf.toString();
	}
}
