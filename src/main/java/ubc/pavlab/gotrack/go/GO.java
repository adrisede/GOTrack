package ubc.pavlab.gotrack.go;

import static ubc.pavlab.gotrack.constants.Species.LOG;
import static ubc.pavlab.gotrack.constants.Species.LOG_FILE;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import ubc.pavlab.gotrack.constants.Species;
/**
 * GOtrack extends GO.java
 * This class contains general functionality for GOtrack, like reading input arguments and
 * defining variables to control the program
 * */

public class GO {

	public LinkedList<String> globalSpecies = null;
	public Integer MAXEDITIONS = null;
	int MAX_THREAD_CONCURRENT = 1; //

	/*----------------------------Default values---------------------------------*/
	protected String speciesToCompute = "human";

	public static String edition2DatesFile = "edition2Dates.txt";
	/* By default Count will use genes always present */
	protected String typeOfGenesUsedFile = "GenesAlwaysPresent.txt";

	/* By default gomatrix will use terms always present */
	protected String termsBaseFile = "TermsAlwaysPresent.txt";

	/*
	 * This threshold is for the # of editions where genes can be missing (genes
	 * almost always present)
	 */
	protected Integer threshold = 5;

	protected boolean doMapping = true;
	protected boolean doCountGenesperGo = true;
	protected boolean doPubmedAndEvidenceCodeAnalysis = false;
	protected boolean doCountGoTerms = true;
	protected boolean doPubmedMapping =true;

	/* For the analysis of all editions */
	protected boolean doCreateAlwaysPresentAndFullTermsFiles = true;
	protected boolean doCreateEdition2DatesFile = true;
	protected boolean doComputeAllEditions = true;
	protected boolean doComputeOnlyLastEdition = false;
	protected Integer startEdition = 1;

	/* For the analysis in each edition */
	protected boolean doCreateParentsAndLeaves = true;
	protected boolean doSimplifyParents = true;
	protected boolean doGetOnlyGenes = true;
	protected boolean doMultifunc = true;
	protected boolean doGoMatrix = true;
	

	/*
	 * If true, it will count and check which parents are leaves, this is
	 * obviously not possible but this works as a sanity check. As a rule of
	 * thumb this variable should always be false
	 */
	protected final boolean doTreeSanityCheck = false;

	public static boolean overwriteExistingFiles = true;

	static LinkedList<String> AnalysisOfGenesList = new LinkedList<String>();
	static {
		AnalysisOfGenesList.add("genesLastEdition.txt");
		AnalysisOfGenesList.add("GenesAlwaysPresent.txt");
		AnalysisOfGenesList.add("GenesAlwaysPresentWithSymbols.txt");
	}
	
	
	
	/**
	 * This function will fill the class instance data with user defined
	 * arguments
	 * @param args Arguments array
	 * @param species Species to compute
	 * @return Returns a gotrack instance that will contain all the
	 * control variables to execute the program
	 * */
	protected static GOtrack readUserArguments(String[] args, String species) {
		GOtrack gotrack = new GOtrack();
		/* fill program data from user */
		/*
		 * parse typeofgenesused(almost/always/all), threshold,
		 * termsbasefile(allterms/alwayspresent)
		 */
		if (args.length < 4) {
			System.err.println("Number of arguments is incorrect.");
			System.exit(1);
		}
		if ((args[0].compareTo("GenesAlwaysPresent.txt") == 0)
				|| (args[0].compareTo("GenesAlmostAlwaysPresent.txt") == 0)
				|| (args[0].compareTo("AllGenes.txt") == 0)) {
			gotrack.typeOfGenesUsedFile = args[0];
		} else {
			System.err
					.println("Possible values for first argument: GenesAlwaysPresent.txt|GenesAlmostAlwaysPresent.txt|AllGenes.txt");
			System.exit(1);
		}

		gotrack.threshold = Integer.valueOf(args[1]);
		gotrack.termsBaseFile = "TermsAlwaysPresent.txt";
		gotrack.speciesToCompute = species;
		gotrack.doGoMatrix = true;
		gotrack.doMultifunc = true;
		gotrack.doMapping = Boolean.valueOf(args[3]); // first time to run= true, subsequent
									// analysis
									// set as false
		gotrack.startEdition=1;
		gotrack.doCreateEdition2DatesFile = true;
		gotrack.doComputeAllEditions = true;
		gotrack.doCountGenesperGo=true;
		gotrack.doPubmedMapping = true;//cambiar a true
		gotrack.doPubmedAndEvidenceCodeAnalysis = true;
		return gotrack;
	}
	
	/**
	 * Initializes a specific logger for each species, (avoid asynchronous
	 * collapse)
	 * 
	 * @param species
	 * @return Logger for this run
	 */
	public static Logger getLoggerforSpecies(String species) {
		Logger logger = null;
		FileHandler fh;
		try {
			logger = Logger.getLogger(GOtrack.class.getName());
			// This block configures the logger with handler and formatter
			fh = new FileHandler(LOG_FILE + species + "-" + Species.date());
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			// logger.setLevel(LOG.);
		} catch (SecurityException e) {
			LOG.severe(Arrays.toString(e.getStackTrace()));
		} catch (IOException e) {
			LOG.severe(Arrays.toString(e.getStackTrace()));
		}

		return logger;
	}

	/**
	 * Auxiliar function to rename a .gz file to .syn 
	 * 
	 * @param fileName
	 * @return filename with .syn extension
	 */
	public static String renameGZFile(String fileName) {
		return fileName.substring(0, fileName.indexOf(".gz.syn"));
	}
}
