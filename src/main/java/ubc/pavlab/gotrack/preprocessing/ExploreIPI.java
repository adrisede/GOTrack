package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;

import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import ubc.pavlab.gotrack.constants.Species;

/**
 * Class meant for the exploration of IPI files
 * 
 * @author asedeno
 */
public class ExploreIPI {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// getALLIPIMappings(null); // create logger first
	}

	/**
	 * 
	 */
	public static void getALLIPIMappings(final Logger logger) {

		Species species = new Species();

		try {

			for (final String singleSpecies : species.getSpecies()) {
				Thread thread;
				Runnable runnable = new Runnable() {

					public void run() {
						try {
							System.out.println("IPI analysis for : "
									+ singleSpecies);
							getIPIMapping(singleSpecies, logger);

						} catch (Exception e) {

							e.printStackTrace();

							System.err
									.println("\n\nIT WAS NOT POSSIBLE TO MAP WITH IPIS :"
											+ singleSpecies + "\n\n");
						}
					}
				};

				thread = new Thread(runnable);
				thread.run();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("FATAL ERROR WHILE COMPUTING IPI FILIES : ");
		}

	}

	/**
	 * Parse gene association file to get edition number
	 * 
	 * @param geneAssociationFile
	 * */
	private static int getEditionNumber(String geneAssociationFile) {
		String edition = geneAssociationFile.replaceAll("\\D+", "");
		return Integer.valueOf(edition);
	}

	/**
	 * 
	 * @param species
	 */
	public static void getIPIMapping(String species, Logger logger) {
		try {
			getIPIMappingAux(species);
		} catch (IOException e) {
			e.printStackTrace();
			logger.warning("The mapping couldn't be completed for: " + species);
			logger.info(e.getLocalizedMessage());
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
	}

	/**
	 * Start the analysis for each species with the AnalysisFile
	 * 
	 * @param species
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static void getIPIMappingAux(String species) throws IOException {

		final String prefix = "Analysis";
		final String suffix = ".txt";
		final String fileName = "data/" + species + "/" + prefix + species
				+ suffix;
		final String directory = "data/" + species;
		File dir = new File(directory);
		FileFilter fileFilter = new WildcardFileFilter("gene_association.goa*");
		File[] files = dir.listFiles(fileFilter);

		BufferedWriter writer = Species.getDataWriter(species, "Analysis"
				+ species + "-mapping.txt");

		Arrays.sort(files, ExploreGenes.getFileComparator());
		int numEditions = files.length;

		Object[] mapArray = new Object[numEditions];
		Object[] mapArrayIPI = new Object[numEditions];
		// preprocess all assoc files with uniprot as KEY to be ready for the
		// mapping.
		HashMap<String, String> editionMap = new HashMap<String, String>();
		HashMap<String, String> ipiMap = new HashMap<String, String>();
		BufferedReader reader;
		int n = 0;
		System.out.print("Processing maps for every edition >> ");
		for (int i = 0; i < files.length; i++) {

			File file = files[i];
			reader = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new FileInputStream(file))));
			editionMap = new HashMap<String, String>();
			ipiMap = new HashMap<String, String>();
			String line;
			String[] details = null;
			String uniprot;
			String IPI;
			while ((line = reader.readLine()) != null) {
				details = line.split("\t");
				if (details.length < 5) {
					continue;
				}
				// map uniprot with IPI
				uniprot = details[1]; // uniprot
				IPI = details[10]; // IPI
				if (!editionMap.containsKey(uniprot)) {
					editionMap.put(uniprot, IPI);
				}
				if (!ipiMap.containsKey(IPI)) {
					ipiMap.put(IPI, uniprot);
				}
			}// process of one file Map complete for this edition.
			mapArray[i] = editionMap;
			mapArrayIPI[i] = ipiMap;
			if ((++n) % 13 == 0)
				System.out.print(".");
		}
		System.out.println("done.");

		// Verify the file exists
		File file = new File(fileName);
		// System.out.println(file.getAbsolutePath());

		reader = new BufferedReader(new InputStreamReader((new FileInputStream(
				file))));

		String line;
		String[] details;
		String uniprot;
		String editions;
		StringTokenizer tokenizer;

		// READ COMPLETE MATRIX
		System.out.print("Processing the matrix >> ");
		while ((line = reader.readLine()) != null) {
			if ((++n) % 1049 == 0)
				System.out.print(".");
			details = line.split(" ");
			uniprot = details[0];
			editions = line.substring(uniprot.length()).trim();

			if (editions.charAt(0) == '0') {
				continue; // we do not want Uniprots starting with zeros.
			}

			editions = new StringBuilder(editions).reverse().toString();
			// reverse the string and start looking for the first 1 in the
			// reversed string.

			tokenizer = new StringTokenizer(editions, " ");
			String token = null;

			// System.out.println(numEditions + " " + line);
			while (tokenizer.hasMoreElements()) {
				token = tokenizer.nextToken();
				if (token.equals("0")) {
					continue; // do not matter until found a 1.
				} else if (token.equals("1")) { //
					// we want to go out when we find the first 1
					break;
					// uncomment if want to skip first subset of 1
					// while (tokenizer.hasMoreTokens()
					// && (token = tokenizer.nextToken()).equals("1")) {
					// continue;
					// }
				}// if - else

			}// while

			int editionFound = tokenizer.countTokens();
			editionMap = (HashMap<String, String>) mapArray[editionFound];
			String IPI = editionMap.get(uniprot);
			String followingUniprot = "";
			try {
				ipiMap = (HashMap<String, String>) mapArrayIPI[(editionFound + 1)]; // next
																					// edition.
				followingUniprot = ipiMap.get(IPI);
				if (followingUniprot == null) {
					followingUniprot = "no_IPI_found_at_edition_"
							+ (getEditionNumber(files[editionFound].getName()) + 1);
				}
			} catch (ArrayIndexOutOfBoundsException iobe) {
				followingUniprot = "no_data_for_edition_"
						+ (getEditionNumber(files[editionFound].getName()) + 1);
			}

			// Write this mapping to the file. including the number of edition
			String outputLine = uniprot + "\t" + IPI + "\t"
					+ getEditionNumber(files[editionFound].getName()) + "\t"
					+ followingUniprot + "\n";
			writer.write(outputLine);

		}// search within file
		System.out.println("Done.");

		reader.close();
		writer.close();

		System.out.println("IPI analysis COMPLETED for : " + species);
	}// map IPI

}// class
