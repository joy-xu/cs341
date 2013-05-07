package retrieWin.Indexer;

import java.io.File;


public class Downloader {
	
	public static String downloadfile(String folder, String filename, String downloadDirectory)
	{
		String downloadfile = downloadDirectory + filename;
		String decryptedfile = downloadfile.substring(0,downloadfile.length()-4);
		String unxzfile = decryptedfile.substring(0,decryptedfile.length()-3);
		
		File f = new File(unxzfile);
		
		if (f.exists())	
			return unxzfile;
		
		String downloadURL = 
    			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
    			+ folder + filename;
    	
		
    	Process p;
    	
		String downloadCommand = "wget -O " + downloadfile + " " + downloadURL;
		//System.out.println(downloadCommand);
		
		try {
		
		// wget	
		p = Runtime.getRuntime().exec(downloadCommand);
		p.waitFor();
		
		// Decryption
		
		String decryptCommand = "gpg --yes -o " + decryptedfile + 
							" -d " + downloadfile;
		p = Runtime.getRuntime().exec(decryptCommand);
		p.waitFor();
		
		// UnXZ

    	String unxzCommand = "unxz " + decryptedfile;
    	p = Runtime.getRuntime().exec(unxzCommand);
		p.waitFor();
		
    	// Deleting downloaded file
		
    	String deleteCommand = "rm -f " + downloadfile;
		p = Runtime.getRuntime().exec(deleteCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			return null;
		}
    	return unxzfile;
	}
}
