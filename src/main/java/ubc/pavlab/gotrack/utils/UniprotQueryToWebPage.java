package ubc.pavlab.gotrack.utils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

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
			String error = "Couldn't retrieve information for uniprot: "
					+ id + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}
				
		return mappedId;
	}
	
	/**
	 * This will update the ids of the file AllGenes for a given species
	 * @param species
	 */
	public static void processSpecies(String species){
		BufferedReader readerOfdic;
		try {
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
				bufferedWriterOfSyns.write(geneAssocId+"\t"+wpId+"\n");
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
	 * @param args
	 */
	public static void main(String[] args) {
		
		processSpecies("ecoli");
	}

}
