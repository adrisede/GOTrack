package ubc.pavlab.gotrack.pmid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubc.pavlab.gotrack.tools.Sys;
import ubc.pavlab.gotrack.utils.HTTPWrapper;

/**
 * This class models the pubmed analysis
 * @author asedeno
 * 
 * Dictionary sources:
 * 
 * http://mbr.nlm.nih.gov/Download/MUIDtoPMID/index.shtml
 * ftp://ftp.ncbi.nlm.nih.gov/pubmed
 *
 */
public class PmidAnalysis {

	/*Auxiliary structure to compute speciesUniprotPmid*/
	private Map<String, Integer[]> speciesUniprotPmid = new HashMap<String, Integer[]>();
	/*Auxiliary structure to compute the evidence code analysis*/
	private Map<String, String[]> speciesUniprotPmidEvidenceCode = new HashMap<String, String[]>();

	/**
	 * Write the evidence code analysis to a file
	 * The output file evidenceCodeHistory_*.txt each row store:
	 * Column 1: uniprot id
	 * Column 2: goterm
	 * Column 3: pubmed id
	 * it has n columns one for each edition, the entry of this matrix is the evidence
	 * code
	 * @param species           Species to compute 
	 * @param numberOfEditions  Max number of exisiting editions
	 * @param missingEditions   Editions that are not existent (if any)
	 * @param outputFile        Where the analysis will be writen
	 */
	public void writeAnalysisEvidenceCode(String species,
			Integer numberOfEditions, Integer[] missingEditions,
			String outputFile) {
		try {
			BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
					outputFile + species + ".txt");
			bufferedWriterOfSyns.write("Uniprot\tGoid\tPMID\t");
			for (int i = 1; i < numberOfEditions; i++) {
				bufferedWriterOfSyns.write(i + "\t");
			}
			bufferedWriterOfSyns.write("\n");
			for (String key : speciesUniprotPmidEvidenceCode.keySet()) {
				String keyWithTabs = key.replace("|", "\t");
				bufferedWriterOfSyns.write(keyWithTabs);
				String[] array = speciesUniprotPmidEvidenceCode.get(key);
				for (int i = 0; i < numberOfEditions; i++) {
					if (i == 0)
						continue;

					if (missingEditions[i] == -1) {
						bufferedWriterOfSyns.write("\t");
						continue;
					}
					if (array[i] == null) {
						bufferedWriterOfSyns.write("\t");
					} else {
						bufferedWriterOfSyns.write("\t" + array[i]);
					}
				}
				bufferedWriterOfSyns.write("\n");
			}
			bufferedWriterOfSyns.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function will read file pubmedDatesMapping.txt inside the tmp folder
	 * for a given species then it will build a dictionary of pubmed id -> date
	 * that will be used to prevent the code from looking up the web page.
	 * 
	 * @param species
	 * @return Dictionary (pubmed id, date)
	 */
	public Map<String, String> readPubmedDateDictionary(String species) {
		Map<String, String> dictionary = new HashMap<String, String>();
		try {
			BufferedReader readerOfdic = Species.getReader(species,
					"pubmedDatesMapping.txt");
			String line;
			String key;
			String pid;
			String date;
			while ((line = readerOfdic.readLine()) != null) {
				String[] columnDetail = new String[500];
				columnDetail = line.split("\t");
				key = columnDetail[0];
				if (!key.contains(":"))
					continue;
				pid = key.substring(0, key.indexOf(":"));
				date = key.substring(key.indexOf(":") + 1, key.length());
				dictionary.put(pid, date);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dictionary;
	}

	/**
	 * Write the analysis to the output file
	 * pubmedHistory_*.txt
	 * That file contains the following columns:
	 * Column 1: uniprot Id
	 * Column 2: pudbmed Id
	 * Column 3: publication date
	 * Column 4: publication year
	 * n columns one for each edition, each entry is 0 or 1
	 * 1 if the combination of uniprot/pubmed is in the edition, 0 otherwise
	 *  
	 * @param species
	 *            The species we are analyzing
	 * @param numberOfEditions
	 *            Max number of editions
	 * @param missingEditions
	 *            An array that keep track which editions are missing
	 * 
	 * */
	public void writeAnalysisPubmed(String species, Integer numberOfEditions,
			Integer[] missingEditions, String outputFile,
			Map<String, String> pubmedDateDic) {
		try {
			BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
					outputFile + species + ".txt");
			if (outputFile.compareTo("pubmedHistory_") == 0)
				bufferedWriterOfSyns.write("Uniprot\tPMID\tDate\tYear\t");
			for (int i = 1; i < numberOfEditions; i++) {
				bufferedWriterOfSyns.write(i + "\t");
			}
			bufferedWriterOfSyns.write("\n");
			String date;
			String year = "";
			String[] columnDetail = new String[200];
			for (String key : speciesUniprotPmid.keySet()) {
				String keyWithTabs = key.replace("|", "\t");
				bufferedWriterOfSyns.write(keyWithTabs);
				columnDetail = keyWithTabs.split("\t");
				if (columnDetail.length == 2) {
					if (pubmedDateDic.containsKey(columnDetail[1])) {
						date = pubmedDateDic.get(columnDetail[1]);
					} else {
						date = HTTPWrapper
								.getPubmedDateFromWeb(columnDetail[1]);
						if (date != null && !date.contains("null"))
							pubmedDateDic.put(columnDetail[1], date);
					}
					if (date != null && !date.contains("null")) {
						year = date.substring(date.lastIndexOf("/") + 1,
								date.length());
						bufferedWriterOfSyns.write("\t" + date + "\t" + year);
					} else
						bufferedWriterOfSyns.write("\t\t");

				} else
					bufferedWriterOfSyns.write("\t\t");

				Integer[] array = speciesUniprotPmid.get(key);
				for (int i = 0; i < numberOfEditions; i++) {
					if (i == 0)
						continue;

					if (missingEditions[i] == -1) {
						bufferedWriterOfSyns.write("\t");
						continue;
					}
					if (array[i] == null) {
						bufferedWriterOfSyns.write("\t0");
					} else
						bufferedWriterOfSyns.write("\t" + array[i]);
				}
				bufferedWriterOfSyns.write("\n");
			}
			bufferedWriterOfSyns.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function will try to get the pubmed date from the web page but since
	 * that is incredibly expensive, we have a set of pre-computed files per
	 * species that might already have the date, if we can't find it there, then
	 * we will look up the web page
	 * 
	 * @param numberOfEditions
	 *            Total number of editions
	 * @param geneAssociationFiles
	 *            Array of the gene association files
	 */
	public void analyzeSpeciesPubmedHistory(Integer numberOfEditions,
			File[] geneAssociationFiles) {

		speciesUniprotPmid.clear();
		for (File geneAssoc : geneAssociationFiles) {			
			try {
				BufferedReader bufferedReaderOfAssoc = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(geneAssoc))));
				String temporaryString;
				while ((temporaryString = bufferedReaderOfAssoc.readLine()) != null) {
					String[] columnDetail = new String[200];
					columnDetail = temporaryString.split("\t");
					if (columnDetail.length < 2)
						continue; // This row might be the header so skip it
					if (columnDetail.length < 5)
						continue;
					String uniprot = columnDetail[1];
					String pmid = columnDetail[5];
					if (!pmid.contains("PMID") && !pmid.contains("PUBMED"))
						pmid = "\\N|";
					pmid = pmid.replaceAll("PMID", "");
					pmid = pmid.replaceAll("PUBMED", "");
					pmid = pmid.replaceAll(":", "");
					Integer[] allEditionVector;
					StringTokenizer tok = new StringTokenizer(pmid, "|");
					while (tok.hasMoreElements()) {
						pmid = tok.nextToken();
						if (pmid != null) {
							try {
								Integer.valueOf(pmid);
							} catch (NumberFormatException e) {
								continue;
							}
							if(pmid.contains("FB"))
								continue;
						}
						if (!speciesUniprotPmid.containsKey(uniprot + "|"
								+ pmid)) {
							allEditionVector = new Integer[numberOfEditions];
							Arrays.fill(allEditionVector, null);

						} else {
							allEditionVector = speciesUniprotPmid
									.remove(uniprot + "|" + pmid);
						}
						Integer editionNumber = Integer.valueOf(GenUtils
								.getEditionNo(geneAssoc.getName()));
						allEditionVector[editionNumber] = 1;
						speciesUniprotPmid.put(uniprot + "|" + pmid,
								allEditionVector);
					}
				}
				bufferedReaderOfAssoc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}// end for gene association file
	}

	/**
	 * Run evidence code analysis. The function will fill dictionary
	 * speciesUniprotPmidEvidenceCode that contains a key made of the
	 * concatenation of uniprot + "|" + goid + "|" + pmid and the object is an
	 * array of the size numberOfEditions.
	 * 
	 * @param numberOfEditions
	 * @param geneAssociationFiles
	 */
	public void analyzeSpeciesEvidenceCodeHistory(Integer numberOfEditions,
			File[] geneAssociationFiles) {

		speciesUniprotPmidEvidenceCode.clear();
		for (File geneAssoc : geneAssociationFiles) {
			try {
				BufferedReader bufferedReaderOfAssoc = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(geneAssoc))));
				String temporaryString;
				while ((temporaryString = bufferedReaderOfAssoc.readLine()) != null) {
					String[] columnDetail = new String[200];
					columnDetail = temporaryString.split("\t");
					if (columnDetail.length < 7)
						continue; // This row might be the header so skip it
					String uniprot = columnDetail[1];
					String goid = columnDetail[4];
					String pmid = columnDetail[5];
					if (!pmid.contains("PMID") && !pmid.contains("PUBMED"))
						pmid = "";
					pmid = pmid.replaceAll("PMID", "");
					pmid = pmid.replaceAll("PUBMED", "");
					pmid = pmid.replaceAll(":", "");
					String evidenceCode = columnDetail[6];
					String[] allEditionVector;
					StringTokenizer tok = new StringTokenizer(pmid, "|");
					while (tok.hasMoreElements()) {
						pmid = tok.nextToken();
						if (pmid != null) {
							try {
								Integer.valueOf(pmid);
							} catch (NumberFormatException e) {
								continue;
							}
							if(pmid.contains("FB")||pmid.contains("SGD_REF"))
								continue;
						}
						if (!speciesUniprotPmidEvidenceCode.containsKey(uniprot
								+ "|" + goid + "|" + pmid)) {
							allEditionVector = new String[numberOfEditions];
							Arrays.fill(allEditionVector, null);

						} else {
							allEditionVector = speciesUniprotPmidEvidenceCode
									.remove(uniprot + "|" + goid + "|" + pmid);
						}
						Integer editionNumber = Integer.valueOf(GenUtils
								.getEditionNo(geneAssoc.getName()));
						allEditionVector[editionNumber] = evidenceCode;
						speciesUniprotPmidEvidenceCode.put(uniprot + "|" + goid
								+ "|" + pmid, allEditionVector);
					}
				}
				bufferedReaderOfAssoc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}// end for gene associ file
	}

	/**
	 * This will write the number of annotations per evidence code in each edition
	 * (this function was written for exploratory analysis and is not used by gotrack)
	 * 
	 * @param numberOfEditions
	 * @param species
	 */
	public void writeEvidencePercentage(Integer numberOfEditions, String species) {
		BufferedWriter bufferedWriterOfSyns;
		try {
			bufferedWriterOfSyns = Species.getWriter(species,
					"evidenceCodesPercentage_" + species + ".txt");
			Integer iss = 0, exp = 1;
			Integer tas = 2, nas = 3, ic = 4, nd = 5, iea = 6;
			Integer[][] editions = new Integer[numberOfEditions + 1][7];
			for (Integer[] row : editions)
				Arrays.fill(row, 0);
			// Arrays.fill(editions, 0);
			for (String key : speciesUniprotPmidEvidenceCode.keySet()) {
				String[] evArray = speciesUniprotPmidEvidenceCode.get(key);
				for (int i = 0; i < evArray.length; i++) {
					if (evArray[i] == null)
						continue;
					// ISS,ISO,ISA,ISM,IGC,IBA,IBD,IKR,IRD,RCA -> iss
					if (evArray[i].compareToIgnoreCase("ISS") == 0
							|| evArray[i].compareToIgnoreCase("ISO") == 0
							|| evArray[i].compareToIgnoreCase("ISA") == 0
							|| evArray[i].compareToIgnoreCase("ISM") == 0
							|| evArray[i].compareToIgnoreCase("IGC") == 0
							|| evArray[i].compareToIgnoreCase("IBA") == 0
							|| evArray[i].compareToIgnoreCase("IBD") == 0
							|| evArray[i].compareToIgnoreCase("IKR") == 0
							|| evArray[i].compareToIgnoreCase("IRD") == 0
							|| evArray[i].compareToIgnoreCase("RCA") == 0) {
						editions[i][iss]++;
					}
					if (evArray[i].compareToIgnoreCase("TAS") == 0)
						editions[i][tas]++;
					if (evArray[i].compareToIgnoreCase("NAS") == 0)
						editions[i][nas]++;
					if (evArray[i].compareToIgnoreCase("IC") == 0)
						editions[i][ic]++;
					if (evArray[i].compareToIgnoreCase("ND") == 0)
						editions[i][nd]++;
					if (evArray[i].compareToIgnoreCase("IEA") == 0)
						editions[i][iea]++;
					if (evArray[i].compareToIgnoreCase("EXP") == 0)
						editions[i][exp]++;
				}
			}

			bufferedWriterOfSyns
					.write("Edition\tISS\tTAS\tNAS\tIC\tND\tIEA\tEXP\n");

			for (int j = 1; j < numberOfEditions; j++) {
				bufferedWriterOfSyns.write(j + "\t");
				bufferedWriterOfSyns.write(editions[j][iss] + "\t");
				bufferedWriterOfSyns.write(editions[j][tas] + "\t");
				bufferedWriterOfSyns.write(editions[j][nas] + "\t");
				bufferedWriterOfSyns.write(editions[j][ic] + "\t");
				bufferedWriterOfSyns.write(editions[j][nd] + "\t");
				bufferedWriterOfSyns.write(editions[j][iea] + "\t");
				bufferedWriterOfSyns.write(editions[j][exp] + "\t");
				bufferedWriterOfSyns.write("\n");
			}
			bufferedWriterOfSyns.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method to analyze a particular species. This function might be called
	 * from main directly
	 * It is not used by gotrack
	 * 
	 * @param species Species to compute
	 */
	public void analyzeSpecies(String species) {
		ArrayList<File> geneAssocFiles = GenUtils.readDirectory(Species
				.getDataFolder(species).getAbsolutePath() + "/",
				"gene_association*gz");
		File[] geneAssocs = new File[geneAssocFiles.size()];
		for (int i = 0; i < geneAssocFiles.size(); i++)
			geneAssocs[i] = geneAssocFiles.get(i);
		System.out.println("Analyzing " + species);
		Integer numberOfEditions = GenUtils
				.getTotalNumberOfEditions(geneAssocFiles) + 1;
		// analyzeSpeciesPubmedHistory(numberOfEditions,geneAssocs);
		analyzeSpeciesEvidenceCodeHistory(numberOfEditions, geneAssocs);
		writeEvidencePercentage(numberOfEditions, species);
	}

	/**
	 * This code will run this analysis for all species
	 * This function is written for exploratory analysis and is not called bu gotrack
	 */
	public void analyzeAllSpecies() {
	
		LinkedList<String> listOfAllSpecies = Species.getSpecies();
		for (String species : listOfAllSpecies) {
			ArrayList<File> geneAssocFiles = GenUtils.readDirectory(Species
					.getDataFolder(species).getAbsolutePath() + "/",
					"gene_association*gz");
			File[] geneAssocs = new File[geneAssocFiles.size()];
			for (int i = 0; i < geneAssocFiles.size(); i++)
				geneAssocs[i] = geneAssocFiles.get(i);
			System.out.println("Analyzing " + species);
			Integer numberOfEditions = GenUtils
					.getTotalNumberOfEditions(geneAssocFiles) + 1;
			// analyzeSpeciesPubmedHistory(numberOfEditions,geneAssocs);
			analyzeSpeciesEvidenceCodeHistory(numberOfEditions, geneAssocs);
			writeEvidencePercentage(numberOfEditions, species);
			// writeAnalysisPubmed(species,
			// numberOfEditions,GenUtils.missingEditions(geneAssocs),"pubmedhistory_");
		}
	}

	/*-------------------------------------------------------*/

	/**
	 * This function will read files MuId-PmId-*.ids.gz and build a dictionary
	 * that will be used to map the pubmed ids
	 * 
	 * @param log   Logger to record the progress of the function
	 * @return The dictionary in a Hash table
	 */
	private static Map<String, String> loadPubmedDictionary(Logger log) {
		Map<String, String> dictionary = new HashMap<String, String>();
		ArrayList<File> dicFiles = new ArrayList<File>();
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-1.ids.gz"));
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-2.ids.gz"));
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-3.ids.gz"));
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-4.ids.gz"));
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-5.ids.gz"));
		dicFiles.add(new File("data/pubmedmaps/MuId-PmId-6.ids.gz"));

		String[] columnDetail = new String[100];

		for (File mapfile : dicFiles) {
			log.info("Loading dictionary file " + mapfile.getName());
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(mapfile))));
				String tmp;
				// Read line by line
				while ((tmp = reader.readLine()) != null) {
					Arrays.fill(columnDetail, "");
					columnDetail = tmp.split("\t");
					if (columnDetail.length >= 2) {
						dictionary.put(columnDetail[0], columnDetail[1]);
					}
				}
				reader.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return dictionary;
	}

	/**
	 * This function will map pubmed ids inside a gene association file
	 * 
	 * @param file
	 *            Gene association file
	 * @param species
	 *            Species to compute
	 * @param logger
	 * @param dic
	 *            Dictionary using files MuId-PmId-*.ids.gz
	 * @param replacedPubmeds
	 *            BufferedWriter that will keep track of which maps were done
	 */
	public static void pubmedMappingForFile(File file, String species,
			Logger logger, Map<String, String> dic,
			BufferedWriter replacedPubmeds) {

		try {
			String editionNumber = GenUtils.getEditionNo(file.getName());
			BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
					file.getName() + ".syn");
			BufferedReader bufferedReaderOfAssoc = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(file))));
			String temporaryString;
			while ((temporaryString = bufferedReaderOfAssoc.readLine()) != null) {
				String[] columnDetail = new String[200];
				columnDetail = temporaryString.split("\t");
				if (columnDetail.length < 6)
					continue; // This row might be the header so skip it
				String pubmed = columnDetail[5];
				if (pubmed.contains("PUBMED")) {
					pubmed = columnDetail[5].replaceAll("\\D+", "");
					if (dic.containsKey(pubmed)) {
						replacedPubmeds
								.write(columnDetail[5] + "\t" + dic.get(pubmed)
										+ "\t" + editionNumber + "\n");
						columnDetail[5] = "PMID:" + dic.get(pubmed);
					}
				}
				for (int i = 0; i < columnDetail.length; i++) {
					bufferedWriterOfSyns.write(columnDetail[i] + "\t");
				}
				bufferedWriterOfSyns.write("\n");
			}
			bufferedReaderOfAssoc.close();
			bufferedWriterOfSyns.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function will traverse all the gene association files for a given
	 * species and try to map the pubmed id using files MuId-PmId-*.ids.gz as
	 * dictionaries The function will generate new gene association files with
	 * the mapped pubmed id and will produce a file called replacedPubmeds.txt
	 * that will save the mappings
	 * 
	 * @param species       Species to compute
	 * @param log           Logger to record the progress
	 * @param startedtion   Start analysis from this edition
	 */
	public static void pubmedMappingAllAnnotations(String species, Logger log, Integer startedtion) {
		Map<String, String> dictionary = loadPubmedDictionary(log);
		// Get all gene_association files
		ArrayList<File> geneAssociationFiles = GenUtils.readDirectory(
				Species.getDataFolder(species).getAbsolutePath() + "/",
				"gene_association*gz");

		try {
			// file replacedPubmeds.txt will keep track of which pubmeds were
			// replaced using
			// the dictionary
			BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
					"replacedPubmeds.txt");
			for (File file : geneAssociationFiles) {
				if(startedtion>Integer.valueOf(GenUtils.getEditionNo(file.getName())))
					continue;
				log.info("Computing " + file.getName());
				pubmedMappingForFile(file, species, log, dictionary,
						bufferedWriterOfSyns);
			}
			bufferedWriterOfSyns.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Compress and move the generated .syn files
		File[] synFiles = GenUtils.readDirectory(Species.getTmpFolder(species),
				"gene_association*.gz.syn", log);

		for (File synFile : synFiles) {
			
			if(startedtion>Integer.valueOf(GenUtils.getEditionNo(synFile.getName())))
				continue;
			
			// rename the files
			String synRenamed = GenUtils.renameGZFile(synFile.getAbsolutePath());
			String command = "mv " + synFile.getAbsolutePath() + " "
					+ synRenamed;
			Sys.bash(command);

			command = "gzip " + synRenamed;
			Sys.bash(command);

			command = "mv " + synRenamed + ".gz ./data/" + species;
			Sys.bash(command);
		}
	}

}
