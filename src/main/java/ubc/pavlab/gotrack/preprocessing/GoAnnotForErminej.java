package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.go.GOtrack;
import ubc.pavlab.gotrack.mapping.MappingUtils;
import ubc.pavlab.gotrack.utils.GenUtils;
/**
 * This class implements a transformation of gene_association files into the erminej format
 * 
 * 
 * @author asedeno
 *
 */
public class GoAnnotForErminej {

	/**
	 * Compute a gene association file into an erminej format
	 * 
	 * @param species
	 * @param geneFile
	 * @param logger
	 */
	public void computeGeneAssociationFile(String species, File geneFile,
			Logger logger) {
		try {
			Map<String, Set<String>> dic = new HashMap<String, Set<String>>();
			BufferedReader readerOfGeneAssoc = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(geneFile))));
			String temporary;
			logger.info("Reading " + geneFile.getName());
			String symbol;
			String uniprot;
			String goterms;
			File directory = new File("data/termdb/gonames/");
			while ((temporary = readerOfGeneAssoc.readLine()) != null) {
				String[] columnDetail = new String[20];
				columnDetail = temporary.split("\t");
				if (columnDetail.length > 2) {
					uniprot = columnDetail[1];
					symbol = columnDetail[2];
					goterms = columnDetail[4];
					Set<String> t;
					if (!dic.containsKey(symbol + "\t" + uniprot)) {
						t = new HashSet<String>();
					} else {
						t = dic.remove(symbol + "\t" + uniprot);
					}
					t.add(goterms);
					dic.put(symbol + "\t" + uniprot, t);
				}
			}
			readerOfGeneAssoc.close();

			BufferedWriter bufferedWriter = Species.getWriter(species,
					"goannots." + GenUtils.getEditionNo(geneFile.getName())
							+ ".txt");

			for (String key : dic.keySet()) {
				Set<String> t = dic.get(key);
				bufferedWriter.write(key + "\t");
				int counter = 0;
				for (String go : t) {
					if (t.size() - 1 == counter++)
						bufferedWriter.write(go);
					else
						bufferedWriter.write(go + "|");
				}
				bufferedWriter.write("\n");
			}
			bufferedWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void computeAllGeneAssociationFiles(String species, Logger logger) {
		ArrayList<File> geneAssociationFiles = MappingUtils.readDirectory(
				Species.getDataFolder(species).getAbsolutePath() + "/",
				"gene_association*gz");
		for (File fgen : geneAssociationFiles) {
			computeGeneAssociationFile(species, fgen, logger);
		}

	}

	public static void main(String[] args) {

		GoAnnotForErminej instance = new GoAnnotForErminej();
		final Logger log = GOtrack.getLoggerforSpecies(args[0]);
		instance.computeAllGeneAssociationFiles(args[0], log);

	}
}
