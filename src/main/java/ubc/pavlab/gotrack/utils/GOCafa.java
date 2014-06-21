package ubc.pavlab.gotrack.utils;
 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
 
import org.apache.commons.io.filefilter.WildcardFileFilter;
 
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.preprocessing.ExploreGenes;
 
import com.google.common.collect.HashMultimap;
 
public class GOCafa {
     
    public static void main(String[] args) {
        GOCafa cafa = new GOCafa();
        Species species = new Species();
        for (String species2 : species.getSpecies()) {
            try {
                cafa.generateAllEditionMappers(species2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
     
    private static String getNo(String key) {
        return key.replaceAll("\\D+", ""); // default, return only numbers
    }
     
    public void generateAllEditionMappers(String species)
            throws FileNotFoundException, IOException {
         
        File dir = Species.getDataFolder(species);
         
        FileFilter fileFilter = new WildcardFileFilter("gene_association.goa*");
        File[] files = dir.listFiles(fileFilter);
        Arrays.sort(files, ExploreGenes.getFileComparator());
         
        HashMultimap<String, String[]> combinedMaps = HashMultimap.create();
         
        String header = "id\tgo\t";
        Integer[] editions = new Integer[files.length];
        int i = 0;
        for (File file : files) {
            editions[i] = Integer.valueOf(getNo(file.getName()));
            i++;
            // return uniprot:go
            HashMultimap<String, String[]> mapPerEdition = returnMapPerEdition(file);
            Set<String> uniprotsGos = mapPerEdition.keySet();
            for (String uniprotGo : uniprotsGos) {
                Set<String[]> goValues = mapPerEdition.get(uniprotGo);
                Iterator<String[]> iterator = goValues.iterator();
                while (iterator.hasNext()) {
                    String[] current = iterator.next();
                     
                    combinedMaps.put(uniprotGo, current);
                }
            }
        }// after this, we will have in combinedMaps, ALL of the current values.
        int first = editions[0];
        int last = editions[editions.length - 1];
        int max = first > last ? first : last;
         
        String[] alleditions = new String[max];
        for (int j = 0; j < editions.length; j++) {
            alleditions[editions[j]] = editions[j] + "";
        }
        for (int j = 0; j < max; j++) {
            header += alleditions[j] == null ? (j + 1) + "(NA)" + "\t"
                    : alleditions[j] + "\t";
        }
         
        BufferedWriter writer = Species.getWriter(species, species
                + "-ALL-CAFA-mapping.txt");
        writer.write(header + "\n");
        writer.flush();
         
        Set<String> uniprotsGOs = combinedMaps.keySet();
        // Starting batch of rows
        for (String uniprotGo : uniprotsGOs) {
            Set<String[]> goValues = combinedMaps.get(uniprotGo);
            Iterator<String[]> iterator = goValues.iterator();
             
            String row = getUniprot(uniprotGo) + "\t";
            row += getGo(uniprotGo) + "\t";
             
            String[] evidences = new String[max];
            while (iterator.hasNext()) {
                 
                String[] current = iterator.next();
                // String go = current[0];
                String evidence = current[1];
                String edition = current[2];
                evidences[Integer.valueOf(edition)] = evidence;
            } // populate evidence codes
             
            for (String evidence : evidences) {
                row += evidence == null ? "no-data\t" : evidence + "\t";
            }
            writer.write(row + "\n");
            writer.flush();
        }
         
        writer.close();
         
        HashMultimap<String, String[]> combinedMaps2 = HashMultimap.create();
         
        String header2 = "id\tgo\t";
        Integer[] editions2 = new Integer[files.length];
        i = 0;
        for (File file : files) {
            editions2[i] = Integer.valueOf(getNo(file.getName()));
            i++;
            // return uniprot:go
            HashMultimap<String, String[]> mapPerEdition = returnMapPerEdition(file);
            Set<String> uniprotsGos = mapPerEdition.keySet();
            for (String uniprotGo : uniprotsGos) {
                Set<String[]> goValues = mapPerEdition.get(uniprotGo);
                Iterator<String[]> iterator = goValues.iterator();
                while (iterator.hasNext()) {
                    String[] current = iterator.next();
                     
                    combinedMaps.put(uniprotGo, current);
                }
            }
        }// after this, we will have in combinedMaps, ALL of the current values.
        first = editions2[0];
        last = editions2[editions.length - 1];
        max = first > last ? first : last;
         
        alleditions = new String[max];
        for (int j = 0; j < editions.length; j++) {
            alleditions[editions[j]] = editions[j] + "";
        }
        for (int j = 0; j < max; j++) {
            header += alleditions[j] == null ? (j + 1) + "(NA)" + "\t"
                    : alleditions[j] + "\t";
        }
         
        BufferedWriter writer3 = Species.getWriter(species, species
                + "-ALL-SYMBOL-CAFA-mapping.txt");
        writer3.write(header2 + "\n");
        writer3.flush();
         
        uniprotsGOs = combinedMaps2.keySet();
        // Starting batch of rows
        for (String uniprotGo : uniprotsGOs) {
            Set<String[]> goValues = combinedMaps2.get(uniprotGo);
            Iterator<String[]> iterator = goValues.iterator();
             
            String row = getUniprot(uniprotGo) + "\t";
            row += getGo(uniprotGo) + "\t";
             
            String[] evidences = new String[max];
            while (iterator.hasNext()) {
                 
                String[] current = iterator.next();
                // String go = current[0];
                String evidence = current[1];
                String edition = current[2];
                evidences[Integer.valueOf(edition)] = evidence;
            } // populate evidence codes
             
            for (String evidence : evidences) {
                row += evidence == null ? "no-data\t" : evidence + "\t";
            }
            writer3.write(row + "\n");
            writer3.flush();
        }
         
        writer3.close();
         
        // GET SPECIFIC INPUT FROM CAFA
        BufferedWriter writer2 = Species.getWriter(species, species
                + "-CAFA-mapping.txt");
        writer2.write(header + "\n");
         
        Set<String> uniprotsGOsCafa = parseCAFAFiles(species);
        // Starting batch of rows
        for (String uniprotGo : uniprotsGOsCafa) {
            Set<String[]> goValues = combinedMaps.get(uniprotGo);
            Iterator<String[]> iterator = goValues.iterator();
             
            String row = getUniprot(uniprotGo) + "\t";
            row += getGo(uniprotGo) + "\t";
             
            String[] evidences = new String[max];
            while (iterator.hasNext()) {
                 
                String[] current = iterator.next();
                // String go = current[0];
                String evidence = current[1];
                String edition = current[2];
                evidences[Integer.valueOf(edition)] = evidence;
            } // populate evidence codes
             
            for (String evidence : evidences) {
                row += evidence == null ? "no-data\t" : evidence + "\t";
            }
            writer2.write(row + "\n");
            writer2.flush();
        }
         
        writer2.close();
    }
     
    private Set<String> parseCAFAFiles(String species) throws IOException {
        // the set must be Uniprot:go
         
        File dir = new File(".");
         
        FileFilter fileFilter = new WildcardFileFilter("*_info_*");
        File[] files = dir.listFiles(fileFilter);
         
        Set<String> values = new HashSet<String>();
         
        HashMap<String, String> mapCAFA = new HashMap<String, String>();
        for (File file : files) {
            // retrieve the mapping between the CAFA TARGET VS THE UNIPROT AND
            // GO
             
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    (new FileInputStream(file))));
            String line = "";
             
            while ((line = input.readLine()) != null) {
                String[] details = line.split("\t");
                String uniprot = details[0];
                String TargetID = details[1];
                if (!mapCAFA.containsKey(TargetID)) {
                    mapCAFA.put(TargetID, uniprot);
                }
            }
             
            input.close();
        }
         
        // CAFA HASH MAPPED
        File dir2 = Species.getTmpFolder(species);
         
        FileFilter fileFilter2 = new WildcardFileFilter("*.csv");
        File[] files2 = dir2.listFiles(fileFilter2);
         
        for (File file : files2) {
             
            // get uniprot value to map
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    (new FileInputStream(file))));
            String line = "";
             
            while ((line = input.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, " ");
                tokenizer.nextToken(); // skipp ID
                String targetID = tokenizer.nextToken().replaceAll("\"", "");
                // search for GO ID
                String goID = "";
                while (tokenizer.hasMoreTokens()) {
                    String nexttoken = tokenizer.nextToken();
                    if (nexttoken.indexOf("GO:") != -1) { // we found it!
                        goID = nexttoken.replaceAll("\"", "");
                        break;
                    }
                }
                // MAP THE CAFA TARGET ID TO GET THE INPUT FILES
                values.add(mapCAFA.get(targetID) + ":" + goID);
            }
             
            input.close();
        }
        return values;
    }
     
    private String getUniprot(String uniprotgo) {
        return uniprotgo.substring(0, uniprotgo.indexOf(":"));
    }
     
    private String getGo(String uniprotgo) {
        return uniprotgo.substring(uniprotgo.indexOf(":"));
    }
     
    public HashMultimap<String, String[]> returnMapPerEdition(File file)
            throws FileNotFoundException, IOException {
         
        HashMultimap<String, String[]> mapPerEdition = HashMultimap.create();
        HashSet<String> keyValues = new HashSet<String>();
         
        BufferedReader input = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file))));
        String line = "";
         
        while ((line = input.readLine()) != null) {
            if (line.indexOf("!") != -1) {
                continue;
            }
            String[] details = line.split("\t");
            if (details.length < 5) {
                continue;
            }
            if (details[3].equals("NOT")) {
                continue;
            }
             
            String uniprot = details[1];
            String go = details[3];
            String evidence = details[5];
             
            String edition = getNo(file.getName());
            if (!keyValues.contains(uniprot + go)) {
                keyValues.add(uniprot + go);
                String[] mapping = { go, evidence, edition };
                mapPerEdition.put(uniprot + ":" + go, mapping);
            }
             
        }// while
         
        input.close();
        return mapPerEdition;
    }
     
    public HashMultimap<String, String[]> returnSymbolMapPerEdition(File file)
            throws FileNotFoundException, IOException {
         
        HashMultimap<String, String[]> mapPerEdition = HashMultimap.create();
        HashSet<String> keyValues = new HashSet<String>();
         
        BufferedReader input = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file))));
        String line = "";
         
        while ((line = input.readLine()) != null) {
            if (line.indexOf("!") != -1) {
                continue;
            }
            String[] details = line.split("\t");
            if (details.length < 5) {
                continue;
            }
            if (details[3].equals("NOT")) {
                continue;
            }
             
            String uniprot = details[2];
            String go = details[3];
            String evidence = details[5];
             
            String edition = getNo(file.getName());
            if (!keyValues.contains(uniprot + go)) {
                keyValues.add(uniprot + go);
                String[] mapping = { go, evidence, edition };
                mapPerEdition.put(uniprot + ":" + go, mapping);
            }
             
        }// while
         
        input.close();
        return mapPerEdition;
    }
}