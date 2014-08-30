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
import java.util.List;
import java.util.Map;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
/**
 * This class computes the jaccard similarity. That is how similar is a gene to itself across different goa editions
 * 
 * 
 * @author asedeno
 *
 */
public class SimilarityAnalysis {

	/**
	 * This variable saves the state of each gene in the last edition
	 */
	private HashMap<String, ArrayList<JaccardEntry>> analysisB = new HashMap<String, ArrayList<JaccardEntry>>();

	/**
	 *  This function will read *.gotmatrix.txt files of a specific species as
	 * well as GenesAlmostAlwaysPresent.txt to compute the similarity file ready
	 * to be uploaded
	 * @param species Species to compute
	 * @param genesFile List of genes to consider
	 * @param outputfile Where the analysis will be stored
	 */
	public void similarityAnalysisJaccard(String species, String genesFile,
			String outputfile) {
		/* Read genesFile to get the genes in order */
		ArrayList<String> genes = new ArrayList<String>();

		try {
			BufferedReader br = Species.getReader(species, genesFile);
			String line;
			genes.add("dummy");
			while ((line = br.readLine()) != null) {
				genes.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Get a list of *gomatrix.txt files */
		File[] gomatrixfiles = GenUtils.readDirectory(
				Species.getGoMatrixFolder(species), "*gomatrix.txt", null);

		/* Compare each edition to the last one */
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					Species.getGoMatrixFolder(species) + "/" + outputfile));
			bw.write("Gene\tEdition\tSimilarity\n");
			for (int i = 0; i < gomatrixfiles.length; i++) {
				System.out.println("Compare "+gomatrixfiles[i].getName()+" to "+
			                        gomatrixfiles[gomatrixfiles.length-1].getName());
				HashMap<String, ArrayList<JaccardEntry>> analysisAB = (HashMap<String, ArrayList<JaccardEntry>>) compareTwoEditions(
						genes, gomatrixfiles[i],
						gomatrixfiles[gomatrixfiles.length - 1]);
				/* Write the analysis */
				writeAnalysis(analysisAB, bw);
			}
			System.out.print("Analysis written to "+Species.getGoMatrixFolder(species) 
					+ "/" + outputfile);
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * This function uses the jaccard similarity to tell how similar is one gene in edition X to itself
	 * in edition Y (last edition)
	 * @param genes  Is a list of ordered genes
	 * @param editionA Edition number A
	 * @param editionB Edition number B
	 * 
	 * @return Map that links a gene to the similarity score in edition A
	 * 
	 * */
	public Map<String, ArrayList<JaccardEntry>> compareTwoEditions(
			List<String> genes, File editionA, File editionB) {

		HashMap<String, ArrayList<JaccardEntry>> analysisA = new HashMap<String, ArrayList<JaccardEntry>>();
		HashMap<String, ArrayList<JaccardEntry>> analysisAB = new HashMap<String, ArrayList<JaccardEntry>>();

		/* Read all genes in A */
		try {			
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(editionA)));
			br.readLine();
			br.readLine();
			String line;
			String edition = GenUtils.getEditionNo(editionA.getName());
			while ((line = br.readLine()) != null) {
				String[] columnDetail = new String[11];
				columnDetail = line.split(" ");
				SimilarityAnalysis.JaccardEntry jtmp = this.new JaccardEntry();
				jtmp.geneidx = columnDetail[0];
				jtmp.gene = jtmp.geneidx;
				jtmp.edition = Integer.valueOf(edition);
				jtmp.goterm = columnDetail[1];
				
				ArrayList<JaccardEntry> tm = analysisA.remove(jtmp.gene);
				if (tm == null)
					tm = new ArrayList<SimilarityAnalysis.JaccardEntry>();
				tm.add(jtmp);				
				analysisA.put(jtmp.gene, tm);
			}
			br.close();
			/* Read all genes in B (if needed) */
			if (analysisB == null || analysisB.isEmpty()) {
				br = new BufferedReader(new InputStreamReader(
						new FileInputStream(editionB)));
				br.readLine();
				br.readLine();
				edition = GenUtils.getEditionNo(editionA.getName());
				while ((line = br.readLine()) != null) {
					String[] columnDetail = new String[11];
					columnDetail = line.split(" ");
					SimilarityAnalysis.JaccardEntry jtmp = this.new JaccardEntry();
					jtmp.geneidx = columnDetail[0];
					jtmp.gene = jtmp.geneidx;
					jtmp.edition = Integer.valueOf(edition);
					jtmp.goterm = columnDetail[1];

					ArrayList<JaccardEntry> tm = analysisB.remove(jtmp.gene);
					if (tm == null)
						tm = new ArrayList<SimilarityAnalysis.JaccardEntry>();
					tm.add(jtmp);
					analysisB.put(jtmp.gene, tm);
				}
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Compare gene i in A and B */
		for (String gene : analysisA.keySet()) {
			ArrayList<JaccardEntry> entriesA = analysisA.get(gene);
			ArrayList<JaccardEntry> entriesB = analysisB.get(gene);

			JaccardEntry comp = new JaccardEntry();

			Integer cardinalityA = entriesA.size();
			if (entriesB == null) {
				//The gene exists in A but not in edition B (the latest)
				comp.score = -1f;
			} else {
				Integer cardinalityB = entriesB.size();
				/* Get intersection */
				Integer cardAintB = 0;
				HashSet<String> goA = new HashSet<String>();
				HashSet<String> goB = new HashSet<String>();
				for (JaccardEntry j : entriesA) {
					goA.add(j.goterm);
				}
				for (JaccardEntry j : entriesB) {
					goB.add(j.goterm);
				}
				goB.retainAll(goA);// goB now contains only elements in both
									// sets
				cardAintB = goB.size();
				Integer cardAunionB = cardinalityA + cardinalityB - cardAintB;
				// j = (|A int B|)/( |A union B| )   
				Float jaccard = (float) cardAintB / cardAunionB;				
				comp.score = jaccard;				
			}
			comp.gene = gene;			
			comp.edition = entriesA.get(0).edition;			
			ArrayList<JaccardEntry> tmp = analysisAB.remove(gene);
			if (tmp == null) {
				tmp = new ArrayList<SimilarityAnalysis.JaccardEntry>();
			}
			tmp.add(comp);
			analysisAB.put(gene, tmp);
		}
		return analysisAB;
	}

	/**
	 * Write to disk the jaccard analysis
	 * @param data Contains the similarity analysis of one edition
	 * @param bw BufferedWriter that will be used to write the data
	 */
	private static void writeAnalysis(
			Map<String, ArrayList<JaccardEntry>> data, BufferedWriter bw) {
		for (String gene : data.keySet()) {
			ArrayList<JaccardEntry> entry = data.get(gene);
			if (entry.size() > 1) {
				System.out.print("Saniticheck");
			}
			JaccardEntry theentry = entry.get(0);
			try {
				if(theentry.score<0f){
					bw.write(gene + "\t" + theentry.edition + "\t" + "\n");
				}else
				bw.write(gene + "\t" + theentry.edition + "\t" + theentry.score
						+ "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * This is a wrapper function to be called by GOtrack
	 * 
	 * @param species Species to Compute
	 * @param genesFile Genes always present or Genes almost always present file
	 * @param outputfile Where the jaccard analysis is written 
	 */
	public static void gotrackSemanticAnalysis(String species, String genesFile, String outputfile){
		SimilarityAnalysis t = new SimilarityAnalysis();
		t.similarityAnalysisJaccard(species, genesFile,
				outputfile);
	}
	
	/**
	 * For testing only
	 * @param args
	 */
	public static void main(String[] args) {
		SimilarityAnalysis t = new SimilarityAnalysis();
		t.similarityAnalysisJaccard("human","GenesAlmostAlwaysPresent.txt",
				"jaccardpergeneovertime.txt");

	}
/**
 * 
 * @author asedeno
 *
 *This inner class models a jaccard entry in the matrix
 */
	class JaccardEntry {
		public String gene;
		public String geneidx;
		public Integer edition;
		public String goterm;
		public Float score;
	}

}
