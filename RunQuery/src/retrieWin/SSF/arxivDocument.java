package retrieWin.SSF;

import java.util.ArrayList;
import java.util.List;

public class arxivDocument {
	private List<String> authors;
	private List<String> acknowledgements;
	private List<List<String>> references;
	
	public arxivDocument() {
		setAuthors(new ArrayList<String>());
		setAcknowledgements(new ArrayList<String>());
		setReferences(new ArrayList<List<String>>());
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