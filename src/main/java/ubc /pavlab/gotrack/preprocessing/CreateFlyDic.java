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


/**
 * This class will parse file flydict0.txt into data/ecoli/IdWebMaps/Dictionary.txt
 * 
 * flydict0.txt contains the information of http://www.uniprot.org/docs/fly
 * 
 * */
public class CreateFlyDic {

	/**
	 * flydict0.txt has three columns, this function will map column 1 and 3 (old ids) to column number 2 (uniprot)
	 * 
	 * @return map of column 1 and 3 to number 2
	 * */
	public HashMap<String, String> parseBiocyMap(){
		 HashMap<String, String> dic = new HashMap<String, String>();
		 try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
					"data/fly/IdWebMaps/flydict0.txt")));;
						
			String temporaryString;
			reader.readLine();
            while ((temporaryString = reader.readLine()) != null) {
                    String[] columnDetail = new String[200];
                    columnDetail = temporaryString.split("\t");
                    if(columnDetail.length<3)
                    	continue;
                    if(columnDetail[0].compareTo("")!=0){
                    	dic.put(columnDetail[0],columnDetail[1]);
                    }                    
                    
                    if( columnDetail[2].compareTo("")!=0){
                    	dic.put(columnDetail[2],columnDetail[1]);
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
	 * The first column is the synonym, the second is the uniprot
	 * @param dictionary  the dictionary to be written
	 * */
	public void writeFile(HashMap<String, String> dictionary){
		
		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File("data/fly/IdWebMaps/Dictionary.txt")));
			for(String syn: dictionary.keySet()){
				bf.write(syn+"\t"+dictionary.get(syn)+"\n");
			}
			bf.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}

	}
	
	/**
	 * 
	 * This main function can be run alone
	 * 
	 * @param args
	 */
	public static void main(String[] args) {	
		CreateFlyDic dic = new CreateFlyDic();
		HashMap<String, String> dictionary= dic.parseBiocyMap();		
		dic.writeFile(dictionary);
	}

}
