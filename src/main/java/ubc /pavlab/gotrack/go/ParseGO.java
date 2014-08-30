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

/**
 * This class contains functions to Associate a gene id with 
 * the parent go terms for a given edition.
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
	public  String getParents(File assoc, String isLastEdition,
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
	private  String parseFile(File associationArrayOfFiles,
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
								.mapUniprotYeast(uniprotid);
						if (uniprotid == null) {
							continue;							
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
	 * for yeast. This is needed because the dictionary try to find an element by
	 * comparing 2 strings. If the strings are slightly different (fewer number of zeroes)
	 * the match fails
	 * @param uniprotid
	 *            Gene id
	 * @return Fixed id
	 */
	public static String removeZeros(String uniprotid) {
		// uniprot has less zeros, S0007287
		String originalString = uniprotid;
		try{
			  uniprotid = uniprotid.replaceAll("\\D+", "");
			 Integer tmpstr = Integer.valueOf(uniprotid);
			 uniprotid = originalString.charAt(0)+""+tmpstr;			 
			}catch(Exception e){
					
			}		
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

	/*---------------------------------------------------------------------------------------------------*/
	
	/**
	 * This inner class implements a container that matches the Uniprot id with its
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

		/**
		 * 
		 * @return
		 */
		public String getUniprotid() {
			return uniprotid;
		}

		/**
		 * 
		 * @param uniprotid
		 */
		public void setUniprotid(String uniprotid) {
			this.uniprotid = uniprotid;
		}

		/**
		 * 
		 * @return
		 */
		public String getGOTerm() {
			return GOTerm;
		}

		/**
		 * 
		 * @param gOTerm
		 */
		public void setGOTerm(String gOTerm) {
			GOTerm = gOTerm;
		}

		/**
		 * 
		 * @return
		 */
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

}
