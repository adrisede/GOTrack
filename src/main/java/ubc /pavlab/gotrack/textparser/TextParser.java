/*
 */
package ubc.pavlab.gotrack.textparser;

import com.google.common.base.Joiner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

/**
 *
 * @author asedeno
 */
public class TextParser {

    private static final String SOURCE_FAMILY = "\\assets\\info";
    private static final String SOURCE_DOMAIN = "\\assets\\domain";
    private static String familyFile = "family.txt";
    private static String domainFile = "domain.txt";

    /**
     *
     */
    private static void parseFamily() throws Exception {

        File here = new File("");
        File file = new File(here.getAbsolutePath() + SOURCE_FAMILY);
        BufferedReader stream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(familyFile))));
        System.out.println("parse family : " + file.getAbsolutePath() + SOURCE_FAMILY);
        // to parse each line
        Joiner joiner = Joiner.on("\t").skipNulls();
        StringTokenizer tokenizer;
        String currentLine;
        while ((currentLine = stream.readLine()) != null) {

            if (currentLine == null || currentLine.trim().isEmpty()) {
                continue;
            }
            // loop per family
            String family = "no-family";
            // is family ?
            if (!currentLine.contains(",")) {
                family = currentLine;
            }

            String nextLine;
            while ((nextLine = stream.readLine()) != null) {
                if (nextLine == null || nextLine.trim().isEmpty()) {
                    break;
                }
                tokenizer = new StringTokenizer(nextLine, ",");
                while (tokenizer.hasMoreElements()) {

                    String token = tokenizer.nextToken();
                    if (token.length() > 2) {
                        String uniprot = token.substring(token.indexOf("(")).trim();
                        String name = token.substring(0, token.indexOf("(")).trim();

                        output.write(joiner.join(family, name, uniprot) + "\n");
                    }

                }
            }
        }

        System.out.println("done parse family");
        output.close();
        stream.close();
    }

    /**
     *
     */
    private static void parseDomain() throws Exception {

        File here = new File("");
        File file = new File(here.getAbsolutePath() + SOURCE_DOMAIN);
        BufferedReader stream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(domainFile))));
        System.out.println("parse domain : " + file.getAbsolutePath() + SOURCE_DOMAIN);
        // to parse each line
        Joiner joiner = Joiner.on("\t").skipNulls();
        StringTokenizer tokenizer;
        String currentLine;
        while ((currentLine = stream.readLine()) != null) {

            if (currentLine == null || currentLine.trim().isEmpty()) {
                continue;
            }
            // loop per domain
            String domain = "no-domain";
            // is domain ?
            if (!currentLine.contains(",")) {
                domain = currentLine;
            }

            String nextLine;
            while ((nextLine = stream.readLine()) != null) {
                if (nextLine == null || nextLine.trim().isEmpty()) {
                    break;
                }
                if (!nextLine.contains(",")) {
                    domain += "\t" + nextLine;

                    continue;
                }
                tokenizer = new StringTokenizer(nextLine, ",");
                while (tokenizer.hasMoreElements()) {

                    String token = tokenizer.nextToken();
                    if (token.length() > 2) {
                        String uniprot = token.substring(token.indexOf("("), token.indexOf(")")+1).trim();
                        String name = token.substring(0, token.indexOf("(")).trim();

                        output.write(joiner.join(domain, name, uniprot) + "\n");
                    }
                }
            }
        }
        System.out.println("done parse domain");
        output.close();
        stream.close();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        parseFamily();
        parseDomain();
    }
}
