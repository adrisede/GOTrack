package ubc.pavlab.gotrack.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubic.erminej.data.Gene;
import ubic.erminej.data.GeneSetTerm;
/**
 * 
 * This class compute the gomatrix files in order to calculate the jaccard similarity
 * for each gene.
 * 
 * @author asedeno
 *
 */
public class ComputeMatrixAAP {

	/**
	 * This function creates a gomatrix file for an specific edition
	 * A gomatrix file is just a file that lists genes and the go terms
	 * it is annotated to in the given edition.
	 * 
	 * This files are used to compute how similar a gene is to itself
	 * across editions using the jaccard similarity function.
	 * 
	 * These files are needed because the number of editions increase over time,
	 * therefore, it will be really expensive to get this information form the gene
	 * annotation file and keep all the editions in memory
	 * 
	 * @param species Species to compute
	 * @param assoc  The gene association file	 
	 * @param genesFile The list of genes to consider(Genes always present or almost always present)
	 * @param goTerms The termdb for assoc
	 * @param editionNumber The edition number
	 */
	public static void runGoMatrix(String species, File assoc,			 
			String genesFile, String goTerms, int editionNumber) {
		try {						
				String edition = editionNumber + "";
				try {					

					ArrayList<Gene> genes = new ArrayList<Gene>();
					if (genesFile != null) {
						// read in list of genes.
						BufferedReader br = Species.getReader(species,
								genesFile);
						String line;
						while ((line = br.readLine()) != null) {
							String[] columnDetail = new String[11];
							columnDetail = line.split("\t");
							if (columnDetail.length >= 1)
								genes.add(new Gene(columnDetail[0]));
						}
						br.close();
					}

					HashMap<String, HashSet<String>> annots= new HashMap<String, HashSet<String>>();					
					if (genesFile != null) {
						annots = (HashMap<String, HashSet<String>>) readGotermPerGene(assoc);					
					}

					ArrayList<GeneSetTerm> ttu = new ArrayList<GeneSetTerm>();
					if (goTerms != null) {
						BufferedReader br = Species.getReader(species, goTerms);
						String line;
						while ((line = br.readLine()) != null) {
							String[] columnDetail = new String[11];
							columnDetail = line.split("\t");
							ttu.add(new GeneSetTerm(columnDetail[0]));
						}
						br.close();
					}
					File gomatrixDir = Species.getGoMatrixFolder(species);
					String outputName = goTerms + "_" + genesFile + "_"
							+ edition + ".gomatrix.txt";
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							gomatrixDir.getAbsolutePath() + "/" + outputName));

					bw.write("%%MatrixMarket matrix coordinate real general\n");
					bw.write(genes.size() + " " + ttu.size() + " " + 10000000
							+ "\n");				
					
					for (Gene gene : genes) {
						if (annots.containsKey(gene.getSymbol())) {
							HashSet<String> geneTerms = annots.get(gene.getSymbol());							
							for (GeneSetTerm term : ttu) {
								if (geneTerms.contains(term.getId())) {
									bw.write(gene.getSymbol() + " " + term.getId() + " 1\n");
								}								
							}
						}else{
							//This gene is not Always Present
							bw.write(gene.getSymbol() + " -1 1\n");
						}					
					}

					bw.close();
				} catch (IOException e) {
					System.err.println("Fatal error in goMatrix: NO GENES: "
							+ e.toString());					
				} catch (IllegalStateException e) {
					System.err.println("Fatal error using "+genesFile+" in goMatrix: NO GENES: "
							+ e.toString());
					 e.printStackTrace();
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Auxiliary function that takes a gene association file and maps each gene to the set GO terms
	 * annotated to that gene
	 * @param f Gene association File
	 * @return
	 */
	private static Map<String, HashSet<String>> readGotermPerGene(
			File f) {
		Map<String, HashSet<String>> moirai = new HashMap<String, HashSet<String>>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
					new FileInputStream(f))));

			String temporary = reader.readLine();
			while ((temporary = reader.readLine()) != null) {
				if (temporary.length()==0)
					continue;
				String[] column = temporary.split("\t");
				if (column.length<5)
					continue;
				String gene = column[1];
				String goterm = column[4];								
				HashSet<String> predForGene = moirai.remove(gene);
				if (predForGene == null) {
					predForGene = new HashSet<String>();
				}
				String pred = goterm;
				predForGene.add(pred);				
				moirai.put(gene, predForGene);
			}
			reader.close();
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return moirai;
	}
	
}
