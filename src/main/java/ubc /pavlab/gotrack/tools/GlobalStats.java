package ubc.pavlab.gotrack.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.xml.sax.SAXException;
import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;
import ubic.erminej.data.GeneSetTerm;
import ubic.erminej.data.GeneSetTerms;

/** This class stores code to compute global statistics and exploratory analysis
 * 
 * */

public class GlobalStats {

	/**
	 * Number of manual annotations that are promoted to more specific ones
	 * This function is called by gotrack
	 * 
	 * @param species Species to analyze
	 * @param editionA First edition to compare
	 * @param editionB Second edition to compare
	 * @param childrenA children of the goterms in edition A
	 * @param childrenB children of the goterms in edition B
	 * @return
	 */
	public static String createAnnotationsFromIEAToManual(String species,
			File editionA, File editionB, HashMap<String, HashSet<String>> childrenA,
			HashMap<String, HashSet<String>> childrenB) {
		if(childrenA==null || childrenA.isEmpty())
			childrenA=childrenB;
		String entry = "";

		HashSet<String> ieaA = new HashSet<String>();
		HashSet<String> manualA = new HashSet<String>();
		HashSet<String> manualB = new HashSet<String>();
		HashSet<String> ieaB = new HashSet<String>();

		/* Get iea annotations from A */

		try {
			BufferedReader readerFileForEdition = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(editionA))));
			String temporary;
			while ((temporary = readerFileForEdition.readLine()) != null) {
				String[] columnDetail = temporary.split("\t");
				if (columnDetail.length < 2) {
					continue; // This row might be the header so skip it
				}
				String id = columnDetail[1];
				String go = columnDetail[4];
				String annotation = columnDetail[6];

				if (annotation.contains("IEA")) {
					ieaA.add(id + "\t" + go);
				}else
					manualA.add(id + "\t" + go);
			}
			readerFileForEdition.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Get manual annotations from B */

		try {
			BufferedReader readerFileForEdition = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(editionB))));
			String temporary;
			while ((temporary = readerFileForEdition.readLine()) != null) {
				String[] columnDetail = temporary.split("\t");
				if (columnDetail.length < 2) {
					continue; // This row might be the header so skip it
				}
				String id = columnDetail[1];
				String go = columnDetail[4];
				String annotation = columnDetail[6];

				if (!annotation.contains("IEA")) {
					manualB.add(id + "\t" + go);
				}else
					ieaB.add(id + "\t" + go);
			}
			readerFileForEdition.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* Get number of annotations from A to B */

		Integer genesIEAtoManual = 0;
		Integer genestochildrenA = 0;
		for (String genegoterm : ieaA) {
			/*If it is still direct. Don't count it*/
			if (ieaB.contains(genegoterm)) {
				continue;
			}
			if(manualB.contains(genegoterm)){
			   genesIEAtoManual++;
			   continue;
			}
			String gene = genegoterm.substring(0,genegoterm.indexOf("\t"));
			String goterm = genegoterm.substring(genegoterm.indexOf("\t")+1,genegoterm.length());
			HashSet<String> chGoterm = childrenA.get(goterm);
			if(chGoterm!=null){
				for(String children:chGoterm){
					if(manualB.contains( gene+"\t"+children)){
						genesIEAtoManual++;
						continue;
					}
				}
			}
		}
		entry = GenUtils.getEditionNo(editionA.getName()) + "\t"
				+ GenUtils.getEditionNo(editionB.getName()) + "\t"
				+ genesIEAtoManual;
		
		/*Manual to manual*/
		for (String genegoterm : manualA) {	
			/*if it's a direct annotation. Don't count it*/
			if (manualB.contains(genegoterm) || ieaB.contains(genegoterm)) {
				continue;
			}
			String gene = genegoterm.substring(0,genegoterm.indexOf("\t"));
			String goterm = genegoterm.substring(genegoterm.indexOf("\t")+1,genegoterm.length());
			HashSet<String> chGoterm = childrenA.get(goterm);
			if(chGoterm!=null){
				for(String children:chGoterm){
					if(manualB.contains( gene+"\t"+children)){						
						genestochildrenA++;
					}
				}
			}
		}
		
		entry += "\t"+genestochildrenA + "\t" + species;
		
		System.out.println(entry);
		return entry;
	}

	/**
	 * Compute the ratio of NOT annotation 
	 * @param geneAssoc File to analyze
	 * @return The ration. Number of NOT annotation divided by the number of annotations
	 */
	public static String notRatio(File geneAssoc){
		
		try {
			BufferedReader readerFileForEdition = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(geneAssoc))));
			HashMap<String, HashSet<String>> geneToNot = new HashMap<String, HashSet<String>>();
			HashMap<String, HashSet<String>> geneToTotalAnot = new HashMap<String, HashSet<String>>();
			String temporary;
			while ((temporary = readerFileForEdition.readLine()) != null) {
				String[] columnDetail = temporary.split("\t");
				if (columnDetail.length < 2) {
					continue; // This row might be the header so skip it
				}
				String id = columnDetail[1];
				String go = columnDetail[4];				
				String notv = columnDetail[3];
				if(notv.length()>0 && (notv.contains("NOT")||notv.contains("not"))){
					HashSet<String> notCount = geneToNot.remove(id);
					if(notCount==null)
						notCount = new HashSet<String>();
					notCount.add(go);
					geneToNot.put(id, notCount);					
				}
				HashSet<String> totalAnnot = geneToTotalAnot.remove(id);
				if(totalAnnot==null){
					totalAnnot=new HashSet<String>();
				}
				totalAnnot.add(go);
				geneToTotalAnot.put(id, totalAnnot);							
			}
			readerFileForEdition.close();
			Float total=0f;
			for(String gene : geneToNot.keySet()){				
			    total+=(float)geneToNot.get(gene).size()/(float)geneToTotalAnot.get(gene).size();				
			}
			
			if(geneToNot.size()>0)
			  return ""+total/geneToNot.size();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "0";
	}
	
	/**
	 * Main function to be called by gotrack
	 * Computes the number of manual annotations that are promoted to more specific ones
	 * @param speciesToCompute Species to analyze
	 */
	public static void computeAnnotAnalysis(String speciesToCompute){		

		// Read the association files and reuse the array
		File[] assocArrayOfFiles = GenUtils.readDirectory(
				Species.getDataFolder(speciesToCompute), "gene_association*",
				null);

		Map<String, Calendar> edtodates = readEdition2Dates(speciesToCompute,
				"edition2Dates.txt");

		HashMap<String, HashSet<String>> chA = (HashMap<String, HashSet<String>>) GetChildren(getTermdbFromDate(edtodates
				.get(assocArrayOfFiles[0].getName())));
		HashMap<String, HashSet<String>> chB =null;
		try {
			String outputFile=Species.getDataFolder(speciesToCompute)+"/tmp/dbAnnotAnalysis.txt";
			BufferedWriter writerSpecies = new BufferedWriter(new FileWriter(outputFile));
			writerSpecies.write("editionA\teditionB\tannotationsIEAToManual\tAnnotGeneralToSpecificManual\n");
			for (int i = 0; i < assocArrayOfFiles.length - 1; i++) {	
				System.out.println("Computing "+assocArrayOfFiles[i]);
				try{
				chB = (HashMap<String, HashSet<String>>) GetChildren("data/termdb/"+getTermdbFromDate(edtodates
						.get(assocArrayOfFiles[i+1].getName())));
				}catch(Exception e){
					System.err.print("Cannot read ");
				}
				
				writerSpecies.write(createAnnotationsFromIEAToManual(speciesToCompute,
						assocArrayOfFiles[i], assocArrayOfFiles[i + 1], chA, chB)+"\t"+notRatio(assocArrayOfFiles[i])+"\n");
				chA=chB;
			}
			writerSpecies.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	

	}
		
/**
 * Given a date, get the termdb that matches the date
 * @param cal Date
 * @return A string of the form "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";
 */
	public static String getTermdbFromDate(Calendar cal) {

		int year = cal.get(Calendar.YEAR);
		if (year < 2000)
			year += 2000;
		String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
		if (month.length() == 1)
			month = "0" + month;
		String res = "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";

		if (!Species.termDBFile(res).exists()) {
			cal.add(Calendar.MONTH, -1);
			year = cal.get(Calendar.YEAR);
			if (year < 2000)
				year += 2000;
			month = String.valueOf(cal.get(Calendar.MONTH) + 1);
			if (month.length() == 1)
				month = "0" + month;
			System.err.println("Problem. File " + res
					+ " is not found. Trying " + "go_daily-termdb.rdf-xml."
					+ year + "-" + month + ".gz");
		}
		return "go_daily-termdb.rdf-xml." + year + "-" + month + ".gz";

	}

	/**
	 * Read children for each term in the termdb
	 * 
	 * @param termdbFile
	 *            Termdb that will be used as a reference tree
	 * @return HashMap that associate a goterm with its children
	 * */
	public static Map<String, HashSet<String>> GetChildren(String termdbFile) {
		GeneSetTerms geneSetTerms = null;
		HashMap<String, HashSet<String>> allChildren = new HashMap<String, HashSet<String>>();
		try {
			/* Read termdb */
			geneSetTerms = new GeneSetTerms(termdbFile,
					Boolean.valueOf("false"));

			/* Traverse each term */
			for (GeneSetTerm term : geneSetTerms.getTerms()) {
				if (term.getDefinition().startsWith("OBSOLETE"))
					continue;
				/* Get children */
				Collection<GeneSetTerm> children = geneSetTerms
						.getAllChildren(term);
				for (GeneSetTerm child : children) {
					if (child != null && child.getId() != null) {
						HashSet<String> allChildrenTmp;

						if (allChildren.containsKey(term.getId())) {
							allChildrenTmp = allChildren.remove(term.getId());
						} else
							allChildrenTmp = new HashSet<String>();
						allChildrenTmp.add(child.getId());
						allChildren.put(term.getId(), allChildrenTmp);
					}
				}
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allChildren;
	}

	/**
	 * Match edition with date using given file
	 * 
	 * @param dir
	 *            Directory where file is stored
	 * @param edition2datesFile
	 *            Matching file
	 * 
	 * */
	public static Map<String, Calendar> readEdition2Dates(String species,
			String edition2datesFile) {
		Map<String, Calendar> dictionary = new HashMap<String, Calendar>();
		try {
			BufferedReader b;
			b = Species.getReader(Species.getTmpFolder(species)
					.getAbsolutePath() + "/" + edition2datesFile);
			String line;
			while ((line = b.readLine()) != null) {
				String[] columnDetail = new String[11];
				columnDetail = line.split("\t");
				if (columnDetail.length >= 2) {
					Calendar calendar = Calendar.getInstance();
					String[] dt = new String[3];
					dt = columnDetail[1].split("/");
					calendar.set(Integer.valueOf(dt[2]),
							Integer.valueOf(dt[0]) - 1, Integer.valueOf(dt[1]));
					dictionary.put(columnDetail[0], calendar);
				}
			}
			b.close();
			return dictionary;
		} catch (IOException e) {
			return null;
		}
	}

}
