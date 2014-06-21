package ubc.pavlab.gotrack.mapping;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Utilities used by the mapping algorithm
 * @author asedeno
 * 
 */
public class MappingUtils {

	/**
	 * This function reads all files in a directory that has termination
	 * filetype
	 * 
	 * @param indir
	 *            Directory we will look at
	 * @param filetype
	 *            Regular expression to filter files
	 *            @return list of files that match the expression
	 * */
	public static ArrayList<File> readDirectory(String dir, String filetype) {
		File[] files = null;
		ArrayList<File> arrayOfFiles = null;
		File directory = new File(dir);
		try {
			FileFilter fileFilter = new WildcardFileFilter(filetype);
			files = directory.listFiles(fileFilter);
			Arrays.sort(files);
			if (files != null) {
				arrayOfFiles = new ArrayList<File>(files.length);
				for (int i = 0; i < files.length; i++) {
					arrayOfFiles.add(files[i]);
				}
			} else {

			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return arrayOfFiles;
	}
	

}
