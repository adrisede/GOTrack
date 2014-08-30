package ubc.pavlab.gotrack.go;

import static ubc.pavlab.gotrack.constants.Species.LOG;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ubc.pavlab.gotrack.constants.Species;
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
public class GOtrack extends GO {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* Get a program instance using arguments provided by the user */
		if (args.length == 4) {
			/*
			 * Read user arguments and fill variables accordingly to control the
			 * program flow
			 */
			GOtrack gotrack = readUserArguments(args, args[2]);
			/* This is the main function that will execute the whole analysis */
			gotrack.runSpecies();
		} else {
			/* In case of user error */
			System.err.println("Number of arguments is incorrect.");
			/*GenesAlwaysPresent.txt|GenesAlmostAlwaysPresent.txt|AllGenes.txt threshold doMapping(boolean)*/
			System.err.println("First argument: GenesAlwaysPresent.txt|GenesAlmostAlwaysPresent.txt|AllGenes.txt");
			System.err.println("Second argument: threshold. A gene will be considered if it is absent in 'threshold' number of editions.");
			System.err.println("Third argument: doMapping. Boolean value. Do the gene id mapping");
			System.exit(1);
		}

	}

	/**
	 * This is the main function. At this point all the control flow variables
	 * are filled.
	 * 
	 */
	public void runSpecies() {
		/* Get the start time of the program */
		long startTime = System.currentTimeMillis();
		try {
			/* Run just one species */
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
	 * @throws Exception
	 */
	public void runSpeciesAux() throws Exception {

		LOG.info("Start the analysis for " + speciesToCompute);
		/* This function starts the logger into the log/ directory */
		final Logger log = getLoggerforSpecies(speciesToCompute);

		/*
		 * The defaul logging level is INFO. For more detailed logging, set the
		 * level to bigger values
		 */
		log.setLevel(Level.INFO);

		/*
		 * The first step is downloading the new files. This code check if there
		 * is files in the ftp servers that are not in the local file system. In
		 * particular, it checks GOA files.
		 */
		DownloadNewGOAFiles df = new DownloadNewGOAFiles();

		/* Download new goa files and update edition2dates */
		/*
		 * if there are new editions then minNewEdition will be the smallest of
		 * those new editions. This variable is used to know if there are new
		 * editions
		 */
		Integer minNewEdition = df.updateSpecies(speciesToCompute);

		// Read the association files into assocArrayOfFiles
		File[] assocArrayOfFiles = GenUtils.readDirectory(
				Species.getDataFolder(speciesToCompute), "gene_association*",
				log);
		/*
		 * numEditions will have the max number of editions in the local file
		 * system. For instance if there are 10 files in the local machine but
		 * they correspond to editions from 20 to 30, numEditions will be 30
		 */
		int numEditions = GenUtils.getTotalNumberOfEditions(assocArrayOfFiles);

		if (minNewEdition != numEditions) {
			/* Get here when there are new editions */

			/*
			 * The analysis specific an edition will run for only for the new
			 * editions. Analysis that are global to all editions will consider
			 * all editions
			 */
			startEdition = minNewEdition;

			/* Download new termdb files, don't download existing ones */
			df.updateTermdb(DownloadNewGOAFiles.REMOTE_DIR_TERMDB);
			df.updateTermdb(DownloadNewGOAFiles.REMOTE_DIR_TERMDB2);
		}

		/* Get the full list of goterms always present */
		if (overwriteExistingFiles || doCreateAlwaysPresentAndFullTermsFiles) {
			GetAllGOTerms.createAlwaysPresentAndFullTermsFiles(
					speciesToCompute, numEditions, assocArrayOfFiles, log);
		}


		/*Will try to map old pubmeds
		  Correct mappings will be saved/replaced in gene_association files
		  file replacedPubmeds.txt will contain all the replacements*/
		if (doPubmedMapping && doMapping == false) {
			PmidAnalysis.pubmedMappingAllAnnotations(speciesToCompute, log,
					startEdition);
		}

		/* Gene id mapping using synonyms and a custom dictionary
		   Once again, the mapped id's will be saved in the gene association
		   file*/
		if (this.doMapping
				&& (speciesToCompute.equalsIgnoreCase(Species.HUMAN)
						|| speciesToCompute
								.equalsIgnoreCase(Species.ARABIDOPSIS)
						|| speciesToCompute.equalsIgnoreCase(Species.MOUSE)
						|| speciesToCompute.equalsIgnoreCase(Species.YEAST)
						|| speciesToCompute.equalsIgnoreCase(Species.ECOLI)
						|| speciesToCompute.equalsIgnoreCase(Species.FLY)
						|| speciesToCompute.equalsIgnoreCase(Species.COW) ||
						/* for theses species we will only use idMapping file */
						speciesToCompute.compareTo(Species.RAT) == 0
						|| speciesToCompute.compareTo(Species.WORM) == 0
						|| speciesToCompute.compareTo(Species.ZEBRAFISH) == 0
						|| speciesToCompute.compareTo(Species.DICTY) == 0 || speciesToCompute
						.compareTo(Species.CHICKEN) == 0)) {

			// last edition .
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
				e.printStackTrace();
				System.exit(0);
			}

			// create all of the mapping files. (*syn files)
			try {
				GOMapping mapping = new GOMapping();
				if (speciesToCompute.compareToIgnoreCase(Species.YEAST) == 0) {
					/* Yeast is a special case*/
					mapping.doYeastMapping(log, startEdition);
				} else {
					/* Normal case for other species*/
					mapping.mainMApping(speciesToCompute,
							assocArrayOfFiles[assocArrayOfFiles.length - 1],
							log, startEdition);
				}
				String command = "";

				/* Get an array of the new *.syn files*/
				File[] synFiles = GenUtils.readDirectory(
						Species.getTmpFolder(speciesToCompute),
						"gene_association*.gz.syn", log);
				/*The *syn files have the correct mapping of ids, the next step is replace the *gz with the new
				 * *syn files, so that the next steps work with the updated goa files */
				for (File synFile : synFiles) {				
					// rename the files
					String synRenamed = GenUtils.renameGZFile(synFile
							.getAbsolutePath());
					command = "cp " + synFile.getAbsolutePath() + " " + synRenamed;
					Sys.bash(command);

					command = "gzip " + synRenamed;
					Sys.bash(command);

					command = "mv " + synRenamed + ".gz ./data/" + speciesToCompute;
					Sys.bash(command);
				}
				/*This function will create the AllGenes.txt file and GenesAlmostAlwaysPresent.txt*/
				GenUtils.createAllGenesFiles(
						speciesToCompute, assocArrayOfFiles, log, threshold);
				boolean updateUsingLocalFile = false;
				mapping.updateUniprotWithWebPage(speciesToCompute,updateUsingLocalFile,log);	
				for (File synFile : synFiles) {				
					// rename the files
					String synRenamed = GenUtils.renameGZFile(synFile
							.getAbsolutePath());
					command = "mv " + synFile.getAbsolutePath() + " " + synRenamed;
					Sys.bash(command);

					command = "gzip " + synRenamed;
					Sys.bash(command);

					command = "mv " + synRenamed + ".gz ./data/" + speciesToCompute;
					Sys.bash(command);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			
			// run the analysis with these new files
			this.doMapping = false;
			/*Make a recursive call to this function, now that the goa files
			 * are updated the complete analysis must be done from scratch.*/
			runSpeciesAux();
		}// end mapping

		System.gc();
		
		/*
		 * geneasAlmostAlwaysHasData is true if there are genes almost always
		 * present considering the user threshold (number of editions where a
		 * gene might be absent)
		 * 
		 * geneasAlmostAlwaysHasData is false if GenesAlmostAlwaysPresent.txt is empty
		 */
		Boolean geneasAlmostAlwaysHasData = true;

		/*This function will create the AllGenes.txt file and GenesAlmostAlwaysPresent.txt*/
		geneasAlmostAlwaysHasData = GenUtils.createAllGenesFiles(
				speciesToCompute, assocArrayOfFiles, log, threshold);

		log.info("File used " + typeOfGenesUsedFile
				+ " for method: createAllGenesFiles");

		if (!doMapping && !geneasAlmostAlwaysHasData) {
			/*There is no data in GenesAlmostAlwaysPresent.txt then the procedure can't continue*/
			log.severe("File GenesAlmostAlwaysPresent has no data. Try incrementing the threshold.");
			System.exit(1);
		}

		/* CREATE THE FILE : GenesAlwaysPresent.txt GenesAlwaysPresentWithSymbols.txt 
		 * genesLastEdition.txt */
		GenUtils.genesAlwaysPresentWithSymbols(speciesToCompute, log);		


		// Compute all the parents files first
		/* for each GOA file, this code will get the list of parents 
		 * for each go term in gene association*/
		for (File fassociationFile : assocArrayOfFiles) {
			if (startEdition > Integer.valueOf(GenUtils
					.getEditionNo(fassociationFile.getName())))
				continue;
			String isOldVersion = GenUtils.getEditionType(speciesToCompute,
					fassociationFile.getName(), edition2DatesFile, log);
			int editionNumber = Integer.valueOf(GenUtils
					.getEditionNo(fassociationFile.getName()));
			Parents.getParents(speciesToCompute, fassociationFile,
					isOldVersion, log, editionNumber);
		}

		/* Pubmed and envidence code analysis*/
		if (this.doPubmedAndEvidenceCodeAnalysis) {			
			log.info("Running pubmed analysis for " + speciesToCompute);
			PmidAnalysis p = new PmidAnalysis();
			Integer numberOfEditions = GenUtils
					.getTotalNumberOfEditions(assocArrayOfFiles) + 1;

			// This function will try to get the pubmed date from the web page
			// but since that is incredibly expensive, we have a set of pre-computed
			// files per species that might already have the date, if we can't find it there, then
			// we will look up the web page
			p.analyzeSpeciesPubmedHistory(numberOfEditions, assocArrayOfFiles);

			p.writeAnalysisPubmed(speciesToCompute, numberOfEditions,
					GenUtils.missingEditions(assocArrayOfFiles),
					"pubmedHistory_",
					p.readPubmedDateDictionary(speciesToCompute));

			// Evidence code analysis
			log.info("Running evidence code analysis for " + speciesToCompute);
			p.analyzeSpeciesEvidenceCodeHistory(numberOfEditions,
					assocArrayOfFiles);
			p.writeAnalysisEvidenceCode(speciesToCompute, numberOfEditions,
					GenUtils.missingEditions(assocArrayOfFiles),
					"evidenceCodeHistory_");
		}// end pubmed and evidence code analysis

		// Now do the actual computation for each edition
		// This loop can be skipped if doComputeAllEditions is false
		boolean isLastEdition = true;
		for (int i = assocArrayOfFiles.length - 1; i >= 0
				&& doComputeAllEditions; i--) {
			final File associationFile = assocArrayOfFiles[i];
			final String edition2DatesFileAux = edition2DatesFile;
			final boolean isLastEditionAux = isLastEdition;

			/*Only consider new editions*/
			if (startEdition > Integer.valueOf(GenUtils
					.getEditionNo(associationFile.getName())))
				continue;
			
		    log.info("Computing edition " + associationFile);

			System.gc();

			/*An old version is < 2005*/
			String isOldVersion = GenUtils.getEditionType(speciesToCompute,
					associationFile.getName(), edition2DatesFileAux, log);
			int editionNumber = Integer.valueOf(GenUtils
					.getEditionNo(associationFile.getName()));
			runFullAnalysisForEdition(numEditions, associationFile,
					isOldVersion, editionNumber, termsBaseFile, log,
					isLastEditionAux);
			isLastEdition = false;
		}// End computing all editions

		System.gc(); // Free memory

		/* Count genes per go. */
		if (this.doCountGenesperGo) {
			/* File countGenesperGoTerm.txt will be created. This file has an entry per goterm
		     * followed by n columns (one per edition), each column is the number of genes associated to that goterm
		     * in each edition*/
			CountGenesPerGo.countGenesperGo(speciesToCompute, log, numEditions);
			
			/*countDirectTermspergene.txt has an entry per gene
		     * followed by n columns (one per edition), each column is the number of go terms per gene
		     * in each edition */
			Map<String, Integer[]> directs = CountGenesPerGo
					.countDirectGoPerGene(speciesToCompute, log, numEditions);
			
			/* countInferredTermspergene.txt has an entry per gene
		     * followed by n columns (one per edition), each column is the he number of inferred terms per gene
		     * in each edition */
			Map<String, Integer[]> inferred = CountGenesPerGo
					.countInferredGoPerGene(speciesToCompute, log, numEditions);
			
			/*countInferredTermsperSymbol.txt has an entry per gene symbol
		     * followed by n columns (one per edition), each column is the he number of inferred terms per gene symbol
		     * in each edition*/
			CountGenesPerGo.countInferredGoPerSymbol(speciesToCompute, log,
					numEditions);

			/*directsInferred.data contains three columns (gene,number of direct genes, number of inferred, edition)
			 * This file is ready to be loaded directly to the database*/
			CountGenesPerGo.combineDirectsInferredCounst(speciesToCompute,
					numEditions, directs, inferred);

		}

		// Call Jaccard Similarity (all editions)
		/* This function creates a file jaccardpergeneovertime.txt that shows how similar
		 * is a gene to itself (in the last edition). The values varies from 0 to 1, 0 means the gene
		 * is very different from the last edition, 1 means the gene is exactly the same as the last edition*/
		SimilarityAnalysis.gotrackSemanticAnalysis(speciesToCompute,
				"GenesAlmostAlwaysPresent.txt", "jaccardpergeneovertime.txt");

		// Create file dbAnnotAnalysis.txt
		/* Computes the number of manual annotations that are promoted to more specific ones*/
		GlobalStats.computeAnnotAnalysis(speciesToCompute);

		/* CLEAN UNWANTED FILES.*/
		File[] allTMPFiles = Species.getTmpFolder(speciesToCompute).listFiles();
		for (File file : allTMPFiles) {
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
			/*get all parents for each go term in gene association file
			 * geneSetTerms models the parents for this edition*/
			geneSetTerms = Parents.getParents(speciesToCompute, assoc,
					isOldEdition, logger, editionNumber);
		}
		if(geneSetTerms==null)
			return;
		
		logger.info("Parents file created in method Parents.getParents for: "
					+ assoc.getName());
		
		/* PARSE GOPARENTS FOR EACH EDITION*/
		String parseGoParents = "";
		if (doSimplifyParents) {
			/* Associate a gene id with the parent go terms for a given
	         * edition.*/
			ParseGO parse = new ParseGO();
			parseGoParents = parse.getParents(assoc, isOldEdition,
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
			/*The simplified version is file edition.*annots.simple.txt
			 * that have two columns (geneid, list of parents separated by a pipe)*/
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
		
		String getOnlyGenes = null;
		if (doGetOnlyGenes || doGoMatrix) {
			/* Create file  "genes." + edition + ".txt" that is only a list of the 
			 * genes in this edition */
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
		
		if (doMultifunc) {
			/*Compute multifunctionality score*/
			logger.info("Multifunc starting for: "
					+ AnalysisOfGenesList.toString() + " in: "
					+ assoc.getName());
			String termdbFile = Parents.getTermdbFromGeneAssoc(
					speciesToCompute, assoc.getName(),
					GenUtils.matchdates2edition, logger);
			Multifunc.multi(speciesToCompute, assoc, logger, isOldEdition,
					editionNumber, typeOfGenesUsedFile, termdbFile,
					geneSetTerms);
		}
		System.gc();
		
		if (doGoMatrix) {
			/*Compute *.gomatrix.txt file that will be used to create the similarity analysis (jaccard)
			 * after all the editions are computed */
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
