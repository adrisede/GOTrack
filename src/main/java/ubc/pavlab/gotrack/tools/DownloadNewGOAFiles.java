package ubc.pavlab.gotrack.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;

/**
 * This class download new files from the ftp servers
 * 
 * @author asedeno
 * 
 */
public class DownloadNewGOAFiles {

	private static String DOMAIN = "ftp.ebi.ac.uk";
	private static String DOMAINFLY = "ftp.flybase.org";
	private static String DOMAINECOLI = "http://cvsweb.geneontology.org";
	private static String REMOTEDIR_ECOLI = "cgi-bin/cvsweb.cgi/~checkout~/go/gene-associations/";
	private static String REMOTE_DIR = "/pub/databases/GO/goa/old/";
	private static String DOMAINTERMDB = "ftp.geneontology.org";
	public static String REMOTE_DIR_TERMDB2 = "/pub/go/godatabase/archive/termdb/";
	public static String REMOTE_DIR_TERMDB = "/pub/go/godatabase/archive/full/";

	/**
	 * This program will download new gene association files.
	 * 
	 * It will check the already existing editions in the species directory and
	 * download new ones if available
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		DownloadNewGOAFiles df = new DownloadNewGOAFiles();
		df.updateSpecies(Species.YEAST);
		/*
		 * df.updateTermdb(REMOTE_DIR_TERMDB);
		 * df.updateTermdb(REMOTE_DIR_TERMDB2);
		 * 
		 * LinkedList<String> allspecies = Species.getSpecies(); for (String
		 * species : allspecies) { System.out.println("Downloading " + species);
		 * df.updateSpecies(species); }
		 */
	}

	/**
	 * This function will try to download the new GOA files
	 * 
	 * @param species
	 *            Species to update
	 * @return The number of the new edition (if new data was downloaded) or -1
	 *         of nothing is updated
	 */
	public Integer updateSpecies(String species) {

		File edition2dates = new File(Species.getDataFolder(species)
				.getAbsolutePath() + Species.TMP_FOLDER + "edition2Dates.txt");

		HashMap<String, String> newdata = new HashMap<String, String>();

		/* Read existing files in the system */
		File[] exFiles = GenUtils.readDirectory(Species.getDataFolder(species),
				"gene_association*.gz", null);

		HashSet<Integer> existingEditions = new HashSet<Integer>();

		Integer maxEdition = 0;
		for (File f : exFiles) {
			Integer currEdition = Integer.valueOf(GenUtils.getEditionNo(f
					.getName()));
			existingEditions.add(currEdition);
			if (currEdition > maxEdition)
				maxEdition = currEdition;

		}
		Integer numberOfGOAfilesBefore = exFiles.length;

		if (species.equalsIgnoreCase(Species.FLY)) {
			String tmpdir = "";
			Integer curredition = 1;
			Integer currentYear = Calendar.getInstance().get(Calendar.YEAR);
			for (int y = 6; y <= currentYear - 2000; y++) {
				String year = String.valueOf(y);
				if (year.length() < 2)
					year = "0" + year;

				for (int m = 1; m <= 12; m++) {
					if (!existingEditions.contains(curredition)
							|| !edition2dates.exists()) {
						String month = String.valueOf(m);
						if (month.length() < 2)
							month = "0" + month;

						tmpdir = "/releases/FB20" + year + "_" + month
								+ "/precomputed_files/go/";
						System.out
								.println("Looking for edition " + curredition);
						HashMap<String, String> newdata2 = queryServerForGOAfiles(
								species, existingEditions, DOMAINFLY, tmpdir,
								"gene_association", curredition, month
										+ "/01/20" + year, null);
						curredition++;

						/* Update edition 2 dates for this server */
						if (!newdata2.isEmpty()) {
							updateEdition2Dates(species, newdata2);
							for (String date : newdata2.keySet()) {
								newdata.put(date, newdata2.get(date));
							}
						}
					} else {

						System.out.println("Edition " + curredition
								+ " is already in the local system");
						curredition++;
					}
				}
			}
		} else /* Special case for ECOLI */
		if (species.compareTo(Species.ECOLI) == 0) {
			String downloadedFileDate = "";
			int revisionNumber = 1;
			HashMap<String, String> downloadedFilesDates = new HashMap<String, String>();
			while (downloadedFileDate != null) {
				downloadedFileDate = downloadUsingHttpClient(DOMAINECOLI + "/"
						+ REMOTEDIR_ECOLI + "gene_association.ecocyc.gz?rev=1."
						+ revisionNumber, Species.getDataFolder(species) + "/"
						+ "gene_association.ecocyc." + revisionNumber + ".gz");
				if (downloadedFileDate != null
						&& downloadedFileDate.length() > 1)
					downloadedFilesDates.put("gene_association.ecocyc."
							+ revisionNumber + ".gz", downloadedFileDate);
				revisionNumber++;
			}
			HashMap<String, String> newdata2 = (HashMap<String, String>) renameEcoliFiles(downloadedFilesDates);
			updateEdition2Dates(species, newdata2);			
		} else {
			/* Query the servers looking for new editions */
			exFiles = GenUtils.readDirectory(Species.getDataFolder(species),
					"gene_association*.gz", null);
			existingEditions.clear();
			for (File f : exFiles) {
				Integer currEdition = Integer.valueOf(GenUtils.getEditionNo(f
						.getName()));
				existingEditions.add(currEdition);
			}

			newdata = queryServerForGOAfiles(species, existingEditions, DOMAIN,
					REMOTE_DIR + species.toUpperCase() + "/",
					"gene_association", maxEdition, null, newdata);
			/* Update edition 2 dates for this species */
			updateEdition2Dates(species, newdata);
		}
		// If there are new files, get the min new edition
		File[] exFiles2 = GenUtils.readDirectory(
				Species.getDataFolder(species), "gene_association*.gz", null);
		Integer numberOfGOAFiles = exFiles2.length;
		HashSet<String> newfiles = new HashSet<String>();
		for (File f : exFiles) {
			newfiles.add(f.getName());
		}

		if (numberOfGOAFiles != numberOfGOAfilesBefore) {
			Integer minnewedition = GenUtils.getTotalNumberOfEditions(exFiles);
			for (File f : exFiles2) {
				if (!newfiles.contains(f.getName())
						&& minnewedition > Integer.valueOf(GenUtils
								.getEditionNo(f.getName()))) {
					minnewedition = Integer.valueOf(GenUtils.getEditionNo(f
							.getName()));
				}
			}

			return minnewedition; // we have new files
		} else
			return -1;

	}

	/**
	 * Depending on the species this function query the ftp server looking for
	 * new files
	 * 
	 * @param species
	 *            Species to update
	 * @param existingFiles
	 *            Files that are already in the local system
	 * @param domain
	 *            FTP domain
	 * @param remotedir
	 *            FTP remote dir
	 * @param filewildcard
	 *            File wildcard
	 * @param maxEdition
	 *            Max edition number in the system
	 * @param flydate
	 * @param newdata
	 *            files that are updated
	 * @return
	 */
	public HashMap<String, String> queryServerForGOAfiles(String species,
			HashSet<Integer> existingFiles, String domain, String remotedir,
			String filewildcard, Integer maxEdition, String flydate,
			HashMap<String, String> newdata) {
		HashSet <String> datesInLocalSystem = null;
		File edition2dates = new File(Species.getDataFolder(species)
				.getAbsolutePath() + Species.TMP_FOLDER + "edition2Dates.txt");
		Boolean edition2datesDoesntexisit = edition2dates.exists();

		String originalflydate = flydate;
		System.out.println("Query server for " + species + " domain " + domain
				+ " remotedir " + remotedir);
		FTPClient client = new FTPClient();
		HashMap<String, String> edition2datesUpdate = new HashMap<String, String>();
		String working_dir = remotedir;
		Integer tries = 0;
		String localfileshort = "gene_association.goa_" + species + "."
				+ maxEdition + ".gz";
		if (species.equalsIgnoreCase(Species.FLY) && flydate != null) {
			edition2datesUpdate.put(flydate, String.valueOf(maxEdition));
		}
		if (species.equalsIgnoreCase(Species.YEAST)) {
			edition2datesUpdate = (HashMap<String, String>) 
					updateEdition2Dates(species, edition2datesUpdate);
			datesInLocalSystem = new HashSet<String>();
			for(String file: edition2datesUpdate.keySet()){				
				datesInLocalSystem.add(edition2datesUpdate.get(file));
			}
		}
		do {

			try {
				client.connect(domain);
				client.enterLocalPassiveMode();

				client.login("anonymous", "");
				client.changeWorkingDirectory(working_dir);
				client.setFileType(FTPClient.BINARY_FILE_TYPE);
				client.setFileTransferMode(FTPClient.COMPRESSED_TRANSFER_MODE);
				client.setBufferSize(1024 * 1024);

				FTPFile[] ftpFiles = client.listFiles();
				for (FTPFile ftpFile : ftpFiles) {
					tries = 0;
					try {
						if (ftpFile.getName().indexOf(filewildcard) == -1
								|| ftpFile.getName().contains("ref")) {
							continue;
						}

						Integer edition = 0;
						try {
							edition = Integer.valueOf(GenUtils
									.getEditionNo(ftpFile.getName()));
						} catch (NumberFormatException e) {
							edition = maxEdition;
						}
						localfileshort = "gene_association.goa_" + species
								+ "." + edition + ".gz";
						if(species.compareTo(Species.YEAST)==0){
							localfileshort = "gene_association.goa_" + species
									+ "." + (maxEdition+1) + ".gz";
						}
						String localFile = Species.getDataFolder(species) + "/"
								+ localfileshort;

						int year = 0;
						int day =0;
						String month = "00";
						Calendar cal;
						cal = ftpFile.getTimestamp();
						cal.add(Calendar.DATE, -1);
						cal.get(Calendar.YEAR);
						day = cal.get(Calendar.DAY_OF_MONTH)+1;
						month = String.valueOf(cal.get(Calendar.MONTH) + 1);
						year = cal.get(Calendar.YEAR);
												
						if (month.length() < 2)
							month = "0" + month;
						String serverFileDate = month+ "/" + day + "/" + year;
						if (species.equalsIgnoreCase(Species.FLY)
								&& flydate == null) {

							flydate = month + "/" + "01" + "/" + year;
							String finalGoaName = newdata.get(flydate);
							if (!finalGoaName.contains("gene")
									&& !finalGoaName.contains(GenUtils
											.getEditionNo(localfileshort))) {
								flydate = null;
								localfileshort = "gene_association.goa_"
										+ species + "." + finalGoaName + ".gz";
								localFile = Species.getDataFolder(species)
										+ "/" + localfileshort;
								edition = Integer.valueOf(finalGoaName);
							} else {
								System.out.println("Date " + flydate
										+ " points to " + finalGoaName
										+ " but we have " + localfileshort);
								flydate = null;
								continue;
							}
						}
						if (!edition2datesDoesntexisit
								|| (!existingFiles.contains(edition))) {
							/*
							 * The file is was not created when the program
							 * started to run, then update the file for this
							 * species
							 */
							edition2datesUpdate.put(localfileshort, month + "/"
									+ "01" + "/" + year);
							updateEdition2Dates(species, edition2datesUpdate);
						}
						
						if (!existingFiles.contains(edition) || 
								(species.equalsIgnoreCase(Species.YEAST) && 
										downloadThisYeastFile(serverFileDate, datesInLocalSystem))
										) {
							System.out.println("Attempting to download "
									+ ftpFile.getName() + " date " + month
									+ "/" + day + "/" + year);
							FileOutputStream fos = new FileOutputStream(
									localFile);

							boolean download = client.retrieveFile(
									ftpFile.getName(), fos);

							if (download) {

								System.out.println("File " + ftpFile.getName()
										+ " downloaded to local file "
										+ localFile + " successfully !");
								if(species.compareTo(Species.YEAST)==0){
									maxEdition++;
								}
							} else {
								System.out.println("Error in downloading file "
										+ ftpFile.getName());
								throw new IOException();
							}

							if (species.equalsIgnoreCase(Species.FLY)
									&& flydate != null) {
								edition2datesUpdate
										.put(flydate, localfileshort);
								fos.close();
								maxEdition++;
								return edition2datesUpdate;
							} else if (species.equalsIgnoreCase(Species.FLY)
									&& flydate == null) {

								flydate = month + "/" + "01" + "/" + year;
								String finalGoaName = newdata.get(flydate);
								if (!finalGoaName.contains("gene")) {
									finalGoaName = "gene_association.goa_fly."
											+ newdata.get(flydate) + ".gz";
									edition2datesUpdate.put(flydate,
											finalGoaName);
									flydate = null;
								}

							} else {

								edition2datesUpdate.put(localfileshort, month
										+ "/" + day + "/" + year);
							}
							fos.close();
							/* Update edition 2 dates for this species */
							updateEdition2Dates(species, edition2datesUpdate);
						}
						tries = 4;
					} catch (SocketTimeoutException e) {
						System.err.println(" Socket Error. Retrying ");
					} catch (FTPConnectionClosedException e) {
						System.err.println("FTP Error. Retrying ");
					} catch (Exception e) {
						System.err.println("Other Error. Retrying ");
					} finally {
						flydate = originalflydate;
						tries++;
					}
				}

				tries = 4;
			} catch (Exception e1) {
				System.err.println("Error. Retrying ");
				tries++;
			}
		} while (tries++ < 3);

		return edition2datesUpdate;
	}

	/**
	 * This function validates if a date is after the maximum date of a list of dates.
	 * For yeast edition2dates have the relationship of which files and dates are already in the
	 * system. If a provided date is bigger than the files in the system, this
	 * function returns true, false otherwise
	 * @param date Date that will be validated
	 * @param datesInLocalSystem List of dates in the system
	 * @return True if date is after the max date in datesInLocalSystem
	 */
	public Boolean downloadThisYeastFile(String date, Set<String> datesInLocalSystem){
		Calendar calendar = Calendar.getInstance();
		Calendar currentDate = Calendar.getInstance();
		Calendar maxDate = null;
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		try {
			currentDate.setTime(sdf.parse(date));
		} catch (ParseException e1) {			
			e1.printStackTrace();
		}
		for(String localdate : datesInLocalSystem){		
			StringTokenizer tok = new StringTokenizer(localdate,"/");
			String month = tok.nextToken();
			String day = tok.nextToken();
			String year = tok.nextToken();
			if(month.length()==1)
				month ="0"+month;
			if(day.length()==1)
				day = "0"+day;
			 try {
				 calendar = Calendar.getInstance();
				calendar.setTime(sdf.parse(month+"/"+day+"/"+year));
				/*Get max date*/
				if(maxDate==null || maxDate.before(calendar)){
					maxDate=calendar;
				}
			} catch (ParseException e) {				
				e.printStackTrace();
			}
			
		}
		//System.out.println(sdf.format(currentDate.getTime())+" "+sdf.format(maxDate.getTime()));
		if(currentDate.compareTo(maxDate)>0){	
			/*currentDate is after maxDate*/
			return true;
		}
		return false;
	}
	
	/**
	 * This function updates the edtion2dates file using the information in
	 * newdata and the existing information in the file
	 * 
	 * @param species
	 * @param newdata
	 * @return
	 */
	public Map<String, String>  updateEdition2Dates(String species,
			HashMap<String, String> newdata) {
		System.out.println("Update edition2Dates.txt for " + species);
		try {
			BufferedReader edition = Species.getReader(species,
					"edition2Dates.txt");

			String line;
			while ((line = edition.readLine()) != null) {
				String[] columnDetail = new String[2];
				columnDetail = line.split("\t");
				if (columnDetail.length < 2)
					continue;
				if (species.compareTo(Species.FLY) == 0) {
					newdata.put(columnDetail[1], columnDetail[0]);
				} else
					newdata.put(columnDetail[0], columnDetail[1]);
			}
			edition.close();
		} catch (FileNotFoundException e1) {
			System.err
					.println("File edition2Dates.txt doesn't exists. Creating it.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					Species.getDataFolder(species).getAbsolutePath()
							+ Species.TMP_FOLDER + "edition2Dates.txt")));
			for (String file : newdata.keySet()) {
				if (species.compareTo(Species.FLY) == 0) {
					if (newdata.get(file).contains("gene"))
						bw.write(newdata.get(file) + "\t" + file + "\n");
				} else
					bw.write(file + "\t" + newdata.get(file) + "\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newdata;
	}

	/**
	 * Downloads new termb files
	 * 
	 * @param directory
	 *            remote directory
	 */
	public void updateTermdb(String directory) {
		Integer tries = 0;
		System.out.println("Query server for new termdb: domain "
				+ DOMAINTERMDB + " remotedir " + directory);
		FTPClient client = new FTPClient();
		String working_dir = directory;
		try {
			client.connect(DOMAINTERMDB);
			client.enterLocalPassiveMode();

			client.login("anonymous", "");
			client.setFileType(FTPClient.BINARY_FILE_TYPE);
			client.setFileTransferMode(FTPClient.COMPRESSED_TRANSFER_MODE);
			client.setBufferSize(1024 * 1024);

			Integer currentYear = Calendar.getInstance().get(Calendar.YEAR);
			for (int y = 2001; y <= currentYear; y++) {
				for (int m = 1; m <= 12; m++) {
					String months = String.valueOf(m);
					if (months.length() < 2)
						months = "0" + months;
					System.out.println("Searching files for " + y + "-"
							+ months);
					int maxday = 31;
					if (directory.compareTo(REMOTE_DIR_TERMDB) == 0)
						maxday = 2;
					for (int d = 1; d <= maxday; d++) {
						String days = String.valueOf(d);
						if (days.length() < 2)
							days = "0" + days;

						String localfileshort = "go_daily-termdb.rdf-xml." + y
								+ "-" + months + ".gz";
						String localFile = Species.getTermDBFolder() + "/"
								+ localfileshort;
						File t = new File(localFile);
						if (t.exists()) {
							System.out.println("Files are updated");
							d = 32;
							continue;
						}
						boolean dirExitst = client
								.changeWorkingDirectory(working_dir + y + "-"
										+ months + "-" + days);
						if (!dirExitst) {

							continue;
						}
						d = 32;
						FTPFile[] ftpFiles = client.listFiles();
						for (FTPFile ftpFile : ftpFiles) {
							tries = 0;
							try {
								if (!ftpFile.getName().contains(
										"termdb.rdf-xml")
										&& !ftpFile.getName().contains(
												"termdb.xml")) {
									continue;
								}

								localfileshort = "go_daily-termdb.rdf-xml." + y
										+ "-" + months + ".gz";
								localFile = Species.getTermDBFolder() + "/"
										+ localfileshort;

								System.out.println("Attempting to download "
										+ ftpFile.getName() + " date " + months
										+ "/" + "01" + "/" + y);
								FileOutputStream fos = new FileOutputStream(
										localFile);
								boolean download = client.retrieveFile(
										ftpFile.getName(), fos);
								if (download) {
									System.out.println("File "
											+ ftpFile.getName()
											+ " downloaded to local file "
											+ localFile + " successfully !");
								} else {
									System.out
											.println("Error in downloading file "
													+ ftpFile.getName());
									throw new IOException();
								}

								fos.close();

								tries = 4;
							} catch (SocketTimeoutException e) {
								System.err.println(" Socket Error. Retrying ");
							} catch (FTPConnectionClosedException e) {
								System.err.println("FTP Error. Retrying ");
							} catch (Exception e) {
								System.err.println("Other Error. Retrying ");
							} finally {
								tries++;
							}
						}
					}
				}
			}
		} catch (SocketTimeoutException e) {
			System.err.println(" Socket Error. Retrying ");
		} catch (FTPConnectionClosedException e) {
			System.err.println("FTP Error. Retrying ");
		} catch (Exception e) {
			System.err.println("Other Error " + e.getMessage() + ". Retrying ");
			e.printStackTrace();
		} finally {
			tries++;
		}
	}

	/**
	 * Download a file from a web page using an http request
	 * 
	 * @param url
	 *            The URL of the file
	 * @param ouputfile
	 *            Where it's going to be stored locally
	 * @return null if there is no more data in the server, empty string if the file couldn't be downloaded
	 * returns the last modified date if the file could be downloaded
	 */
	public String downloadUsingHttpClient(String url, String ouputfile) {
		File file = new File(ouputfile);
		String date = null;
		try {
			file.createNewFile();
			OutputStream stream = new DataOutputStream(new FileOutputStream(
					file));
			BufferedInputStream in = new BufferedInputStream(
					new URL(url).openStream());
			URLConnection connection = new URL(url).openConnection();
			long lastModified = connection.getHeaderFieldDate("Last-Modified",
					0l);
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			date = sdf.format(lastModified);
			byte data[] = new byte[1024];
			int count;
			System.out.println("Attempting to download file " + url);
			boolean hasData = false;
			while ((count = in.read(data, 0, 1024)) != -1) {
				stream.write(data, 0, count);
				hasData = true;
			}
			stream.close();
			if (hasData) {
				System.out.println("Download successful " + ouputfile);
				return date;
			}
		} catch (IOException e) {
			return null;
		}

		return "";
	}

	/**
	 * For ecoli a set of ecocyc files are downloaded, this function will rename that files to
	 * gene_association.goa_ecoli.*
	 * 
	 * @param edition2dates Map of goa file to a date of the downloaded files
	 * @return Map of new goa name to a date
	 */
	public Map<String, String> renameEcoliFiles(
			Map<String, String> edition2dates) {
		HashMap<String, String> newdata = new HashMap<String, String>();
		File[] ecolifiles = GenUtils.readDirectory(
				Species.getDataFolder(Species.ECOLI),
				"gene_association.ecocyc*.gz", null);
		int fileCount = 1;
		for (File downloadedFile : ecolifiles) {
			File exisitingFile = new File(Species.getDataFolder(Species.ECOLI)
					+ "/gene_association.goa_ecoli." + fileCount + ".gz");
			fileCount++;
			if (edition2dates.get(downloadedFile.getName()) != null) {
				newdata.put(exisitingFile.getName(),
						edition2dates.get(downloadedFile.getName()));
				System.out.println("File " + exisitingFile.getName()
						+ " last modified on :"
						+ edition2dates.get(downloadedFile.getName()));
				downloadedFile.renameTo(exisitingFile);
			}
			if (exisitingFile.exists()) {
				downloadedFile.delete();
				continue;
			}
		}
		return newdata;
	}

}
