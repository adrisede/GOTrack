package ubc.pavlab.gotrack.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.tools.Sys;

/**
 * This class implements the mechanism to extract the human readable name for a go term
 * Class created to be used by Load.java
 * 
 * @author asedeno
 *
 */
public class GONames {

	/**
	 * Extract gonames with human readable names in termdb
	 * 
	 * @param path The directory where the termdb files are stored
	 * @param root  
	 * @param termdbdatesInDb The termdb dates that are already in the database
	 * @return ArrayList of *.go-names.txt files that will store the gonames for each termdb
	 * These files will be used to fill the database information. (GO_names table)
	 */
	public static ArrayList<String> extractGOTermNames(String path, String root, HashSet<String> termdbdatesInDb) {
		ArrayList<String> gonamesFiles = new ArrayList<String>();
		final File directory;
		final File rootFile;
		if (path == null) {
			directory = new File("data/termdb/gonames/");
			rootFile = new File("data/termdb/");
			if(!directory.exists()){
				String cmd = "mkdir -p " + directory.getAbsolutePath() + "/";
				System.out.println(cmd + "\n");
				
				Sys.execAux(cmd, "log.txt");
			}
			path = directory.getAbsolutePath();
		} else {
			directory = new File(path);
			rootFile = new File (root);
		}
		
		System.out.println(directory.getAbsolutePath());
		
		

		FileFilter fileFilter = new WildcardFileFilter("*.gz");
		File[] files = rootFile.listFiles(fileFilter);
		ArrayList <Thread> allThreads = new ArrayList<Thread>();
		for (final File file : files) {
			
			if(termdbdatesInDb.contains(file.getName()+".go-names.txt")){
				continue;
			}
			
			gonamesFiles.add(Species.getTermDBFolder()+"/gonames/"+file.getName()+".go-names.txt");
			Thread thread;
			Runnable runnable = new Runnable() {

				public void run() {

					try {
						System.out.println("Now on: " + file.getName());
						extractNames(directory.getAbsolutePath(), file); // operates file
						System.out.println("Done with: " + file.getName());
					} catch (Exception e) {
						System.err.println("Error while processing : "
								+ file.getName() + "\n" + e.toString());
						e.printStackTrace();
					} finally {
						try {
							this.finalize(); // no thread leak
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}// run
			};

			thread = new Thread(runnable);
			thread.start();
			
			allThreads.add(thread);

		}// for
		for(Thread t : allThreads){
			try {
				t.join();
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		return gonamesFiles;

	}

	/**
	 * This will extract the go annotation with it's name from the termdb file.
	 * It will create a *go_names.txt file
	 * @param file  Termdb file
	 * @throws FileNotFoundException  If the termdb is not existent
	 * @throws IOException   If there is any problem reading the file
	 */
	public static HashMap<String, String> extractNames(String root, File file)
			throws FileNotFoundException, IOException {
		HashMap<String, String>  names = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(new FileInputStream(file))));

		String name = file.getName();
		BufferedWriter writer = new BufferedWriter(new FileWriter(root + "/"
				+ name + ".go-names.txt"));

		String line; // search for
		String filename = file.getName();
		String edition = filename.substring(24,31);
		edition +="-01";
		/*This code try to parse the termdb file to identify the goterm id and 
		 * its name*/
		while ((line = reader.readLine()) != null) {
			if (line.indexOf("<go:accession>") == -1)
				continue;
			else {
				int index1 = line.indexOf("<go:accession>") + 14;
				int index2 = line.indexOf("</go:accession>");
				String goname = line.substring(index1, index2);
				if( goname.equals("all")){
					continue; // skip line with all 
				}			
				String humanname = reader.readLine();				
				index1 = humanname.indexOf("<go:name>") + 9;
				index2 = humanname.indexOf("</go:name>");
				humanname = humanname.substring(index1, index2);
				if(humanname.length()==0){
					/*This should not happen, but record the issue if the code gets here*/
					System.out.println("No name in termdb for "+goname);
				}
				writer.write(goname + "\t" + humanname+"\t"+edition + "\n");
				names.put(goname, humanname);
			}
		}

		writer.close();
		reader.close();
		return names;
	}

}
