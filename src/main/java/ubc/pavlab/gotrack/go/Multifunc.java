package ubc.pavlab.gotrack.go;
 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.erminej.Settings;
import ubic.erminej.data.Gene;
import ubic.erminej.data.GeneAnnotationParser;
import ubic.erminej.data.GeneAnnotationParser.Format;
import ubic.erminej.data.GeneAnnotations;
import ubic.erminej.data.GeneSetTerm;
import ubic.erminej.data.GeneSetTerms;
import ubic.erminej.data.Multifunctionality;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
 
/**
 * This class implements the multifunctionality analysis 
 * 
 * @author asedeno 
 * 
 */
 
public class Multifunc {
 
    /**
     * Computes the multi functionality score
     * 
     * @param termdbdir
     *            Directory where termdb file is found
     * @param dir
     *            Directory where geneList is found
     * @param termdb
     *            Termdb file to operate
     * @param isNew
     *            Whether termdb file follows old or new format
     * @param edition
     *            String that contains the edition number
     * @param geneList
     *            List of genes for that edition
     *
     * */
    @SuppressWarnings("unused")
    public static void multi(String species, File assoc, Logger logger,
            String isOldEdition, int editionNumber,
            String typeOfGenesUsedFile, String termdb,
            GeneSetTerms geneSetTerms) {
        String edition = editionNumber + "";
        // files are like "/edition."+edition+"annots.simple.txt"
        String simplifiedParents = "edition." + edition + "annots.simple.txt";
 
        try {
            if (geneSetTerms == null) {
 
                try {
                    Map<String, Calendar> matchdates2edition = GenUtils.matchdates2edition;
 
                    String termdbFile = Parents.getTermdbFromGeneAssoc(species,
                            assoc.getName(), matchdates2edition, logger);
                    geneSetTerms = new GeneSetTerms(Species.getTermDBFolder()
                            .getAbsolutePath() + "/" + termdbFile,
                            Boolean.valueOf(isOldEdition));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
             
                try {
 
                    GeneAnnotationParser parser = new GeneAnnotationParser(
                            geneSetTerms);
 
                    Collection<Gene> genes = new ArrayList<Gene>();
                    if (typeOfGenesUsedFile != null) {
                        // read in list of genes.
                        BufferedReader reader = Species.getReader(species,
                                typeOfGenesUsedFile);
                        String line;
                        StringTokenizer strtkn;
                        while ((line = reader.readLine()) != null) {
                            strtkn = new StringTokenizer(line, "\t");
                            String symbol = strtkn.nextToken();
                            String name = "";
                            if (strtkn.hasMoreTokens()) {
                                // String name = strtkn.nextToken();
                                name = strtkn.nextToken();
                            }
                            genes.add(new Gene(symbol, name));
                        }
                        reader.close();
                    }
 
                    Settings settings = new Settings(false);
                    settings.setProperty("useUserDefinedGroups", false);
                    settings.setProperty("minimum.geneset.size", 10);
 
                    File TMPFolder = Species.getTmpFolder(species);
                    GeneAnnotations annots;
 
                    if (typeOfGenesUsedFile != null) {
                        annots = parser.readDefault(TMPFolder.getAbsolutePath()
                                + "/" + simplifiedParents, genes, settings,
                                true);
                    } else {
                        annots = parser.read(TMPFolder.getAbsolutePath() + "/"
                                + simplifiedParents, Format.SIMPLE, settings);
                    }
 
                    Multifunctionality multifunctionality = annots
                            .getMultifunctionality();
 
                    int i = 0;
                    int sum = 0;
                    DoubleArrayList vals = new DoubleArrayList();
                    for (GeneSetTerm term : annots.getGeneSetTerms()) {
                        if (term.getDefinition().startsWith("OBSOLETE"))
                            continue;
                        double d = multifunctionality
                                .getGOTermMultifunctionality(term);
                        if (d < 0)
                            continue;
                        vals.add(d);
                        i++;
                        sum += d;
                    }
 
                    if (genes != null && !genes.isEmpty()) {
 
                        BufferedWriter writer = Species.getWriter(species,
                                typeOfGenesUsedFile + "edition" + editionNumber
                                        + ".multifunc.score.txt");
 
                        for (Gene gene : genes) {
                            Double score = multifunctionality
                                    .getMultifunctionalityScore(gene);
                            writer.write(gene.getName() + "\t" + score + "\t"
                                    + gene.getSymbol() + "\n");
                        }
                        writer.close();
                    }
 
                    double m = Descriptive.mean(vals);
                    double sd = Math
                            .sqrt(DescriptiveWithMissing.variance(vals));
                    BufferedWriter writer = Species.getWriter(species, typeOfGenesUsedFile
                            + "." + edition + ".multifunc.txt");
                    writer.write(termdb + "\t" + edition + "\t" + m + "\t" + sd);
                    writer.close();
 
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    logger.severe(e.getMessage());
                } catch (IOException e) {
                    logger.severe("EMPTY FILE: " + simplifiedParents
                            + " while calculating multifunctionality for "
                            + termdb);
                } catch (IllegalStateException e) {
                    logger.severe("Fatal error calculating multifunctionality for "
                            + termdb);
                    e.printStackTrace();
                } catch (Exception e) {
                    logger.severe(e.getMessage());
                    e.printStackTrace();
                }
                 
                    } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    } 
 
}
