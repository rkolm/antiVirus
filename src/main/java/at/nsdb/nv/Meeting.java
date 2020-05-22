package at.nsdb.nv;


/*--------------------
 * connection (in neo4j relationship) between 2 persons
 */
public class Meeting {
	private int id1, id2;
	int distance;
	
	
	
	public String toString() {
		return String.format( "%7d <---> %7d, distance=%d", id1, id2, distance);
	}
	
	

	/*--------------------
	 * Setters, Getters
	 */
	public int getId1() {
		return id1;
	}

	public int getId2() {
		return id2;
	}
	
	public int getDistance() {
		return distance;
	}
	
	/*--------------------
	 * constructor
	 */
	public Meeting( int id1, int id2, int distance) {
		this.id1 = id1;
		this.id2 = id2;
		this.distance = distance;
	}
	
	
	
}
