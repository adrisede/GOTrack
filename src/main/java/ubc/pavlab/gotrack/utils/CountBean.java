package ubc.pavlab.gotrack.utils;

/**
 * This type is used for an ordered output of countgo files
 * 
 * @author asedeno
 * 
 */
@SuppressWarnings("rawtypes")
public class CountBean implements Comparable {

	public int priority;
	public String data;

	/**
	 * Constructor 
	 * @param priority 
	 * @param data
	 */
	public CountBean(int priority, String data) {
		this.priority = priority;
		this.data = data;
	}

	/**
	 * Function to compare to CountBean objects based on the priority
	 */
	public int compareTo(Object o) {
		CountBean obj = (CountBean) o;
		if (obj.priority < this.priority) {
			return -1;
		} else if (obj.priority == this.priority) {
			return 0;
		} else {
			return 1;
		}
	}

}
