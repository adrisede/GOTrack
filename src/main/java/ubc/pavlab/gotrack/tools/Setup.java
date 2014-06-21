package ubc.pavlab.gotrack.tools;

import static ubc.pavlab.gotrack.constants.Species.LOG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import ubc.pavlab.gotrack.constants.Species;

/**
 * This will generate the path for every species in the list and download the
 * termdb. Maven must execute this before any analysis.
 * 
 * Sequential per species, so it can be run prior to the big analysis
 * 
 * @author asedeno
 */
public class Setup {

	private static final String RELATIVE_DOWNLOADS = "downloadScripts";

	/**
	 * All steps to setup the project for every species
	 */
	public static void setup() {

		Species speciesObj = new Species();
		LinkedList<String> speciesList = speciesObj.getSpecies();
		for (final String species : speciesList) {
			createFolder(species);
		}// for
		downloadFiles();
	}

	/**
	 * Download the files for this species
	 * 
	 * @param species
	 */
	private static void downloadFiles() {
		LOG.info("Downloading files.");
		try {
			File file = new File(RELATIVE_DOWNLOADS + "/allDownloads.txt");
			// File file = new File(RELATIVE_DOWNLOADS +
			// "/onlyOneDownload.txt");
			BufferedReader reader = Species.getReader(file.getAbsolutePath());
			String line;

			while ((line = reader.readLine()) != null) {
				Thread thread;
				final String download = line;
				Runnable runnable = new Runnable() {
				
					public void run() {
						String command = "./" + RELATIVE_DOWNLOADS + "/"
								+ download;
						Sys.bash(command);

						LOG.info("COMPLETED { " + download + " } COMPLETED");
					}
				};

				thread = new Thread(runnable);
				thread.start();
			}

		} catch (FileNotFoundException e) {
			LOG.severe(RELATIVE_DOWNLOADS + " not found. >>"
					+ e.getLocalizedMessage());
		} catch (IOException e) {
			LOG.severe("Error while reading. >>" + e.getLocalizedMessage());
		}
	}

	/**
	 * Create the path for this species
	 * 
	 * @param species
	 */
	private static void createFolder(String species) {
		LOG.info("Creating path for " + species);
		String command = "mkdir -p "
				+ Species.getDataFolder(species).getAbsolutePath();
		Sys.bash(command);
		command = "mkdir -p " + Species.getTmpFolder(species).getAbsolutePath();
		Sys.bash(command);
		command = "mkdir -p "
				+ Species.getDataFolder(species).getAbsolutePath()
				+ Species.GOMATRIX_FOLDER;
		Sys.bash(command);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setup();
	}

}
