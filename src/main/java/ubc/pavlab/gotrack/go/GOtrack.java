package ubc.pavlab.gotrack.go;

import static ubc.pavlab.gotrack.constants.Species.LOG;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.database.Load;
import ubc.pavlab.gotrack.mapping.GOMapping;
import ubc.pavlab.gotrack.pmid.PmidAnalysis;
import ubc.pavlab.gotrack.tools.GlobalStats;
import ubc.pavlab.gotrack.tools.Sys;
import ubc.pavlab.gotrack.tools.DownloadNewGOAFiles;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubc.pavlab.gotrack.utils.GetAllGOTerms;
import ubic.erminej.data.GeneSetTerms;

/**
 * This is the principal class and entry point of the project
 * 
 * @author asedeno Main class
 */
public class GOtrack extends GO{

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* Get a program instance using arguments provided by the user */		
		if (args.length == 4) {
			GOtrack gotrack = readUserArguments(args, args[2]);
			gotrack.runSpecies();
		}else{
			System.err.println("Number of arguments is incorrect.");
			System.exit(1);
		}

	}

	/**
	 * 
	 * @param gotrack
	 * @param species
	 * @param max
	 */
	public void runSpecies() {
		long startTime = System.currentTimeMillis();
		try {
			// Run just one species
			runSpeciesAux();
					
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		long time = ((endTime - startTime) / 1000); // seconds
		LOG.info("Analysis for  " + speciesToCompute + "  completed +++ "
				+ time);
		System.out.println("Analysis for   " + speciesToCompute.toUpperCase()
				+ "   completed in: " + time + " seconds");
	}

	/**
	 * Run the complete analysis just for the selected organism
	 * 
	 * @param speciesToCompute
	 * @throws Exception
	 */
	public boolean runSpeciesAux() throws Exception {

		LOG.info("Start the analysis for " + speciesToCompute);
		final Logger log = getLoggerforSpecies(speciesToCompute);
		log.setLevel(Level.INFO);

		DownloadNewGOAFiles df = new DownloadNewGOAFiles();
		// Download new goa files and update edition2dates 
		Integer minNewEdition = df.updateSpecies(speciesToCompute);
		
		// Read the association files and reuse the array
		File[] assocArrayOfFiles = GenUtils.readDirectory(
						Species.getDataFolder(speciesToCompute), "gene_association*",
						log);
	    int numEditions = GenUtils.getTotalNumberOfEditions(assocArrayOfFiles);
		
		//Download new termdb files
		if(minNewEdition!=numEditions){
			startEdition = minNewEdition;
		  df.updateTermdb(DownloadNewGOAFiles.REMOTE_DIR_TERMDB);
		  df.updateTermdb(DownloadNewGOAFiles.REMOTE_DIR_TERMDB2);
		}
		
		
		// Get the full list of genes and those that are always present
		if (overwriteExistingFiles || doCreateAlwaysPresentAndFullTermsFiles) {
			GetAllGOTerms.createAlwaysPresentAndFullTermsFiles(
					speciesToCompute, numEditions, assocArrayOfFiles, log);
		}		

		// Retrieving genesFile (default genes file)
		
		Boolean almosAlwaysHasData=true;
		if (speciesToCompute.equalsIgnoreCase(Species.HUMAN)) {
		    almosAlwaysHasData= GenUtils.createAllGenesFiles(speciesToCompute, assocArrayOfFiles,
					log, threshold);

			log.info("File used " + typeOfGenesUsedFile
					+ " for method: createAllGenesFiles");
		} else if (!speciesToCompute.equalsIgnoreCase(Species.HUMAN)) {
			almosAlwaysHasData=GenUtils.createAllGenesFiles(speciesToCompute, assocArrayOfFiles,
					log, threshold);
			log.info("File used " + typeOfGenesUsedFile
					+ " for method: createAllGenesFiles");
		}
		if(!doMapping && !almosAlwaysHasData){
			log.severe("File GenesAlmostAlwaysPresent has no data. Try incrementing the threshold.");
			System.exit(1);
		}

		// CREATE THE FILE : GenesAlwaysPresentWithSymbols.txt
		boolean genesAlwaysPresentWithSymbols = GenUtils
				.genesAlwaysPresentWithSymbols(speciesToCompute, log);
		if (!genesAlwaysPresentWithSymbols) {
			String last = AnalysisOfGenesList.removeLast();
			log.info("REMOVING :" + last + " FROM THE LIST OF EVALUATIONS.");
		}

		//Will try to map old pubmeds
		//Mappings will be saved in gene_association files
		//file replacedPubmeds.txt will contain all the replacements
		if(doPubmedMapping && doMapping==false )
		{	
			PmidAnalysis.pubmedMappingAllAnnotations(speciesToCompute,log,startEdition);
		}
		
		//Gene id mapping using synonyms and a custom dictionary
		//Once again, the mapped id's will be saved in the gene association file
		if (this.doMapping
				&& (speciesToCompute.equalsIgnoreCase(Species.HUMAN)|| 
					speciesToCompute.equalsIgnoreCase(Species.ARABIDOPSIS)|| 
					speciesToCompute.equalsIgnoreCase(Species.MOUSE) || 
					speciesToCompute.equalsIgnoreCase(Species.YEAST) ||
					speciesToCompute.equalsIgnoreCase(Species.ECOLI)||
					speciesToCompute.equalsIgnoreCase(Species.FLY)||
					speciesToCompute.equalsIgnoreCase(Species.COW)||
					/*for theses species we will only use idMapping file*/
					speciesToCompute.compareTo(Species.RAT) == 0
					|| speciesToCompute.compareTo(Species.WORM) == 0
					|| speciesToCompute.compareTo(Species.ZEBRAFISH) == 0
					|| speciesToCompute.compareTo(Species.DICTY) == 0
					|| speciesToCompute.compareTo(Species.CHICKEN) == 0)) 
		{
			
			// create the mapping of last edition done.
			try {
				log.info("Computing last edition for first time");
				final File associationFile = assocArrayOfFiles[assocArrayOfFiles.length - 1];
				final String edition2DatesFileAux = edition2DatesFile;

				String isOldVersion = GenUtils.getEditionType(speciesToCompute,
						associationFile.getName(), edition2DatesFileAux, log);
				int editionNumber = Integer.valueOf(GenUtils
						.getEditionNo(associationFile.getName()));
				runFullAnalysisForEdition(numEditions, associationFile,
						isOldVersion, editionNumber, termsBaseFile, log, true);

			} catch (Exception e) {
			}

			// create all of the mapping files.
			try {
				if (speciesToCompute.compareToIgnoreCase(Species.YEAST) == 0) 
				{
					//Yeast is a special case
					GOMapping.doYeastMapping(log, startEdition);
				}else{
					//Normal case for other species
					GOMapping.mainMApping(speciesToCompute,
							assocArrayOfFiles[assocArrayOfFiles.length - 1],
							log, startEdition);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			String command = "";			

			// Compress and move the generated .syn files
			File[] synFiles = GenUtils.readDirectory(
					Species.getTmpFolder(speciesToCompute),
					"gene_association*.gz.syn", log);

			for (File synFile : synFiles) 
			{
				if(startEdition>Integer.valueOf(GenUtils.getEditionNo(synFile.getName())))
					continue;
				// rename the files
				String synRenamed = renameGZFile(synFile.getAbsolutePath());
				command = "mv " + synFile.getAbsolutePath() + " " + synRenamed;
				Sys.bash(command);

				command = "gzip " + synRenamed;
				Sys.bash(command);

				command = "mv " + synRenamed + ".gz ./data/" + speciesToCompute;
				Sys.bash(command);
			}
			// run the analysis with these new files
			this.doMapping = false;
			return runSpeciesAux();
		}//end mapping 
		
		System.gc();

		//Compute all the parents files first
		for (File fassociationFile : assocArrayOfFiles) 
		{
			if(startEdition>Integer.valueOf(GenUtils.getEditionNo(fassociationFile.getName())))
				continue;
			String isOldVersion = GenUtils.getEditionType(speciesToCompute,
					fassociationFile.getName(), edition2DatesFile, log);
			int editionNumber = Integer.valueOf(GenUtils
					.getEditionNo(fassociationFile.getName()));			
			Parents.getParents(speciesToCompute, fassociationFile,
					isOldVersion, log, editionNumber);
		}

		// Count genes per go
		if (this.doCountGenesperGo) 
		{
			CountGenesPerGo.countGenesperGo(speciesToCompute, log, numEditions);
			Map<String, Integer[]> directs = CountGenesPerGo.countDirectGoPerGene(speciesToCompute, log,
					numEditions);
			Map<String, Integer[]> inferred = CountGenesPerGo.countInferredGoPerGene(speciesToCompute, log, numEditions);
			CountGenesPerGo.countInferredGoPerSymbol(speciesToCompute, log, numEditions);
			
			CountGenesPerGo.combineDirectsInferredCounst(speciesToCompute,numEditions, directs, inferred);
			
			
		}
		// Pubmed and envidence code analysis
		if(this.doPubmedAndEvidenceCodeAnalysis)
		{
			//history analysis
			log.info("Running pubmed analysis for "+speciesToCompute);
			PmidAnalysis p = new PmidAnalysis();
			Integer numberOfEditions=GenUtils.getTotalNumberOfEditions(assocArrayOfFiles)+1;
			
			//This function will try to get the pubmed date from the web page but
			//since that is incredibly expensive, we have a set of pre-computed files per species
			//that might already have the date, if we can't find it there, then we will look up the
			//web page
			p.analyzeSpeciesPubmedHistory(numberOfEditions,assocArrayOfFiles);
					
			p.writeAnalysisPubmed(speciesToCompute, numberOfEditions,
					GenUtils.missingEditions(assocArrayOfFiles),"pubmedHistory_",
					p.readPubmedDateDictionary(speciesToCompute));
			
			//Evidence code analysis
			log.info("Running evidence code analysis for "+speciesToCompute);			
			p.analyzeSpeciesEvidenceCodeHistory(numberOfEditions,assocArrayOfFiles);
			p.writeAnalysisEvidenceCode(speciesToCompute, numberOfEditions,
					GenUtils.missingEditions(assocArrayOfFiles),"evidenceCodeHistory_");
		}//end pubmed and evidence code analysis
		

		
		//Now do the actual computation for each edition
		//This loop can be skipped if doComputeAllEditions is false
		boolean isLastEdition = true;
		for (int i = assocArrayOfFiles.length - 1; i >= 0
				&& doComputeAllEditions; i--) 
		{
			final File associationFile = assocArrayOfFiles[i];
			final String edition2DatesFileAux = edition2DatesFile;
			final boolean isLastEditionAux = isLastEdition;
			
			if(startEdition>Integer.valueOf(GenUtils.getEditionNo(associationFile.getName())))
				continue;

			if (isLastEdition) 
			{
				log.info("Computing last edition");
				String isOldVersion = GenUtils.getEditionType(speciesToCompute,
						associationFile.getName(), edition2DatesFileAux, log);
				int editionNumber = Integer.valueOf(GenUtils
						.getEditionNo(associationFile.getName()));
				runFullAnalysisForEdition(numEditions, associationFile,
						isOldVersion, editionNumber, termsBaseFile, log,
						isLastEditionAux);
				isLastEdition = false;
				if (doComputeOnlyLastEdition)
					doComputeAllEditions = false;
				continue;
			} else
				log.info("Computing edition " + associationFile);

			System.gc();

			String isOldVersion = GenUtils.getEditionType(speciesToCompute,
					associationFile.getName(), edition2DatesFileAux, log);
			int editionNumber = Integer.valueOf(GenUtils
					.getEditionNo(associationFile.getName()));
			runFullAnalysisForEdition(numEditions, associationFile,
					isOldVersion, editionNumber, termsBaseFile, log,
					isLastEditionAux);
			isLastEdition = false;
		}//End computing all editions		

		System.gc(); // Free memory
		
		// COUNT GO TERMS
		if (doCountGoTerms)
			CountGO.countGoTerms(speciesToCompute, typeOfGenesUsedFile,
							edition2DatesFile, log, doTreeSanityCheck);
		
		//Call Jaccard Similarity (all species)
		SimilarityAnalysis.gotrackSemanticAnalysis(speciesToCompute, 
				"GenesAlmostAlwaysPresent.txt","jaccardpergeneovertime.txt");
		
		//Create file dbAnnotAnalysis.txt
		GlobalStats.computeAnnotAnalysis(speciesToCompute);
		
		// CLEAN UNWANTED FILES.
		File[] allTMPFiles = Species.getTmpFolder(speciesToCompute).listFiles();
		for (File file : allTMPFiles) 
		{
			try {
				if (file.isDirectory()) {
					continue;
				}

				BufferedReader reader = Species.getReader(speciesToCompute,
						file.getName());
				String line = reader.readLine();
				if (line == null || line.equals("")) { // file is empty
					reader.close();
					file.delete();
				} else {
					reader.close();
				}
			} catch (FileNotFoundException e) {
				log.severe("Could not delete empty file: " + file.getName());
			} catch (IOException e) {
				e.printStackTrace();
				log.severe("Could not delete empty file: " + file.getName());
			}

		}

		Load load = new Load();
		load.loadAll( speciesToCompute, log);		
		return true;
	}

	/**
	 * This function runs all the analysis for a given edition
	 * 
	 * 
	 * @param dir
	 *            : Where gene association files are found
	 * @param geneAssociationFile
	 *            : File that will be computed
	 * @param goTerms
	 *            : File that contains a list of all GO Terms always present in
	 *            all the association files or a list of all GO Terms that
	 *            appeared in at least one association file
	 * @param edition2DatesFile
	 *            : File that matches edition with publication date
	 * @param editionNo
	 *            : Edition number in text format
	 * @param isOldEdition
	 *            : Whether termdb is an old or new edition
	 * @param termdbDir
	 *            : Where termdb (one edition of the GO acyclic graph) files are
	 *            found.
	 * @param goMatrixDir
	 *            : Where goMatrix files will be saved
	 * @param tmpDir
	 *            : Temporary directory where the intermediate files will be
	 *            stored
	 * */
	private void runFullAnalysisForEdition(int numEditions, File assoc,
			String isOldEdition, int editionNumber, String baseFile,
			Logger logger, Boolean isLastEdition) {
		System.gc();

		// CREATE PARENTS FILE && LEAF SET TERMS FOR TERMDB
		// parentsFileName will be a file like
		// "parents/go.parents."+year+"-"+month+".txt"
		GeneSetTerms geneSetTerms = null;
		if (doCreateParentsAndLeaves) {
			geneSetTerms = Parents.getParents(speciesToCompute, assoc,
					isOldEdition, logger, editionNumber);
		}

		if (geneSetTerms == null) {
			logger.severe("Parents file for " + assoc.getName() + " skipped");
			try {
				Map<String, Calendar> matchdates2edition = GenUtils.matchdates2edition;

				String termdbFile = Parents.getTermdbFromGeneAssoc(speciesToCompute,
						assoc.getName(), matchdates2edition, logger);
				geneSetTerms = new GeneSetTerms(Species.getTermDBFolder()
						.getAbsolutePath() + "/" + termdbFile,
						Boolean.valueOf(isOldEdition));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.info("Parents file created in method Parents.getParents for: "
					+ assoc.getName());
		}

		// PARSE GOPARENTS FOR EACH EDITION
		String parseGoParents = "";
		if (doSimplifyParents) {
			parseGoParents = ParseGO.getParents(assoc, isOldEdition,
					editionNumber, speciesToCompute, logger);
			if (parseGoParents == null || parseGoParents.equals("")) {
				logger.severe("Parse go file for " + assoc.getName()
						+ " skipped");
			} else {
				logger.info("File: " + parseGoParents
						+ " created in method ParseGO.getParents for: "
						+ assoc.getName());
			}
		}
		// CONVERT PARENTS INTO A SIMPLIFIED VERSION
		String simplified = null;
		if (doSimplifyParents || doGoMatrix) {
			simplified = Parents.getSimpleParents(speciesToCompute,
					parseGoParents, editionNumber + "", logger);
			if (simplified == null || simplified.equals("")) {
				logger.severe("Simplified file for " + assoc.getName()
						+ " skipped");
			} else {
				logger.info("File: " + simplified
						+ " created in method Parents.getSimpleParents for: "
						+ assoc.getName());
			}
		}
		System.gc();
		// GET ONLY GENES
		String getOnlyGenes = null;
		if (doGetOnlyGenes || doGoMatrix) {
			getOnlyGenes = GenUtils.getOnlyGenes(speciesToCompute,
					parseGoParents, isLastEdition + "", editionNumber, logger);
			if (getOnlyGenes == null || getOnlyGenes.equals("")) {
				logger.severe(" Get only genes file for " + assoc.getName()
						+ " skipped");
			} else {
				logger.info("File: " + getOnlyGenes
						+ " created in method GenUtils.getOnlyGenes for: "
						+ assoc.getName());
			}
		}
		// MULTIFUNC
		if (doMultifunc) {
			logger.info("Multifunc starting for: " + AnalysisOfGenesList.toString()
					+ " in: " + assoc.getName());
			String termdbFile = Parents.getTermdbFromGeneAssoc(
					speciesToCompute, assoc.getName(),
					GenUtils.matchdates2edition, logger);
			Multifunc.multi(speciesToCompute, assoc, logger, isOldEdition,
					editionNumber, typeOfGenesUsedFile, termdbFile,
					geneSetTerms);
		}
		System.gc();
		// GOMATRIX
		if (doGoMatrix) {
			logger.info("GoMatrix done for: " + AnalysisOfGenesList.toString()
					+ " in: " + assoc.getName());
			ComputeMatrixAAP.runGoMatrix(speciesToCompute, assoc, 
					 "GenesAlmostAlwaysPresent.txt", "TermsAlwaysPresent.txt", 
                                        editionNumber);			
		}
		System.gc();
		logger.info("Done for edition: " + editionNumber);
	}

}
