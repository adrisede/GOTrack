package ubc.pavlab.gotrack.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.CoreConnectionPNames;

/**
 * Functions to query the uniprot.org web page
 * @author asedeno
 */
public class SingleFileWrapper {

	public static final String URL = "http://www.uniprot.org/uniprot/";
	public static final String XML = ".xml";

	public static String getContent(String line) {
		return parseOutputPerLine(getContentFromWeb(line));
	}

	/**
	 * Connects and retrieve the content for a pubmed ID
	 * 
	 * @param nameId
	 * @return
	 */
	public static LinkedList<Object> getContentFromWeb(String nameId) {
		try {

			System.out.println("Executing: " + nameId);
			HttpClient client = new DefaultHttpClient();

			DefaultHttpRequestRetryHandler defaultHttp = new DefaultHttpRequestRetryHandler(
					0, false);
			((AbstractHttpClient) client)
					.setHttpRequestRetryHandler(defaultHttp);

			client.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);

			// Timeout when server does not send data.
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
					2000);
			HttpGet request = new HttpGet(URL + nameId + XML);

			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			String line = "";

			String accession = null;
			String name = nameId;
			String recommendedName = null;
			String alternativeName = null;

			LinkedList<String> goList = new LinkedList<String>();

			while ((line = rd.readLine()) != null) {

				if (accession == null && line.indexOf("accession") != -1) {
					accession = getAccession(line);
				}
				if (recommendedName == null
						&& line.indexOf("recommendedName") != -1) {
					recommendedName = getFullName(rd.readLine());
				}
				if (alternativeName == null
						&& line.indexOf("alternativeName") != -1) {
					alternativeName = getFullName(rd.readLine());
				}
				if (line.indexOf("<dbReference type=\"GO\" id=\"") != -1) {

					String goName = extractGO(line);
					String term = extractTerm(rd.readLine());
					String evidence = extractEvidence(rd.readLine());

					goList.add(goName);
					goList.add(term);
					goList.add(evidence);
				}
			}

			if (accession != null && recommendedName != null
					&& alternativeName != null) {
				LinkedList<Object> list = new LinkedList<Object>();

				list.add(accession);
				list.add(name);
				list.add(recommendedName);
				list.add(alternativeName);
				list.add(goList);
				return list;
			}

			return null;
		} catch (Exception e) {
			String error = "Couldn't retrieve information for : " + nameId
					+ " << CAUSE: >> ";
			e.printStackTrace();
			System.err.print(error + e.toString() + "\n");
		}
		return null;
	}

	/**
	 * @param numberString
	 * 
	 */
	public static String getN(String numberString) {
		return numberString.replaceAll("\\D+", "");
	}
/**
 * Parse the accession node
 * @param line
 * @return
 */
	private static String getAccession(String line) {
		return line.substring(line.indexOf("<accession>") + 11,
				line.indexOf("</accession>"));
	}
/**
 * Parse the name node
 * @param line
 * @return
 */
	private static String getFullName(String line) {
		return line.substring(line.indexOf("<fullName>") + 10,
				line.indexOf("</fullName>"));
	}
/**
 * Parse the go id node
 * @param line
 * @return
 */
	private static String extractGO(String line) {
		return line.substring(line.indexOf("<dbReference type=\"GO\" id=\"")
				+ "<dbReference type=\"GO\" id=\"".length(),
				line.indexOf("\">"));
	}

	/**
	 * Parse the evidence code node
	 * @param line
	 * @return
	 */
	private static String extractEvidence(String line) {
		return line.substring(
				line.indexOf("<property type=\"evidence\" value=\"")
						+ "<property type=\"evidence\" value=\"".length(),
				line.indexOf("\"/>"));
	}
/**
 * Parse the human readable go name
 * @param line
 * @return
 */
	private static String extractTerm(String line) {

		return line.substring(line.indexOf("<property type=\"term\" value=\"")
				+ "<property type=\"term\" value=\"".length(),
				line.indexOf("\"/>"));
	}

	@SuppressWarnings("unchecked")
	public static String parseOutputPerLine(LinkedList<Object> list) {

		if (list == null) {
			return null;
		}

		String accession = list.get(0).toString() + "\t";
		String name = list.get(1).toString() + "\t";
		String fullName = list.get(2).toString() + "\t";
		String alternativeName = list.get(3).toString() + "\t";

		LinkedList<String[]> gos = new LinkedList<String[]>();
		LinkedList<String> golist = new LinkedList<String>();
		golist = (LinkedList<String>) list.get(4);
		String output = "";
		if (!golist.isEmpty()) {
			for (int i = 0; i < golist.size(); i += 3) {
				String[] godetails = new String[3];

				godetails[0] = golist.get(i) + "\t";
				godetails[1] = golist.get(i + 1) + "\t";
				godetails[2] = golist.get(i + 2);
				gos.add(godetails);
			}

			for (int i = 0; i < gos.size(); i++) {
				String[] details = gos.get(i);
				output += accession + name + fullName + alternativeName
						+ details[0] + details[1] + details[2] + "\n";
			}
		} else {
			output += accession + name + fullName + alternativeName + "no-data\t"
					+ "no-data\t" + "no-data\t" + "\n";
		}

		return output;
	}

	public static void main(String[] args) throws IOException {
		//LinkedList<Object> connection = getContentFromWeb("14331_ARATH"); //

		// 14331_ARATH
		//System.out.println(parseOutputPerLine(connection));

		 parseFolder(".");
	}

	public static void parseFolder(String route) {
		File dir = new File(route);
		File[] files = dir.listFiles();
		for (File file : files) {
			try {
				parseFile(file);
			} catch (Exception e) {
				System.out.println(e.getLocalizedMessage());
				System.out.println("Could not be completed for:"
						+ file.getName());
			}
		}
	}

	/**
	 * Parse one file.
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void parseFile(File file) throws FileNotFoundException,
			IOException {

		if (validInputName(file)) {
			return;
		}

		System.out.println("Processing: " + file.getAbsolutePath());
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		String foutput = getN(file.getName()) + "_info.txt";

		BufferedWriter writer = new BufferedWriter(new FileWriter(foutput));

		String line;
		while ((line = reader.readLine()) != null) {

			String output = null;
			try {
				output = getContent(line);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (output == null) {
				continue;
			}

			writer.write(output);
			writer.flush();
		}
		reader.close();
		writer.close();
	}

	private static boolean validInputName(File file) {
		String fileNumber = getN(file.getName());
		if (fileNumber.equals(""))
			return true;
		else
			return false;
	}
}