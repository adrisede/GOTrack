
package ubc.pavlab.gotrack.utils;
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;
 
import ubc.pavlab.gotrack.constants.Species;
 
import com.google.common.collect.HashMultimap;
 
/**
 * Uniprot Pubmed Mapping overtime
 *
 * @author asedeno
 *
 */
public class GOUPOverTime {
     
    //
    final static String PUBMED_MAPPING_FILE_NAME = "pubmedsDatesMapping-Sorted.txt";
     
    public static void mapEditionsFile(File[] assoc,
            HashMap<String, String> pubmedDates, String Species) {
         
    }
     
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
            if (token.contains("PMID:")) {
                return token.replaceAll("\\D+", "");
            }
        }
        return key.replaceAll("\\D+", ""); // default, return only numbers
    }
     
    /**
     *
     * @param species
     * @return
     */
    public static HashMap<String, String> getAllPubmedDates(String species) {
        Logger logger = Logger.getLogger("Logger");
         
        GenUtils.readDirectory(Species.getDataFolder(species),
                "gene_association*", logger);
         
        HashMap<String, String> dateMapping = getPubmedMapping(species);
        return dateMapping;
         
    }
     
    /**
     *
     * @param file
     * @param species
     * @return
     * @throws IOException
     */
    public static HashMultimap<String, String> getPubmedEdition(File file,
            String species) throws IOException {
         
        HashMultimap<String, String> map = HashMultimap.create();
        HashSet<String> keys = new HashSet<String>();
         
        BufferedReader reader = Species.getGzReader(file);
         
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (line.indexOf("!") != -1) {
                continue;
            }
            String[] details = line.split("\t");
            if (details.length <= 5) {
                continue;
            }
             
            // search for PMID
            if (line.indexOf("PMID") == -1) {
                continue;
            }
             
            String uniprot = details[1]; // uniprot
             
            String pmid = details[5];
            pmid = getNo(pmid);
             
            if (uniprot == null || uniprot.equals("")) {
                continue;
            }
             
            if (pmid == null || pmid.equals("")) {
                continue;
            }
             
            if (!keys.contains(uniprot + pmid)) {
                keys.add(uniprot + pmid);
                map.put(uniprot, pmid);
            } else {
                continue;
            }
             
        }
         
        return map;
    }
     
    /**
     * Will return the pubmed mapping
     *
     * @return
     */
    private static HashMap<String, String> getPubmedMapping(String species) {
        try {
            BufferedReader reader = Species.getReader(species,
                    PUBMED_MAPPING_FILE_NAME);
            HashMap<String, String> mappingDates = new HashMap<String, String>();
             
            reader.readLine(); // skip header
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] details = line.split("\t");
                 
                String pubmed = details[0];
                if (!mappingDates.containsKey(pubmed.substring(0,
                        pubmed.indexOf(":")))) {
                    mappingDates.put(pubmed.substring(0, pubmed.indexOf(":")),
                            pubmed);
                } else {
                    continue;
                }
                 
            }
             
            return mappingDates;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
         
    }    
     
}