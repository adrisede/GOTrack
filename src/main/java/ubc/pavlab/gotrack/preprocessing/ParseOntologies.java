package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class ParseOntologies {

	public static void parseFile(){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(
					"data/gene_ontology_ext.obo")));
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File("data/ontologies.txt")));
			String temporaryString;
	        while ((temporaryString = reader.readLine()) != null) {
	        	String goname;
	        	String onto;
	        	if(temporaryString.contains("id:") && temporaryString.contains("GO:")){
	        		goname = temporaryString;
	        		do{
	        			temporaryString = reader.readLine();
	        		}while(!temporaryString.contains("namespace:"));
	        		onto = temporaryString;
	        		// get goterm and ontology
	        		goname = goname.substring(goname.indexOf(":")+1,goname.length());
	        		goname = goname.trim();
	        		onto = onto.substring( onto.indexOf(":")+1,onto.length());
	        		onto= onto.trim();
	        		if(onto.contains("biological_process"))
	        			onto = "P";
	        		else if(onto.contains("molecular_function"))
	        			onto = "F";
	        		else 
	        			onto = "C";
	        		bf.write(goname+"\t"+onto+"\n");
	        	}	        	
	        }
	        reader.close();
	        bf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
					

	}

}
