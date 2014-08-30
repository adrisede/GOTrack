package ubc.pavlab.gotrack.utils;

import java.util.ArrayList;

/**
 * This class implements a container that matches the Uniprot id with its
 * properties (parents, edition, evidence, etc)
 * */
public class GeneAssoc {
	private String uniprotid;
	@SuppressWarnings("unused")
	private ArrayList<String> otherNames = new ArrayList<String>();
	private String GOTerm;
	private String GoParents;
	private String edition;
	private String evidence;
	private String symbol;

	public String getUniprotid() {
		return uniprotid;
	}

	public void setUniprotid(String uniprotid) {
		this.uniprotid = uniprotid;
	}

	public String getGOTerm() {
		return GOTerm;
	}

	public void setGOTerm(String gOTerm) {
		GOTerm = gOTerm;
	}

	public String getGoParents() {
		return GoParents;
	}

	public void setGoParents(String goParents) {
		GoParents = goParents;
	}

	public String getEdition() {
		return edition;
	}

	public void setEdition(String edition) {
		this.edition = edition;
	}

	public String getEvidence() {
		return evidence;
	}

	public void setEvidence(String evidence) {
		this.evidence = evidence;
	}

	/**
	 * Generic constructor for this class
	 * 
	 * @param uniprotid
	 * @param Goterm
	 * @param goParents
	 * @param evidence
	 * */
	public GeneAssoc(String symbol, String uniprotid, String Goterm,
			String goParents, String evidence) {
		this.symbol = symbol;
		this.uniprotid = uniprotid;
		this.GOTerm = Goterm;
		this.GoParents = goParents;
		this.evidence = evidence;
	}

	/**
	 * 
	 * @return
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * 
	 * @param symbol
	 */
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
}
