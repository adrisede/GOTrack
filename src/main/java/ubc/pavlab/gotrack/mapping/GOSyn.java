package ubc.pavlab.gotrack.mapping;

/**
 * This class models a synonym 
 * @author asedeno
 * 
 */
public class GOSyn {

	private String goTerm;
	private String synonyms;

	public GOSyn(String goterm, String synonyms) {
		goTerm = goterm;
		this.synonyms = synonyms;
	}

	public String getGoTerm() {
		return goTerm;
	}

	public void setGoTerm(String goTerm) {
		this.goTerm = goTerm;
	}

	public String getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(String synonyms) {
		this.synonyms = synonyms;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
