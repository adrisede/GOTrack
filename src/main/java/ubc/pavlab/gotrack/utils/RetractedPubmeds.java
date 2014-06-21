package ubc.pavlab.gotrack.utils;
 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;
 
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.GOtrack;
 
import com.google.common.collect.HashMultimap;
 
/**
 *
 * @author asedeno
 *
 */
public class RetractedPubmeds {
     
    /**
     * @param args
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        // runSpecies(Species.HUMAN);
        // runForSpecies(); // THIS WILL RUN ALL SPECIES
         
        subsetOrdered();
        long endTime = System.currentTimeMillis();
        long time = ((endTime - startTime) / 1500); // seconds
        System.out.println("Analysis COMPLETED IN: " + time + " SECONDS");
    }
     
    /**
     *
     * @param species
     */
    private static void subsetOrdered() {
        try {
            Species speciesObj = new Species();
            for (final String species : speciesObj.getSpecies()) {
                try {
                    subsetOrderedAux(species);
                } catch (Exception e) {
                    logger.severe("Analysis could not be completed for: "
                            + species);
                }
                 
            }
             
        } catch (Exception e) {
            logger.severe(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
     
    /**
     *
     * @param species
     * @throws IOException
     */
    public static void subsetOrderedAux(String species) throws IOException {
        BufferedReader reader = Species.getReader(species,
                "pubmedDatesMapping.txt");
         
        String header = reader.readLine();
        String line = "";
        TreeMap<String, String> sortedSet = new TreeMap<String, String>();
        while ((line = reader.readLine()) != null) {
            String[] details = line.split("\t");
             
            String pubmed = details[0];
            String key = getKey(pubmed);
            sortedSet.put(key, line);
            // System.out.println(key);
        }
         
        Collection<String> list = sortedSet.values();
        BufferedWriter writer = Species.getWriter(species,
                "pubmedDatesMapping-Sorted.txt");
         
        writer.write(header + "\n");
        for (String pubmedRow : list) {
            writer.write(pubmedRow + "\n");
            writer.flush();
        }
        writer.close();
    }
     
    /**
     *
     * @param pubmed
     * @return
     */
    private static String getKey(String pubmed) {
        try {
            String date = pubmed.substring(pubmed.indexOf(":") + 1);
            String[] details = date.split("/");
            String month = details[0];
            String day = details[1];
            String year = details[2];
            return year + day + month + pubmed;
        } catch (Exception e) {
            return "-no-date";
        }
    }
     
    static Logger logger = Logger.getLogger(GOtrack.class.getName());
     
    /**
     *
     */
    public static void runForSpecies() {
        Species speciesObj = new Species();
        for (final String species : speciesObj.getSpecies()) {
             
            runSpecies(species);
        }
    }
     
    /**
     *
     * @param species
     */
    private static void runSpecies(String species) {
        try {
            // RETRIEVE ASYNCHRONOUSLY THE RETRACTED PUBMED FILES.
            // http connections, might take a while.
            File[] assoc = GenUtils
                    .readDirectory(Species.getDataFolder(species),
                            "gene_association*", logger);
            // generatePubmedFromWebServices(species, assoc);
             
            generatePubmedMapping(species, assoc);
        } catch (Exception e) {
            logger.info("Analysis could not be completed." + e.getMessage());
            e.printStackTrace();
        }
    }
     
    /**
     * Work with edition 2 dates, if this file do not exist, create it.
     *
     * @return
     * @throws Exception
     */
    public static Map<Integer, String> getEditionDate(String species,
            Logger logger) throws Exception {
        BufferedReader edition2dates;
        TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
        try {
            edition2dates = Species.getReader(species, "edition2Dates.txt");
        } catch (Exception e) {
            GenUtils.createEdition2DatesFile(species, logger);
            edition2dates = Species.getReader(species, "edition2Dates.txt");
        }
         
        String line;
         
        while (((line = edition2dates.readLine()) != null)) {
            String[] details = line.split("\t");
            Integer key = extractNumber(details[0]);
            String value = details[1];
            treeMap.put(key, value);
        }
         
        return treeMap;
    }
     
    /**
     *
     * @param array
     * @return
     */
    public <T extends Number> int countRow(T[] array) {
        int count = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i].intValue() == 1) {
                count++;
            }
        }
        return count;
    }
     
    /**
     * Will generate each one of the connections
     *
     * @param species
     * @param fs
     * @throws Exception
     */
    public static void generatePubmedMapping(final String species,
            final File[] fs) throws Exception {
         
        HashSet<String> pubmeds1 = new HashSet<String>();
        HashSet<String> map = new HashSet<String>();
        HashMultimap<String, Integer> pubmedEdition = HashMultimap.create();
         
        Map<Integer, String> mapper = getEditionDate(species, logger);
         
        String header = "pubmed\t";
         
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return extractNumber(o1.getName()).compareTo(
                        extractNumber(o2.getName()));
            }
        });
        // Traverse geneAssociation files
        for (int i = 0; i < fs.length; i++) {
             
            int editionNumber = extractNumber(fs[i].getName());
            header += editionNumber + ":" + mapper.get(editionNumber) + "\t";
            final int n = i;
             
            try {
                 
                // i // assoc file in which we are right now
                BufferedReader br;
                br = Species.getGzReader(fs[n]);
                 
                System.out.println("Reading pubmeds for file: "
                        + fs[n].getName());
                 
                String edition = "" + getNo(fs[n].getName());
                String line;
                // Read line by line
                 
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("!") != -1) {
                        continue; // skip comments
                    }
                     
                    // Verify for retracted synchronously (Might take a
                    // while)
                    if ((line.indexOf("PUBMED:") != -1)
                            || (line.indexOf("PMID:") != -1)) { // pubmed
                                                                // editions
                        String[] columnDetail = new String[11];
                        columnDetail = line.split("\t");
                        // Sanity check. Make sure we have read 3 columns or
                        // more
                        if (columnDetail.length <= 5) {
                            continue;
                        }
                        String uniprot = columnDetail[1];
                        String symbol = columnDetail[2];
                        String goAnnotation = columnDetail[4];
                        String pubmed = getNo(columnDetail[5]);
                        String key = pubmed + "\t" + uniprot + "\t" + symbol
                                + "\t" + goAnnotation + "\t" + edition;
                         
                        if (uniprot.equals("")) {
                            continue;
                        }
                        if (symbol.equals("")) {
                            continue;
                        }
                        if (goAnnotation.equals("")) {
                            continue;
                        }
                        if (pubmed.equals("")) {
                            continue;
                        }
                        if (!map.contains(key)) {
                            map.add(key);
                        }
                        if (!pubmeds1.contains(pubmed)) {
                            pubmeds1.add(pubmed);
                        }
                         
                        if (!pubmedEdition.containsEntry(pubmed, edition)) {
                             
                            pubmedEdition.put(pubmed, Integer.valueOf(edition));
                        }
                         
                    }// if exists pubmed
                }// while
                br.close();
                 
            } catch (FileNotFoundException e) {
                System.err.println(e.getLocalizedMessage());
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
            } finally {
                 
                // System.out.println("DONE FOR EDITION: " +
                // getNo(fs[n].getName()));
                try {
                     
                } catch (Throwable e) {
                    // e.printStackTrace();
                }
            }
        }// for
         
        // Time for the header.
        BufferedWriter mapping = Species.getWriter(species,
                "pubmedDatesMapping.txt");
         
        mapping.write(header + "\n");
        // mash up the values in an array.
         
        for (String pubmed : pubmeds1) {
            String row = "";
            int[] array = new int[extractNumber(fs[fs.length - 1].getName())];
            String date = HTTPWrapper.getPubmedDateFromWeb(pubmed);
             
            row += pubmed + ":" + date + "\t";
             
            Set<Integer> listEditions = pubmedEdition.get(pubmed);
            for (Integer edition : listEditions) {
                array[edition - 1] = 1; // put a value when it appeared
            }
             
            // add the array to the current row
            for (int i = 0; i < array.length; i++) {
                row += array[i] + "\t";
            }
             
            mapping.write(row + "\n");
            mapping.flush();
        }
         
        mapping.close();
    }//
     
    /**
     * Will generate each one of the connections
     *
     * @param species
     * @param fs
     */
    public static void generatePubmedFromWebServices(final String species,
            final File[] fs) {
         
        HashSet<String> pubmeds1 = new HashSet<String>();
        HashSet<String> map = new HashSet<String>();
         
        // Traverse geneAssociation files
        for (int i = 0; i < fs.length; i++) {
             
            final int n = i;
             
            try {
                 
                BufferedReader br;
                br = Species.getGzReader(fs[n]);
                 
                System.out.println("reading pubmeds for file: "
                        + fs[n].getName());
                 
                String edition = "" + getNo(fs[n].getName());
                String line;
                // Read line by line
                 
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("!") != -1) {
                        continue; // skip comments
                    }
                     
                    // Verify for retracted synchronously (Might take a
                    // while)
                    if ((line.indexOf("PUBMED:") != -1)
                            || (line.indexOf("PMID:") != -1)) { // pubmed
                                                                // editions
                        String[] columnDetail = new String[11];
                        columnDetail = line.split("\t");
                        // Sanity check. Make sure we have read 3 columns or
                        // more
                        if (columnDetail.length <= 5) {
                            continue;
                        }
                        String uniprot = columnDetail[1];
                        String symbol = columnDetail[2];
                        String goAnnotation = columnDetail[4];
                        String pubmed = getNo(columnDetail[5]);
                        String key = pubmed + "\t" + uniprot + "\t" + symbol
                                + "\t" + goAnnotation + "\t" + edition;
                         
                        if (uniprot.equals("")) {
                            continue;
                        }
                        if (symbol.equals("")) {
                            continue;
                        }
                        if (goAnnotation.equals("")) {
                            continue;
                        }
                        if (pubmed.equals("")) {
                            continue;
                        }
                        if (!map.contains(key)) {
                            map.add(key);
                        }
                        if (!pubmeds1.contains(pubmed)) {
                            pubmeds1.add(pubmed);
                        }
                         
                    }// if exists pubmed
                }// while
                br.close();
                 
            } catch (FileNotFoundException e) {
                System.err.println(e.getLocalizedMessage());
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
            } finally {
                 
                // System.out.println("DONE FOR EDITION: " +
                // getNo(fs[n].getName()));
                try {
                     
                } catch (Throwable e) {
                    // e.printStackTrace();
                }
            }
        }
         
        // when all the terms are in pubmeds, we can start downloading files.
        try {
             
            BufferedWriter bwr;
            bwr = Species.getWriter(species, species + "-map-pubmed-gos.txt");
            bwr.write("Total number of pubmeds for " + species + " : "
                    + pubmeds1.size() + "\n");
            bwr.write("pubmed\tuniprot\tsymbol\tgoAnnotation\tedition\n");
             
            String[] values = (map.toArray(new String[0]));
             
            Arrays.sort(values);
            for (String str : values) {
                bwr.write(str + "\n");
            }
            bwr.close();
            System.out.println("The pubmed-go-mapping was successfully created");
             
            BufferedWriter typeInError = Species.getWriter(species, species
                    + "-type-in-error.txt");
             
            BufferedWriter typeRetracted = Species.getWriter(species, species
                    + "-type-retracted");
            HashSet<String> pubmeds = pubmeds1;
            System.out.println("INITIALIZING DOWNLOADS FOR [" + pubmeds.size()
                    + "] PUBMEDS\nThis can take a while");
            int i = -1;
            for (String pubmed : pubmeds) {
                ++i;
                String[] retracted = HTTPWrapper
                        .getRetractedPubmedFromWeb(pubmed);
                 
                if (retracted == null) {
                    continue; // we discard anything that is not retracted.
                }
                 
                String type = retracted[0];
                String url = retracted[1];
                String date = retracted[2];
                 
                String output = pubmed + "\t" + type + "\t" + url + "\t" + date;
                 
                if (type.equals("in-error")) {
                    typeInError.write(output + "\n");
                } else {
                    typeRetracted.write(output + "\n");
                }
                 
                if (i % 100 == 0) {
                    typeInError.flush(); // write every 100 elements to disc
                    typeRetracted.flush();
                    System.out.print("." + i + "/" + pubmeds.size() + ".");
                }
            }
            typeInError.close();
            typeRetracted.close();
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }// read all terms
     
    /**
     * Parse key to just digits
     *
     * @param key
     * */
    private static String getNo(String key) {
        // locate pubmed or pmid ONLY
        StringTokenizer tokenizer = new StringTokenizer(key, "|");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.contains("PMID:") || token.contains("PUBMED:")) {
                return token.replaceAll("\\D+", "");
            }
        }
        return key.replaceAll("\\D+", ""); // default, return only numbers
    }
     
    /**
     *
     *Extract the number in a string
     * @param key
     * @return
     */
    private static Integer extractNumber(String key) {
        return Integer.valueOf(key.replaceAll("\\D+", ""));
    }
}