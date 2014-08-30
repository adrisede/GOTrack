package ubc.pavlab.gotrack.utils;
 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.GOtrack;
 
/**
 * This class implements several auxiliary functions
 *
 * @author asedeno
 * @version $Id$
 */
public class GenUtils {
 
	/**This is the generic domain to download the files for all species*/
    private static String DOMAIN = "ftp.ebi.ac.uk";
    
    /** Fly is a special case, it has its own domain*/
    private static String DOMAINFLY = "ftp.flybase.org";
    
    /**The remote directory where the GOA files are stored*/
    private static String REMOTE_DIR = "pub/databases/GO/goa/old/";
    
    /**The yeast dictionary made from idmapping.yeast.SGD.txt*/
    private static Map<String, String> yeastDictionary;
 
    /**This is the edition to dates dictionary. It maps an edition to a publication date*/
    public static Map<String, Calendar> matchdates2edition;
 
    /**
     * This function reads all files in a directory that has termination
     * filetype
     * 
     * If the file is a GOA file or a gomatrix file, the array will be ordered by edition
     *
     * @param indir
     *            Directory we will look at
     * @param filetype
     *            Regular expression to filter files
     * */
    public static File[] readDirectory(File dir, String filetype, Logger log) {
        File[] files = null;
        try {
            FileFilter fileFilter = new WildcardFileFilter(filetype);
            files = dir.listFiles(fileFilter);
            if (files != null) {
                if (filetype.contains("gene_association") || filetype.compareTo("*gomatrix.txt") == 0) {
                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File o1, File o2) {
                            String e1 = getEditionNo(o1.getName());
                            String e2 = getEditionNo(o2.getName());
                            Integer ed1 = Integer.valueOf(e1);
                            Integer ed2 = Integer.valueOf(e2);
 
                            if (ed1 == ed2)
                                return 0;
                            if (ed1 > ed2)
                                return 1;
                            return -1;
                        }
                    });
                } else {
                    Arrays.sort(files);
                }
            } else {
                log.severe("Fatal error. No files " + filetype
                        + "in directory " + dir.getAbsolutePath());
            }
        } catch (NullPointerException e) {
            log.severe("There are no files to read in the directory. (readDirectory)"
                    + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return files;
    }
 
    /**
     * Parse gene association file to get edition number
     *
     * @param geneAssociationFile
     * */
    public static String getEditionNo(String geneAssociationFile) {
        String edition = geneAssociationFile.replaceAll("\\D+", "");
        return edition;
 
    }
 
    /**
     * This is a special function to make the edition2dates file for
     * fly. We have to use a different server and traverse multiple directories
     *
     * @param client Ftp client where the files are stored
     * @param bw  Buffer where we will write the edition2dates file
     * */
    public static void getFlyFiles(FTPClient client, BufferedWriter bw ){      
        String tmpdir;
        try {          
            for(int y=6; y<=13; y++){
                for(int m=1; m<=12; m++){
                    tmpdir = "/releases/FB20"+y+"_"+m+
                                 "/precomputed_files/go/";
                    client.changeWorkingDirectory(tmpdir);
                  FTPFile[] ftpFiles = client.listFiles();
                  for (FTPFile ftpFile : ftpFiles) {
                      if (ftpFile.getName().indexOf("gene_association") == -1) {
                          continue;
                      }                  
                        bw.write(ftpFile.getName()+"\t"+m+"/1"+"/"+ y+ "\n");                                  
                  }
                  client.changeWorkingDirectory("/");
                }
            }      
        } catch (IOException e) {          
            e.printStackTrace();
        }
    }
     
     
    /**
     * Create edition2date file for geneAssociation files using last modified
     * date
     *
     * @param domain
     *            FTP domain
     * @param working_dir
     *            Directory in FTP server to look at
     * @param dir
     *            Directory where edition2Dates.txt file will be saved
     * @throws Exception
     * */
    public static String createEdition2DatesFile(String species, Logger log)
            throws Exception {
        FTPClient client = new FTPClient();
 
    try{
            if (species.equalsIgnoreCase(Species.YEAST) ||
                    species.equalsIgnoreCase(Species.FLY)||
                    species.equalsIgnoreCase(Species.ECOLI))
            {
                String pathToEditions = Species.getDataFolder(species)
                     .getAbsolutePath()
                      + Species.TMP_FOLDER
                      + "edition2Dates.txt";
            File edition2Dates = new File(pathToEditions);
            if (edition2Dates.exists()) {
                 return "edition2Dates.txt";
            } else
            {
                throw new Exception(
                    "You need the file: \"edition2Dates.txt\" to continue the analysis for the species: "
                            + species);
            }
            }else
            {
                BufferedWriter bw = Species.getWriter(species,
                        "edition2Dates.txt", GOtrack.overwriteExistingFiles);
              if (species.equalsIgnoreCase(Species.FLY)) {             
                   
                  log.info("Connecting and retrieving files in " + DOMAINFLY
                        + ", this might take a while...");
                  client.connect(DOMAINFLY);
                  client.enterLocalPassiveMode();
                  client.login("anonymous", "");
                         
                  //call special routine for fly
                  getFlyFiles(client, bw);
 
              } else
              {            
                  String working_dir = REMOTE_DIR + species.toUpperCase() + "/";
                  log.info("Connecting and retrieving files in " + DOMAIN
                        + ", this might take a while...");
                  client.connect(DOMAIN);
                  client.enterLocalPassiveMode();
                  client.login("anonymous", "");
                  client.changeWorkingDirectory(working_dir);
               
                  FTPFile[] ftpFiles = client.listFiles();
                  for (FTPFile ftpFile : ftpFiles) {
                      if (ftpFile.getName().indexOf("gene_association") == -1) {
                          continue;
                      }
                      if(ftpFile.getName().contains("_ref_"))
                      	continue;
                      if (ftpFile.getType() == FTPFile.FILE_TYPE) {
                          Calendar cal = ftpFile.getTimestamp();
                          cal.add(Calendar.DATE, -1);
                          int year = cal.get(Calendar.YEAR);
                          String month = String
                                .valueOf(cal.get(Calendar.MONTH) + 1);
                          bw.write(ftpFile.getName() + "\t" + month + "/"
                                + cal.get(Calendar.DAY_OF_MONTH) + "/" + year
                                + "\t"+getEditionNo(ftpFile.getName())+"\n");
                      }
                     
                  }
              } 
                client.logout();
                bw.close();
                return "edition2Dates.txt";
            }
             
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
        } catch (RuntimeException e) {
            log.info(e.getMessage());
            return "edition2Dates.txt";
        } finally {
 
            try {
                client.disconnect();
            } catch (IOException e) {
                log.severe(e.getLocalizedMessage());
            }
        }
 
        return null;
    } // edition 2 dates
 
    /**
     *  Reads all genes and get not repeated elements
     * @param species Species to compute
     * @param fs     Array of files to compute
     * @param log    Logger
     * @param threshold Threhsold to include or not a gene to GenesAlmostAlwaysPresent
     * @return true if there is data in GenesAlmostAlwaysPresent
     */
    public static boolean createAllGenesFiles(String species, File[] fs, Logger log,Integer threshold) {
       boolean almostAlwaysHasData=false;
        try {
            BufferedWriter bw = Species.getWriter(species,
                    "GenesAlwaysPresent.txt");
 
            BufferedWriter bw2 = Species.getWriter(species, "AllGenes.txt");
 
            BufferedWriter bw3 = Species.getWriter(species,
                    "GenesAlmostAlwaysPresent.txt");
 
            Map<String, int[]> genes = new HashMap<String, int[]>();
 
            // Traverse geneAssociation files
            for (int i = 0; i < fs.length; i++) {
                BufferedReader br;
 
                try {
                    br = Species.getGzReader(fs[i]);
                    String line;
                    // Read line by line
                    while ((line = br.readLine()) != null) {
                        String[] columnDetail = new String[11];
                        columnDetail = line.split("\t");
                        if (columnDetail.length < 4)
                            continue;
                        //We won't take those rows where NOT is written on the 3rd column
              					if (columnDetail[3].equals("NOT")) {
                          continue;
                        }
                        String uniprot = columnDetail[1];
                        if(uniprot.compareTo("")==0)
                      		continue;                       
                        // Add gene to allGenes hash table if not already there
                        if (!genes.containsKey(uniprot)) {
                            int[] array = new int[fs.length];
                            Arrays.fill(array, 0);
                            array[i] = 1;
                            genes.put(uniprot, array);
                        } else {
                            int[] tm = genes.get(uniprot);
                            genes.remove(uniprot);
                            tm[i] = tm[i] + 1;
                            genes.put(uniprot, tm);
                        }
 
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    log.severe(e.getLocalizedMessage());
 
                } catch (IOException e) {
                    log.severe(e.getLocalizedMessage());
 
                }
            }// Assoc
 
            // Print to file
            for (String key : genes.keySet()) {
                genes.get(key);
                // This gene is present in all editions (0 as threshold)
                if (isArrayFull(genes.get(key), genes.get(key).length-fs.length)) {
                    bw.write(key + "\n");
                }
                // This gene is present in almost all editions (it might not
                // be present in 5 editions
                if (isArrayFull(genes.get(key), threshold)){
                    bw3.write(key + "\n");
                    almostAlwaysHasData = true;
                }
                bw2.write(key + "\n");
            }
 
            bw.close();
            bw2.close();
            bw3.close();
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
        } catch (RuntimeException e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
        return almostAlwaysHasData;
 
    }
 
    /**
     *This function will scan an array and tell how full it is
     *Function called by createAllGenesFiles to see if a gene is included depending on the threshold
     * @param array
     * @param threshold  The gene can be absent in this number of editions
     * @return true if the number of missing elements is less than the threshold, false otherwise
     */
    private static boolean isArrayFull(int[] array, int threshold) {
         
        int count = 0;
        for (int i = 0; i < array.length; i++)
            if (array[i] == 0)
                count++;
        // if threshold is 0 -> gene is in all editions
        // because count will be more than one (gene not present in more than
        // one edition)
        if (count <= threshold)
            return true;
        else
            return false;      
    }
 
    /**
     * Return whether is an old or new edition
     * New editions are newer than year 2005.
     * This is important because the parser in erminej varies depending 
     * on the version
     * @param species Species to compute
     * @param geneAssoc The GOA file to compute
     * @param matchEditionDateFile edition2dates file
     * @param log Logger
     * @return true if year < 2005 false otherwise
     * */
    public static synchronized String getEditionType(String species,
            String geneAssoc, String matchEditionDateFile, Logger log) {
        try {
            // populate map only once
            if (matchdates2edition == null) {
                matchdates2edition = readMatchingFile(species,
                        matchEditionDateFile, log);
            }
            Calendar cal = matchdates2edition.get(geneAssoc);          
 
            int year = cal.get(Calendar.YEAR);
            if (year<2000)
                year+=2000;
             
            if (year < 2005)
                return "TRUE";
            else
                return "FALSE";
 
        } catch (Exception e) {
            return "FALSE";
        }
    }
 
    /**
     * Match edition with date using given file
     *
     *@param species Species to compute
     * @param dir
     *            Directory where file is stored
     * @param edition2datesFile
     *            Matching file
     * @return A Map of GOA file names to its corresponding date as a Calendar
     *
     * */
    public static Map<String, Calendar> readMatchingFile(String species,
            String edition2datesFile, Logger log) {
        Map<String, Calendar> dictionary = new HashMap<String, Calendar>();
        try {
            BufferedReader b;
            b = Species.getReader(Species.getTmpFolder(species)
                    .getAbsolutePath() + "/" + edition2datesFile);
            String line;
            while ((line = b.readLine()) != null) {
                String[] columnDetail = new String[11];
                columnDetail = line.split("\t");
                if (columnDetail.length >= 2) {
                    Calendar calendar = Calendar.getInstance();
                    String[] dt = new String[3];
                    dt = columnDetail[1].split("/");
                    calendar.set(Integer.valueOf(dt[2]),
                            Integer.valueOf(dt[0]) - 1, Integer.valueOf(dt[1]));
                    dictionary.put(columnDetail[0], calendar);
                }
            }
            b.close();
            return dictionary;
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
            return null;
        }
    }
 
    /**
     *
     * This function maps an old id to an Uniprot id for Yeast
     * @param uniprot Id to map
     * @return returns the same Id if the id is valid or the matching uniprot 
     * if the id was replaced in the dictionary
     */
    public static String mapUniprotYeast(String uniprot) {
 
        if (yeastDictionary == null) {
 
            BufferedReader reader;
            try {
                reader = Species.getReader(Species.YEAST, "idmapping.yeast.SGD.txt");
                String line;
                yeastDictionary = new HashMap<String, String>();
                while ((line = reader.readLine()) != null) {
                    String[] details = line.split("\t");
                    yeastDictionary.put(details[1], details[0]);
                }
                reader.close();
            } catch (FileNotFoundException e) {
                return uniprot;
 
            } catch (IOException e) {
                return uniprot;
 
            }
        }// oldYeast complete
 
        return yeastDictionary.get(uniprot);
    }
 
    /**
     * This function gets only the gene id's of the parent file*
     *
     * @param dir
     *            Where the parent file can be found
     * @param parentFile
     *            File to operate
     * @param edition
     *            Edition number
     *
     * @return "genes."+edition+".txt"
     */
    public static String getOnlyGenes(String species, String parentFile,
            String isOldEdition, int editionNumber, Logger logger) {
        String newName;
        String edition = editionNumber + "";
 
        Boolean isLastEdtion = Boolean.valueOf(isOldEdition);
        if (!isLastEdtion) {
            newName = "genes." + edition + ".txt";
        } else {
 
            newName = "genesLastEdition.txt";
 
        }
        try {
            BufferedReader reader = Species.getReader(species, parentFile);
            BufferedWriter writerLastEdition = null;
            if (isLastEdtion) {
                writerLastEdition = Species.getWriter(species, "genes."
                        + edition + ".txt");
            }
 
            BufferedWriter bwriter = Species.getWriter(species, newName);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columnDetail = new String[11];
                columnDetail = line.split("\t");
                String uniprot = columnDetail[0];
                String iea = columnDetail[1];
                           
                if (species.equalsIgnoreCase("yeast") && editionNumber <= 105) {
                    String oldUniprot = uniprot;
                    uniprot = GenUtils.mapUniprotYeast( uniprot);
                    if (uniprot == null) { // it do not exist in maping file
                        uniprot = oldUniprot;
                    }
                }
                bwriter.write(uniprot + "\t" + iea + "\n");
                if (isLastEdtion) {
                    writerLastEdition.write(uniprot + "\t" + iea + "\n");
                }
            }
            if (isLastEdtion) {
                writerLastEdition.close();
            }
            bwriter.close();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            logger.severe(e.getMessage());
        }
        return newName;
    }
 
    /**
     * This function calculates the name of the termdb file associated with a
     * gene association file using matchEditionDateFile
     *
     * @param termdbdir
     *            Where termdb files are found
     * @param dir
     *            Where result will be saved
     * @param geneAssoc
     *            gene association file
     * @param matchEditionDateFile
     *            File that matches an edition to its date
     * */
    public static String getTermdbFromGeneAssoc(String species, File assoc,
            Logger logger) {
        Map<String, Calendar> matchdates2edition = GenUtils.matchdates2edition;
 
        Calendar cal = (Calendar)matchdates2edition.get(assoc.getName()).clone();
 
        int year = cal.get(Calendar.YEAR);
        if(year<2000)
            year+=2000;
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        if (month.length() == 1)
            month = "0" + month;
        String res = "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";
 
        if (!Species.termDBFile(res).exists()) {
            cal.add(Calendar.MONTH, -1);
            year = cal.get(Calendar.YEAR);
            if(year<2000)
                year+=2000;
            month = String.valueOf(cal.get(Calendar.MONTH) + 1);
            if (month.length() == 1)
                month = "0" + month;
            logger.warning("Problem. File " + res + " is not found. Trying "
                    + "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz");
        }
        return "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";
    }
 
    /**
     * Gets the symbols for each gene in GenesAlwaysPresent. Create file GenesAlwaysPresentWithSymbols.txt
     * @param species Species to compute
     * @param logger Where to write the progress
     * @return true if the file was created
     */
    public static void genesAlwaysPresentWithSymbols(String species,
            Logger logger) {
        try {
            BufferedReader always = Species.getReader(species,
                    "GenesAlwaysPresent.txt");
 
            BufferedReader last = Species.getReader(species,
                    "genesLastEdition.txt");
 
            BufferedWriter alwaysWithSymbols = Species.getWriter(species,
                    "GenesAlwaysPresentWithSymbols.txt");
            // always have just one column.
            // last contains map of uniprot, symbol
            Map<String, String> mapLastEdition = new HashMap<String, String>();
 
            String line; // LAST EDITION
            while ((line = last.readLine()) != null) {
                String[] columnDetail = new String[2];
                columnDetail = line.split("\t");
                
              //We won't take those rows where NOT is written on the 3rd column
      					if (columnDetail[3]!=null && columnDetail[3].compareTo("NOT")==0) {
                  continue;
                }
                // uniprot, symbol
                mapLastEdition.put(columnDetail[0], columnDetail[1]);
            }
 
            // WITH SYMBOLS
            while ((line = always.readLine()) != null) {
                alwaysWithSymbols.write(line + "\t" + mapLastEdition.get(line)
                        + "\n");
            }
 
            always.close();
            last.close();
            alwaysWithSymbols.close();
            
        } catch (Exception e) {
            logger.severe(e.getMessage()
                    + " SKIPPING, file has not been created");
        }
        
    }
 
    /**
     * Return the maximum edition of a list of files
     * @param files Gene association files
     * @return Max edition in the list
     */
    public static Integer getTotalNumberOfEditions(File[] files){
    	int max=0;
    	for(File f : files){
    		if(max<=Integer.valueOf(getEditionNo(f.getName()))){
    			max = Integer.valueOf(getEditionNo(f.getName()));
    		}
    	}
    	return max;
    }
    
    /**
     * Return the maximum edition of a list of files
     * @param files Gene association files
     * @return Max edition in the list
     */
    public static Integer getTotalNumberOfEditions(ArrayList<File> files){
    	int max=0;
    	for(File f : files){
    		if(max<=Integer.valueOf(getEditionNo(f.getName()))){
    			max = Integer.valueOf(getEditionNo(f.getName()));
    		}
    	}
    	return max;
    }
    
    /**
     * Given a list of files if will get which editions are missing
     * @param files List of gene association files
     * @return Array that has a 1 in the position i if the edition is missing
     */
    public static Integer[] missingEditions(File[] files){
    	Integer total= getTotalNumberOfEditions(files);
    	Integer[] missingEds = new Integer[++total];
    	Arrays.fill(missingEds, -1);
    	Integer ed=0;
    	for(int i=0; i<files.length;i++){
    		ed = Integer.valueOf(getEditionNo(files[i].getName()));
    		missingEds[ed]=1;
    	}
    	    	
    	return missingEds;
    }
    
    /**
     * Get the date in the filename
     * @param filename
     * @return String that contains the date
     */
    public static String getDateFromFile(String filename){
    	String date = "";
    	// date should be in format 'YYYY-MM-DD'
    	date = filename.substring(11, 18);
    	date = date +"-01";
    	//Extract date from filename
    	
    	//parse it
    	
    	
    	return date;
    }
    
    /**
     *  We are using the parent file for this edition to read all the inferred parents and load them to the database
     * @param species Species to compute
     * @param edition Edition to compute
     * @param log Where to keep track of the progress
     * @return Map of goterm and the number of parents
     */
   public static Map<String, Integer> readAndCountParents(String species, String edition, Logger log){
	   Map<String, Integer> count = new HashMap<String, Integer>();	   
	   
	   try {			
		String parseGoParents = Species.getDataFolder(species)+"/tmp/gene_association.goa_"+species+"."+edition+".gz.parents";
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(parseGoParents)));
		String tmp;
		String[] columnDetail = new String[100];
		while ((tmp = in.readLine()) != null) {
			Arrays.fill(columnDetail, "");
			columnDetail = tmp.split("\t");
			if (columnDetail.length >= 2) {
				StringTokenizer st = new StringTokenizer(columnDetail[3],"|");
				count.put(columnDetail[2], st.countTokens());
			}
			
		}
		in.close();
		
	} catch (Exception e) {
		log.severe("Error reading parents file");
		log.severe(e.getMessage());		
	}
	   
	   return count;
   }
   
   /**
	 * This function reads all files in a directory that has termination
	 * filetype
	 * 
	 * @param indir
	 *            Directory we will look at
	 * @param filetype
	 *            Regular expression to filter files
	 *            @return list of files that match the expression
	 * */
	public static ArrayList<File> readDirectory(String dir, String filetype) {
		File[] files = null;
		ArrayList<File> arrayOfFiles = null;
		File directory = new File(dir);
		try {
			FileFilter fileFilter = new WildcardFileFilter(filetype);
			files = directory.listFiles(fileFilter);
			Arrays.sort(files);
			if (files != null) {
				arrayOfFiles = new ArrayList<File>(files.length);
				for (int i = 0; i < files.length; i++) {
					arrayOfFiles.add(files[i]);
				}
			} else {

			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return arrayOfFiles;
	}
	
	/**
	 * Auxiliar function to rename a .gz file to .syn 
	 * This function is used to create a temporary mapping files *syn
	 * @param fileName
	 * @return filename with .syn extension
	 */
	public static String renameGZFile(String fileName) {
		return fileName.substring(0, fileName.indexOf(".gz.syn"));
	}
		
	
}