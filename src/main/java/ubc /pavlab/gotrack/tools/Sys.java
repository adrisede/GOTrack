/*
 * Project: GOtrack 
 * Author: Adriana Sedeno 
 * Email: asedeno@chibi.ubc.ca 
 * Written: Jul 4, 2013 
 * University of British Columbia
 * Compiler: Java 
 * Platform: Ubuntu 
 */

package ubc.pavlab.gotrack.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import ubc.pavlab.gotrack.constants.Species;
import ubc.pavlab.gotrack.utils.GenUtils;

/**
 * 
 * @author asedeno
 * 
 */
public class Sys {

	private static Process process;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Logger logger = Logger.getLogger("Syslogger");
		File[] synFiles = GenUtils.readDirectory(Species.getTmpFolder(Species.HUMAN),
				"gene_association*.gz.syn", logger);		
		
		for(File file : synFiles){
			System.out.println( renameGZFile( file.getAbsolutePath()) );
		}

	}

	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	private static String renameGZFile(String fileName){
		return fileName.substring(0, fileName.indexOf(".gz.syn"));
	}
	/**
	 * Executes a R script, output in a file. The input and output values are
	 * defined in the properties file called rfiles.properties
	 * 
	 * @param input
	 *            input R files
	 * @param output
	 *            output R files
	 */
	public static void exec(String species, String numeditions) {

		try {

			File file = new File(".");
			String absoluteFolder = file.getAbsolutePath();
			absoluteFolder = absoluteFolder.substring(0,
					absoluteFolder.length() - 1);
			// move files
			// cp data/species/tmp/genesLastEdition.txt ../gomatrix/
			String command = "cp " + absoluteFolder + "data/" + species
					+ "/tmp/genesLastEdition.txt " + absoluteFolder + "data/"
					+ species + "/gomatrix/";
			execAux(command, "tmp.txt");

			String route;
			String semanticRoute;

			route = absoluteFolder + "data/" + species + "/gomatrix/";
			semanticRoute = absoluteFolder + "R/";

			String count = "setwd(\""
					+ route
					+ "\")\n"
					+ "source(\""
					+ semanticRoute
					+ "semantic.R\")\n"
					+ "library(Matrix)\n"
					+ "library(multicore)\n"
					+ "rm(list=ls())\n"
					+ "g <-read.delim(\"genesLastEdition.txt\", header=F)\n"
					+ "mats = list()\n"
					+ "numeds <- "
					+ numeditions
					+ "\n"
					+ "for(i in c(1:numeds)) {\n"
					+ "  try(mats[[as.character(i)]] <- readMM(paste(\"edition.\", i, \".gomatrix.txt\", sep=\"\")), silent=T);\n"
					+ "}\n"
					+ "counts.goterms.pergeneL <-lapply(mats, apply, 1, sum)\n"
					+ "counts.goterms.pergene <-data.frame(lapply(counts.goterms.pergeneL, unlist))\n"
					+ "str(counts.goterms.pergene)\n"
					+ "genes <- g[,1]\n"
					+ "row.names(counts.goterms.pergene)<- genes\n"
					+ "counts <- as.data.frame(counts.goterms.pergene)\n"
					+ "write.table(counts, quote=F, sep='\t', file=\"counts.pergene.asedeno.overtime.txt\")\n"
					+ "print(\"done.\")";

			String runFile = semanticRoute + "count_" + species + ".R";
			FileWriter writer = new FileWriter(runFile);
			writer.write(count);
			writer.close();

			// System.out.println("Executing R file: " + runFile);
			execAux("Rscript " + runFile, "log_R.txt");


		} catch (FileNotFoundException e) {
			//
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	/**
	 * Just executes the bash command
	 * 
	 * @param command
	 */
	public static void bash(String command) {
		execAux(command, null);
	}

	/**
	 * 
	 * @param command
	 * @param output
	 */
	public static void execAux(String command, String output) {
		try {

			String line = "";
			ProcessBuilder builder = new ProcessBuilder("/bin/bash");
			builder.redirectErrorStream(true);
			process = builder.start();

			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stdout));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					stdin));

			String input = "";
			writer.write(input);
			writer.flush();

			input = command;
			System.out.println("~$: " + input);

			writer.write("((" + input + ") && echo --EOF--) || echo --EOF--\n");

			writer.flush();

			line = reader.readLine();

			FileWriter file = null;
			if (output != null) {
				file = new FileWriter(output, false);
			}
			while (line != null && !line.trim().equals("--EOF--")) {

				System.out.println(": " + line);
				if (file != null) {
					file.write(line + "\n");
				}
				line = reader.readLine();
			}

			reader.close();
			writer.close();
			if (file != null)
				file.close();
			process.destroy();

		} catch (IOException ex) {
			Logger.getLogger(Sys.class.getName()).log(Level.SEVERE, null, ex);
			System.err.println(ex.getMessage());
			System.err.println("Error in the execution of the command: "
					+ command);
		}
	}

}
