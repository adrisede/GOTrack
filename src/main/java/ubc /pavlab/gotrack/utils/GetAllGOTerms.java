package ubc.pavlab.gotrack.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;

/**
 * This is an auxiliary class to compute the full list of goterms always present 
 * 
 * @author asedeno
 * 
 */
public class GetAllGOTerms {

	/**
	 * This function calculates GO terms present in each edition and the GO
	 * Terms always present (in all editions)
	 * 
	 * This function is called by gotrack
	 * 
	 * @param indir
	 *            Directory to look at
	 * @param tmpdir
	 *            Directory where files TermsAlwaysPresent and FullTerms will be
	 *            stored
	 * 
	 * */
	public static void createAlwaysPresentAndFullTermsFiles(String species,
			int numOfEd, File[] assoc, Logger log) {
		log.info("Creating files TermsAlwaysPresent.txt and FullTerms.txt for >> "
				+ species);
		Map<String, Integer> goterms = null;
		try {

			BufferedWriter bwalways = Species.getWriter(species,
					"TermsAlwaysPresent.txt");
			BufferedWriter bterms = Species.getWriter(species, "FullTerms.txt");

			goterms = readAllTerms(species, assoc, log);
			for (String key : goterms.keySet()) {
				Integer val = goterms.get(key);
				if (val >= numOfEd || val>=assoc.length) {
					bwalways.write(key + "\t" + val + "\n");
				}
				bterms.write(key + "\t" + val + "\n");
			}
			// close writers
			bwalways.close();
			bterms.close();
			log.info("Files TermsAlwaysPresent.txt and FullTerms.txt created");
		} catch (IOException e) {
			log.severe("Error getting GO Terms present: "
					+ e.getLocalizedMessage());
		} catch (RuntimeException e) {
			log.info(e.getMessage());
		}

	} // get Always present and full

	/**
	 * Given a directory, this function reads all the gene association files and
	 * extracts all the GO terms
	 * 
	 * @author asedeno
	 * @param indir
	 *            Directory to look at
	 * 
	 * */
	private static Map<String, Integer> readAllTerms(String species, File[] fs,
			Logger log) {
		Map<String, Integer> goterms = new HashMap<String, Integer>();

		// Traverse geneAssociation files
		for (int i = 0; i < fs.length; i++) {
			try {
				BufferedReader br;
				br = Species.getGzReader(fs[i]);
				String line;
				// Read line by line
				while ((line = br.readLine()) != null) {
					if (line.indexOf("!") != -1) {
						continue; // skip comments
					}
					
					String[] columnDetail = new String[11];
					columnDetail = line.split("\t");
					// Sanity check. Make sure we have read 3 columns or more
					if (columnDetail.length <= 5)
						continue;
					
					//We won't take those rows where NOT is written on the 3rd column
					if (columnDetail[3]!=null && columnDetail[3].compareTo("NOT")==0) {
            continue;
          }
					
					// Add gene to allGOTerms hash table if not already there
					if (!goterms.containsKey(columnDetail[4])) {
						goterms.put(columnDetail[4], 1);
					} else {
						if (goterms.get(columnDetail[4]) <= i)
							goterms.put(columnDetail[4],
									goterms.get(columnDetail[4]) + 1);
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				log.severe("Error reading "+fs[i].getName()+" "+e.getLocalizedMessage());
				return null;
			} catch (IOException e) {
				log.severe("Error reading "+fs[i].getName()+" "+e.getLocalizedMessage());
				return null;
			}

		}
		return goterms;
	}// read all terms

}
