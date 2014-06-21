package ubc.pavlab.gotrack.preprocessing;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * 
 * @author asedeno
 */
public class GeneAka {
	@SuppressWarnings("rawtypes")
	private ArrayList list;

	@SuppressWarnings("unchecked")
	public GeneAka(String names) {
		StringTokenizer token = new StringTokenizer(names, "|");
		list = new ArrayList<String>();
		while (token.hasMoreElements()) {
			list.add(token.nextElement());
		}

	}

	@SuppressWarnings("rawtypes")
	public ArrayList getList() {
		return list;
	}
}
