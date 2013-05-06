import java.io.File;
import java.util.Comparator;

public class CorpusFileName implements Runnable{
	
	public static class Filenamecomparator implements Comparator<CorpusFileName>{

		@Override
		public int compare(CorpusFileName c1, CorpusFileName c2) {

			int folderComparison = c1.folder.compareTo(c2.folder);
			if (folderComparison != 0)
				return folderComparison;
			else
			{
				int fileComparison = c1.filename.compareTo(c2.filename);
				return fileComparison;
			}
		}
		
	}
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
	
	public void downloadfile()
	{
		File f = new File(localfilename.substring(0, localfilename.length()-7));
		String downloadfilename = localfilename;
		File downloadF = new File(localfilename);
		String decryptedfilename = localfilename.substring(0,localfilename.length()-4);
		File decryptF = new File(decryptedfilename);
		
		if (f.exists() || downloadF.exists() || decryptF.exists())
			return;
		
		String downloadURL = 
    			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
    			+ folder + "/" + filename;
    	
	
    	Process p;
    	
		String downloadCommand = "wget -O " + downloadfilename + " " + downloadURL;
		//System.out.println(downloadCommand);
		
		try {
			p = Runtime.getRuntime().exec(downloadCommand);
			p.waitFor();
		}
		catch (Exception e)
		{
			return;
		}

		String decryptCommand = "gpg -o " + decryptedfilename + 
							" -d " + downloadfilename;
		
		try 
		{
			p = Runtime.getRuntime().exec(decryptCommand);
			p.waitFor();
		}
		catch (Exception e)
		{
			return;
		}
    	
    	String unxzCommand = "unxz " + decryptedfilename;
    	
    	//System.out.println(unxzCommand);
		try {
			p = Runtime.getRuntime().exec(unxzCommand);
			p.waitFor();
		}
		catch (Exception e)
		{
			return;
		}
    	
    	String deleteCommand = "rm -f " + downloadfilename;
		try {
			p = Runtime.getRuntime().exec(deleteCommand);
			p.waitFor();
		}
		catch (Exception e)
		{
			return;
		}
    	
	}
	
	public void run()
	{
		downloadfile();
		return;
	}
}
