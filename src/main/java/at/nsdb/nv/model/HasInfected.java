package at.nsdb.nv.model;


/*--------------------
 * Relation: Person.id1 has infected Person.id2
 */
public class HasInfected {
	private long id1, id2;
	int day;
	
	
	
	public String toString() {
		return String.format( "%7d <---> %7d, day=%d", id1, id2, day);
	}
	public String toExportFile() {
		return String.format( "%d;%d;%d", id1, id2, day);
	}
	public static String toExportFileHeader() {
		return "id1;id2;day";
	}
	
	/*--------------------
	 * constructor
	 */
	public HasInfected( long id1, long id2, int day) {
		this.id1 = id1;
		this.id2 = id2;
		this.day = day;
	}
	
	
	
}
