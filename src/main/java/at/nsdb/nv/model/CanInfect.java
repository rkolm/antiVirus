package at.nsdb.nv.model;


/*--------------------
 * connection (in neo4j relationship) between 2 persons
 */
public class CanInfect {
	private long id1, id2;
	int distance;
	
	
	
	public String toString() {
		return String.format( "%7d <---> %7d, distance=%d", id1, id2, distance);
	}
	public String toExportFile() {
		return String.format( "%d;%d;%d", id1, id2, distance);
	}
	public static String toExportFileHeader() {
		return "id1;id2;distance";
	}
	
	

	/*--------------------
	 * Setters, Getters
	 */
	public long getId1() {
		return id1;
	}

	public long getId2() {
		return id2;
	}
	
	public int getDistance() {
		return distance;
	}
	
	/*--------------------
	 * constructor
	 */
	public CanInfect( long id1, long id2, int distance) {
		this.id1 = id1;
		this.id2 = id2;
		this.distance = distance;
	}
	
	
	
}
