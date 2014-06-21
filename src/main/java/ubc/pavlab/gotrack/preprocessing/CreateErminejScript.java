package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.GOtrack;
import ubc.pavlab.gotrack.mapping.MappingUtils;
import ubc.pavlab.gotrack.utils.GenUtils;

public class CreateErminejScript {

	
	public void createScript (ArrayList<File> files, String species, Logger logger){
		 GenUtils.matchdates2edition = GenUtils.readMatchingFile(species,
		 GOtrack.edition2DatesFile, logger);
		 try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("erminejScript_"+species+".sh")));
			for(File assoc: files){
			     String termdb = GenUtils.getTermdbFromGeneAssoc(species, assoc, logger);
			     if(termdb!=null && termdb.compareTo("")!=0)
			       bw.write("ermineJ.sh -s geneScores.txt -c ~/ermineJ.data/"+termdb+ " -a ~/ermineJ.data/"+assoc.getName()+
			    		   " -n ORA -t 0.0001 > results"+GenUtils.getEditionNo(assoc.getName())+"_"+termdb+".txt \n");
			 }
			bw.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		 
		 
		 
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * ermineJ.sh -s geneScores.txt -c ~/ermineJ.data/go_daily-termdb.rdf-xml.gz \
   -a ~/ermineJ.data/annotationfile1.txt.gz -n ORA -t 0.0001 \
    > resultados con edicion x de annotation file y edicion x de arbol.txt
		 * */
		
		
		final Logger log = GOtrack.getLoggerforSpecies(args[0]);
		ArrayList<File> geneAssociationFiles = MappingUtils.readDirectory(
				Species.getDataFolder(args[0]).getAbsolutePath() + "/",
				"gene_association*gz");
		CreateErminejScript ces = new CreateErminejScript();
		ces.createScript(geneAssociationFiles, args[0], log);
		
		
	}

}
