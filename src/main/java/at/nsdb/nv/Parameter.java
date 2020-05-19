package at.nsdb.nv;

public abstract class Parameter {
	
	/*--------------------
	 * VersionsNr
	 */
	public static final String versionNr = "v1.1";
	
	// number of selected persons
	public static final int numPersonsSelected = 5000;
		
	
	// 1. add biometric attributes to node 
	// 2. create relations with distance
	public static final boolean initialize = true;

	
	// name of the log file
	private static String logFile = "logging.txt"; 
	

	
	/*--------------------
	 * get the full path/filename on relative directory
	 */
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
	 * init: is there a connection due to distance? randomly calculated
	 */
	public static boolean meetingPossible( int distance) {
		return Utils.randomGetDouble() < 
			Math.min( 0.5, Math.max( 0.001, 1 / Math.max( 1,  Math.pow( distance/1000.0, 2))));
	}
	
	
	
	/*--------------------
	 * day: probability to infect, depending on distance
	 */
	public static boolean infected( int distance) {
		boolean infected = Utils.randomGetDouble() < 
			Math.min( 0.25, Math.max( 0.01, 1 / Math.max( 1,  distance/1000.0)));
		return infected;
	}
	
	

}
