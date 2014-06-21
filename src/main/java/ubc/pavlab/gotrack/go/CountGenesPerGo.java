package ubc.pavlab.gotrack.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;

/**
 * 
 * @author asedeno
 * 
 */
public class CountGenesPerGo {

	/**
	 * 
	 * @param file
	 * @param editionNo
	 * @param countEdition
	 * @param logger
	 * @return
	 */
	private static Map<String, Integer[]> processEditionDirectGoTerms(
			File file, Integer editionNo, Map<String, Integer[]> countEdition,
			Logger logger) {
		Map<String, Set<String>> visitedGenes = new HashMap<String, Set<String>>();
		try {
			BufferedReader readerFileForEdition = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(file))));
			logger.info("Reading gene assoc file " + file.getName());
			String temporary;
			while ((temporary = readerFileForEdition.readLine()) != null) {
				String[] columnDetail = temporary.split("\t");
				if (columnDetail.length < 2) {
					continue; // This row might be the header so skip it
				}
				String id = columnDetail[1];
				String go = columnDetail[4];							
				// Sanity check
				// if we find idx -> go more than once in the file
				// we will only count one
				if (visitedGenes.containsKey(id)) {
					Set<String> tvisitedGenesWithoutId = visitedGenes
							.remove(id);
					tvisitedGenesWithoutId.add(go);
					visitedGenes.put(id, tvisitedGenesWithoutId);
				} else {
					Set<String> treeSet = new TreeSet<String>();
					treeSet.add(go);
					visitedGenes.put(id, treeSet);
				}

				if (!countEdition.containsKey(id)) {
					Integer editionsInteger[] = new Integer[500];
					editionsInteger[editionNo] = -1;
					countEdition.put(id, editionsInteger);
				}
			}
			readerFileForEdition.close();

			for (String key : visitedGenes.keySet()) {				
				Integer[] integerCountKeys = countEdition.remove(key);
				Integer numgos = visitedGenes.get(key).size();				
				integerCountKeys[editionNo] = numgos;
				countEdition.put(key, integerCountKeys);
			}
		} catch (Exception e) {
			logger.severe("Error reading " + file.getName());
		}
		return countEdition;
	}

	/**
	 * 
	 * @param file
	 * @param editionNo
	 * @param countEdition
	 * @param logger
	 * @return
	 */
	private static Map<String, Integer[]> processEditionInferedGoTerms(
			File file, Integer editionNo, Map<String, Integer[]> countEdition,
			Logger logger, Boolean perGene) {
		Map<String, Set<String>> visitedGenes = new HashMap<String, Set<String>>();
		try {
			BufferedReader readerFileForEdition = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));
			logger.info("Reading file " + file.getName());
			String temporary;
			Integer analysisColumn;
			if (perGene)
				analysisColumn = 0;
			else
				// per Symbol
				analysisColumn = 1;
			while ((temporary = readerFileForEdition.readLine()) != null) {
				String[] columnDetail = temporary.split("\t");
				if (columnDetail.length < 2) {
					continue; // This row might be the header so skip it
				}
				String id = columnDetail[analysisColumn];

				String parents = columnDetail[3];
				// Sanity check
				// if we find idx -> go more than once in the file
				// we will only count one
				if (visitedGenes.containsKey(id)) {
					Set<String> tvisitedGenesWithoutId = visitedGenes
							.remove(id);
					StringTokenizer st = new StringTokenizer(parents, "|");
					while (st.hasMoreTokens()) {
						tvisitedGenesWithoutId.add(st.nextToken());
					}
					visitedGenes.put(id, tvisitedGenesWithoutId);
				} else {
					Set<String> treeSet = new TreeSet<String>();
					StringTokenizer st = new StringTokenizer(parents, "|");
					while (st.hasMoreTokens()) {
						treeSet.add(st.nextToken());
					}
					visitedGenes.put(id, treeSet);
				}

				if (!countEdition.containsKey(id)) {
					Integer editionsInteger[] = new Integer[500];
					editionsInteger[editionNo] = -1;
					countEdition.put(id, editionsInteger);
				}
			}
			readerFileForEdition.close();

			for (String key : visitedGenes.keySet()) {
				Integer[] integerCountKeys = countEdition.remove(key);
				Integer numgos = visitedGenes.get(key).size();
				integerCountKeys[editionNo] = numgos;
				countEdition.put(key, integerCountKeys);
			}
		} catch (Exception e) {
			logger.severe("Error reading " + file.getName());
		}
		return countEdition;
	}

	/**
	 * 
	 * @param file
	 * @param editionNo
	 * @param countEdition
	 * @param logger
	 * @return
	 */
	private static Map<String, Integer[]> processEdition(File file,
			Integer editionNo, Map<String, Integer[]> countEdition,
			Logger logger) {
		HashMap<String, Integer> visitedIdsGos = new HashMap<String, Integer>();
		try {
			BufferedReader ireaderInput = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(file))));
			logger.info("Reading gene assoc file " + file.getName());
			String tmp;
			while ((tmp = ireaderInput.readLine()) != null) {
				String[] columnDetail = new String[200];
				columnDetail = tmp.split("\t");
				if (columnDetail.length < 2)
					continue; // This row might be the header so skip it
				String id = columnDetail[1];
				String go = columnDetail[4];
				// Sanity check
				// if we find idx -> go more than once in the file
				// we will only count one
				if (visitedIdsGos.containsKey(id + go))
					continue;
				else
					visitedIdsGos.put(id + go, 1);

				if (!countEdition.containsKey(go)) {
					Integer t[] = new Integer[200];
					t[editionNo] = 1;
					countEdition.put(go, t);
				} else {
					Integer t[] = countEdition.remove(go);
					if (t[editionNo] == null)
						t[editionNo] = 1;
					else
						t[editionNo]++;
					countEdition.put(go, t);
				}
			}
			ireaderInput.close();
		} catch (Exception e) {

		}

		return countEdition;
	}

	/**
	 * Counts the number of go terms per gene in all editions
	 *  
	 * @param species Species to compute
	 * @param logger Logger to write errors and info
	 * @param numEditions Total number of editions
	 * @return Map that will associate a gene to an array of Integers of size numEditions
	 * each entry of the array is the number of go terms of that gene
	 */
	public static Map<String, Integer[]> countDirectGoPerGene(String species,
			Logger logger, Integer numEditions) {

		File fileSpecies = Species.getDataFolder(species);
		File geneAssocSyn[] = GenUtils.readDirectory(fileSpecies,
				"*gene_association*gz", logger);
		Map<String, Integer[]> count = new HashMap<String, Integer[]>();
		int i = 1;
		for (File ffile : geneAssocSyn) {
			count = processEditionDirectGoTerms(ffile,
					Integer.valueOf(GenUtils.getEditionNo(ffile.getName())),
					count, logger);
		}
		try {

			BufferedWriter bspeciesWriter = new BufferedWriter(new FileWriter(
					fileSpecies.getAbsolutePath()
							+ "/tmp/countDirectTermspergene.txt"));
			bspeciesWriter.write("Gene");

			for (String geneid : count.keySet()) {
				Integer editionsArray[] = count.get(geneid);
				for (int j = 1; j < numEditions + 1 && i == 1; j++) {
					bspeciesWriter.write("\t" + j);
				}
				i = 2;
				bspeciesWriter.write("\n");
				bspeciesWriter.write(geneid);
				int k = 0;
				for (Integer editionIndividual : editionsArray) {
					if (k++ == 0)
						continue;
					if ((k) > numEditions + 1)
						break;
					if (editionIndividual == null)
						bspeciesWriter.write("\t");
					else
						bspeciesWriter.write("\t" + editionIndividual);
				}
				bspeciesWriter.write("\n");
			}
			bspeciesWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return count;
	}

	/**
	 * This function counts the number of inferred terms per gene in all editions
	 * @param species Species to compute
	 * @param logger Logger to keep track of the errors
	 * @param numEditions Total number of editions
	 * @return Map that will associate a gene to an array of Integers of size numEditions
	 * each entry of the array is the number of inferred go terms of that gene
	 */
	public static Map<String, Integer[]> countInferredGoPerGene(String species,
			Logger logger, Integer numEditions) {

		File fileSpecies = Species.getTmpFolder(species);
		File geneAssocSyn[] = GenUtils.readDirectory(fileSpecies,
				"gene_association*parents", logger);
		Map<String, Integer[]> count = new HashMap<String, Integer[]>();
		int i = 1;
		for (File ffile : geneAssocSyn) {
			count = processEditionInferedGoTerms(ffile,
					Integer.valueOf(GenUtils.getEditionNo(ffile.getName())),
					count, logger, true);
		}
		try {

			BufferedWriter bspeciesWriter = new BufferedWriter(new FileWriter(
					fileSpecies.getAbsolutePath()
							+ "/countInferredTermspergene.txt"));
			bspeciesWriter.write("Gene");

			for (String geneid : count.keySet()) {
				Integer editionsArray[] = count.get(geneid);
				for (int j = 1; j < numEditions + 1 && i == 1; j++) {
					bspeciesWriter.write("\t" + j);
				}
				i = 2;
				bspeciesWriter.write("\n");
				bspeciesWriter.write(geneid);
				int k = 0;
				for (Integer editionIndividual : editionsArray) {
					if (k++ == 0)
						continue;
					if ((k) > numEditions + 1)
						break;
					if (editionIndividual == null)
						bspeciesWriter.write("\t");
					else
						bspeciesWriter.write("\t" + editionIndividual);
				}
				bspeciesWriter.write("\n");
			}
			bspeciesWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return count;
	}

	/**
	 * This function counts the number of inferred terms per symbol in all editions
	 * @param species Species to compute
	 * @param logger Logger to keep track of the errors
	 * @param numEditions Total number of editions
	 * @return Map that will associate a symbol to an array of Integers of size numEditions
	 * each entry of the array is the number of inferred go terms of that symbol
	 */
	public static Map<String, Integer[]> countInferredGoPerSymbol(
			String species, Logger logger, Integer numEditions) {

		File fileSpecies = Species.getTmpFolder(species);
		File geneAssocSyn[] = GenUtils.readDirectory(fileSpecies,
				"*gene_association*parents", logger);
		Map<String, Integer[]> count = new HashMap<String, Integer[]>();		
		for (File ffile : geneAssocSyn) {
			count = processEditionInferedGoTerms(ffile,
					Integer.valueOf(GenUtils.getEditionNo(ffile.getName())),
					count, logger, false);
		}
		try {

			BufferedWriter bspeciesWriter = new BufferedWriter(new FileWriter(
					fileSpecies.getAbsolutePath()
							+ "/countInferredTermsperSymbol.txt"));
			bspeciesWriter.write("Symbol");

			for (String geneid : count.keySet()) {
				Integer editionsArray[] = count.get(geneid);

				bspeciesWriter.write("\t" + numEditions);
				bspeciesWriter.write("\n");
				bspeciesWriter.write(geneid);
				if (editionsArray[numEditions ] == null)
					bspeciesWriter.write("\t");
				else
					bspeciesWriter.write("\t" + editionsArray[numEditions ]);

				bspeciesWriter.write("\n");
			}
			bspeciesWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return count;
	}

	/**
	 * Counts number of genes annotated to a go term over the time
	 * @param species Species to compute
	 * @param logger Logger to keep track of the error messages
	 * @param numEditions Total number of editions
	 * @return Map that will associate a go term to an array of Integers of size numEditions
	 * each entry of the array is the number of genes annotated to that go term
	 */
	public static Map<String, Integer[]> countGenesperGo(String species,
			Logger logger, Integer numEditions) {

		File filesForSpecies = Species.getDataFolder(species);
		File geneAssocSyn[] = GenUtils.readDirectory(filesForSpecies,
				"*gene_association*gz", logger);
		Map<String, Integer[]> count = new HashMap<String, Integer[]>();
		int i = 1;
		for (File file : geneAssocSyn) {
			count = processEdition(file,
					Integer.valueOf(GenUtils.getEditionNo(file.getName())),
					count, logger);
		}

		try {
			BufferedWriter writerSpecies = new BufferedWriter(new FileWriter(
					filesForSpecies.getAbsolutePath()
							+ "/tmp/countGenesperGoTerm.txt"));
			writerSpecies.write("Goterm");

			for (String go : count.keySet()) {
				Integer editionArray[] = count.get(go);
				for (int j = 1; j < numEditions + 1 && i == 1; j++) {
					writerSpecies.write(" " + j);
				}
				i = 2;
				writerSpecies.write("\n");
				writerSpecies.write(go);
				int k = 0;
				for (Integer edition : editionArray) {
					if (k++ == 0)
						continue;
					if (k > numEditions + 1)
						break;
					if (edition == null)
						writerSpecies.write(" 0");
					else
						writerSpecies.write(" " + edition);
				}
				writerSpecies.write("\n");
			}
			writerSpecies.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return count;
	}
	
	/**
	 * This function writes a file that combines the directs and inferred counts per gene
	 * @param species Species to compute
	 * @param numberOfEditions Total number of editions
	 * @param directs Output of function countDirectGoPerGene
	 * @param inferred Output of function countInferredGoPerGene
	 */
	public static void combineDirectsInferredCounst( String species,Integer numberOfEditions, Map<String, Integer[]> directs,  Map<String, Integer[]> inferred){
		try {
			BufferedWriter writer = Species.getWriter(species, "directsInferred.data");
			String di="",in="";
			for(String id: directs.keySet()){
				Integer[] dirs = directs.get(id);
				Integer[] infs = inferred.get(id);				
				for(int i = 1; i< numberOfEditions+1;i++){
					if(dirs!=null)
					   di = String.valueOf(dirs[i]);
					if(infs!=null)
						in = String.valueOf(infs[i]);
					
					if(dirs==null || dirs[i]==null)
						di = "\\N";
					if(infs == null || infs[i]==null)
						in = "\\N";
														
					writer.write(id+"\t"+di+"\t"+in+"\t"+i+"\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Main function of this class for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		
		String speciesToCompute = "human";
		final Logger log = GOtrack.getLoggerforSpecies(speciesToCompute);
		Integer numEditions = 131;
		CountGenesPerGo.countGenesperGo(speciesToCompute, log, numEditions);
		Map<String, Integer[]> directs = CountGenesPerGo.countDirectGoPerGene(speciesToCompute, log,
				numEditions);
		Map<String, Integer[]> inferred = CountGenesPerGo.countInferredGoPerGene(speciesToCompute, log, numEditions);
		CountGenesPerGo.countInferredGoPerSymbol(speciesToCompute, log, numEditions);
		
		CountGenesPerGo.combineDirectsInferredCounst(speciesToCompute,numEditions, directs, inferred);
	}
}
