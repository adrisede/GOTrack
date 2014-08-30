package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;

public class GenesAddedDeleted {

	/**
	 * This class will tell which genes are added/removed from one edition to the following
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String species = args[0];		
		String takeHistory = args[1];
		getAddedDeletedGenes(species, Boolean.valueOf(takeHistory));

	}
	
	public static File[] getAllFilesForSpecies(String species){
		File[] assocArrayOfFiles = GenUtils.readDirectory(
				Species.getDataFolder(species), "gene_association*",
				null);
		
		Arrays.sort(assocArrayOfFiles, ExploreGenes.getFileComparator());
		
		return assocArrayOfFiles;
	}

	
	public static void getAddedDeletedGenes(String species, Boolean takeHistory) {
		File [] assocArrayOfFiles = getAllFilesForSpecies(species);		
		HashSet<String> genesPrevEd = new HashSet<String>();
		HashSet<String> genesNextEd = new HashSet<String>();
		genesPrevEd = getGenesInGAF(assocArrayOfFiles[0]);
		
	    try {
	    	String outputfile = Species.getDataFolder(species) + "/AnalysisNew-RemovedGenes_" + species + "_NOhistory.txt";
	    	if(takeHistory)
	    		outputfile = Species.getDataFolder(species) + "/AnalysisNew-RemovedGenes_" + species + "_history.txt";
	    	
	    	BufferedWriter buf = new BufferedWriter(new FileWriter(outputfile));
			buf.write("PrevEd\tNextEd\tnewGenes\tremGenes\n");
			for(int i =1; i<assocArrayOfFiles.length-1;i++){
				System.out.print("Compare "+assocArrayOfFiles[i].getName()+" to "+assocArrayOfFiles[i].getName()+"\n");
				int prevEdition = Integer.valueOf(GenUtils.getEditionNo(assocArrayOfFiles[i-1].getName()));
				int nextEdition = Integer.valueOf(GenUtils.getEditionNo(assocArrayOfFiles[i].getName()));
				genesNextEd = getGenesInGAF(assocArrayOfFiles[i]);
				Integer newGenes = newGenes(genesPrevEd, genesNextEd);
				Integer remGenes = removedGenes(genesPrevEd, genesNextEd);
				buf.write(prevEdition+"\t"+nextEdition+"\t"+newGenes+"\t"+remGenes+"\n");
				if(takeHistory)
					genesNextEd.addAll(genesPrevEd);
				genesPrevEd = genesNextEd;
			}
			buf.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
	}
	
	public static HashSet<String> getGenesInGAF(File gaf){
		HashSet<String> genes = new HashSet<String>();
		BufferedReader bufferedReaderOfAssocFiles;
		try {
			bufferedReaderOfAssocFiles = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(gaf))));
			String line;
			/* skip first line. Some archives have header */
			line = bufferedReaderOfAssocFiles.readLine();
			/* Read line by line */
			while ((line = bufferedReaderOfAssocFiles.readLine()) != null) {

				String[] columnDetail = new String[11];
				columnDetail = line.split("\t");
				/* Sanity check. Make sure we have read 3 columns or more */
				if (columnDetail.length <= 2)
					continue;
				genes.add(columnDetail[1]);
			}
         bufferedReaderOfAssocFiles.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				return genes;		
	}
	
	public static Integer newGenes(HashSet<String> genesPrevEd, HashSet<String> genesNextEd){
		Integer newGenes = 0;
		/*genes that are in next edition but not in prev edition -> added*/
		for(String g:genesNextEd){
			if(!genesPrevEd.contains(g))
				newGenes++;
		}
		return newGenes;
	}
	
	public static Integer removedGenes(HashSet<String> genesPrevEd, HashSet<String> genesNextEd){
		Integer remGenes = 0;
		/*Genes that are in prev edition but no in next edition -> removed*/
		for(String g:genesPrevEd){
			if(!genesNextEd.contains(g))
				remGenes++;
		}
		return remGenes;
	}

}
