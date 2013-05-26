package retrieWin.Indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.Entity;
import retrieWin.SSF.SlotPattern;
import retrieWin.Utils.NLPUtils;


public class Downloader implements Runnable{
	
	public String fld;
	public String f;
	public String wd;
	public static String downloadfile(String folder, String filename, String downloadDirectory)
	{
		String downloadfile = downloadDirectory + filename;
		String decryptedfile = downloadfile.substring(0,downloadfile.length()-4);
		String unxzfile = decryptedfile.substring(0,decryptedfile.length()-3);
		
		File f = new File(unxzfile);
		
		if (f.exists())	
		{
			return unxzfile;
		}	
		
		String downloadURL = 
    			"http://s3.amazonaws.com/aws-publicdatasets/trec/kba/kba-streamcorpus-2013-v0_2_0-english-and-unknown-language/"
    			+ folder + filename;
    	
		
    	Process p;
    	
		String downloadCommand = "wget -O " + downloadfile + " " + downloadURL;
		
		
		try {
		
		//wget	
		p = Runtime.getRuntime().exec(downloadCommand);
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while((line = input.readLine())!=null)
			System.out.println(line);
		
		// Decryption
		
		String decryptCommand = "gpg --yes -o " + decryptedfile + 
							" -d " + downloadfile;
		p = Runtime.getRuntime().exec(decryptCommand);
		input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		while((line = input.readLine())!=null)
			System.out.println(line);
		
		// UnXZ

    	String unxzCommand = "unxz " + decryptedfile;
    	p = Runtime.getRuntime().exec(unxzCommand);
    	input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		while((line = input.readLine())!=null)
			System.out.println(line);
		
    	// Deleting downloaded file
		
    	String deleteCommand = "rm -f " + downloadfile;
		p = Runtime.getRuntime().exec(deleteCommand);
		p.waitFor();
		}
		catch (Exception e)
		{
			System.out.println("Failed to download");
			e.printStackTrace();
			return null;
		}
    	return unxzfile;
	}

	public Downloader(String fldIn, String fIn, String wdIn)
	{
		fld = fldIn;
		f = fIn;
		wd = wdIn;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		downloadfile(fld,f,wd);
	}
}
