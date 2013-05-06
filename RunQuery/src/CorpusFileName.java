import java.io.File;
import java.io.IOException;

public class CorpusFileName {
	public String folder;
	public String streamID;
	public String timeStamp;
	public String filename;
	public String localfilename;
	public String date;
	public String time;
	public CorpusFileName(String fullName)
	{
		String[] a = fullName.split("_");
		streamID = a[0];
		localfilename = a[1];
		String[] b = localfilename.split("/");
		filename = b[b.length-1];
		
		timeStamp = a[2];
		String timeTokens[] = timeStamp.split("T");
		date = timeTokens[0];
		time = timeTokens[1];
		folder = timeTokens[0] + "-" + timeTokens[1].substring(0,2);
	}
	
	
	public void setlocalfilename(String localname)
	{
		localfilename = localname;
	}
	
	public void downloadfile() throws IOException, InterruptedException
	{
		File f = new File(localfilename);
		if (f.exists())
			return;
		
		String downloadURL = 
    			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
    			+ folder + "/" + filename;
    	
		String downloadfilename = localfilename + ".xz.gpg";
    	Process p;
    	File downloadF = new File(downloadfilename);
    	if (!downloadF.exists())
    	{
    		String downloadCommand = "wget -O " + downloadfilename + " " + downloadURL;
    		System.out.println(downloadCommand);
    		p = Runtime.getRuntime().exec(downloadCommand);
    		p.waitFor();
    	}
    	String decryptedfilename = localfilename + ".xz";
    	File decryptF = new File(decryptedfilename);
    	if (!decryptF.exists())
    	{
    		String decryptCommand = "gpg -o " + decryptedfilename + 
    							" -d " + downloadfilename;
    		p = Runtime.getRuntime().exec(decryptCommand);
    		p.waitFor();
    	}
    	
    	String unxzCommand = "unxz " + decryptedfilename;
    	
    	//System.out.println(unxzCommand);
    	p = Runtime.getRuntime().exec(unxzCommand);
    	p.waitFor();
    	String deleteCommand = "rm -f " + downloadfilename;
    	p = Runtime.getRuntime().exec(deleteCommand);
    	p.waitFor();
	}
}
