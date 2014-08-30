package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.tools.Sys;

/**
 * Class meant to explore the genes in the files for pre-processing
 * go-annotation
 * 
 * @author asedeno
 * 
 */
public class ExploreGenes {

	private static boolean VERBOSE = true;

	/**
	 * compare the editions
	 * 
	 * @param editions
	 * @param maxNumberOfEditions
	 * @param directory
	 * @param species
	 * @return
	 */
	public static boolean compareEditions(AssociationFile[] editions,
			Integer maxNumberOfEditions, String directory, String species) {
		BufferedWriter bufferedWriterOfEditions;
		try {
			System.out.println("Writing analysis file " + directory
					+ "Analysis" + species + ".txt");
			bufferedWriterOfEditions = new BufferedWriter(new FileWriter(
					directory + "/Analysis" + species + ".txt"));

			for (int i = 0; i < maxNumberOfEditions; i++) {
				if (editions[i] == null) {
					bufferedWriterOfEditions
							.write("Edition "
									+ (i + 1)
									+ " doesn't exist----------------------------------------------\n");
					continue;
				}
				@SuppressWarnings("rawtypes")
				Enumeration enumerationOfGenes = editions[i].getGeneTable()
						.keys();
				bufferedWriterOfEditions
						.write("Edition "
								+ (i + 1)
								+ " --------------------------------------------------------------------------------------- \n");
				while (enumerationOfGenes.hasMoreElements()) {
					String genename = (String) enumerationOfGenes.nextElement();
					bufferedWriterOfEditions.write(genename + " ");
					for (int j = 0; j < maxNumberOfEditions; j++) {
						if (editions[j] != null) {
							if (editions[j].isGeneInTable(genename))
								bufferedWriterOfEditions.write(" 1 ");
							else
								bufferedWriterOfEditions.write(" 0 ");
						} else {
							bufferedWriterOfEditions.write(" NE ");
						}
					}
					bufferedWriterOfEditions.write("\n");
				}
			}
			bufferedWriterOfEditions.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return true;

	}

	/**
	 * This function generates a matrix of presence/absence of GO symbols across
	 * editions. Rows are genes followed by columns indicating presence, absence
	 * or No edition for each edition (0, 1 , NA)
	 * 
	 * @param allGenes
	 *            It contains all genes seen in all the association files
	 * @param editions
	 *            Array of array of genes
	 */
	public static void compareEditionsShort(AssociationFile allGenes,
			AssociationFile[] editions, Integer maxNumberOfEditions,
			String directory, String species) {
		BufferedWriter bufferedWriterOfEditions;
		try {
			System.out.println("Writing analysis file " + directory
					+ "Analysis" + species + ".txt");
			bufferedWriterOfEditions = new BufferedWriter(new FileWriter(
					directory + "/Analysis" + species + ".txt"));

			Enumeration<?> enumerationOfGenes = allGenes.getGeneTable().keys();

			while (enumerationOfGenes.hasMoreElements()) {
				String genename = (String) enumerationOfGenes.nextElement();
				bufferedWriterOfEditions.write(genename + " ");
				for (int i = 0; i < maxNumberOfEditions; i++) {
					if (editions[i] != null) {
						if (editions[i].isGeneInTable(genename))
							bufferedWriterOfEditions.write(" 1 ");
						else
							bufferedWriterOfEditions.write(" 0 ");
					}
					// else ( no existing edition )
				}
				bufferedWriterOfEditions.write("\n");
			}
			bufferedWriterOfEditions.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			System.out.println("Process done.");
		}
	}

	/**
	 * Parse gene association file to get edition number
	 * 
	 * @param geneAssociationFile
	 * */
	private static String getEditionNumber(String geneAssociationFile) {
		String edition = geneAssociationFile.replaceAll("\\D+", "");
		return edition;

	}

	/**
	 * To order the assoc files (number of editions).
	 * 
	 * @return
	 */
	public static Comparator<File> getFileComparator() {
		Comparator<File> comparator = new Comparator<File>() {
			public int compare(File file1, File file2) {
				String e1 = getEditionNumber(file1.getName());
				String e2 = getEditionNumber(file2.getName());
				Integer edition1 = Integer.valueOf(e1);
				Integer edition2 = Integer.valueOf(e2);

				if (edition1 == edition2)
					return 0;
				if (edition1 > edition2)
					return 1;
				return -1;
			}
		};
		return comparator;
	}

	/**
	 * Main point of entrance
	 * 
	 * @param args
	 * @param maxNumberOfEditions
	 * @param directory
	 * @param species
	 */
	public static void exploreGenes(String args[], Integer maxNumberOfEditions,
			String directory, String species) {
		if (args.length > 0) {
			// First argument is the annotation files directory
			directory = args[0];
			// Second argument is the species to consider
			species = args[1];
			// Third argument is the current max number of editions
			maxNumberOfEditions = Integer.parseInt(args[2]);
		}

		File directoryFolder = new File(directory);
		FileFilter fileFilter = new WildcardFileFilter("gene_association.goa*");
		File[] files = directoryFolder.listFiles(fileFilter);

		Arrays.sort(files, getFileComparator());

		if (maxNumberOfEditions == null) {
			maxNumberOfEditions = files.length;
		}

		AssociationFile[] editions = new AssociationFile[maxNumberOfEditions];
		AssociationFile allGenes = new AssociationFile();
		/* Traverse file by file */
		for (int i = 0; i < files.length; i++) {
			System.out.println(species + " >>> File " + (i + 1) + " out of "
					+ files.length + " " + files[i]);

			/*
			 * Get number of annotation file in defined in file name to preserve
			 * order
			 */
			String numbersInFileName = files[i].getName().replaceAll("[^0-9]+",
					"");
			int position = Integer.parseInt(numbersInFileName);
			position--;
			editions[position] = new AssociationFile();

			try {
				BufferedReader bufferedReaderOfAssocFiles = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(files[i]))));
				String line;
				/* skip first line. Some archives have header */
				line = bufferedReaderOfAssocFiles.readLine();
				/* Read line by line */
				while ((line = bufferedReaderOfAssocFiles.readLine()) != null) {

					String[] columnDetail = new String[11];
					columnDetail = line.split("\t");
					/* Sanity check. Make sure we have read 3 columns or more */
					if (columnDetail.length <= 2)
						continue;
					/* Add gene to allGenes hash table if not already there */
					if (!allGenes.isGeneInTable(columnDetail[1])) // 1 for
																	// uniprot,
																	// 2 for
																	// symbol
						allGenes.addGene(columnDetail[1]);

					/*
					 * Add gene to hash table at position "position" to preserve
					 * increasing order of editions
					 */
					if (!editions[position].isGeneInTable(columnDetail[1])) {
						if (VERBOSE)
							System.out.println(columnDetail[1]);
						editions[position].addGene(columnDetail[1]);
					}
				}// while
				bufferedReaderOfAssocFiles.close();
			} catch (IOException e) {
				System.err.println("Can't open file " + files[i]);
			}

		}
		compareEditionsShort(allGenes, editions, maxNumberOfEditions,
				directory, species);
	}

	/**
	 * Explore the genes for just one species.
	 * 
	 * @param species
	 * @param maxNumberOfEditions
	 */
	public static void exploreSpecies(String species, int maxNumberOfEditions) {

		try {
			File file = new File("data/");
			final String subdirectory;

			subdirectory = file.getAbsolutePath() + "/" + species + "/";
			exploreGenes(null, maxNumberOfEditions, subdirectory, species);

		} catch (Exception exeption) {
			exeption.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		fullExploration(args);
	}

	/**
	 * TODO
	 * 
	 * @param args
	 */
	public static void fullExploration(String[] args) {
		try {
			fullExplorationAux(args);
		} catch (Exception e) {
			System.err.println("the exploration failed for every species");
			e.printStackTrace();
		}
	}

	/**
	 * Run the exploration for every species.
	 * 
	 * @param args
	 */
	public static void fullExplorationAux(String[] args) {
		File file = new File("data/");
		final Integer maxNumberOfEditions = 130;
		String directory = null; // = "/home/asedeno/workspace/GOtrack/data";
		final String[] arguments = args;

		Species specieslist = new Species();

		for (final String species : specieslist.getSpecies()) {
			final String subdirectory;
			if (directory == null) {
				subdirectory = file.getAbsolutePath() + "/" + species + "/";
			} else {
				subdirectory = directory + "/" + species + "/";
			}

			System.out.println(subdirectory);

			Thread thread;
			Runnable runnable = new Runnable() {

				public void run() { // per species
					try {

						String command = "rm " + subdirectory + "*.tmp.txt";
						Sys.execAux(command, "log.txt");
						exploreGenes(arguments, maxNumberOfEditions,
								subdirectory, species);
					} catch (Exception e) {
						System.err
								.println("The species: "
										+ species
										+ " can not be completed due to: "
										+ e.getLocalizedMessage()
										+ "\nPrint stacktrace (line 227) for more information.");
						// e.printStackTrace();
					}
				}
			};
			// missingGOAnalysisForSpecies(species);

			thread = new Thread(runnable);
			thread.start();

		}// for every species
	}

}// explore genes
