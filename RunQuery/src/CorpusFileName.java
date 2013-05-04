
public class CorpusFileName {
	public String folder;
	public String streamID;
	public String timeStamp;
	public String filename;
	public String date;
	public String time;
	public CorpusFileName(String fullName)
	{
		String[] a = fullName.split("_");
		streamID = a[0];
		String localFileName = a[1];
		String[] b = localFileName.split("/");
		filename = b[b.length-1];
		
		timeStamp = a[2];
		String timeTokens[] = timeStamp.split("T");
		date = timeTokens[0];
		time = timeTokens[1];
		folder = timeTokens[0] + "-" + timeTokens[1].substring(0,2);
	}
}
