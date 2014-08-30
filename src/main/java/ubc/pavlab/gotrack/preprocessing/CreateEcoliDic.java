package ubc.pavlab.gotrack.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

public class CreateEcoliDic {
	/**
	 * This file (data/ecoli/IdWebMaps/biocycMapEcoli.txt) consists of 7 columns separated into tabs
	 * It will match column 1,2,3,4,5 and 7 to 6
	 * */
	public HashMap<String, String> parseBiocyMap(){
		 HashMap<String, String> dic = new HashMap<String, String>();
		 try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
					"data/ecoli/IdWebMaps/dic0.txt")));;
						
			String temporaryString;
            while ((temporaryString = reader.readLine()) != null) {
                    String[] columnDetail = new String[200];
                    columnDetail = temporaryString.split("\t");
                    if(columnDetail.length<4)
                    	continue;
                    if(columnDetail[0].compareTo("")!=0){
                    	dic.put(columnDetail[0],columnDetail[1]);
                    }                    
                    
                    if(columnDetail[2].compareTo("-")!=0 &&columnDetail[2].compareTo("")!=0){
                    	dic.put(columnDetail[2],columnDetail[1]);
                    }
                    String tmp= columnDetail[3].replaceAll(";", "|");
            		tmp =	tmp.replaceAll("( )+", "\t");
            		StringTokenizer tk = new StringTokenizer(tmp,"|");
                    
            		while(tk.hasMoreElements()){
                    	String k = tk.nextToken();
                    	if(k.compareTo("-")!=0)
                    	   dic.put(k,columnDetail[1]);
                    }
            		
                    if(columnDetail[3].compareTo("")!=0 && !columnDetail[3].contains(";")){
                    	dic.put(columnDetail[3],columnDetail[1]);
                    }                                     
            }
			reader.close();
		} catch (FileNotFoundException e) {				
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		 
		 return dic;
	}
	
	public HashMap<String, String> parseMapEcoli(HashMap<String, String> dic){		 
		 try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
						"data/ecoli/IdWebMaps/dic0.txt")));
			String temporaryString;
            while ((temporaryString = reader.readLine()) != null) {
            
            	String tmp= temporaryString.replaceAll(";( )+", "|");
            		tmp =	tmp.replaceAll("( )+", "\t");
            	String[] columnDetail = new String[200];
                columnDetail = tmp.split("\t");
                if(columnDetail.length<6)
                	continue;
                dic.put(columnDetail[1], columnDetail[2]);
                dic.put(columnDetail[3], columnDetail[2]);
            			
                StringTokenizer tk = new StringTokenizer(columnDetail[5],"|");
                while(tk.hasMoreElements()){
                	String k = tk.nextToken();
                	if(k.compareTo("-")!=0)
                	   dic.put(k,columnDetail[2]);
                }
            }
			
			reader.close();
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		 
		 return dic;
	}
	
	/**This function will write the dictionary into data/ecoli/IdWebMaps/Dictionary.txt
	 * The first column is the synonym, the second is the uniprot*/
	public void writeFile(HashMap<String, String> dictionary){
		
		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File("data/ecoli/IdWebMaps/dic.txt")));
			for(String syn: dictionary.keySet()){
				bf.write(syn+"\t"+dictionary.get(syn)+"\n");
			}
			bf.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {	
		CreateEcoliDic dic = new CreateEcoliDic();
		HashMap<String, String> dictionary= dic.parseBiocyMap();
		//dic.parseMapEcoli(dictionary);
		dic.writeFile(dictionary);
	}

}
