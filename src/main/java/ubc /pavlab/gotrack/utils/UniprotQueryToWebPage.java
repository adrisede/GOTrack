package ubc.pavlab.gotrack.utils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.CoreConnectionPNames;

import ubc.pavlab.gotrack.constants.Species;

/**
 * This class will create the *_webpage_map.txt. 
 * A file that contains the most updated ids for our gene ids
 * 
 * This class is designed to run as a stand alone program
 * 
 * @author asedeno
 *
 */
public class UniprotQueryToWebPage {

	public static final String URL = "http://www.uniprot.org/uniprot/";
	
	/**
	 * Query the web page looking for an id
	 * @param id The Id we are looking for
	 * @return The id in the web page
	 */
	public static String getUniprotFromWebPage(String id){
		String mappedId="";
		try {

			HttpClient client = new DefaultHttpClient();

			DefaultHttpRequestRetryHandler defaultHttp = new DefaultHttpRequestRetryHandler(
					0, false);
			((AbstractHttpClient) client)
					.setHttpRequestRetryHandler(defaultHttp);

			client.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);

			// Timeout when server does not send data.
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
					1000);
			HttpGet request = new HttpGet(URL + id+".xml");

			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			String line = "";			
			boolean dataRetrieved = false;
			while ((line = rd.readLine()) != null) {

				if ((line.indexOf("accession") != -1) && !dataRetrieved) {
					dataRetrieved = true;
					mappedId = line; 
					mappedId = mappedId.replace("<accession>", "");
					mappedId = mappedId.replace("</accession>", "");
					break;
				}

			}

			return mappedId;
		} catch (IOException e) {
			String error = "Couldn't retrieve information for uniprot: "
					+ id + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}catch (Exception e) {
			String error = "Couldn't retrieve information for : "
					+ id + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}
				
		return mappedId;
	}
	
	/**
	 * This will update the ids of the file AllGenes for a given species
	 * @param species species to compute
	 */
	public static void processSpecies(String species, HashMap<String, String> localDictionary, Logger log){
		BufferedReader readerOfdic;
		try {
			log.info("Updating "+species+"_webpage_map.txt");
			readerOfdic = Species.getReader(species,
					"AllGenes.txt");
			BufferedWriter bufferedWriterOfSyns = Species.getWriter(species,
					species+"_webpage_map.txt");
			bufferedWriterOfSyns.write("idInFile\tWebId\n");
			String geneAssocId;
			String wpId="";
			while ((geneAssocId = readerOfdic.readLine()) != null) {
				for(int count = 0; count<4 && wpId.compareTo("")==0;count++)
					wpId = getUniprotFromWebPage(geneAssocId);
				if(wpId.compareTo("")==0){
					/*If the server returned no data but the id is in the local dictionary
					 * then use the value in the local dictionary, the server might be out of line*/
					wpId = localDictionary.get(geneAssocId);
				}
				if(wpId!=null && wpId.compareTo("")!=0 && wpId.length()>1)
				   bufferedWriterOfSyns.write(geneAssocId+"\t"+wpId+"\n");
				
				String localitem = localDictionary.get(geneAssocId);
				if(localitem!= null && localitem.compareTo(wpId)!=0 && wpId.compareTo("")!=0){
					log.info(wpId+" is a newer version for "+localitem);
				}
				
				wpId="";
				bufferedWriterOfSyns.flush();
				
				
			}
			bufferedWriterOfSyns.close();
			readerOfdic.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * This is only for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		//processSpecies("ecoli");
	}

}
