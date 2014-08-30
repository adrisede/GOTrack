package ubc.pavlab.gotrack.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.CountBean;
import ubc.pavlab.gotrack.utils.GenUtils;

/**
 * Class that lets you count how many go terms each gene has
 * 
 * @author asedeno
 * */
public class CountGO {

	/**
	 * To get the maximum number of Editions
	 * 
	 * @param matchEditionChildren
	 * @return The maximum edition number
	 */
	private static Integer getMaxNumOfEditions(
			Map<String, String> matchEditionChildren) {
		try {
			Integer max = 1;
			for (String key : matchEditionChildren.keySet()) {
				if (key.equals("")) {
					continue;
				}
				Integer tmp = Integer.valueOf(GenUtils.getEditionNo(key));
				if (tmp > max)
					max = tmp;
			}
			return max;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Creates the file countGOtermsPerGeneOverTime.txt that contains a list of genes and 
	 * the number of goterms annotated to each gene in each edition
	 * @param species
	 * @param genesFile A file that contains all the genes that are considered
	 * @param datesFile File that maps the goa file to a date
	 * @param logger Logger to write messages
	 * @param countLeaves If true it counts the number of leaves, otherwise count the parents
	 */
	public static void countGoTerms(String species, String genesFile,
			String datesFile, Logger logger, boolean countLeaves) {
		try {

			// For each association file in datesFile we will find the best
			// go.childcount file: The one closest to its correspondent date

			Map<String, String> matchEditionChildren = matchEditionChildrenFile(
					species, datesFile, logger);

			Integer maxNumEds = getMaxNumOfEditions(matchEditionChildren);
			Integer[] grandTotal = new Integer[maxNumEds + 1];
			// matchEditionChildrenFile(geneAssocDir, tmpDir, datesFile);

			// Initialize a map that contains all genes and a counter
			Map<String, Integer[]> counts = initializeGenes(species, genesFile);
			// initializeGenes(tmpDir, genesFile);
			Map<String, Integer> maxPerGene = initializeMaxGenes(species,
					genesFile);
			// iterate over editions
			for (String edition : matchEditionChildren.keySet()) {
				if (edition.matches(".*xrefs.*"))
					continue;

				// find which genes are leaves for this edition
				if (matchEditionChildren.get(edition) == null)
					continue;
				Map<String, Integer> leaves = getLeaves(species,
						matchEditionChildren.get(edition), logger);

				try {
					BufferedReader bufferedReaderSpecies = Species.getReader(
							species, edition + ".parents");
					String line;
					// iterate over gene association file .parents
					while ((line = bufferedReaderSpecies.readLine()) != null) {
						int numgo = 0;
						String[] columnDetail = new String[11];
						columnDetail = line.split("\t");
						String geneid = columnDetail[0];
						// String goid = columnDetail[2];
						String parents = columnDetail[3];
						ArrayList<String> parentArray = parseParents(parents);
						if (countLeaves) {
							for (String parent : parentArray) {
								if (leaves.containsKey(parent))
									numgo++;
							}
						} else {
							numgo = parentArray.size();
						}

						// Update global maximum number of parents across
						// editions

						Integer maxpergeneId = maxPerGene.get(geneid);
						if (maxpergeneId != null && maxpergeneId <= numgo) {
							maxPerGene.remove(geneid);
							maxPerGene.put(geneid, numgo);
						}
						// if(!maxPerGene.containsKey(geneid))
						// maxPerGene.put(geneid, 0);

						// update counts
						Integer editionN = Integer.valueOf(GenUtils
								.getEditionNo(edition));
						if (!counts.containsKey(geneid))
							counts.put(geneid, new Integer[maxNumEds + 1]);
						else{
							if(counts.get(geneid)[editionN]==null)
								counts.get(geneid)[editionN] = numgo;
							else
								counts.get(geneid)[editionN] = numgo+counts.get(geneid)[editionN];
						}
						

						if (grandTotal[editionN] == null)
							grandTotal[editionN] = 0;
						grandTotal[editionN] += numgo;
					}
					bufferedReaderSpecies.close();

				} catch (FileNotFoundException ex) {
					logger.severe("File Not found" + ex.getMessage());
					logger.severe(ex.getMessage());
				} catch (IOException e) {
					logger.severe(e.getMessage());
				}
			}
			// System.out.println("Writing file countGOTermsPerGeneOverTime.txt");

			PriorityQueue<CountBean> priorityQueueOfBeans = new PriorityQueue<CountBean>();
			String header = "";
			try {
				BufferedWriter bufferedWriter = Species.getWriter(species,
						"countGOtermsPerGeneOverTime.txt");

				header = "Gene\t";
				for (int i = 1; i <= maxNumEds; i++) {
					header += (i + "\t");
				}
				bufferedWriter.write(header + "\n");

				for (String geneid : maxPerGene.keySet()) {

					int priority = maxPerGene.get(geneid);
					String line = "";
					line += (geneid + "[" + priority + "]" + "\t");
					for (int i = 1; i <= maxNumEds; i++) {
						if (counts.containsKey(geneid)) {
							Integer[] tmp = counts.get(geneid);
							if (tmp[i] != null && tmp[i] > 0)
								line += (String.valueOf(counts.get(geneid)[i]) + "\t");
							else
								line += ("\t");
						}
					}
					bufferedWriter.write(line + "\n");
					priorityQueueOfBeans.add(new CountBean(priority, line));
				}
				bufferedWriter.close();
			} catch (IOException ex) {
				logger.severe(ex.getMessage());
				ex.printStackTrace();
			}

			// ordered file
			try {

				BufferedWriter bufferedWriterCountGosw = Species.getWriter(
						species, "countGOtermsPerGeneOverTimeOrdered.txt");

				bufferedWriterCountGosw.write(header + "\n");
				while (!priorityQueueOfBeans.isEmpty()) {
					bufferedWriterCountGosw
							.write(priorityQueueOfBeans.poll().data + "\n");
				}

				bufferedWriterCountGosw.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			// generate count ordered via the recently generated file
		} catch (Exception exception) {
			logger.severe(exception.getMessage());
			exception.printStackTrace();
		}
	}

	/**
	 * Given a string of goterms separeted by a pipe,
	 * this function gets each goterm and adds it into an arraylist
	 * 
	 * @param parents String of goterms separated by |
	 * @return ArrayList of the elements
	 */
	private static ArrayList<String> parseParents(String parents) {
		ArrayList<String> arrayList = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(parents, "|");
		while (tokenizer.hasMoreElements()) {
			arrayList.add((String) tokenizer.nextElement());
		}
		return arrayList;
	}

	/**
	 * 
	 * @param dir
	 * @param file
	 * @return
	 */
	private static Map<String, Integer> getLeaves(String species, String file,
			Logger log) {
		Map<String, Integer> hashMapOfLeaves = new HashMap<String, Integer>();
		try {
			BufferedReader readerOfLeaves = Species.getParentChildrenReader(file);
			String line;
			while ((line = readerOfLeaves.readLine()) != null) {
				String[] columnDetail = new String[11];
				columnDetail = line.split("\t");
				if (columnDetail[1].compareTo("0") == 0)
					hashMapOfLeaves.put(columnDetail[0], 0);
			}
			readerOfLeaves.close();
		} catch (FileNotFoundException e) {
			log.severe(e.toString() + "for species: " + species);
		} catch (IOException e) {
			log.severe(e.toString() + "for species: " + species);
		}
		return hashMapOfLeaves;
	}

	/**
	 * This function creates a map to match edition with its closest children
	 * file in time
	 * 
	 * @param geneAssocDir
	 *            Directory where gene association files are found
	 * @param tmpDir
	 *            Temporary directory where edition2Dates.txt file is found
	 * 
	 * */
	private static Map<String, String> matchEditionChildrenFile(String species,
			String edition2DatesFile, Logger logger) {
		Map<String, String> matchEditionChildrenFile = new HashMap<String, String>();
		Map<String, Calendar> edition2dates = GenUtils.matchdates2edition;

		try {
			BufferedReader readerOfChildrenFile = Species.getReader(species,
					edition2DatesFile);
			String line;
			String geneAssocFile = "";
			// Read edition2Dates file to get gene association file one by one
			while ((line = readerOfChildrenFile.readLine()) != null) {
				String[] columnDetail = new String[11];
				columnDetail = line.split("\t");
				if (columnDetail.length >= 2) {
					geneAssocFile = columnDetail[0];
					Calendar date = edition2dates.get(geneAssocFile);
					int month = date.get(Calendar.MONTH) + 1;

					String goChildrenFile = "go.childcount."
							+ date.get(Calendar.YEAR) + "-" + month + ".txt";

					if (month < 10)
						goChildrenFile = "go.childcount."
								+ date.get(Calendar.YEAR) + "-0" + month
								+ ".txt";

					while (!Species.newFile(species, goChildrenFile).exists()
							&& date.get(Calendar.YEAR) >= 2001) {
						// look for previous file in time
						date = edition2dates.get(geneAssocFile);
						date.add(Calendar.MONTH, -1);
						month = date.get(Calendar.MONTH) + 1;
						if (month >= 10)
							goChildrenFile = "go.childcount."
									+ date.get(Calendar.YEAR) + "-" + month
									+ ".txt";
						else
							goChildrenFile = "go.childcount."
									+ date.get(Calendar.YEAR) + "-0" + month
									+ ".txt";
					}
					if (date.get(Calendar.YEAR) <= 1999) {
						matchEditionChildrenFile.put(geneAssocFile, null);
					} else
						matchEditionChildrenFile.put(geneAssocFile,
								goChildrenFile);
				}
			}

			readerOfChildrenFile.close(); // avoid memory leak

		} catch (FileNotFoundException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}

		return matchEditionChildrenFile;

	}

	/**
	 * This function will read all genes and initialize a Map
	 * 
	 * @param species The species to compute
	 * @param allGenesFile file of genes to consider
	 * @return Map of genes to an arraylist of integers
	 */
	private static Map<String, Integer[]> initializeGenes(String species,
			String allGenesFile) {
		BufferedReader bufferedReaderOfGenes = null;
		try {
			Map<String, Integer[]> genes = new HashMap<String, Integer[]>();
			bufferedReaderOfGenes = Species.getReader(species, allGenesFile);

			String line;
			while ((line = bufferedReaderOfGenes.readLine()) != null) {
				Integer[] obj = new Integer[500];
				for (int i = 0; i < 500; i++)
					obj[i] = 0;
				genes.put(line, obj);
			}

			return genes;
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			try {
				bufferedReaderOfGenes.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Given a list of genes it will return a HashMap that associate
	 * a gene to an integer, in this case the integer is zero
	 * 
	 * @param species
	 * @param allGenesFile
	 * @return HashMap of genes that point to a zero
	 */
	private static Map<String, Integer> initializeMaxGenes(String species,
			String allGenesFile) {
		BufferedReader readerOfGenesFiles = null;
		try {
			Map<String, Integer> genes = new HashMap<String, Integer>();
			readerOfGenesFiles = Species.getReader(species, allGenesFile);

			String line;
			while ((line = readerOfGenesFiles.readLine()) != null) {
				genes.put(line, 0);
			}

			return genes;
		} catch (FileNotFoundException ex) {

			ex.printStackTrace();
		} catch (IOException ex) {

			ex.printStackTrace();
		} finally {
			try {
				readerOfGenesFiles.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
