package ubc.pavlab.gotrack.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubic.erminej.Settings;
import ubic.erminej.data.Gene;
import ubic.erminej.data.GeneAnnotationParser;
import ubic.erminej.data.GeneAnnotationParser.Format;
import ubic.erminej.data.GeneAnnotations;
import ubic.erminej.data.GeneSetTerm;
import ubic.erminej.data.GeneSetTerms;

/**
 * + * Creates a file that contains a boolean matrix where rows are genes,
 * columns are GO ids, and entries are whether the + * gene has that term
 * annotated. A matrix like this is created for each edition. + * + * @author
 * asedeno + * @version $Id$ +
 */

public class GOMatrix {

	/**
	 * @param tmpdir
	 *            Sub directory where intermediate files are found
	 * @param gomatrixdir
	 *            Sub directory where gomatrix file will be saved
	 * @param termdbdir
	 *            Directory where termdb file are found.
	 * @param dir
	 *            Base directory, all other directories are under this directory
	 * @param termdbFile
	 *            Termdb that will be used to compute gomatrix file
	 * @param isOldEdition
	 *            Whether termdbFile follows old or new format
	 * @param editionAnnotSimple
	 *            File that contains only two columns geneid and Go parents
	 * @param goTerms
	 *            Go terms that will be considered (always present or the
	 *            complete list)
	 * @param edition
	 *            String that contains the edition number
	 * */
	public static void runGoMatrix(String species, File assoc, Logger logger,
			String isOldEdition, String editionAnnotSimple,
			String genesFile, String goTerms, int editionNumber,
			GeneSetTerms gs) {
		try {
			if (gs == null) {
				throw new Exception(
						"GOMATRIX >>> GENESETTERMS NULL for edition "
								+ editionNumber + "and species: " + species);

			}			
				logger.info("Running analysis using "+genesFile+"\n");
				String edition = editionNumber + "";
				try {
					GeneAnnotationParser p = new GeneAnnotationParser(gs);

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
					Settings settings;
					settings = new Settings(false);
					settings.setProperty("useUserDefinedGroups", false);
					settings.setProperty("minimum.geneset.size", 1);

					GeneAnnotations annots;
					File f = Species.getTmpFolder(species);
					if (genesFile != null) {
						annots = p.readDefault(f.getPath() + "/"
								+ editionAnnotSimple, genes, settings, true);
					} else {
						annots = p.read(f.getPath() + "/" + editionAnnotSimple,
								Format.SIMPLE, settings);
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
					bw.write(genes.size() + " " + (ttu.size()+1) + " " + 10000000
							+ "\n");

					Collection<Gene> usedGenes = annots.getGenes();

					int i = 1;
					for (Gene gene : genes) {
						if (usedGenes.contains(gene)) {
							Collection<GeneSetTerm> geneTerms = annots
									.findGene(gene.getSymbol()).getGeneSets();							
							for (GeneSetTerm term : ttu) {
								if (geneTerms.contains(term)) {
									bw.write(i + " " + term + " 1\n");
								}								
							}
						}else{
							//System.out.println("Gene "+gene+" is not AP ");
							bw.write(i + " "+(ttu.size()+1)+" 1\n");
						}
						i++;
					}

					bw.close();
				} catch (IOException e) {
					logger.severe("Fatal error in goMatrix: NO GENES: "
							+ e.toString());
					// e.printStackTrace();
				} catch (IllegalStateException e) {
					logger.severe("Fatal error using "+genesFile+" in goMatrix: NO GENES: "
							+ e.toString());
					 e.printStackTrace();
				} catch (Exception e) {
					logger.severe(e.getMessage());
					e.printStackTrace();
				}
			

		} catch (Exception e) {
			logger.severe(e.getMessage());
			// e.printStackTrace();
		}
	}

}
