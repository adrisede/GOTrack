package ubc.pavlab.gotrack.preprocessing;

import java.util.Hashtable;

/**
 * 
 * @author asedeno
 * 
 */
@SuppressWarnings("unchecked")
public class AssociationFile {
	@SuppressWarnings("rawtypes")
	private Hashtable geneTable = new Hashtable();

	public void addGene(String geneName, GeneAka akaList) {
		geneTable.put(geneName, akaList);
	}

	public void addGene(String geneName) {
		geneTable.put(geneName, new Integer(0));
	}

	public boolean isGeneInTable(String geneName) {
		return geneTable.containsKey(geneName);
	}

	@SuppressWarnings("rawtypes")
	public Hashtable getGeneTable() {
		return geneTable;
	}
}
