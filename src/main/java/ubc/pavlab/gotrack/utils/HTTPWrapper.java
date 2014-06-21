package ubc.pavlab.gotrack.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.CoreConnectionPNames;

/**
 * pubmed mapping using the web pages
 * 
 * @author asedeno
 * 
 */
public class HTTPWrapper {

	public static final String URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=";

	/**
	 * Connects and retrieve the content for a pubmed ID
	 * 
	 * @param pubmedId
	 * @return
	 */
	public static String[] getRetractedPubmedFromWeb(String pubmedId) {
		try {

			HttpClient client = new DefaultHttpClient();

			DefaultHttpRequestRetryHandler defaultHttp = new DefaultHttpRequestRetryHandler(
					0, false);
			((AbstractHttpClient) client)
					.setHttpRequestRetryHandler(defaultHttp);

			client.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 95520);

			// Timeout when server does not send data.
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
					1000);
			HttpGet request = new HttpGet(URL + pubmedId);

			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			String result[] = null;
			String notes = null;
			String types = null;
			String line = "";
			String date = "";
			boolean dateRetrieved = false;
			while ((line = rd.readLine()) != null) {

				if ((line.indexOf("medent {") != -1) && !dateRetrieved) {
					dateRetrieved = true;
					line = rd.readLine(); // em std {
					String y = rd.readLine();
					String m = rd.readLine();
					String d = rd.readLine();
					date = getN(m) + "/" + getN(d) + "/" + getN(y);
				}

				if ((line.indexOf("retract {") != -1)
						|| (line.indexOf("Retracted Publication") != -1)) {
					notes = line;
				} else if ((line.indexOf("type retracted") != -1)) {
					types = "retracted";
				} else if ((line.indexOf("type in-error") != -1)) {
					types = "in-error";
				} else {
					continue;
				}
			}
			notes = notes == null ? null : URL + pubmedId;

			if ((types != null) && (notes != null)) {
				result = new String[3];
				result[0] = types;
				result[1] = notes;
				result[2] = date;
			}
			return result;
		} catch (IOException e) {
			String error = "Couldn't retrieve information for pubmed: "
					+ pubmedId + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}
		return null;
	}

	/**
	 * Connects and retrieve the content for a pubmed ID
	 * 
	 * @param pubmedId
	 * @return
	 */
	public static String getPubmedDateFromWeb(String pubmedId) {
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
			HttpGet request = new HttpGet(URL + pubmedId);

			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			String line = "";
			String date = "";
			boolean dateRetrieved = false;
			while ((line = rd.readLine()) != null) {

				if ((line.indexOf("medent {") != -1) && !dateRetrieved) {
					dateRetrieved = true;
					line = rd.readLine(); // em std {
					String y = rd.readLine();
					String m = rd.readLine();
					String d = rd.readLine();
					date = getN(m) + "/" + getN(d) + "/" + getN(y);
					break;
				}

			}

			return date;
		} catch (IOException e) {
			String error = "Couldn't retrieve information for pubmed: "
					+ pubmedId + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}catch (Exception e) {
			String error = "Couldn't retrieve information for pubmed: "
					+ pubmedId + " << CAUSE: >> ";
			// e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}
		return null;
	}

	/**
	 * Get a number inside a string
	 * @param numberString
	 * */
	public static String getN(String numberString) {
		return numberString.replaceAll("\\D+", "");
	}

	/**
	 * for testing only
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String[] connection = getRetractedPubmedFromWeb("16862176"); // not
																		// retracted
		System.out.println(Arrays.toString(connection));
		connection = getRetractedPubmedFromWeb("22191421"); // retracted
		System.out.println(Arrays.toString(connection));
		connection = getRetractedPubmedFromWeb("10024883"); // retracted
		System.out.println(Arrays.toString(connection));

		String date = getPubmedDateFromWeb("16862176"); // not
		// retracted
		System.out.println((date));
		date = getPubmedDateFromWeb("22191421"); // retracted
		System.out.println((date));
		date = getPubmedDateFromWeb("10024883"); // retracted
		System.out.println((date));

		// 10024883

	}
}