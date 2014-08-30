package ubc.pavlab.gotrack.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubic.erminej.data.GeneSetTerm;
import ubic.erminej.data.GeneSetTerms;

/**
 * Get the evaluation for the parents
 * 
 * @author asedeno
 * 
 */
public class Parents {

	/**
	 * This function will get all parents for each go term in gene association
	 * file Given an gene association file we will get its parents by using
	 * matchingDatesfile to associate date to edition number
	 * 
	 * @param dir
	 *            : Where gene association files are found
	 * @param termdbdir
	 *            : Where termdb file are found
	 * @param geneAssocFile
	 *            : The file we are going to operate with
	 * @param isOldEdition
	 *            : Whether termdb file has to be parsed as an old edition
	 * @param matchingDatesfile
	 *            : File that matches edition with publication date
	 * 
	 * @return File name that contains all go parents
	 * */
	public static GeneSetTerms getParents(String species, File geneAssocFile,
			String isOldEdition, Logger logger, int editionNumber) {
		GeneSetTerms geneSetTerms = null;
		String parentsFileName = null;
		try {

			Map<String, Calendar> matchdates2edition = GenUtils.matchdates2edition;

			if (matchdates2edition == null) {
				logger.severe("A problem with edition2Dates file");
				System.exit(1);
			}
			// termdFile will be a file like
			// "go_daily-termdb.rdf-xml."+year+"-"+month+".gz";

			parentsFileName = getParentFile(
					matchdates2edition.get(geneAssocFile.getName()));

			if (parentsFileName == null
					|| parentsFileName.compareTo("null") == 0) {
				logger.severe("There is no parent file for this edition: "
						+ geneAssocFile);
				System.exit(1);
			}

			String termdbFile = getTermdbFromGeneAssoc(species,
					geneAssocFile.getName(), matchdates2edition, logger);

			if (termdbFile == null || termdbFile.compareTo("null") == 0) {
				logger.severe("There is no termdb file for this edition");
				System.exit(1);
			}

			BufferedWriter writer = Species
					.getWriterParentChildren(parentsFileName);

			// TO USE WITH EVERY FILE
			geneSetTerms = new GeneSetTerms(Species.getTermDBFolder()
					.getAbsolutePath() + "/" + termdbFile,
					Boolean.valueOf(isOldEdition));

			int j = 0;
			if (writer != null) {
				for (GeneSetTerm term : geneSetTerms.getTerms()) {
					if (term.getDefinition().startsWith("OBSOLETE"))
						continue;
					Collection<GeneSetTerm> pars = geneSetTerms
							.getAllParents(term);
					writer.write(term.getId() + "\t");

					for (GeneSetTerm par : pars) {
						if (par != null && par.getId() != null) {
							writer.write(par.getId() + "|");
						}
					}
					writer.write("\n");
					if (++j % 1000 == 0)
						System.err.print(".");
				}
				writer.close();
			}

			// ----------------------------------------------------------------
			// LEAFTERMS ------------------------------------------------------
			// -----------------------------------------------------------------

			String termdbFileStr = GenUtils.getTermdbFromGeneAssoc(species,
					geneAssocFile, logger);
			String date = extractDate(termdbFileStr);
			BufferedWriter bwufferedWriterOfsetTerms = Species
					.getWriterParentChildren("go.childcount." + date + ".txt");
			GeneSetTerms geneSetTerm = geneSetTerms;
			if (bwufferedWriterOfsetTerms != null) {
				j = 0;
				for (GeneSetTerm term : geneSetTerm.getTerms()) {
					if (term.getDefinition().startsWith("OBSOLETE"))
						continue;

					Collection<GeneSetTerm> collectionOfSetTerms = geneSetTerm
							.getAllChildren(term); // before
					// getChildren
					bwufferedWriterOfsetTerms.write(term.getId() + "\t"
							+ collectionOfSetTerms.size() + "\n");
					if (++j % 1000 == 0)
						bwufferedWriterOfsetTerms.write(".");

				}
				bwufferedWriterOfsetTerms.close();
			}		
			
		} catch (IOException e) {
			// e.printStackTrace();
			logger.severe(e.getLocalizedMessage()
					+ " >>  COULD NOT READ IO FOR FILE : " + geneAssocFile);
			Species.removeFile(species, parentsFileName);
		} catch (IllegalStateException e) {
			// e.printStackTrace();
			logger.severe(e.getLocalizedMessage()
					+ " >>>  INVALID STATE OF THE TERMDB FILE FOR: "
					+ geneAssocFile);
		} catch (Exception e) {
			logger.severe(e.getLocalizedMessage()
					+ " >>>>>> FATAL: Fatal error getting parents for "
					+ geneAssocFile);
			// e.printStackTrace();
		}

		return geneSetTerms;
	}

	/**
	 * 
	 * @param termdbFileStr
	 * @return
	 */
	private static String extractDate(String termdbFileStr) {
		return termdbFileStr.substring(termdbFileStr.indexOf("20"),
				termdbFileStr.indexOf(".gz"));
	}

	/**
	 * This function calculates the name of the termdb file associated with a
	 * gene association file using matchEditionDateFile
	 * 
	 * @param termdbdir
	 *            Where termdb files are found
	 * @param dir
	 *            Where result will be saved
	 * @param geneAssoc
	 *            gene association file
	 * @param matchEditionDateFile
	 *            File that matches an edition to its date
	 * */
	public static String getTermdbFromGeneAssoc(String species,
			String geneAssoc, Map<String, Calendar> matchdates2edition,
			Logger logger) {

		Calendar calendar = matchdates2edition.get(geneAssoc);
		if (calendar == null) {
			matchdates2edition = GenUtils.readMatchingFile(species,
					GOtrack.edition2DatesFile, logger);
			calendar = matchdates2edition.get(geneAssoc);

		}
		calendar = (Calendar) matchdates2edition.get(geneAssoc).clone();

		int year = calendar.get(Calendar.YEAR);
		if (year < 2000)
			year += 2000;
		String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
		if (month.length() == 1) {
			month = "0" + month;
		}
		String go_daily_termdb = "go_daily-termdb.rdf-xml." + year + "-"
				+ month + ".gz";

		if (!Species.termDBFile(go_daily_termdb).exists()) {
			calendar.add(Calendar.MONTH, -1);
			year = calendar.get(Calendar.YEAR);
			if (year < 2000)
				year += 2000;
			month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
			if (month.length() == 1) {
				month = "0" + month;
			}
			logger.warning("Problem. File " + go_daily_termdb
					+ " is not found. Trying " + "go_daily-termdb.rdf-xml."
					+ year + "-" + month + ".gz");
		}
		return "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";
	}

	/**
	 * Given a particular date, this function will calculate the parent's file
	 * name
	 * 
	 * @param date
	 * @return files like parents/go.parents."+year+"-"+month+".txt
	 */
	public static String getParentFileAdjust(Calendar date, String species) {
		try {
			int year = date.get(Calendar.YEAR);
			String month = String.valueOf(date.get(Calendar.MONTH) + 1);
			if (month.length() == 1)
				month = "0" + month;
			File f = new File(Species.getTmpFolder(species) + "/go.parents."
					+ year + "-" + month + ".txt");
			Calendar calendarCloned = (Calendar) date.clone();
			if (!f.exists()) {
				calendarCloned.add(Calendar.MONTH, -1);
				year = calendarCloned.get(Calendar.YEAR);
				month = String.valueOf(calendarCloned.get(Calendar.MONTH) + 1);
				if (month.length() == 1) {
					month = "0" + month;
				}
			}

			return "go.parents." + year + "-" + month + ".txt";
		} catch (Exception e) {
			return null; // "go.parents.2000-00.txt"; // if something goes wrong
		}
	}

	/**
	 * Given a particular date, this function will calculate the parent's file
	 * name
	 * 
	 * @param date
	 * @return files like parents/go.parents."+year+"-"+month+".txt
	 */
	public static String getParentFile(Calendar date) {
		try {
			int year = date.get(Calendar.YEAR);
			if (year < 2000)
				year += 2000;
			String month = String.valueOf(date.get(Calendar.MONTH) + 1);
			if (month.length() == 1)
				month = "0" + month;
			return "go.parents." + year + "-" + month + ".txt";
		} catch (Exception e) {
			return null; // "go.parents.2000-00.txt"; // if something goes wrong
		}
	}

	/**
	 * We generate a new simplified file out of the previous parent file.
	 * Simplified file will only have two columns
	 * 
	 * @param dir
	 *            Where previous file can be found
	 * @param parentFile
	 *            Previous parent file
	 * @param edition
	 *            Edition number
	 * @return New parent file name
	 * */
	public static String getSimpleParents(String species, String parentFile,
			String edition, Logger logger) {
		String newName = "/edition." + edition + "annots.simple.txt";
		try {
			if (parentFile == null) {
				throw new IOException("PARENT NULL");
			}
			BufferedWriter bufferedWriterofParents = Species.getWriter(species,
					newName);

			BufferedReader readerofParentFile = Species.getReader(species,
					parentFile);

			String line;
			while ((line = readerofParentFile.readLine()) != null) {
				String[] columnDetail = new String[11];
				columnDetail = line.split("\t");
				bufferedWriterofParents.write(columnDetail[0] + "\t"
						+ columnDetail[3] + "\n");
			}
			bufferedWriterofParents.close();
			readerofParentFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.severe(e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe(e.getLocalizedMessage());
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return newName;
	}

	/**
	 * Gets parents reading the xml directly
	 * This function is not used in gotrack but it was written for exploratory analysis
	 * @param termdbFile
	 * @return Map of a go term and its parents using directly the termdb
	 */
	public static Map<String, HashSet<String>> getParentsFromXML(
			String termdbFile) {
		HashMap<String, HashSet<String>> parents = new HashMap<String, HashSet<String>>();
		try {
			BufferedReader readerofParentFile = new BufferedReader(
					new InputStreamReader(new FileInputStream(Species
							.getTermDBFolder().getAbsolutePath()
							+ "/"
							+ termdbFile)));
			String line = "";
			while ((line = readerofParentFile.readLine()) != null) {
				if (line.contains("<go:term")) {

				}
			}
			readerofParentFile.close();
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}

		return parents;
	}

}
