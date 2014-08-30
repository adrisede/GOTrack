package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.mapping.MappingUtils;

/**
 * This class updates the pubmed id in the gene association files 
 * 
 * @author asedeno
 *
 */
public class PubmedPreprocess {

	
	private static String PUBMED_MAPS_DIR = "pubmedmaps";
	private ArrayList<String> mappingFiles = new ArrayList<String>();
	Map<String, String> pubmedDictionary = new HashMap<String, String>();
	/**
	 * Constructor. It will set up some variables
	 */
	public PubmedPreprocess(){
		mappingFiles.add("MuId-PmId-1.ids.gz");
		mappingFiles.add("MuId-PmId-2.ids.gz");
		mappingFiles.add("MuId-PmId-3.ids.gz");
		mappingFiles.add("MuId-PmId-4.ids.gz");
		mappingFiles.add("MuId-PmId-5.ids.gz");
		mappingFiles.add("MuId-PmId-6.ids.gz");
		loadDictionary();
	}
	
	/**
	 * Load a dictionary using the MuId-PmId-*.ids.gz files
	 */
	private void loadDictionary() {
		for(String mapFile: mappingFiles){
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(
						Species.DATA + PUBMED_MAPS_DIR+ mapFile))));
				String[] columnDetail = new String[15];
				String line;
				while ((line = reader.readLine()) != null) {					
					columnDetail = line.split("\t");
					if(columnDetail.length<2)
						continue;
					pubmedDictionary.put(columnDetail[0],columnDetail[1]);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}//end files loop
	}

	/**
	 * Compute all the gene association files of a species and update the pubmed id using
	 * the dictionary
	 * @param species
	 */
	public void processSpecies(String species){
		try {
			BufferedWriter dicWriter = Species.getWriter(species, "dictionary_"+species+".txt");
		//clean pubmedDictionary
			
			// get all gene_association
			ArrayList<File> geneAssocs = MappingUtils.readDirectory(
					Species.getDataFolder(species).getAbsolutePath(),"gene_association*gz");
			//for each gene association
			for(File geneAssoc:geneAssocs){
				BufferedReader bufferedReaderOfAssoc = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(
								new FileInputStream(geneAssoc))));
				BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
						geneAssoc.getName() + ".syn");
				String temporaryString;
				while ((temporaryString = bufferedReaderOfAssoc.readLine()) != null) {
					String[] columnDetail = new String[200];
					columnDetail = temporaryString.split("\t");
					if (columnDetail.length < 5)
						continue; // This row might be the header so skip it					
								
			   // get the pubmed id
					String pubmedid = columnDetail[5];
					String newPubmed = pubmedid.substring(0,pubmedid.indexOf(":"));
					if(pubmedid.contains("PUBMED")){
					// if the string contains a "PUBMED" look for it in the big dictionary
						if(pubmedDictionary.containsKey(newPubmed)){
			        //if the pubmed is in the dictionary save it to pubmedDictionary
							newPubmed="PMID:"+pubmedDictionary.get(newPubmed);
						}
						columnDetail[5]=newPubmed;
					}
					for (int i = 0; i < columnDetail.length; i++) {
						bufferedWriterOfSyns.write(columnDetail[i] + "\t");
					}
					bufferedWriterOfSyns.write("\n");
				}
				bufferedReaderOfAssoc.close();
				bufferedWriterOfSyns.close();
			}
		//save pubmedDictionary into a file
			for(String key:pubmedDictionary.keySet()){
				dicWriter.write(key+"\t"+pubmedDictionary.get(key)+"\n");
			}
			
		} catch (IOException e) {			
			e.printStackTrace();
		}			
	}

}
