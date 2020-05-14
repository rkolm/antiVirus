package at.nsdb.nv;

public abstract class Parameter {
	
	/*--------------------
	 * location/name of the neo4J database (will be crated if createNewND = true
	 * relative to <projectdirectory>
	 */
	private static final String relDBPath = "DBs\\antiVirus";
	
	// if true, old database will be destroyed, a new one will be created
	public static final boolean createNewDB = false;	
	
	// 1. add biometric attributes to node 2. create relations with distance
	public static final boolean initMeetings = false;
	
	// if true do Day0, fill biometric attributes e.g. inkubationPeriod randomly with content
	public static final boolean day0 = false;
	
	// name of the log file
	private static String logFile = "logging.txt";  // no path allowed, no logFile if ""
	

	
	/*--------------------
	 * get the full path/filename on relative directory
	 */
	public static String neo4jFullFileName() {
		String projectDirectory = System.getProperty("user.dir");
		return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + relDBPath;
	}
	public static String logFileFullFileName() {
		if( logFile == "") return "";
		else {
			String projectDirectory = System.getProperty("user.dir");
			return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + logFile;
		}
	}
	
	
	
	/*--------------------
	 * calculate randomly the incubation and illness period
	 */
	public static int calculateRandomlyIncubationPeriod() {
		// min=1, max=14, avg=7, dev=2
		return Utils.randomGetGauss( 1, 14, 7, 2); 
	}
	public static int calculateRandomlyIllnessPeriod() {
		return Utils.randomGetGauss( 1, 17, 9, 2);
	}
	
	
	
	/*--------------------
	 * how many meetings has a person ?
	 */
	public static int calculateRandomlyNumbMeetings() {
		return Utils.randomGetGauss( 5, 15, 10, 2);
	}
	
	
	
	/*--------------------
	 * is there a connection due to distance? randomly calculated
	 */
	public static boolean calculateRandomlyIfConnection( int distance) {
		return Utils.randomGetDouble() < Math.min( 0.1, Math.max( 0.01, 1.0 / distance));
	}
	
	

}
