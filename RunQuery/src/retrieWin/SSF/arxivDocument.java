package retrieWin.SSF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrieWin.Indexer.ThriftReader;
import retrieWin.Utils.NLPUtils;
import retrieWin.Utils.Utils;
import streamcorpus.StreamItem;

public class arxivDocument {
	private List<String> authors;
	private List<String> acknowledgements;
	private List<List<String>> references;
	
	public arxivDocument() {
		setAuthors(new ArrayList<String>());
		setAcknowledgements(new ArrayList<String>());
		setReferences(new ArrayList<List<String>>());
	}
	
	public arxivDocument(String docNo) {
		boolean inAckSec, inRefSec;
        Matcher matcher;
        NLPUtils coreNLP = new NLPUtils();
        
        String[] a = docNo.split("__");
		String streamID = a[0];
		String localfilename = a[1];
		String[] b = localfilename.split("/");
		String fileName = b[b.length-1];
		String folder = a[2];
		
		try {
			StreamItem item = ThriftReader.GetFilteredStreamItems(folder, fileName, "tmp/", new HashSet<String>(Arrays.asList(streamID))).get(0);
	        
			for(String auth: Utils.bb_to_str(item.other_content.get("abstract").raw).split("Authors:|Categories:")[1].trim().split(",| and ")) {
				if(!auth.isEmpty())
					addAuthor(auth.replaceAll("\n", " ").trim());
			}
			inAckSec = false;
			inRefSec = false;
			for(String sent: Utils.getSentences(item)) {
				if(sent.toLowerCase().contains("acknowledg") || sent.toLowerCase().contains("thank") || sent.toLowerCase().contains("grateful")) {
					inAckSec = true;
					if(sent.length() <= 20)
						continue;
				}
				else if(sent.toLowerCase().replaceAll(" ", "").contains("reference")) {
					inRefSec = true;
					if(sent.length() <= 20)
						continue;
				}
				if(inAckSec) {
					for(String per: coreNLP.getPersons(sent))
						addAcknowledgement(per);
					inAckSec = false;
				}
				
				if(inRefSec || sent.startsWith("[")) {
					matcher = Pattern.compile("\\]").matcher(sent);
					if(!matcher.find()) {
						matcher = Pattern.compile("[a-zA-Z]").matcher(sent);
						if(!matcher.find())
							continue;
						else
							sent = sent.substring(Integer.valueOf(matcher.start()), sent.length()-1);
					}
					else if(matcher.start() == sent.length()-1)
						continue;
					else
						sent = sent.substring(Integer.valueOf(matcher.start()) + 1, sent.length()-1);
					
					List<String> reference = new ArrayList<String>();
					for(String auth: sent.split(",| and ")) {
						if(auth.isEmpty())
							continue;
						if(auth.length() < 20 && Pattern.matches("[a-zA-Z \\.]*[A-Z]\\.[a-zA-Z \\.]*", auth))
							reference.add(auth);
						else
							break;
					}
					if(!reference.isEmpty())
						addReferences(reference);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> _authors) {
		authors = _authors;
	}
	
	public void addAuthor(String author) {
		authors.add(author);
	}

	public List<String> getAcknowledgements() {
		return acknowledgements;
	}

	public void setAcknowledgements(List<String> _acknowledgements) {
		acknowledgements = _acknowledgements;
	}
	
	public void addAcknowledgement(String acknowledgement) {
		acknowledgements.add(acknowledgement);
	}

	public List<List<String>> getReferences() {
		return references;
	}

	public void setReferences(List<List<String>> _references) {
		references = _references;
	}
	
	public void addReferences(List<String> reference) {
		references.add(reference);
	}
}