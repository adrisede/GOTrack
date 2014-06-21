package ubc.pavlab.gotrack.constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.go.GOtrack;



/**
 * Class meant to instantiate and create easy and fast access to species data folders
 * 
 * @author asedeno
 * 
 */
public class Species {

	// species
	public static String ARABIDOPSIS = "arabidopsis";
	public static String CHICKEN = "chicken";
	public static String COW = "cow";
	public static String DICTY = "dicty";
	public static String ECOLI = "ecoli";
	public static String DOG = "dog";
	public static String FLY = "fly";
	public static String HUMAN = "human";
	public static String MOUSE = "mouse";
	public static String PIG = "pig";
	public static String RAT = "rat";
	public static String WORM = "worm";
	public static String YEAST = "yeast";
	public static String ZEBRAFISH = "zebrafish";
	public static String DATA = "data/";
	public static String TMP_FOLDER = "/tmp/";
	public static String GOMATRIX_FOLDER = "/gomatrix";
	public static String LOG_FILE = "logs/gotrack-";

	public static String TERMDB = "termdb/";
	public static String PARENTSCHILDREN = DATA+"parentchildren/";
	private static LinkedList<String> allSpecies;
	public static Map<String, File> dataFolders;
	public static Map<String, File> tmpFolders;
	public static Map<String, File> gomatrixFolders;

	// Get the date and time in any format
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd";

	/**
	 * Get today's date
	 * 
	 * @return
	 */
	public static String date() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);
		return simpleDateFormat.format(calendar.getTime());
	}

	/**
	 * Logger to file
	 */
	public final static Logger LOG = Logger.getLogger(GOtrack.class.getName());

	/**
	 * 
	 */
	static {
		FileHandler fileHandler;
		try {
			// This block configures the logger with handler and formatter
			fileHandler = new FileHandler(LOG_FILE + date());
			LOG.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			LOG.info("GOtrack execution on: " + date());
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("A problem has being found reading the log directory "+e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Default folder locations
	 */
	public static File dataFolder = new File(DATA);

	/**
	 * Return the fileWriter for this species (tmp folder)
	 * 
	 * @param species
	 * @throws IOException
	 */
	public static BufferedWriter getWriterParentChildren(String fileName)
			throws IOException {		
		File file = new File(PARENTSCHILDREN + fileName);
		
		File dir = new File(PARENTSCHILDREN);
		if(!dir.exists())
		   dir.mkdir();
		
		if (!file.exists() ) {
			return new BufferedWriter(new FileWriter(file));
		}else
			return null;
		
	}
	
	/**
	 * Return the fileWriter for this species (tmp folder)
	 * 
	 * @param species
	 * @throws IOException
	 */
	public static BufferedWriter getWriter(String species, String fileName)
			throws IOException {
		File file = new File(getDataFolder(species).getAbsolutePath()
				+ TMP_FOLDER + fileName);
		if (file.exists() && !GOtrack.overwriteExistingFiles) {
			throw new RuntimeException("File " + file.getAbsolutePath()
					+ " already exist, skipping.");
		}
		return new BufferedWriter(new FileWriter(file));
	}

	/**
	 * Return the fileWriter for this species (tmp folder)
	 * 
	 * @param species
	 * @throws IOException
	 */
	public static BufferedWriter getWriter(String species, String fileName,
			boolean explicitOverwrite) throws IOException {
		File file = new File(getDataFolder(species).getAbsolutePath()
				+ TMP_FOLDER + fileName);
		if (file.exists() && !explicitOverwrite) {
			throw new RuntimeException("File " + file.getAbsolutePath()
					+ " already exist, skipping.");
		}
		return new BufferedWriter(new FileWriter(file));
	}

	/**
	 * Return the fileWriter for this species (tmp folder)
	 * 
	 * @param species
	 * @throws IOException
	 */
	public static BufferedWriter getDataWriter(String species, String fileName)
			throws IOException {
		File file = new File(getDataFolder(species).getAbsolutePath() + "/"
				+ fileName);
		if (file.exists() && !GOtrack.overwriteExistingFiles) {
			throw new RuntimeException("File " + file.getAbsolutePath()
					+ " already exist, skipping.");
		}
		return new BufferedWriter(new FileWriter(file));
	}

	/**
	 * Remove one file.
	 * 
	 * @param species
	 * @param fileName
	 */
	public static void removeFile(String species, String fileName) {
		File file = new File(getDataFolder(species).getAbsolutePath()
				+ TMP_FOLDER + fileName + ".txt");
		if (file.exists()) {
			file.delete();
		}
	}

	/**
	 * Returns a file to operate with in the TMP folder for this species.
	 * 
	 * @param species
	 * @param fileNameWithExtention
	 * @return
	 */
	public static File newFile(String species, String fileNameWithExtention) {
		File file = new File(getDataFolder(species).getAbsolutePath()
				+ TMP_FOLDER + fileNameWithExtention);
		return file;
	}

	/**
	 * Returns a file to operate with into the TermDB folder for this species.
	 * 
	 * @param species
	 * @param fileNameWithExtention
	 * @return
	 */
	public static File termDBFile(String fileNameWithExtention) {
		File file = new File(getTermDBFolder().getAbsolutePath() + "/"
				+ fileNameWithExtention);
		return file;
	}

	/**
	 * Return a reader at the designed location
	 * 
	 * @param location
	 * @return
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getReader(String location)
			throws FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(
				location)));
	}

	/**
	 * Return a reader at the designed location
	 * 
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getReader(String species, String fileName)
			throws FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(
				getDataFolder(species).getAbsolutePath() + TMP_FOLDER
						+ fileName)));
	}
	public static BufferedReader getParentChildrenReader( String fileName)
			throws FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(
			
				PARENTSCHILDREN+ fileName)));
	}

	/**
	 * Return a reader at the designed location
	 * 
	 * @param location
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getGzReader(File location) throws IOException {
		return new BufferedReader(new InputStreamReader(new GZIPInputStream(
				new FileInputStream(location))));
	}

	/**
	 * Return the termDB FOLDER
	 * 
	 * @param species
	 * @return
	 */
	public static File getTermDBFolder() {
		return new File(dataFolder.getAbsolutePath() + "/" + TERMDB);
	}

	/**
	 * Will return the dataFolder per species
	 * 
	 * @param species
	 * @return
	 */
	public static synchronized File getDataFolder(String species) {
		if (dataFolders == null) {
			dataFolders = new HashMap<String, File>();
			tmpFolders = new HashMap<String, File>();
			gomatrixFolders = new HashMap<String, File>();
		}

		if (!dataFolders.containsKey(species)) {
			dataFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species));
			tmpFolders.put(species, new File(dataFolder.getAbsolutePath() + "/"
					+ species + TMP_FOLDER));
			gomatrixFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species + GOMATRIX_FOLDER + "/"));
		}

		return dataFolders.get(species);
	}

	/**
	 * Return the File to the head of Data Folder
	 * 
	 * @return
	 */
	public static File getDataFolder() {
		return new File(dataFolder.getAbsolutePath() + "/");
	}

	/**
	 * Will return the tmp Folder per species
	 * 
	 * @param species
	 * @return
	 */
	public static  File getTmpFolder(String species) {
		if (tmpFolders == null) {
			dataFolders = new HashMap<String, File>();
			tmpFolders = new HashMap<String, File>();
			gomatrixFolders = new HashMap<String, File>();
		}

		if (!tmpFolders.containsKey(species)) {
			dataFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species));
			tmpFolders.put(species, new File(dataFolder.getAbsolutePath() + "/"
					+ species + TMP_FOLDER));
			gomatrixFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species + GOMATRIX_FOLDER + "/"));
		}

		return tmpFolders.get(species);
	}

	/**
	 * Will return the tmp Folder per species
	 * 
	 * @param species
	 * @return
	 */
	public static  File getGoMatrixFolder(String species) {
		if (gomatrixFolders == null) {
			dataFolders = new HashMap<String, File>();
			tmpFolders = new HashMap<String, File>();
			gomatrixFolders = new HashMap<String, File>();
		}

		if (!gomatrixFolders.containsKey(species)) {
			dataFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species));
			tmpFolders.put(species, new File(dataFolder.getAbsolutePath() + "/"
					+ species + TMP_FOLDER));
			gomatrixFolders.put(species, new File(dataFolder.getAbsolutePath()
					+ "/" + species + GOMATRIX_FOLDER + "/"));
		}

		return gomatrixFolders.get(species);
	}

	/**
	 * will return the list of all species public
	 * 
	 * @return
	 */
	public static LinkedList<String> getSpecies() {

		if (allSpecies == null) {
			allSpecies = new LinkedList<String>();

			allSpecies.add(ARABIDOPSIS);
			allSpecies.add(CHICKEN);
			allSpecies.add(COW);
			allSpecies.add(DICTY);
			allSpecies.add(ECOLI);
			allSpecies.add(DOG);
			allSpecies.add(FLY);
			allSpecies.add(PIG);
			allSpecies.add(RAT);
			allSpecies.add(WORM);
			allSpecies.add(YEAST);
			allSpecies.add(ZEBRAFISH);
			allSpecies.add(HUMAN);
			allSpecies.add(MOUSE);
		}

		return allSpecies;
	}
}
