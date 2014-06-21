package ubc.pavlab.gotrack.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubc.pavlab.gotrack.utils.GeneAssoc;

/**
 * 
 * @author asedeno
 * 
 */
public class ParseGO {

	/**
	 * This function associates a gene id with the parent go terms for a given
	 * edition.
	 * 
	 * @param tmpdir
	 *            Where go term parent file is found
	 * @param direc
	 *            Where gene association files are found
	 * @param inputfile
	 *            Gene association file
	 * @param matchingDatesFile
	 *            File that matches edition with publication date
	 * @return parents file name
	 **/
	public static String getParents(File assoc, String isLastEdition,
			int editionNumber, String species, Logger logger) {

		Map<String, Calendar> matchdates2edition = GenUtils.matchdates2edition;
		String name = null;

		name = parseFile(assoc, matchdates2edition.get(assoc.getName()),
				(editionNumber + ""), Boolean.valueOf(isLastEdition),
				editionNumber, species, logger);
		return name;

	}

	/**
	 * Reads association file to match gene Uniprot id with its GO parents
	 * 
	 * @param tmpdir
	 *            Where go term parent file is found
	 * @param dir
	 *            Where gene association file is found
	 * @param filename
	 *            Gene association file
	 * @param date
	 * */
	private static String parseFile(File associationArrayOfFiles,
			Calendar date, String edition, Boolean isLastEdition,
			int editionNumber, String species, Logger logger) {
		// Given a date, parents will be a file like go.parents.2003-11.txt
		String parent = Parents.getParentFile(date);
		try {

			BufferedWriter writer = Species.getWriter(species,
					associationArrayOfFiles.getName() + ".parents");
			// Parents
			BufferedReader readerParent = Species.getParentChildrenReader(parent);
			Map<String, String> dicparents = new HashMap<String, String>();
			String line;
			while ((line = readerParent.readLine()) != null) {
				String copy = line;
				int length1 = copy.split("\t").length;
				String[] columnDetail = new String[length1];
				columnDetail = line.split("\t");

				if (columnDetail.length >= 2) { // First column is a go term,
					// second is its parents
					dicparents.put(columnDetail[0], columnDetail[1]); // concatenate
				}
				if (columnDetail.length == 1) { // First column is a go term,
					// second is its parents
					dicparents.put(columnDetail[0], ""); // concatenate
				}
			}

			// Gene Association filename
			GZIPInputStream gzip3OfAssocFile = new GZIPInputStream(
					new FileInputStream(
							associationArrayOfFiles.getAbsolutePath()));
			readerParent = new BufferedReader(new InputStreamReader(
					gzip3OfAssocFile));

			Map<String, ArrayList<GeneAssoc>> dicgeneassociationFile = new HashMap<String, ArrayList<GeneAssoc>>();
			String uniprotid = "";
			String goterm = "";
			String evidence = "";
			String symbol = "";
			// first line not valid
			String[] columnDetail = new String[15];
			while ((line = readerParent.readLine()) != null) {
				if (line.indexOf("!") != -1) {
					line = readerParent.readLine(); // skip file format
				}
				columnDetail = line.split("\t");

				if (columnDetail.length >= 7) {
					// We won't take those rows where NOT is written on the 3rd
					// column
					if (columnDetail[3] != null
							&& columnDetail[3].compareTo("NOT") == 0) {
						continue;
					}
					symbol = columnDetail[2];

					uniprotid = columnDetail[1];
					// YEAST OLD EDITION MAPPING // 105 is the breakpoint for

					if (species.equalsIgnoreCase("yeast")
							&& editionNumber <= 24) {

						/*
						 * The mapping for yeast will add the needed zeroes to
						 * old editions this check is not needed anymore
						 */
						if (columnDetail[0].equals("SGD")) {
							// uniprot has less zeros, S0007287
							// uniprotid = addZeros(uniprotid);
						}
					}
					if (species.equalsIgnoreCase("yeast")
							&& editionNumber <= 105) {
						uniprotid = GenUtils
								.mapUniprotYeast(species, uniprotid);
						if (uniprotid == null) {
							continue;
							// uniprotid = oldUniprot;
						}
					}

					goterm = columnDetail[4]; // 3 before 4
					evidence = columnDetail[6]; // 5 before 6
					// for a given Uniprot ID, we save the parents associated to
					// its GO Term for this edition
					if (!dicgeneassociationFile.containsKey(uniprotid)) {
						ArrayList<GeneAssoc> t = new ArrayList<GeneAssoc>();
						t.add(new GeneAssoc(symbol, uniprotid, goterm,
								dicparents.get(goterm), evidence));
						dicgeneassociationFile.put(uniprotid, t);
					} else {
						ArrayList<GeneAssoc> t = dicgeneassociationFile
								.remove(uniprotid);
						t.add(new GeneAssoc(symbol, uniprotid, goterm,
								dicparents.get(goterm), evidence));
						dicgeneassociationFile.put(uniprotid, t);
					}

				}
			}
			readerParent.close();
			return printGeneAssocParents(writer, species,
					dicgeneassociationFile, associationArrayOfFiles.getName()
							+ ".parents", edition, isLastEdition, logger);
		} catch (IOException e) {
			e.printStackTrace();
			return associationArrayOfFiles.getName() + ".parents";
		} catch (RuntimeException e) {
			if (e.getMessage().toLowerCase().indexOf("skipped") != -1) {
				// just file skipped.
				return associationArrayOfFiles.getName() + ".parents";
			}
		}
		return associationArrayOfFiles.getName() + ".parents";
	}

	/**
	 * This function add trailing zeroes to the gene id for old editions (<25)
	 * for yeast
	 * @param uniprotid
	 *            Gene id
	 * @return Fixed id
	 */
	public static String addZeros(String uniprotid) {
		// uniprot has less zeros, S0007287
		uniprotid = "S00" + uniprotid.substring(1);
		return uniprotid;
	}

	/**
	 * This function stores matching results in disk
	 * 
	 * @param dir
	 *            Where file will be saved
	 * @param dictionary
	 *            Dictionary that associate the Uniprot id with go parents
	 * @param outputFile
	 *            Name of the output file to be created
	 * 
	 **/
	private static String printGeneAssocParents(
			BufferedWriter writerAssocParents, String species,
			Map<String, ArrayList<GeneAssoc>> dictionary, String outputFile,
			String edition, Boolean isLastEdition, Logger logger) {
		try {
			if (dictionary != null && !dictionary.isEmpty()) {
				for (String key : dictionary.keySet()) {
					for (GeneAssoc row : dictionary.get(key)) {						
						if (row.getGoParents() == null)
							continue;

						if (!isLastEdition)
							writerAssocParents.write(key + "\t"
									+ row.getSymbol() + "\t"
									+ row.getGOTerm() + "\t"
									+ row.getGoParents() + "\t"
									+ edition + "\n");
						else
							writerAssocParents.write(key + "\t"
									+ row.getSymbol() + "\t"
									+ row.getGOTerm() + "\t"
									+ row.getGoParents() + "\t"
									+ edition + "\n");
					}
				}
			}
			writerAssocParents.close();
			return outputFile;
		} catch (IOException e) {
			e.printStackTrace();
			logger.severe(e.getLocalizedMessage());
			return outputFile;
		}
	}	

}
