package ubc.pavlab.gotrack.database;
/**
 * This class models one entry in the <species>_count table
 * 
 * @author asedeno
 *
 */
public class CountEntry {
	private String gene;
	private String symbol="\\N";
	private String directs="\\N";
	private String inferred="\\N";
	private String multifunc="\\N";
	private String jaccard="\\N";
	private String edition="\\N";
	
	public String getMultifunc() {
		return multifunc;
	}

	public void setMultifunc(String multifunc) {
		this.multifunc = multifunc;
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getDirects() {
		return directs;
	}

	public void setDirects(String directs) {
		this.directs = directs;
	}

	public String getInferred() {
		return inferred;
	}

	public void setInferred(String inferred) {
		this.inferred = inferred;
	}


	public String getJaccard() {
		return jaccard;
	}

	public void setJaccard(String jaccard) {
		this.jaccard = jaccard;
	}

	public String getEdition() {
		return edition;
	}

	public void setEdition(String edition) {
		this.edition = edition;
	}

}
