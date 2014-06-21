package ubc.pavlab.gotrack.mapping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.ParseGO;
import ubc.pavlab.gotrack.tools.Sys;
import ubc.pavlab.gotrack.utils.GenUtils;

/**
* Class that implements the gene id mapping functionality
* 
* @author asedeno
*
*/
public class GOMapping {

        private static Map<String, String> uniprotGoTerms;
        private static Map<String, String> synGoTerms;
        private static Map<String, String> idMapping;
        private static Map<String, String> idWebPageMapping;
        private static Set<String> uniprotsInDicctionary;

        private static String MAP_EDITION_HUMAN = "105";
        private static String MAP_EDITION_ARABIDOPSIS = "56";
        private static String MAP_EDITION_CHICKEN = "53";
        private static String MAP_EDITION_COW = "46";
        private static String MAP_EDITION_MOUSE = "69";
        private static String MAP_EDITION_ECOLI = "95";
        private static String MAP_EDITION_FLY = "30";
        private static String MAP_EDITION_RAT = "72";
        private static String MAP_EDITION_DICTY = "25";
        private static String MAP_EDITION_DOG = "25";
        private static String MAP_EDITION_PIG = "25";
        private static String MAP_EDITION_ZEBRAFISH = "57";
        private static String MAP_EDITION_WORM = "25";
        // private static String MAP_EDITION_YEAST = "106";

        private static String ID_MAPPING_HUMAN = "geneIdMaps/idMapping_human.dat.gz";
        private static String ID_MAPPING_ARABIDOPSIS = "geneIdMaps/idMapping_arabidopsis.dat.gz";
        private static String ID_MAPPING_CHICKEN = "geneIdMaps/idMapping_chicken.dat.gz";
        private static String ID_MAPPING_DICTY = "geneIdMaps/idMapping_dicty.dat.gz";
        private static String ID_MAPPING_ECOLI = "geneIdMaps/idMapping_ecoli.dat.gz";
        private static String ID_MAPPING_FLY = "geneIdMaps/idMapping_fly.dat.gz";
        private static String ID_MAPPING_MOUSE = "geneIdMaps/idMapping_mouse.dat.gz";
        private static String ID_MAPPING_RAT = "geneIdMaps/idMapping_rat.dat.gz";
        private static String ID_MAPPING_WORM = "geneIdMaps/idMapping_worm.dat.gz";
        private static String ID_MAPPING_YEAST = "geneIdMaps/idMapping_yeast.dat.gz";
        private static String ID_MAPPING_ZEBRAFISH = "geneIdMaps/idMapping_zebrafish.dat.gz";

        private static String WEB_ID_MAPPING_HUMAN = "geneIdMaps/human_webpage_map.txt";
        private static String WEB_ID_MAPPING_ARABIDOPSIS = "geneIdMaps/arab_webpage_map.txt";
        private static String WEB_ID_MAPPING_CHICKEN = "geneIdMaps/chicken_webpage_map.txt";
        private static String WEB_ID_MAPPING_DICTY = "geneIdMaps/dicty_webpage_map.txt";
        private static String WEB_ID_MAPPING_DOG = "geneIdMaps/dog_webpage_map.txt";
        private static String WEB_ID_MAPPING_ECOLI = "geneIdMaps/ecoli_webpage_map.txt";
        private static String WEB_ID_MAPPING_FLY = "geneIdMaps/fly_webpage_map.txt";
        private static String WEB_ID_MAPPING_MOUSE = "geneIdMaps/mouse_webpage_map.txt";
        private static String WEB_ID_MAPPING_RAT = "geneIdMaps/rat_webpage_map.txt";
        private static String WEB_ID_MAPPING_PIG = "geneIdMaps/pig_webpage_map.txt";
        private static String WEB_ID_MAPPING_WORM = "geneIdMaps/worm_webpage_map.txt";
        private static String WEB_ID_MAPPING_YEAST = "geneIdMaps/yeast_webpage_map.txt";
        private static String WEB_ID_MAPPING_ZEBRAFISH = "geneIdMaps/zebrafish_webpage_map.txt";
        private static String WEB_ID_MAPPING_COW = "geneIdMaps/cow_webpage_map.txt";

        /*----------------------------------------------------------------------------------------*/

        /**
         * This function looks for an uniprot in the dictionarym if it's not there, it will check
         * the idmapping file 
         * @param uniprot The Uniprot id we are looking for
         * @param log Where messages are written
         * @return The id mapped in the dictionaries, empty string if there is no match
         */
        public static String lookupUniprotUsingIdMapping(String uniprot, Logger log) {

                // if the uniprot is an uniprot in the dictionary
                if (uniprotsInDicctionary.contains(uniprot)) {
                        // this is already an uniprot, just return the original id
                        log.finest("Uniprot " + uniprot
                                        + " is in the dictionary. Choosing it.");
                        return uniprot;
                } else {
                        // We need to map the id, look for the id into the dictionary and
                        // see
                        // whether there is a match or not
                        if (idMapping.containsKey(uniprot)) {
                                log.finest("There is a match for " + uniprot
                                                + " in the dictionary. Choosing "
                                                + idMapping.get(uniprot));
                                return idMapping.get(uniprot);
                        }
                }
                return "";
        }

        /**
         *Reads id mapping file downloaded from the uniprot web page (idWebMapping)
         * @param idMappingFile 
         * @return It fills the idWebPageMapping variable to be used
         */
        public static void readWebIdMapping(File idMappingFile, String species,
                        Logger logger) {

                idWebPageMapping = new HashMap<String, String>();
                try {
                        BufferedReader readerOfDictionary = new BufferedReader(
                                        new InputStreamReader(new FileInputStream(idMappingFile)));
                        String temporary;
                        logger.finest("Reading " + idMappingFile.getName());
                        int j = 0;
                        while ((temporary = readerOfDictionary.readLine()) != null) {
                                if ((j++) % 30000 == 0)
                                        logger.finest(".");

                                String[] columnDetail = new String[20];
                                columnDetail = temporary.split("\t");
                                if (columnDetail.length >= 2) {
                                        String uniprot = columnDetail[0];
                                        // list of synonyms divided by |
                                        String syn = columnDetail[1];
                                        if (!idWebPageMapping.containsKey(uniprot)) {
                                                idWebPageMapping.put( uniprot,syn);
                                        } else {
                                                String temporary2 = idWebPageMapping.get(uniprot);
                                                idWebPageMapping.put( uniprot,removeDuplicate(syn + "|" + temporary2));
                                        }
                                }
                        }
                        readerOfDictionary.close();
                } catch (Exception e) {
                        e.printStackTrace();
                }

        }

        /**
         *It reads the idMapping file
         * @param idMappingFile
         * @return it fills the idMapping variable to be used later
         */
        public static void readIdMapping(File idMappingFile, String species,
                        Logger logger) {
                uniprotsInDicctionary = new TreeSet<String>();
                idMapping = new HashMap<String, String>();
                try {
                        BufferedReader readerOfDictionary = new BufferedReader(
                                        new InputStreamReader(new GZIPInputStream(
                                                        new FileInputStream(idMappingFile))));
                        String temporary;
                        logger.info("Reading " + idMappingFile.getName());
                        int j = 0;
                        while ((temporary = readerOfDictionary.readLine()) != null) {
                                if ((j++) % 20000 == 0)
                                        logger.info(".");

                                String[] columnDetail = new String[20];
                                columnDetail = temporary.split("\t");
                                if (columnDetail.length > 2) {
                                        String uniprot = columnDetail[0];

                                        uniprotsInDicctionary.add(uniprot);
                                        // list of synonyms divided by |
                                        String syn = columnDetail[2];
                                        if (syn.compareTo("") == 0)
                                                continue;
                                        if (!idMapping.containsKey(syn)) {
                                                idMapping.put(syn,uniprot);
                                        } else {
                                                String temporary2 = idMapping.get(syn);
                                                idMapping.put( syn,uniprot + "|" + temporary2);
                                        }
                                }
                        }
                        readerOfDictionary.close();
                        for (Map.Entry<String, String> entry : idMapping.entrySet()) {
                            String id = entry.getKey();
                            String map = idMapping.get(id);
                            entry.setValue(removeDuplicate(map));                           
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

        }

        /*--------------------------------------------------------------------------------------*/

        /**
         * It writes the dictionary into the dic.txt file
         * @param dic Dictionary. Map  of id to GoSyn class (that contains the synonyms of the id)
         * @param species Species
         * @return Map of id to a string made of the synonyms divided by a pipe
         */
        private static Map<String, String> writeDictionary(
                        Map<String, GOSyn> dic, String species) {
                synGoTerms = new HashMap<String, String>();
                Map<String, String> resultHashMap = new HashMap<String, String>();
                // uniprot, list divided by |
                for (String uniprot : dic.keySet()) {
                        StringTokenizer tokenizer = new StringTokenizer(dic.get(uniprot)
                                        .getSynonyms(), "|");
                        String result;
                        while (tokenizer.hasMoreElements()) {
                                result = tokenizer.nextToken();
                                if (!resultHashMap.containsKey(result))
                                        resultHashMap.put(result, uniprot);
                                else
                                        resultHashMap.put(result,
                                                        uniprot + "|" + resultHashMap.get(result));

                                if (!synGoTerms.containsKey(result)) {
                                        uniprotGoTerms.put(result, uniprotGoTerms.get(uniprot));
                                } else {
                                        String tmp1 = uniprotGoTerms.get(result);
                                        uniprotGoTerms.put(result, uniprotGoTerms.get(uniprot)
                                                        + "|" + tmp1);
                                }
                        }
                }

                try {

                        Sys.bash("rm " + Species.dataFolder.getAbsolutePath() + "/"
                                        + species + Species.TMP_FOLDER + "/dic.txt");
                        BufferedWriter bufferedWriterOfDictionary = Species.getWriter(
                                        species, "dic.txt");
                        for (String syn : resultHashMap.keySet()) {
                                bufferedWriterOfDictionary.write(syn + "\t"
                                                + resultHashMap.get(syn) + "\n");
                        }
                        bufferedWriterOfDictionary.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return resultHashMap;
        }

        /**
         *Given an specific version of a gene association file. This function 
         *computes a dictionary reading the synonyms that are found in the file
         * @param fileOfAssoc An edition of a gene association file
         * @return Map of id to a string made of the synonyms divided by a pipe
         */
        public static Map<String, String> computeDictionaryUsingGeneAssocFile(
                        File fileOfAssoc, String species, Logger logger) {

                Map<String, GOSyn> dictionary = new HashMap<String, GOSyn>();
                
                if(species.compareTo(Species.ECOLI)==0 ){
                	return readEcoliDictionary(logger);
                }
                
                if(species.compareTo(Species.FLY)==0){
                	return readFlyDictionary(logger);
                }
                
                uniprotGoTerms = new HashMap<String, String>();
                try {
                        BufferedReader readerOfDictionary = new BufferedReader(
                                        new InputStreamReader(new GZIPInputStream(
                                                        new FileInputStream(fileOfAssoc))));
                        String temporary;
                        logger.info("Reading gene assoc file");
                        
                        while ((temporary = readerOfDictionary.readLine()) != null) {                                
                                String[] columnDetail = new String[200];
                                columnDetail = temporary.split("\t");
                                if (columnDetail.length > 10) {
                                        String uniprot = columnDetail[1];
                                        String goterm = columnDetail[4];
                                        // list of synonyms divided by |
                                        String syn = columnDetail[10];
                                        if (syn.compareTo("") == 0)
                                                continue;
                                        dictionary.put(uniprot, new GOSyn(goterm, syn));
                                        if (!uniprotGoTerms.containsKey(uniprot)) {
                                                uniprotGoTerms.put(uniprot, goterm);
                                        } else {
                                                String temporary2 = uniprotGoTerms.get(uniprot);
                                                uniprotGoTerms.put(uniprot, goterm + "|" + temporary2);
                                        }
                                }
                        }
                        logger.info("\nComputing dictionary");
                        readerOfDictionary.close();
                } catch (Exception e) {

                }
                return writeDictionary(dictionary, species);
        }

        /**
         * Reads the GenesAlwaysPresent.txt file and return a Set of the genes in that file
         * @param species Species to compute GenesAlwaysPresent.txt
         * @return Set of the genes in 
         */
        private static Set<String> getGenesAlwaysPresent(String species) {
                HashSet<String> gapOfFile = new HashSet<String>();
                try {

                        BufferedReader alwaysPresentInputFile = Species.getReader(species,
                                        "GenesAlwaysPresent.txt");
                        String tmp;
                        while ((tmp = alwaysPresentInputFile.readLine()) != null) {
                                gapOfFile.add(tmp);
                        }

                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return gapOfFile;
        }

        /**
         *Get the set of genes in a gene association file
         * @param file Gene Association file
         * @return Set of genes
         */
        private static Set<String> getIdGeneAssoc(String file) {
                HashSet<String> gaap = new HashSet<String>();
                try {
                        BufferedReader readerOfAssocFile = new BufferedReader(
                                        new InputStreamReader(new GZIPInputStream(
                                                        new FileInputStream(file))));
                        String tmp;
                        while ((tmp = readerOfAssocFile.readLine()) != null) {
                                String[] columnDetail = new String[200];
                                columnDetail = tmp.split("\t");
                                if (columnDetail.length >= 2) {
                                        gaap.add(columnDetail[1]);
                                }
                        }
                        readerOfAssocFile.close();
                } catch (FileNotFoundException fnfex) {
                        fnfex.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return gaap;
        }

        /**
         * Gets a set of genes in genesLastEdition.txt
         * @param species
         * @return set of genes in genesLastEdition.txt
         */
        private static Set< String> getGenesLastEdition(String species) {
                HashSet<String> gleHashMap = new HashSet<String>();
                try {

                        BufferedReader readerOfLastEdition = Species.getReader(species,
                                        "genesLastEdition.txt");
                        String temporary;
                        while ((temporary = readerOfLastEdition.readLine()) != null) {
                                String[] columnDetail = new String[200];
                                columnDetail = temporary.split("\t");
                                if (columnDetail.length >= 2) {
                                        gleHashMap.add(columnDetail[0]);
                                }

                        }

                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return gleHashMap;
        }

        /**
         *Given a string of elements separated by a pipe. It will return a string of unique elements
         * @param synonyms id's separated by pipe
         * @return
         */
        private static String removeDuplicate(String synonyms) {
                String synResultString = "";
                StringTokenizer tokenizer = new StringTokenizer(synonyms, "|");
                HashMap<String, Integer> temporaryHashMap = new HashMap<String, Integer>();
                while (tokenizer.hasMoreElements()) {
                        temporaryHashMap.put(tokenizer.nextToken(), 1);
                }
                int j = 0;
                for (String keyString : temporaryHashMap.keySet()) {
                        if ((j++) == temporaryHashMap.size() - 1)
                                synResultString += keyString;
                        else
                                synResultString += keyString + "|";
                }

                return synResultString;
        }

        /**
         *This function implements the id synonyms mapping algorithm
         *
         * @param editionMap dictionary made out of a special goa edition
         * @param synonyms list of synonyms for the id
         * @param id  Id we want to map
         * @param idsthisedition  Set of ids in this edition
         * @param genesalwayspresent Set of ids always present
         * @param geneslastedition Set id ids in the last edition
         * @param logger Where to write the steps
         * @return Returns the most updated id 
         */
        private static String checkSynonyms(Map<String, String> editionMap,
                        String synonyms, String id, Set<String> idsthisedition,
                        Set<String> genesalwayspresent,
                        Set<String> geneslastedition, Logger logger) {
                synonyms = removeDuplicate(synonyms);
                String uniprot = "";
                // tokenize synonyms by |
                StringTokenizer tokenizer = new StringTokenizer(synonyms, "|");
                Map<String, Integer> uniprotcandidates = new HashMap<String, Integer>(4);
                // check each synonym and get its associated uniprot in map105

                int matchedsynonyms = 0;
                String reskey = "";
                String synonym = "";
                int numberOfSynonyms = 0;
                while (tokenizer.hasMoreElements()) {
                        String syn = tokenizer.nextToken();
                        uniprot = editionMap.get(syn.toLowerCase());
                        if (uniprot == null) {
                                continue;
                        } else {
                                synonym = syn.toLowerCase();
                                matchedsynonyms++;
                                if (uniprot.contains("|")) {
                                        // We have more than one candidate, we have to check
                                        StringTokenizer uniprotTokenizer = new StringTokenizer(
                                                        uniprot, "|");
                                        // If there is only one synonym that matched an uniprot this
                                        // variable will keep its value

                                        while (uniprotTokenizer.hasMoreElements()) {
                                                String uniprotcandidate = uniprotTokenizer.nextToken();
                                                logger.finest("Considering " + uniprotcandidate + ", "
                                                                + syn + " -> " + uniprotcandidate + "\n");
                                                if (!uniprotcandidates.containsKey(uniprotcandidate)) {
                                                        uniprotcandidates.put(uniprotcandidate, 1);
                                                } else {
                                                        Integer temporaryInteger = uniprotcandidates
                                                                        .get(uniprotcandidate);
                                                        uniprotcandidates.put(uniprotcandidate,
                                                                        temporaryInteger + 1);
                                                }
                                        }
                                } else {
                                        logger.finest("Considering " + uniprot + ", " + syn + " -> "
                                                        + uniprot + "\n");
                                        if (!uniprotcandidates.containsKey(uniprot)) {
                                                uniprotcandidates.put(uniprot, 1);
                                        } else {
                                                Integer tmp = uniprotcandidates.get(uniprot);
                                                uniprotcandidates.put(uniprot, tmp + 1);
                                        }
                                }
                        }
                }

                int sanitycheck = 0;
                numberOfSynonyms = 0;
                for (String uniprotKey : uniprotcandidates.keySet()) {
                        numberOfSynonyms += uniprotcandidates.get(uniprotKey);
                }
                for (String uniprotKey2 : uniprotcandidates.keySet()) {
                        if (uniprotcandidates.get(uniprotKey2) >= Math
                                        .ceil(numberOfSynonyms / 2d)) {
                                sanitycheck++;
                                if (reskey.compareTo("") == 0)
                                        reskey = uniprotKey2;
                                else
                                        reskey += "|" + uniprotKey2 + "|";
                        }
                }

                if (uniprotcandidates.size() == 1 && matchedsynonyms == 1) {
                        // There is only one candidate
                        logger.finest("There is only one posible uniprot "
                                        + editionMap.get(synonym.toLowerCase()));
                        uniprot = editionMap.get(synonym.toLowerCase());
                        uniprot = uniprot.replace("|", "");
                        // Look for the uniprot in this edition

                        /*
                         * Per observations. If the uniprot is in the same edition we won't
                         * assume is not a candidate then we comment these lines. if
                         * (idsthisedition.containsKey(uniprot)) { logger.info(
                         * "ID mapped found within this edition. The candidate is not a match for this gene."
                         * ); return ""; // If we could found it we won't save the value }
                         * logger.info(uniprot +
                         * " not found within this edition, the mapped ID is still a potential candidate"
                         * );
                         */

                        // look for it in gap

                        if (genesalwayspresent.contains(uniprot)) {
                                logger.finest("ID mapped found within the list of genes always present. "
                                                + "The candidate is not a match for this gene");
                                return ""; // we could find it there, we won't save the
                                                        // value
                        } else {
                                logger.finest(uniprot
                                                + " not found in the list of genes always present, the mapped "
                                                + uniprot + "is still a potential candidate");
                                logger.fine("Looking for " + uniprot + " in genes last edition");
                                // if not, look for it in gle
                                if (geneslastedition.contains(uniprot)) {
                                        logger.finest("We could find it in the last edition of the gene associations files. "
                                                        + "This means that mapped ID is still used. Taking"
                                                        + uniprot
                                                        + " as a matched ID but its a weak "
                                                        + "association and cannot guarantee its the right.");
                                        return uniprot;// we could find it, overwrite
                                } else {
                                        logger.finest("ID not found in Genes Last edition. Not taking this id");
                                        return "";
                                }
                        }

                }

                if (sanitycheck >= 2) {
                        logger.fine("Two possible candidates " + reskey + "\n");
                        if (editionMap.get(id) != null && !editionMap.get(id).contains("|")) {
                                logger.finest(id + " -> " + editionMap.get(id) + " has precedence\n");
                                return editionMap.get(id);
                        } else {
                                logger.finest("ID "
                                                + id
                                                + " is not pointing to any candidate. Neither candidate is match for this gene \n");
                                return "";
                        }

                } else {
                        if (reskey.compareTo("") == 0)
                                logger.fine("No possible candidate\n");
                        else
                                logger.fine("Choosing " + reskey + "\n");

                        if (reskey.contains("|"))
                                return "";
                        return reskey;
                }
        }

        /**
         * This function implements the id mapping algorithm
         * 
         * @param editionMap
         * dictionary of synonyms using special edition
         * @param genesAlwaysPresent
         * Genes Always Present (GAP)
         * @param genesLastEdition
         * Genes Last Edition (GLE)
         * */
        private static void computeSynGaf(File file, Map<String, String> editionMap,
                        Set<String> genesAlwaysPresent,
                        Set<String> genesLastEdition, String species,
                        Logger logger, BufferedWriter bufferedWriterOfSyns2) {

                String editionNo = GenUtils.getEditionNo(file.getName());

                // If the file is there we skip it
                File fileDataOfSynonyms = new File(Species.getDataFolder(species)
                                .getAbsolutePath()
                                + Species.TMP_FOLDER
                                + file.getName()
                                + ".syn");
                if (fileDataOfSynonyms.exists()) {
                        logger.info("File " + fileDataOfSynonyms.getName()
                                        + " already exists, skipping it.");
                        return;
                }

                HashSet<String> idsthisedition = (HashSet<String>) getIdGeneAssoc(Species
                                .getDataFolder(species).getAbsolutePath()
                                + "/"
                                + file.getName());

                try {

                        BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
                                        file.getName() + ".syn");

                        bufferedWriterOfSyns2
                                        .write("original\t customAlgorithm \t editionNumber\n");
                        BufferedReader bufferedReaderOfAssoc = new BufferedReader(
                                        new InputStreamReader(new GZIPInputStream(
                                                        new FileInputStream(file))));
                        String temporaryString;
                        while ((temporaryString = bufferedReaderOfAssoc.readLine()) != null) {
                                String[] columnDetail = new String[200];
                                columnDetail = temporaryString.split("\t");
                                if (columnDetail.length < 11)
                                        continue; // This row might be the header so skip it
                                String typeOfId = columnDetail[0];
                                String id = columnDetail[1];
                                String symbol = columnDetail[2];
                                String ipi = columnDetail[10];
                                // String pubmedid = columnDetail[5];
                                String uniprot = "";
                                String orignalid = id;
                                boolean matchfound = false;

                                if (idWebPageMapping!=null && idWebPageMapping.containsKey(orignalid)) {
                                        uniprot = idWebPageMapping.get(orignalid);
                                        logger.finest("Web dictionary: " + orignalid + " -> "
                                                        + uniprot);
                                        if(uniprot.contains("|")){
                                        	 logger.finest("More than one posible candidate. Taking original.");
                                        }else
                                        	columnDetail[1] = uniprot;
                                } else {
                                        if (typeOfId.compareToIgnoreCase("Uniprot") == 0
                                                        || typeOfId.compareToIgnoreCase("UniProtKB") == 0) {

                                                if (genesAlwaysPresent.contains(id)
                                                                || genesLastEdition.contains(id)) {
                                                        logger.finest("Taking "
                                                                        + id
                                                                        + " because it is an uniprot and it's found in genes "
                                                                        + "always present or genes last edition");
                                                        bufferedWriterOfSyns.write(temporaryString);// The
                                                                                                                                                // symbol
                                                                                                                                                // is
                                                                                                                                                // in
                                                                                                                                                // the
                                                                                                                                                // genes
                                                                                                                                                // always
                                                                                                                                                // present
                                                        bufferedWriterOfSyns.write("\n"); // file
                                                        continue;// we have nothing to do-> write the line
                                                                                // without
                                                                                // change
                                                } else {// The id is not in the always present                                                        
                                                        // Check symbol and id
                                                        if (editionMap.containsKey(symbol)
                                                                        && editionMap.containsKey(id)) {
                                                                String uniprot1 = editionMap.get(symbol);
                                                                String uniprot2 = editionMap.get(id);
                                                                if (uniprot1.compareTo(uniprot2) == 0
                                                                                && !uniprot1.contains("|")) {
                                                                        uniprot = uniprot1;
                                                                        logger.finest("Choosing " + uniprot + " for "
                                                                                        + id + "|" + symbol + "|" + ipi
                                                                                        + " id and symbol-> " + uniprot
                                                                                        + "\n");
                                                                        matchfound = true;
                                                                }
                                                        }
                                                        if (matchfound == false) {
                                                                // For latest editions we might have more than
                                                                // one
                                                                // synonym
                                                                // We have to check all of them + symbol
                                                                logger.finest("Trying to get uniprot for " + id
                                                                                + "|" + symbol + "|" + ipi + "\n");
                                                                uniprot = checkSynonyms(editionMap, id + "|"
                                                                                + symbol + "|" + ipi, id,
                                                                                idsthisedition, genesAlwaysPresent,
                                                                                genesLastEdition, logger);

                                                                matchfound = true;
                                                        }
                                                }
                                                if (uniprot.contains("|"))
                                                        uniprot = columnDetail[1];
                                                if (uniprot.compareTo("") == 0 || uniprot.contains("|"))
                                                        uniprot = columnDetail[1];
                                                // We have found an uniprot
                                                // Substitute the uniprot for the id
                                                logger.finest("Choosing " + uniprot + "\n");
                                                columnDetail[1] = uniprot;

                                        } /* End mapping algorithm */
                                        else { /*
                                                         * This is not an uniprot id, we should use the
                                                         * idMapping dictionary
                                                         */
                                                logger.fine("Not an uniprot. Try using idMapping dictionary");
                                                if (uniprotsInDicctionary != null
                                                                && uniprotsInDicctionary.size() > 0) {
                                                        uniprot = lookupUniprotUsingIdMapping(id, logger);
                                                        if (uniprot.compareTo("") != 0) {
                                                                logger.finest(columnDetail[1] + " -> " + uniprot);
                                                                columnDetail[1] = uniprot;
                                                        }
                                                }
                                                if(species.compareTo(Species.ECOLI)==0){  
                                                	/*if(id.compareTo("NUOG-MONOMER")==0)
                                                		System.out.println();*/
                                                	uniprot = checkSynonyms(editionMap, id + "|"
                                                            + symbol + "|" + ipi, id,
                                                            idsthisedition, genesAlwaysPresent,
                                                            genesLastEdition, logger);
                                                	if (uniprot.compareTo("") != 0 && !uniprot.contains("|")) {
                                                        logger.fine(columnDetail[1] + " -> " + uniprot);
                                                        columnDetail[1] = uniprot;
                                                    }
                                                }

                                        }
                                }

                                // Print the remaining info for this gene
                                for (int i = 0; i < columnDetail.length; i++) {
                                        bufferedWriterOfSyns.write(columnDetail[i] + "\t");
                                }
                                bufferedWriterOfSyns.write("\n");
                                /* Log the mapping history */
                                bufferedWriterOfSyns2.write(orignalid + "\t" + uniprot + "\t"
                                                + editionNo + "\n");

                        }
                        bufferedReaderOfAssoc.close();
                        bufferedWriterOfSyns.close();
                        bufferedWriterOfSyns2.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }

        }

        /**
         * For testing only
         * @param args
         */
        public static void main(String[] args) {

                String species = Species.HUMAN;
                // ArrayList<File> xrefs = MappingUtils.readDirectory(
                // "/home/asedeno/workspace/GOtrack/data/human/",
                // "*gene_association*105*gz");
                // ArrayList<File> xrefs = MappingUtils.readDirectory(
                // Species.getDataFolder(species).getAbsolutePath() + "/",
                // "*gene_association*105*gz");

                // Map<String, String> map105 = computegeneassoci105(xrefs.get(0),
                // species,log);

                // Get genes always present
                // Get genes almost always present
                // Map<String, String> gap = getGenesAlwaysPresent(species);

                // Map<String, String> gle = getGenesLastEdition(species);

                // Map<String, String> gaap = gle;// getGenesAlmostAlwaysPresent();

                // ArrayList<File> gaf = MappingUtils.readDirectory(
                // "/home/asedeno/workspace/GOtrack/data/human/",
                // "*gene_association*gz");
                ArrayList<File> gaf = MappingUtils.readDirectory(
                                Species.getDataFolder(species).getAbsolutePath() + "/",
                                "*gene_association*gz");
                for (File f : gaf) {
                        System.out.print("Reading file" + f.getName() + "\n");
                        // computeSynGaf(f, map105, gap, gaap, gle, species);
                }

        }

     
/**
 * This function update edition dictionary and the IdMapping file using the web page file
 * @param editionDictionary
 */
        public static void updateIdMapping(Map<String, String> editionDictionary) {

                // idMapping
                // idWebPageMapping

                // Update IdMapping file using web page file
                // Map.Entry<String, Object> entry : map.entrySet()
        	
        	if(idMapping == null || idMapping.isEmpty())
        		return;
        	
                for (Map.Entry<String, String> entry : idMapping.entrySet()) {
                        String newIds = entry.getValue();
                        StringTokenizer st = new StringTokenizer(newIds, "|");
                        String newIdsUpdated = "";
                        while (st.hasMoreTokens()) {
                                String nid = st.nextToken();
                                if (idWebPageMapping.containsKey(nid)) {
                                        // Update new id
                                        newIdsUpdated = idWebPageMapping.get(nid) + "|";
                                } else
                                        newIdsUpdated = nid + "|";
                        }
                        if (newIdsUpdated.length() > 0)
                                newIdsUpdated = newIdsUpdated.substring(0,
                                                newIdsUpdated.length() - 1);
                        entry.setValue(newIdsUpdated);
                }

                // Complement web page file using updated id Mapping file
                for (Map.Entry<String, String> entry : idWebPageMapping.entrySet()) {
                        String id = entry.getKey();
                        String map = idWebPageMapping.get(id);
                        if (map == null || map.compareTo("") == 0) {
                                entry.setValue(idMapping.get(id));
                        }
                }

                // Update edition dictionary file using web page file
                for (Map.Entry<String, String> entry : editionDictionary.entrySet()) {
                        String newIds = entry.getValue();
                        StringTokenizer st = new StringTokenizer(newIds, "|");
                        String newIdsUpdated = "";
                        while (st.hasMoreTokens()) {
                                String nid = st.nextToken();
                                if (idWebPageMapping.containsKey(nid)) {
                                        // Update new id
                                        newIdsUpdated = idWebPageMapping.get(nid) + "|";
                                } else
                                        newIdsUpdated = nid + "|";
                        }
                        entry.setValue(newIdsUpdated);
                }

        }

        /**
         * Same as main, but in a method for the API. This function will compute the
         * mapping for a specific species using a dictionary
         *
         * @param species
         * Species to compute
         * @param lastedition
         * Last edition file
         * @param log
         * Logger
         *
         */
        public static void mainMApping(String species, File lastedition, Logger log, Integer startedition) {

        	/*IdWebMapping are collected in ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/by_organism/*/
        	
                // ArrayList<File> xrefs = MappingUtils.readDirectory(
                // "/home/asedeno/workspace/GOtrack/data/human/",
                // "*gene_association*105*gz");
                String dicEd = "";
                File idMappingFile = null;
                File idMappingWebPageFile = null;

                /* We have a base edition to apply custom mapping algorithm */
                if (species.compareTo(Species.HUMAN) == 0) {
                        dicEd = MAP_EDITION_HUMAN;
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_HUMAN);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_HUMAN);
                }
                if (species.compareTo(Species.ARABIDOPSIS) == 0) {
                        dicEd = MAP_EDITION_ARABIDOPSIS;
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_ARABIDOPSIS);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_ARABIDOPSIS);
                }
                if (species.compareTo(Species.CHICKEN) == 0) {
                        dicEd = MAP_EDITION_CHICKEN;
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_CHICKEN);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_CHICKEN);
                }
                if (species.compareTo(Species.COW) == 0) {
                        dicEd = MAP_EDITION_COW;
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_COW);
                }
                if (species.compareTo(Species.MOUSE) == 0) {
                        dicEd = MAP_EDITION_MOUSE;
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_MOUSE);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_MOUSE);
                }
                if (species.compareTo(Species.ECOLI) == 0) {
                        dicEd = MAP_EDITION_ECOLI;
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_ECOLI);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_ECOLI);
                }
                if (species.compareTo(Species.FLY) == 0) {
                        dicEd = MAP_EDITION_FLY;                        
                        idMappingFile = new File(Species.getDataFolder(species) + "/"
                                        + ID_MAPPING_FLY);
                        idMappingWebPageFile = new File(Species.getDataFolder(species)
                                        + "/" + WEB_ID_MAPPING_FLY);
                }
                if (species.compareTo(Species.RAT) == 0) {
                    dicEd = MAP_EDITION_RAT;
                    idMappingFile = new File(Species.getDataFolder(species) + "/"
                                    + ID_MAPPING_RAT);
                    idMappingWebPageFile = new File(Species.getDataFolder(species)
                                    + "/" + WEB_ID_MAPPING_RAT);
                }
                if (species.compareTo(Species.ZEBRAFISH) == 0) {
                    dicEd = MAP_EDITION_ZEBRAFISH;
                    idMappingFile = new File(Species.getDataFolder(species) + "/"
                                    + ID_MAPPING_ZEBRAFISH);
                    idMappingWebPageFile = new File(Species.getDataFolder(species)
                                    + "/" + WEB_ID_MAPPING_ZEBRAFISH);
                }
                if (species.compareTo(Species.DICTY) == 0) {
                    dicEd = MAP_EDITION_DICTY;
                    idMappingFile = new File(Species.getDataFolder(species) + "/"
                                    + ID_MAPPING_DICTY);
                    idMappingWebPageFile = new File(Species.getDataFolder(species)
                                    + "/" + WEB_ID_MAPPING_DICTY);
            }
                if (species.compareTo(Species.DOG) == 0) {
                    dicEd = MAP_EDITION_DOG;                    
                    idMappingWebPageFile = new File(Species.getDataFolder(species)
                                    + "/" + WEB_ID_MAPPING_DOG);
            }
                if (species.compareTo(Species.WORM) == 0) {
                    dicEd = MAP_EDITION_WORM;
                    idMappingFile = new File(Species.getDataFolder(species) + "/"
                                    + ID_MAPPING_WORM);
                    idMappingWebPageFile = new File(Species.getDataFolder(species)
                                    + "/" + WEB_ID_MAPPING_WORM);
            }
                
                // Load idMapping dictionary
                if (idMappingFile != null && idMappingWebPageFile != null) {
                        readIdMapping(idMappingFile, species, log);
                        readWebIdMapping(idMappingWebPageFile, species, log);
                        if ((species.compareTo(Species.ECOLI)!=0 && species.compareTo(Species.FLY)!=0 ) && 
                        		(idWebPageMapping == null || idWebPageMapping.size() == 0)) {
                                log.severe("Fatal Error loading idMapping or idWebMapping file.");
                                System.exit(1);
                        }
                }

                ArrayList<File> xrefs = MappingUtils.readDirectory(Species
                                .getDataFolder(species).getAbsolutePath() + "/",
                                "gene_association*" + dicEd + "*gz");

                Map<String, String> dictionary = computeDictionaryUsingGeneAssocFile(
                                xrefs.get(0), species, log);

                // Update idMapping dictionary and edition dictionary
                updateIdMapping(dictionary);

                // Get genes always present
                Set<String> genesAlwaysPresent = getGenesAlwaysPresent(species);

                // Get genes of the last edition
                Set<String> genesLastEdition = getGenesLastEdition(species);

                // Get all gene_association files
                ArrayList<File> geneAssociationFiles = MappingUtils.readDirectory(
                                Species.getDataFolder(species).getAbsolutePath() + "/",
                                "gene_association*gz");

                BufferedWriter bufferedWriterOfSyns2;
                try {

                        for (File file : geneAssociationFiles) {
                        	if(startedition>Integer.valueOf(GenUtils.getEditionNo(file.getName())))
            					continue;
                             log.info("Reading file" + file.getName() + "\n");

                             bufferedWriterOfSyns2 = Species.getWriter(
                                                species,
                                                "replaced_geneids_" + species + "_"
                                                                + GenUtils.getEditionNo(file.getName())
                                                                + ".txt");
                             computeSynGaf(file, dictionary, genesAlwaysPresent,
                                                genesLastEdition, species, log, bufferedWriterOfSyns2);
                             bufferedWriterOfSyns2.close();

                        }

                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }

        }

        /*------------------------------Special case: Yeast----------------------*/
        /**
         * This is special mapping code for yeast
         * @param logger Logger to keep the progress
         * @param startedition Do the mapping from this edition 
         */
        public static void doYeastMapping(Logger logger, Integer startedition) {

                /* Read the precomputed dictionary */
                Map<String, String> dictionary = readYeastDictionary(logger);

                File idMappingWebPageFile = new File(
                                Species.getDataFolder(Species.YEAST) + "/"
                                                + WEB_ID_MAPPING_YEAST);

                readWebIdMapping(idMappingWebPageFile, Species.YEAST, logger);
                if (idWebPageMapping == null || idWebPageMapping.size() == 0) {
                        logger.severe("Fatal Error loading idMapping or idWebMapping file.");
                        System.exit(1);
                }

                if (dictionary == null) {
                        /* We couldn't read the dictionary. Exit the program */
                	    logger.severe("We couldn't read the dictionary. Exit");
                        System.exit(1);
                }

                File idMappingFile = new File(Species.getDataFolder(Species.YEAST)
                                + "/" + ID_MAPPING_YEAST);

                // Load idMapping dictionary
                if (idMappingFile != null) {
                        readIdMapping(idMappingFile, Species.YEAST, logger);
                        if (idMapping == null || idMapping.size() == 0) {
                                logger.severe("Fatal Error loading idMapping file.");
                                System.exit(1);
                        }
                }

                // Update idMapping dictionary and edition dictionary
                updateIdMapping(dictionary);
                                
                /* Read all the gene association files */
                ArrayList<File> gaf = MappingUtils.readDirectory(
                                Species.getDataFolder(Species.YEAST).getAbsolutePath() + "/",
                                "*gene_association*gz");
                
                for (File file : gaf) {
                	if(startedition>Integer.valueOf(GenUtils.getEditionNo(file.getName())))
    					continue;
                    logger.info("Reading file" + file.getName() + "\n");
                    computeYeastGeneAssoc(file, dictionary, logger);
                }

        }

        /**
         * Compute one yeast gene association file to map id
         *
         * @param file The gene association file to compute
         * @param dictionary The special yeast dictionary
         * @param logger Where to record the progress
         */
        private static void computeYeastGeneAssoc(File file,
                        Map<String, String> dictionary, Logger logger) {
                File yeastFileSyns = new File(Species.getDataFolder(Species.YEAST)
                                .getAbsolutePath()
                                + Species.TMP_FOLDER
                                + file.getName()
                                + ".syn");
                if (yeastFileSyns.exists()) {
                        logger.info("File " + yeastFileSyns.getName()
                                        + " already exists, skipping it.");
                        return;
                }
                String edition = GenUtils.getEditionNo(file.getName());
                try {
                        BufferedWriter bufferedWriterOfSyns = Species.getWriter(
                                        Species.YEAST, file.getName() + ".syn");

                        BufferedWriter bufferedWriterOfSyns2 = Species.getWriter(
                                        Species.YEAST,
                                        "replaced_geneids_yeast_"
                                                        + GenUtils.getEditionNo(file.getName()) + ".txt");

                        bufferedWriterOfSyns2
                                        .write("original\t customAlgorithm \t editionNumber\n");

                        BufferedReader readerOfAssocFile = new BufferedReader(
                                        new InputStreamReader(new GZIPInputStream(
                                                        new FileInputStream(file))));
                        String temporaryString;
                        while ((temporaryString = readerOfAssocFile.readLine()) != null) {
                                String[] columnDetail = new String[200];
                                columnDetail = temporaryString.split("\t");
                                if (columnDetail.length < 9)
                                        continue; // This row might be the header so skip it
                                String typeOfId = columnDetail[0];
                                String aUniprot = columnDetail[1];
                                String bSymbol = columnDetail[2];
                                String cSynonyms = columnDetail[10];
                                String uniprot = "";
                                String originalId = columnDetail[1];

                                /* First add zeroes if needed */
                                if (columnDetail[0].compareTo("SGD") == 0) {
                                        // uniprot has less zeros, S0007287
                                        if (Integer.valueOf(GenUtils.getEditionNo(file.getName())) < 25
                                                        && !(aUniprot.compareTo("") == 0)) {
                                                aUniprot = ParseGO.addZeros(aUniprot);
                                                columnDetail[1] = aUniprot;
                                        }
                                }

                                if (idWebPageMapping.containsKey(aUniprot)) {
                                        uniprot = idWebPageMapping.get(aUniprot);
                                        logger.fine("Web dictionary: " + aUniprot + " -> "
                                                        + uniprot);
                                        columnDetail[1] = uniprot;
                                } else {
                                        /* if this is not an uniprot use the idMapping file */
                                        if (typeOfId.compareToIgnoreCase("Uniprot") != 0
                                                        && typeOfId.compareToIgnoreCase("UniProtKB") != 0) {
                                                uniprot = lookupUniprotUsingIdMapping(aUniprot, logger);
                                                if (uniprot.compareTo("") != 0)
                                                        columnDetail[1] = uniprot;
                                        } else {
                                                /* otherwise use mapping algorithm */
                                                if (dictionary.containsKey(aUniprot)) {
                                                        /*
                                                         * The id is found and in the dictionary, then we
                                                         * should replace it in the gene association file
                                                         */
                                                        columnDetail[1] = dictionary.get(aUniprot);
                                                } else {
                                                        /* We will check the synonyms of this id */
                                                        uniprot = checkYeastSyns(bSymbol + "|" + cSynonyms,
                                                                        dictionary, bSymbol, logger);
                                                        if (!(uniprot.compareTo("") != 0))
                                                                columnDetail[1] = uniprot;
                                                }

                                        }
                                }
                                for (int h = 0; h < columnDetail.length; h++) {
                                        bufferedWriterOfSyns.write(columnDetail[h] + "\t");
                                }
                                bufferedWriterOfSyns.write("\n");
                                bufferedWriterOfSyns2.write(originalId + "\t" + columnDetail[1]
                                                + "\t" + edition + "\n");
                        }
                        readerOfAssocFile.close();
                        bufferedWriterOfSyns.close();
                        bufferedWriterOfSyns2.close();
                } catch (IOException ex) {
                        ex.printStackTrace();
                }
        }

        /**
         * Given a list of synonyms it will return the most updated id that is most likely to map the synonyms 
         * @param synonyms List of synonyms divided by |
         * @param dictionary The dictionary to be used
         * @param symbol The symbol. The list of synonyms might include the gene symbol, the algorithm has 
         * to tell the difference between a synonym and a symbol. This variable indicates explicitly which synonim 
         * is a gene symbol
         * @param log Where to keep track the progress
         * @return The most likely id for the list of synonyms, empty string in there is no candidate
         */
        private static String checkYeastSyns(String synonyms,
                        Map<String, String> dictionary, String symbol, Logger log) {
                synonyms = removeDuplicate(synonyms);
                String uniprot = "";
                // tokenize synonyms by |
                StringTokenizer tokenizer = new StringTokenizer(synonyms, "|");
                HashMap<String, Integer> uniprotcandidates = new HashMap<String, Integer>(
                                4);
                Integer matchs = 0;
                while (tokenizer.hasMoreElements()) {
                        String syn = tokenizer.nextToken();
                        if (dictionary.containsKey(syn)) {
                                matchs++;
                                uniprot = dictionary.get(syn);
                                if (uniprot.contains("|")) {
                                        StringTokenizer st2 = new StringTokenizer(uniprot, "|");
                                        while (st2.hasMoreElements()) {
                                                String uniptmp = st2.nextToken();
                                                if (!uniprotcandidates.containsKey(uniptmp)) {
                                                        uniprotcandidates.put(uniptmp, 1);
                                                } else {
                                                        Integer tmp = uniprotcandidates.get(uniptmp);
                                                        uniprotcandidates.put(uniptmp, tmp + 1);
                                                }
                                        }
                                } else {
                                        if (!uniprotcandidates.containsKey(uniprot)) {
                                                uniprotcandidates.put(uniprot, 1);
                                        } else {
                                                Integer tmp = uniprotcandidates.get(uniprot);
                                                uniprotcandidates.put(uniprot, tmp + 1);
                                        }
                                }
                        }
                }

                int sanitycheck = 0;
                Integer totalmatchs = 0;
                String reskey = "";
                for (String unip : uniprotcandidates.keySet()) {
                        totalmatchs += uniprotcandidates.get(unip);
                }
                for (String unip : uniprotcandidates.keySet()) {
                        if (uniprotcandidates.get(unip) >= Math.ceil(totalmatchs / 2d)) {
                                sanitycheck++;
                                if (reskey.compareTo("") == 0)
                                        reskey = unip;
                                else
                                        reskey += "|" + unip + "|";
                        }
                }
                if (sanitycheck > 1) {
                        log.fine("More than one possible candidate for " + synonyms + " :"
                                        + reskey);
                        if (dictionary.containsKey(symbol)
                                        && !dictionary.get(symbol).contains("|")) {
                                log.fine("Taking " + dictionary.get(symbol));
                                return dictionary.get(symbol);
                        }
                }
                if (!(reskey.compareTo("") == 0)) {
                        log.fine("Choosing " + reskey + " for " + synonyms);
                }
                return reskey;
        }

        /**
         * Reads the pre computed dictionary for yeast. This is a special case
         * the dictionary has been compiled from http://www.uniprot.org/docs/yeast
         * @param logger Where to keep track the progress
         * @return A map of synonym to uniprot id 
         */
        private static Map<String, String> readYeastDictionary(Logger logger) {
                Map<String, String> dictionary = new HashMap<String, String>();
                File fileOfSyncDictionary = new File(Species.getDataFolder(
                                Species.YEAST).getAbsolutePath()
                                + Species.TMP_FOLDER + "dic.txt");
                if (!fileOfSyncDictionary.exists()) {
                        logger.info("The yeast dictionary is not in "
                                        + fileOfSyncDictionary.getAbsolutePath());
                        logger.info("This is a special case the dictionary has been compiled from http://www.uniprot.org/docs/yeast");
                        return null;
                }
                BufferedReader readerOfSyns;
                try {
                        readerOfSyns = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(fileOfSyncDictionary)));
                        String tmp;
                        logger.info("Reading gene assoc file");
                        while ((tmp = readerOfSyns.readLine()) != null) {
                                String[] columnDetail = new String[5];
                                columnDetail = tmp.split("\t");
                                if (columnDetail.length < 2)
                                        continue;
                                dictionary.put(columnDetail[0], columnDetail[1]);
                        }
                        readerOfSyns.close();
                } catch (FileNotFoundException exception) {

                        exception.printStackTrace();
                } catch (IOException exception) {
                        exception.printStackTrace();
                }

                return dictionary;
        }

        /**
         * Reads the pre computed dictionary for ecoli. This is a special case
         * the dictionary has been compiled from http://www.uniprot.org/docs/ecoli
         * @param logger Where to keep track the progress
         * @return A map of synonym to uniprot id 
         */
        private static Map<String, String> readEcoliDictionary(Logger logger) {
                Map<String, String> dictionary = new HashMap<String, String>();
                File fileOfSyncDictionary = new File(Species.getDataFolder(
                                Species.ECOLI).getAbsolutePath()
                                + Species.TMP_FOLDER + "dic.txt");
                if (!fileOfSyncDictionary.exists()) {
                        logger.info("The ecoli dictionary is not in "
                                        + fileOfSyncDictionary.getAbsolutePath());
                        logger.info("This is a special case the dictionary has been compiled from http://www.uniprot.org/docs/ecoli");
                        return null;
                }
                BufferedReader readerOfSyns;
                try {
                        readerOfSyns = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(fileOfSyncDictionary)));
                        String tmp;
                        logger.info("Reading dictionary");
                        while ((tmp = readerOfSyns.readLine()) != null) {
                                String[] columnDetail = new String[5];
                                columnDetail = tmp.split("\t");
                                if (columnDetail.length < 2)
                                        continue;
                                dictionary.put(columnDetail[0].toLowerCase(), columnDetail[1]);
                                dictionary.put(columnDetail[0], columnDetail[1]);
                        }
                        readerOfSyns.close();
                } catch (FileNotFoundException exception) {

                        exception.printStackTrace();
                } catch (IOException exception) {
                        exception.printStackTrace();
                }

                return dictionary;
        }

        /**
         * Reads the pre computed dictionary for fly. This is a special case
         * the dictionary has been compiled from http://www.uniprot.org/docs/fly
         * @param logger Where to keep track the progress
         * @return A map of synonym to uniprot id 
         */
        private static Map<String, String> readFlyDictionary(Logger logger) {
                Map<String, String> dictionary = new HashMap<String, String>();
                File fileOfSyncDictionary = new File(Species.getDataFolder(
                                Species.FLY).getAbsolutePath()
                                + Species.TMP_FOLDER + "dic.txt");
                if (!fileOfSyncDictionary.exists()) {
                        logger.info("The fly dictionary is not in "
                                        + fileOfSyncDictionary.getAbsolutePath());
                        logger.info("This is a special case the dictionary has been compiled from http://www.uniprot.org/docs/fly");
                        return null;
                }
                BufferedReader readerOfSyns;
                try {
                        readerOfSyns = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(fileOfSyncDictionary)));
                        String tmp;
                        logger.info("Reading dictionary");
                        while ((tmp = readerOfSyns.readLine()) != null) {
                                String[] columnDetail = new String[5];
                                columnDetail = tmp.split("\t");
                                if (columnDetail.length < 2)
                                        continue;
                                dictionary.put(columnDetail[0], columnDetail[1]);
                                dictionary.put(columnDetail[0].toLowerCase(), columnDetail[1]);
                        }
                        readerOfSyns.close();
                } catch (FileNotFoundException exception) {

                        exception.printStackTrace();
                } catch (IOException exception) {
                        exception.printStackTrace();
                }

                return dictionary;
        }
        
}