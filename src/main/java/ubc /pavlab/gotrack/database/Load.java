package ubc.pavlab.gotrack.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.GOtrack;
import ubc.pavlab.gotrack.tools.Sys;
import ubc.pavlab.gotrack.utils.GenUtils;
import static ubc.pavlab.gotrack.constants.Species.LOG;

/**
 * This class loads the information in the package files to the DB
 * This program should be run after gotrack has finished.
 * 
 * @author asedeno
 */
public class Load {
	/**
	 * Variable to instantiate a connection to the database
	 */
	private Connection connect = null;
	/**
	 * Variable to instantiate a statement to be executed in the database
	 */
	private Statement statement = null;
	/**
	 * Variable that models the statement after the binds are made
	 */
	private PreparedStatement preparedStatement = null;

	/**
	 * Name of the database, for example "gotrack"
	 */
	protected String database = null;
	/**
	 * Database user name
	 */
	protected String user = null;
	/**
	 * Password to be used
	 */
	protected String password = null;
	/**
	 * URL of the host where the database lives
	 */
	protected String host = null;

	/**
	 * Reads the properties file (config.properties) where the connection info to the database is
	 * defined.
	 * This function is called by the Load class constructor
	 */
	public void readProperties() {
		Properties prop = new Properties();
		InputStream input = null;
		OutputStream output = null;

		try {
			input = getClass().getClassLoader().getResourceAsStream(
					"config.properties");

			// load a properties file
			prop.load(input);
			// get the property value and print it out
			database = prop.getProperty("database");
			user = prop.getProperty("dbuser");
			password = prop.getProperty("dbpassword");
			host = prop.getProperty("host");

			if (database == null || user == null || password == null) {

				System.err
						.println("Error reading config.properties file. Please define database, dbsuer and dbpassword.\n");
			}
		} catch (Exception io) {
			String testPath = this.getClass().getClassLoader().getResource("")
					.getPath();
			System.err.println("Error reading config.properties file from "
					+ testPath);
			System.exit(1);

		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Constructor.
	 * 
	 * When an instance of this class is created, this constructor will read the
	 * properties file and set a database connection
	 */
	public Load() {
		try {
			/*The first step is validate that the database connection is correct*/
			readProperties();
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection string with the DB
			connect = DriverManager.getConnection("jdbc:mysql://" + host + "/"
					+ database + "?" + "user=" + user + "&password=" + password
					+ "");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * This overloaded constructor will set up a new database connection using
	 * the arguments provided.
	 * This function might be used for testing
	 * 
	 * @param user
	 * @param password
	 * @param url
	 */
	public Load(String user, String password, String url) {
		try {

			System.out.println("Attempting the connection with: ");
			System.out.println(user);
			System.out.println(password);
			System.out.println(url);
			System.out.println("Connecting... ");

			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://" + url + "?"
					+ "user=" + user + "&password=" + password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Insert the edition into the edition_to_date table
	 * 
	 * @param editionNo
	 * @param date
	 * @param species
	 */
	private void insertEdition(String editionNo, String date, String species) {
		String insertStatement = "insert into  edition_to_date (editionNo,date,species) (select ? as editionNo, ? as date, objNo as species from species where name like ?);";
		// Statements allow to issue SQL queries to the database
		try {
			statement = connect.createStatement();
			preparedStatement = connect.prepareStatement(insertStatement);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
					"mm/dd/yyyy");
			Date dateObj = simpleDateFormat.parse(date);
			simpleDateFormat.applyPattern("yyyy-mm-dd");
			date = simpleDateFormat.format(dateObj);

			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date myDate = formatter.parse(date);
			java.sql.Date sqlDate = new java.sql.Date(myDate.getTime());

			preparedStatement.setString(1, editionNo);
			preparedStatement.setDate(2, sqlDate);
			preparedStatement.setString(3, species);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function will update the edition_to_dates table for the specified
	 * species.
	 * The function will remove all the information in edition_to_dates for "species"
	 * then it will load what is in the edition2Dates.txt file
	 * @param species
	 */
	public void loadEdition2Dates(String species) {

		BufferedReader loadEditionReader;
		try {
			/*
			 * Delete existing information from edition_to_dates for this
			 * species
			 */
			statement = connect.createStatement();
			preparedStatement = connect
					.prepareStatement("delete from edition_to_date where species in "
							+ "(select objNo from species where name = '"
							+ species + "')");
			preparedStatement.executeUpdate();

			/* update the database */
			loadEditionReader = Species.getReader(species, "edition2Dates.txt");
			String line;
			// iterate over gene association file .parents
			while ((line = loadEditionReader.readLine()) != null) {
				String[] columnDetail = new String[200];
				columnDetail = line.split("\t");
				if (columnDetail.length < 2)
					continue; // This row might be the header so skip it
				String filen = columnDetail[0];
				String date = columnDetail[1];
				String editionNo = GenUtils.getEditionNo(filen);
				/*All the validations are done, insert the row in the database*/
				insertEdition(editionNo, date, species);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generic function to bulk load a file directly into a table
	 * Bulk loads a formated file directly to the database, it assumes the file follows the format
	 * provided by the arguments
	 * @param file The file to upload
	 * @param arguments a list of columns in the database that correspond to the same columns in the file
	 */
	public void bulkLoad(String file, String arguments) {
		String command = "LOAD DATA LOCAL INFILE '" + file + "' INTO TABLE "
				+ arguments + ";";
		System.out.println(command);
		try {
			statement = connect.createStatement();
			statement.executeUpdate(command);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Uploads a gene association file to the *_gene_annot table
	 * If the edition is already in the table, it won't load the file
	 * 
	 * @param speciesToCompute Species to compute
	 * @log log  Logger to store the progress
	 */
	public void loadGeneAnnot(String speciesToCompute, Logger log) {

		HashSet<Integer> editionsIndBD = getEditionsInDB(speciesToCompute);
		// Read the association files and reuse the array
		ArrayList<File> geneAssocListFiles = GenUtils.readDirectory(Species
				.getDataFolder(speciesToCompute).getAbsolutePath() + "/",
				"gene_association*gz");
		int j = 0;
		String[] columnDetail = new String[100];
		String uniprot = "";
		String symbol = "";
		String goterm = "";
		String evidence = "";
		String ontology = "";
		String name = "";
		String taxon = "";
		String editionNo = "";
		String pubmed = "";
		String infered = "";
		for (File geneAssocFile : geneAssocListFiles) {
			try {
				/* Only consider the genes in editionsToUpdate */
				if (editionsIndBD.contains(Integer.valueOf(GenUtils
						.getEditionNo(geneAssocFile.getName())))) {
					continue;
				}

				System.gc();
				editionNo = GenUtils.getEditionNo(geneAssocFile.getName());

				// This step is very important. We are using the parent file for
				// this
				// edition to read all the inferred parents and load them to the
				// database
				Map<String, Integer> inferedParents = GenUtils
						.readAndCountParents(speciesToCompute, editionNo, log);

				// gene_annot.data will be a temporary file used to upload each
				// edition
				// the file is deleted after each edition is uploaded
				String temporalFile = "gene_annot" + editionNo + ".data";
				BufferedWriter writer = Species.getWriter(speciesToCompute,
						temporalFile);
				// Play safe and call the garbage collector each iteration
				System.gc();
				log.info("Uploading file " + geneAssocFile.getName());
				BufferedReader in = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(geneAssocFile))));
				String tmp;
				// Read line by line
				while ((tmp = in.readLine()) != null) {
					Arrays.fill(columnDetail, "");
					columnDetail = tmp.split("\t");
					if (columnDetail.length >= 12) {
						uniprot = columnDetail[1];
						symbol = columnDetail[2];
						goterm = columnDetail[4];
						evidence = columnDetail[6];
						ontology = columnDetail[8];
						name = columnDetail[9];
						taxon = columnDetail[12];
						pubmed = columnDetail[5];

						// Look for key uniprot+"|"+goterm in parents count to
						// get the number
						// of inferred parents
						if (!inferedParents.isEmpty())
							infered = String
									.valueOf(inferedParents.get(goterm));

						// Root term
						if (infered.compareTo("") == 0)
							infered = "0";

						if (infered == null || infered.contains("null")) {
							// if there is no inferred parents for this edition
							// we have to use NULL
							// so that the database won't save an empty string
							// and we can tell
							// for which goterms we couldn't get their parents
							// reading the termdb file
							// and count properly
							infered = "\\N";
						}
						if (!pubmed.contains("PUBMED")
								&& !pubmed.contains("PMID")) {
							// Once again we have to explicitly say which
							// annotations don't have
							// a pubmed id so that the counting is correct
							pubmed = "\\N";
						}
						editionNo = GenUtils.getEditionNo(geneAssocFile
								.getName());
						writer.write(editionNo + "\t" + uniprot + "\t" + symbol
								+ "\t" + goterm + "\t" + evidence + "\t" + name
								+ "\t" + taxon + "\t" + ontology + "\t"
								+ pubmed + "\t" + infered + "\n");

						if ((j++) % 1000 == 0)
							System.out.print(".");
					}
				}
				in.close();
				j = 0;
				writer.close();
				log.info("Loading info into the database. This will take some time.");
				bulkLoad(
						Species.getDataFolder(speciesToCompute)
								.getAbsolutePath()
								+ Species.TMP_FOLDER
								+ temporalFile,
						speciesToCompute
								+ "_gene_annot(edition,gene,symbol,goterm,evidence,name,taxon,ontology,pubmedid, inferred)");				
			} catch (FileNotFoundException fnfex) {
				fnfex.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * It will read the gene association files looking for each gene product in
	 * counts
	 * 
	 * @param speciesToCompute
	 * @param log
	 * @param counts
	 * @return it return "counts" each element of counts will be filled with the symbol
	 */
	public Map<String, ArrayList<CountEntry>> getSymbols(
			String speciesToCompute, Logger log,
			Map<String, ArrayList<CountEntry>> counts) {
		try {

			// Read the association files and reuse the array
			ArrayList<File> geneAssocListFiles = GenUtils.readDirectory(
					Species.getDataFolder(speciesToCompute).getAbsolutePath()
							+ "/", "*gene_association*gz");
			int j = 0;
			String[] columnDetail = new String[100];
			String uniprot = "";
			String symbol = "";
			String editionNo = "";
			for (File geneAssocFile : geneAssocListFiles) {
				System.gc();
				editionNo = GenUtils.getEditionNo(geneAssocFile.getName());

				// Play safe and call the garbage collector each iteration
				System.gc();
				log.info("Getting symbols of file " + geneAssocFile.getName());
				BufferedReader in = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(geneAssocFile))));
				String tmp;
				
				// Read line by line
				while ((tmp = in.readLine()) != null) {
					Arrays.fill(columnDetail, "");
					columnDetail = tmp.split("\t");
					if (columnDetail.length > 3) {
						uniprot = columnDetail[1];
						symbol = columnDetail[2];

						if (counts.containsKey(uniprot)) {
							ArrayList<CountEntry> t = counts.get(uniprot);
							for (CountEntry tt : t) {
								if (tt.getSymbol().length() == 2
										&& tt.getEdition().compareTo(editionNo) == 0
										&& tt.getSymbol().compareTo("\\N") == 0) {
									/*Set the symbol in the entry*/
									tt.setSymbol(symbol);
									break;
								}
							}
						}
						/*This shows the progress of the function*/
						if ((j++) % 1000 == 0)
							System.out.print(".");
					}
				}
				in.close();
				j = 0;

			}
		} catch (FileNotFoundException fnfex) {
			fnfex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*Now "counts" has the symbol for each uniprot*/
		return counts;
	}

	/**
	 * It will read the file directsInferred.data from the gotrack analysis and
	 * create a map of the gene to an array of CountEntry classes
	 * 
	 * @param species Species to compute
	 * @param log Logger to keep track the progress
	 * 
	 * @return It return a map of gene -> Array List of Count elements
	 * Each entry of the array list contains all the information that will be saved in the *_count table
	 * The structure returned by this function is almost ready to be loaded to the database
	 */
	public Map<String, ArrayList<CountEntry>> loadDirectsInferredGoPerGeneOverTime(
			String species, Logger log) {
		log.info("Loading count GO terms per gene over time");
		HashMap<String, ArrayList<CountEntry>> ret = new HashMap<String, ArrayList<CountEntry>>();

		try {
			BufferedReader readerCountGo = Species.getReader(species,
					"directsInferred.data");
			String temporary;
			String[] columnDetail = new String[50];
			String gene;
			String directs;
			String inferred;
			String edition;
			int counter = 0;
			while ((temporary = readerCountGo.readLine()) != null) {
				// call garbage collector from time to time so that we prevent
				// the memory from exhausting
				if (counter++ % 200000 == 0) {
					System.out.print(".");
					System.gc();
				}
				Arrays.fill(columnDetail, "");
				columnDetail = temporary.split("\t");
				gene = columnDetail[0];
				directs = columnDetail[1];
				if (directs.compareTo("\\N") == 0)
					continue;
				inferred = columnDetail[2];
				edition = columnDetail[3];
				CountEntry newEntry = new CountEntry();
				newEntry.setDirects(directs);
				newEntry.setEdition(edition);
				newEntry.setGene(gene);
				newEntry.setInferred(inferred);
				newEntry.setEdition(edition);
				ArrayList<CountEntry> count = null;
				if (ret.containsKey(gene)) {
					count = ret.remove(gene);
				} else
					count = new ArrayList<CountEntry>();
				count.add(newEntry);
				ret.put(gene, count);
			}
			readerCountGo.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;

	}

	/**
	 * This method load gonames per edition into the database 
	 * Keeping all the go names helps to keep track the changes during the time
	 */
	public void loadGONames() {
		System.out.print("Extracting go names");

		/*Get the termdb files that are already in the database
		 * Don't upload information that is already stored*/
		HashSet<String> termdbInDb = getTermdbsDatesInDB();

		/*Prepare the new names if there are any*/
		ArrayList<String> gofiles = GONames.extractGOTermNames(null, null,
				termdbInDb);
		/*upload the new information for each termdb if there is any*/
		for (String file : gofiles) {
			System.out.print("Loading file " + file + "\n");
			bulkLoad(file, "GO_names(goterm,name,date)");
		}
	}

	/**
	 * The replaces*txt file contain the results of the id mapping This function
	 * will upload the files needed (that are not already in the DB)
	 * 
	 * @param species Species to compute
	 */
	public void loadReplacedIds(String species) {

		HashSet<Integer> editionsIndBD = getEditionsInReplacedIds(species);

		File[] arrayOfReplacedIds = GenUtils.readDirectory(
				Species.getDataFolder(species + Species.TMP_FOLDER),
				"replaced_*txt", LOG);

		for (File parents : arrayOfReplacedIds) {
			String editionNumber = GenUtils.getEditionNo(parents.getName());
			/*
			 * Only update replaced files for the editions that are not in the
			 * database to avoid duplicate information
			 */			
			if (editionsIndBD
					.contains(editionNumber)) {
				continue;
			}

			System.out.println("Loading replaced ids for edition "+editionNumber);
			BufferedWriter writer;
			try {
				BufferedReader speciesReader = Species.getReader(species,
						parents.getName());
				writer = Species.getWriter(species, "replaced.data");
				String temporary;
				String[] columnDetail = new String[3];
				String original = "";
				String mapped = "";
				while ((temporary = speciesReader.readLine()) != null) {

					Arrays.fill(columnDetail, "");
					columnDetail = temporary.split("\t");
					if (columnDetail.length < 2)
						continue;
					original = columnDetail[0];
					mapped = columnDetail[1];
					/*Only load information that is useful.*/
					if(original.length()==0 || mapped.length()==0 ||(original.compareTo(mapped)==0))
						continue;
					writer.write(original + "\t" + mapped + "\t"+editionNumber+"\n");
				}
				writer.close();
				speciesReader.close();
				bulkLoad(Species.getDataFolder(species).getAbsolutePath()
						+ Species.TMP_FOLDER + "replaced.data", species
						+ "_replaced_id(original,replaced, edition)");
				Sys.bash("rm "
						+ Species.getDataFolder(species).getAbsolutePath()
						+ Species.TMP_FOLDER + "replaced.data");

			} catch (IOException e) {				
				e.printStackTrace();
			}

		}
	}

	/**
	 * The file counGenesPerGoTerm.txt contains the information about the number
	 * of genes that are annotated to a go term
	 * 
	 * This function loads it to the database
	 * 
	 * @param species Species to compute
	 * @param log Logger to record the progress of this function
	 */
	public void loadGenesPerGOTerm(String species, Logger log) {
		try {
			log.info("Loading count GO terms per gene over time");
			BufferedReader readerCountGo = Species.getReader(species,
					"countGenesperGoTerm.txt");
			BufferedWriter writer = Species.getWriter(species, "countgpg.data");
			String temporary;
			String[] columnDetail = new String[1000];
			readerCountGo.readLine(); // Skip header
			String goterm = "";
			while ((temporary = readerCountGo.readLine()) != null) {
				Arrays.fill(columnDetail, "");
				columnDetail = temporary.split(" ");
				goterm = columnDetail[0];
				/*countGenesperGoTerm.txt has n columns one for each
				 * edition.*/
				for (int h = 1; h < columnDetail.length; h++) {
					writer.write(goterm + "\t" + h + "\t" + columnDetail[h]
							+ "\n");
				}
			}
			writer.close();
			readerCountGo.close();
			/* Delete existing information for this species */
			try {
				statement = connect.createStatement();
				preparedStatement = connect.prepareStatement("delete from "
						+ species + "_gene_per_go");
				preparedStatement.executeUpdate();
			} catch (SQLException e) {				
				e.printStackTrace();
			}

			log.info("Loading info into the database. This will take some time.");
			bulkLoad(Species.getDataFolder(species).getAbsolutePath()
					+ Species.TMP_FOLDER + "countgpg.data", species
					+ "_gene_per_go(goterm,edition,value)");
			Sys.bash("rm " + Species.getDataFolder(species).getAbsolutePath()
					+ Species.TMP_FOLDER + "countgpg.data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * After a species is loaded (or updated) into the database it is needed to
	 * call a couple of stored procedures to update the global trend tables
	 * 
	 * @param species Species to upload
	 */
	public void postGeneAnnotLoad(String species) {

		try {
			String command = "call updateSpecies ('" + species + "')";
			statement = connect.createStatement();
			System.out.println(command);
			statement.executeUpdate(command);

			command = "call updatedatabase()";
			statement = connect.createStatement();
			System.out.println(command);
			statement.executeUpdate(command);

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Read the mutifunc.score.txt files that have the multifunctionality
	 * information for each gene in the editions
	 * 
	 * @param species
	 * @return This function fill the necessary information into "count"
	 */
	public Map<String, ArrayList<CountEntry>> loadMultifuncScore(
			String species, Logger log, Map<String, ArrayList<CountEntry>> count) {

		log.info("Loading multifunc score");
		HashSet<Integer> editionsIndBD = getEditionsInDB(species);

		File[] arrayOfMultifScores = GenUtils.readDirectory(
				Species.getTmpFolder(species), "*multifunc.score.txt", LOG);

		for (File multifScoreFile : arrayOfMultifScores) {
			try {
				if (editionsIndBD.contains(multifScoreFile.getName())) {
					continue;
				}
				BufferedReader speciesReader = Species.getReader(species,
						multifScoreFile.getName());
				String temporary;
				String[] columnDetail = new String[5];
				String uniprot = "";
				String value = "";
				String editionNo = GenUtils.getEditionNo(multifScoreFile
						.getName());
				while ((temporary = speciesReader.readLine()) != null) {
					Arrays.fill(columnDetail, "");
					columnDetail = temporary.split("\t");
					if (columnDetail.length < 2)
						continue;
					uniprot = columnDetail[2];
					value = columnDetail[1];
					ArrayList<CountEntry> entry = count.get(uniprot);
					for (CountEntry c : entry) {
						if (editionNo.compareTo(c.getEdition()) == 0) {
							c.setMultifunc(value);
						}
					}
				}
				speciesReader.close();
			} catch (IOException e) {
				log.severe("Error loading dictionary");
				e.printStackTrace();

			}
		}
		return count;
	}

	/**
	 * The file jaccardpergeneovertime.txt has the information about how similar
	 * is a gene to itself over time.
	 * 
	 * This function reads the file and fill the count variable with this info
	 * 
	 * @param species  Species to compute
	 * @param log      Logger to record the progress
	 * @param count    Data structure that will be filled with the missing information
	 * @return This function fill the necessary information into "count"
	 */
	public Map<String, ArrayList<CountEntry>> loadSimilarityScore(
			String species, Logger log, Map<String, ArrayList<CountEntry>> count) {
		try {
			log.info("Loading similarity score");
			BufferedReader readerCountGo = new BufferedReader(
					new InputStreamReader(new FileInputStream(Species
							.getDataFolder(species).getAbsolutePath()
							+ Species.GOMATRIX_FOLDER
							+ "/jaccardpergeneovertime.txt")));
			String temporary;
			String[] columnDetail = new String[1000];
			String gene = "";
			String edition = "";
			String similarity = "";
			// skip header
			temporary = readerCountGo.readLine();

			while ((temporary = readerCountGo.readLine()) != null) {
				Arrays.fill(columnDetail, "");
				columnDetail = temporary.split("\t");
				gene = columnDetail[0];
				edition = columnDetail[1];
				int ed = Integer.valueOf(edition);
				

				if (columnDetail.length < 3)
					similarity = "\\N";
				else
					similarity = columnDetail[2];

				ArrayList<CountEntry> entry = count.get(gene);
				if (entry != null) {
					for (CountEntry c : entry) {
						int cedit = Integer.valueOf(c.getEdition());
						if (cedit == ed) {
							c.setJaccard(similarity);
							break;
						}
					}
				}
			}
			readerCountGo.close();

		} catch (IOException e) {
			log.severe("The file data/<species>/gomatrix/jaccardpergeneovertime.txt has a problem.");
			e.printStackTrace();

		}
		return count;
	}

	/**
	 * The file evidenceCodeHistory keeps track of the pubmed information per
	 * annotation over time
	 * 
	 * This function uploads it to the database
	 * 
	 * @param species Species to upload
	 * @param log     Logger to record the progress
	 */
	public void loadEvidenceCodeHistory(String species, Logger log) {
		try {
			/* Delete existing information for this species */
			statement = connect.createStatement();
			preparedStatement = connect.prepareStatement("delete from "
					+ species + "_evidence_code");
			preparedStatement.executeUpdate();

			log.info("Loading Evidence code history");
			BufferedReader readerCountGo = Species.getReader(species,
					"evidenceCodeHistory_" + species + ".txt");
			BufferedWriter writer = Species.getWriter(species, "evcode.data");
			String temporary;
			String[] columnDetail = new String[1000];
			String[] header = new String[1000];
			String gene = "";
			String pubmed;
			String code;
			String goterm;
			temporary = readerCountGo.readLine();
			header = temporary.split("\t");

			while ((temporary = readerCountGo.readLine()) != null) {
				Arrays.fill(columnDetail, "");
				columnDetail = temporary.split("\t");
				gene = columnDetail[0];
				goterm = columnDetail[1];
				pubmed = columnDetail[2];
				if (goterm == null || goterm.compareTo("") == 0
						|| goterm.compareTo("\\N") == 0)
					goterm = "\\N";
				try {
					Integer.valueOf(pubmed);
				} catch (NumberFormatException e) {
					pubmed = "\\N";
				}
				if (pubmed == null || pubmed.compareTo("") == 0)
					pubmed = "\\N";
				for (int h = 3; h < columnDetail.length; h++) {
					code = columnDetail[h];
					if (code == null || code.compareTo("") == 0)
						code = "\\N";

					if (code.compareTo("\\N") == 0)
						continue;
					writer.write(gene + "\t" + goterm + "\t" + pubmed + "\t"
							+ code + "\t" + header[h] + "\n");
				}
			}
			writer.close();
			readerCountGo.close();
			log.info("Loading info into the database. This will take some time.");
			bulkLoad(Species.getDataFolder(species).getAbsolutePath()
					+ Species.TMP_FOLDER + "/evcode.data", species
					+ "_evidence_code(gene,goterm,pubmed,code,edition)");
			Sys.bash("rm " + Species.getDataFolder(species).getAbsolutePath()
					+ Species.TMP_FOLDER + "/evcode.data");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function loads the global table annotAnalysisTab for all species.
	 * File species/dbAnnotAnalysis.txt should contain only the analysis for new
	 * editions that are not already in the database
	 * @param species Species to compute
	 * @param log Logger to keep track the progress
	 */
	public void loadAnnotAnalysis(String species, Logger log) {
		try {

			/*
			 * Delete existing information from edition_to_dates for this
			 * species
			 */
			statement = connect.createStatement();
			preparedStatement = connect
					.prepareStatement("delete from annotAnalysisTab where species = "
							+ getSpeciesNumber(species));
			preparedStatement.executeUpdate();

			String speciesNumber = getSpeciesNumber(species);

			if (speciesNumber.compareTo("") == 0) {
				System.out.print("Cannot find species " + species);
				System.exit(1);
			}

			log.info("Loading annotation analysis");
			BufferedReader readerCountGo = Species.getReader(species,
					"dbAnnotAnalysis.txt");
			BufferedWriter writer = Species.getWriter(species,
					"annotanalysis.data");
			String temporary;
			String[] columnDetail = new String[1000];
			readerCountGo.readLine(); // Skip header
			while ((temporary = readerCountGo.readLine()) != null) {
				Arrays.fill(columnDetail, "");
				columnDetail = temporary.split("\t");
				String notAnalysis = "\\N";
				if (columnDetail.length > 5)
					notAnalysis = columnDetail[5];
				writer.write(columnDetail[0] + "\t" + columnDetail[2] + "\t"
						+ columnDetail[3] + "\t" + speciesNumber + "\t"
						+ notAnalysis + "\n");
			}
			writer.close();
			readerCountGo.close();
			log.info("Loading info into the database. This will take some time.");
			bulkLoad(
					Species.getDataFolder(species).getAbsolutePath()
							+ Species.TMP_FOLDER + "annotanalysis.data",
					"annotAnalysisTab(edition,annotsIEAtoManual,annotsGeneralToSpecific,species,notAnnotRatio)");
			Sys.bash("rm " + Species.getDataFolder(species).getAbsolutePath()
					+ Species.TMP_FOLDER + "annotanalysis.data");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}

	/**
	 * This function will load all the data for an organism
	 * @param speciesToCompute Species to upload
	 * @param log  Logger
	 */
	public void loadAll(String speciesToCompute, Logger log) {

		// Load new termdb symbols
		loadGONames();

		// Load main table
		generateCountFile(speciesToCompute, log);

		// load genes per go count
		loadGenesPerGOTerm(speciesToCompute, log);

		// Load evidence code history
		loadEvidenceCodeHistory(speciesToCompute, log);

		// Load edition 2 dates file for this species
		loadEdition2Dates(speciesToCompute);

		// Load id that were replaced
		loadReplacedIds(speciesToCompute);

		// Load gene annotation file. This function might take a long time
		loadGeneAnnot(speciesToCompute, log);

		loadAnnotAnalysis(speciesToCompute, log);

		// Post db load will create tables *_unique_gene_symbol and
		postGeneAnnotLoad(speciesToCompute);

	}

	/**
	 * This function will upload edition2dates table, gene annotation table for
	 * one species
	 * 
	 * @param args[0] The program receives as argument the species to load if
	 *            args[0]=='loadGoNames' then it will load the go names using
	 *            termdb files
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err
					.print("First argument: The species to upload or 'loadGoNames'\n");
			System.err
					.print("Second argument: all -> to load all the tables\n");
			System.err
					.print("               : maintable -> to load count table\n");
			System.err
					.print("               : evcode    -> to load evidence code table\n");
			System.err
					.print("               : edition2dates    -> to load edition2Date table\n");
			System.err
					.print("               : replacedIds    -> to load replaced ids table\n");
			System.err
					.print("               : geneannot    -> to load gene annotation table\n");
			System.err
					.print("               : postload    -> to create or update *_unique_gene_symbol table after geneannot has been executed\n");
			System.err
					.print("               : geneperGo    -> to load gene_per_go table\n");
			System.err
					.print("               : annotAnalysis    -> to load gene_per_go table\n");
			System.exit(1);
		}

		String speciesToCompute = args[0];

		String instruction = args[1];
		/*Prepare the logger for this run*/
		final Logger log = GOtrack.getLoggerforSpecies(speciesToCompute);

		Load load = new Load();

		/* Check that the species is correct */
		String speciesNumber = load.getSpeciesNumber(speciesToCompute);
		if (speciesNumber.compareTo("") == 0) {
			System.out.print("Cannot find species " + speciesToCompute);
			System.exit(1);
		}

		// Load go names reading directly all the termdbs
		// This operation should be done once.
		if (args[0].compareTo("loadGoNames") == 0) {
			load.loadGONames();
			System.exit(0);
		}

		if (args[1].compareTo("all") == 0) {
			load.loadAll(speciesToCompute, log);
		} else if (instruction.compareTo("maintable") == 0)
			// Load main table
			load.generateCountFile(speciesToCompute, log);
		else if (instruction.compareTo("evcode") == 0)
			// Load evidence code history
			load.loadEvidenceCodeHistory(speciesToCompute, log);
		else if (instruction.compareTo("edition2dates") == 0)
			// Load edition 2 dates file for this species
			load.loadEdition2Dates(speciesToCompute);
		else if (instruction.compareTo("replacedIds") == 0)
			// Load id that were replaced
			load.loadReplacedIds(speciesToCompute);
		else if (instruction.compareTo("geneannot") == 0)
			// Load gene annotation file. This function might take a long time
			load.loadGeneAnnot(speciesToCompute, log);
		else if (instruction.compareTo("postload") == 0)
			// Post db load will create tables *_unique_gene_symbol and
			load.postGeneAnnotLoad(speciesToCompute);
		else if (instruction.compareTo("geneperGo") == 0)
			load.loadGenesPerGOTerm(speciesToCompute, log);
		else if (instruction.compareTo("annotAnalysis") == 0)
			load.loadAnnotAnalysis(speciesToCompute, log);

		System.exit(0);
	}

	/**
	 * Function to upload the species_count table that includes many per gene
	 * 
	 * @param species Species to upload
	 * @param log     Logger to record the progress
	 * @param count   Data structure that will be uploaded
	 * 
	 */
	public void loadCountFile(String species, Logger log,
			HashMap<String, ArrayList<CountEntry>> count) {
		try {

			/* Delete existing information from database for this species */
			statement = connect.createStatement();
			preparedStatement = connect.prepareStatement("delete from "
					+ species + "_count");
			preparedStatement.executeUpdate();

			log.info("Loading count");
			/*Create the temporary file to upload*/
			BufferedWriter writer = Species.getWriter(species, "count.data");

			for (String gene : count.keySet()) {
				ArrayList<CountEntry> editions = count.get(gene);
				for (CountEntry ed : editions) {
					if (ed.getSymbol().compareTo("\\N") == 0)
						continue;
					writer.write(ed.getGene() + "\t" + ed.getSymbol() + "\t"
							+ ed.getDirects() + "\t" + ed.getInferred() + "\t"
							+ ed.getMultifunc() + "\t" + ed.getJaccard() + "\t"
							+ ed.getEdition() + "\n");
				}
			}
			writer.close();
			log.info("Loading info into the database. This will take some time.");
			bulkLoad(
					Species.getDataFolder(species).getAbsolutePath()
							+ Species.TMP_FOLDER + "count.data",
					species
							+ "_count(gene,symbol,directs,inferred, multifunc, jaccard, edition)");
			// Sys.bash("rm " + Species.getDataFolder(species).getAbsolutePath()
			// + Species.TMP_FOLDER + "count.data");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}

	/**
	 * This is the main function to create the file that will be uploaded to
	 * <species>_count table
	 * 
	 * @param species Species to upload
	 * @param log     Logger to record the progress
	 * 
	 * @return This function makes a data structure that is ready to be uploaded to
	 * the database
	 */
	public Map<String, ArrayList<CountEntry>> generateCountFile(String species,
			Logger log) {

		
		/*Variable "count" will store all the information for each gene and each edition
		 * The idea is fill the variable step by step. First the directs and inferred goterms per gene
		 * then the multifunctionality score, afterward the similiarity score and finally the gene symbol*/
		
		// Get direcs
		// Get inferreds			
		HashMap<String, ArrayList<CountEntry>> count = (HashMap<String, ArrayList<CountEntry>>) loadDirectsInferredGoPerGeneOverTime(
				species, log);
		System.gc();
		
		// get multifunc score
		count = (HashMap<String, ArrayList<CountEntry>>) loadMultifuncScore(
				species, log, count);

		System.gc();
		// get jaccard
		count = (HashMap<String, ArrayList<CountEntry>>) loadSimilarityScore(
				species, log, count);

		System.gc();
		// Get symbols
		count = (HashMap<String, ArrayList<CountEntry>>) getSymbols(species,
				log, count);

		System.gc();
		// Upload main count table
		loadCountFile(species, log, count);

		return count;
	}

	/**
	 * Given a species name, get the object number in the database
	 * 
	 * @param species
	 * @return species number
	 */
	public String getSpeciesNumber(String species) {
		String res = "";

		try {
			String sqlStmt = "Select objNo from species where name = ? ";
			PreparedStatement prepStmt = connect.prepareStatement(sqlStmt);
			prepStmt.setString(1, species);
			// prepStmt.setString(2, "%" + symbolOrGene + "%");

			ResultSet rs = prepStmt.executeQuery();
			while (rs.next()) {
				res = rs.getString("objNo");
			}
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * Returns the gene association files loaded to the database
	 * @param species Species to compute
	 * @return Set of editions in the database (stored in *_gene_annot table)
	 * */
	public HashSet<Integer> getEditionsInDB(String species) {
		HashSet<Integer> editionsindb = new HashSet<Integer>();

		try {
			String sqlStmt = "Select distinct(edition) from " + species
					+ "_gene_annot ";
			PreparedStatement prepStmt = connect.prepareStatement(sqlStmt);

			ResultSet rs = prepStmt.executeQuery();
			while (rs.next()) {
				editionsindb.add(Integer.valueOf(rs.getString("edition")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return editionsindb;
	}
	
	/**
	 * Returns the editions loaded in *_replaced_id table
	 * @param species Species to query
	 * @return Set of editions loaded in *_replaced_id table
	 * */
	public HashSet<Integer> getEditionsInReplacedIds(String species) {
		HashSet<Integer> editionsindb = new HashSet<Integer>();

		try {
			String sqlStmt = "Select distinct(edition) from " + species
					+ "_replaced_id ";
			PreparedStatement prepStmt = connect.prepareStatement(sqlStmt);

			ResultSet rs = prepStmt.executeQuery();
			while (rs.next()) {
				editionsindb.add(Integer.valueOf(rs.getString("edition")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return editionsindb;
	}

	/**
	 * Returns the termdbs loaded to the database (GO_names table)
	 * 
	 * @return Set of dates corresponding to the termdbs loaded in the database
	 * */
	public HashSet<String> getTermdbsDatesInDB() {
		HashSet<String> editionsindb = new HashSet<String>();

		try {
			String sqlStmt = "select distinct(date_format(date, '%Y-%m')) as date from GO_names";
			PreparedStatement prepStmt = connect.prepareStatement(sqlStmt);
			ResultSet rs = prepStmt.executeQuery();
			while (rs.next()) {
				editionsindb.add("go_daily-termdb.rdf-xml."
						+ rs.getString("date") + ".gz.go-names.txt");
			}
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		return editionsindb;
	}
	
	
	
	/**
	 * This class models one entry in the <species>_count table
	 * 
	 * @author asedeno
	 *
	 */
	public class CountEntry {
		private String gene;
		private String symbol="\\N";
		private String directs="\\N";
		private String inferred="\\N";
		private String multifunc="\\N";
		private String jaccard="\\N";
		private String edition="\\N";
		
		public String getMultifunc() {
			return multifunc;
		}

		public void setMultifunc(String multifunc) {
			this.multifunc = multifunc;
		}

		public String getGene() {
			return gene;
		}

		public void setGene(String gene) {
			this.gene = gene;
		}

		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}

		public String getDirects() {
			return directs;
		}

		public void setDirects(String directs) {
			this.directs = directs;
		}

		public String getInferred() {
			return inferred;
		}

		public void setInferred(String inferred) {
			this.inferred = inferred;
		}


		public String getJaccard() {
			return jaccard;
		}

		public void setJaccard(String jaccard) {
			this.jaccard = jaccard;
		}

		public String getEdition() {
			return edition;
		}

		public void setEdition(String edition) {
			this.edition = edition;
		}

	}


}
