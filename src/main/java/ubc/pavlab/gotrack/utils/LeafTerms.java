package ubc.pavlab.gotrack.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

import org.xml.sax.SAXException;

import ubc.pavlab.gotrack.constants.Species;
import ubic.erminej.data.GeneSetTerm;
import ubic.erminej.data.GeneSetTerms;

/**
 * This class gets the leaf terms 
 * 
 * @author asedeno
 * */
public class LeafTerms {

	/**
	 * This function creates file go.childcount.20$y-$m.txt
	 * 
	 * @param indir
	 * @param termdbFile
	 * @param outputdir
	 */
	public static void getChildCount(String species, String termdbFile,
			java.util.logging.Logger logger) {
		String isOldEdition;

		if (termdbFile == null) {
			System.out.println("TERMDBFILE NULL!!, verify.");
			return;
		}
		// Get last part of termdb file name (year-month)
		String year = termdbFile.substring(termdbFile.length() - 10,
				termdbFile.length() - 6);

		if (Integer.valueOf(year) < 2007)
			isOldEdition = "TRUE";
		else
			isOldEdition = "FALSE";

		String outputFile = "go.childcount."
				+ termdbFile.substring(termdbFile.length() - 10,
						termdbFile.length() - 3) + ".txt";
		GeneSetTerms gs;
		try {

			BufferedWriter bw = Species.getWriterParentChildren(outputFile);
			if (bw != null) {
				gs = new GeneSetTerms(Species.getTermDBFolder().getAbsolutePath()
						+ "/" + termdbFile, Boolean.valueOf(isOldEdition));

				int j = 0;
				for (GeneSetTerm term : gs.getTerms()) {
					if (term.getDefinition().startsWith("OBSOLETE"))
						continue;

					Collection<GeneSetTerm> c = gs.getAllChildren(term); // before
																			// getChildren
					bw.write(term.getId() + "\t" + c.size() + "\n");
					if (++j % 1000 == 0)
						bw.write(".");
				}
				bw.close();
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			logger.severe("Fatal error getting children for " + termdbFile);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		}

	}

}
