package at.nsdb.nv;

import at.nsdb.nv.utils.Utils;

public abstract class InfectionCalculator {
	
	/*--------------------
	 * Programstop after day
	 */
	public static final int stopAfterDay = 365; // best value 365
	public static boolean running = true;
	
	/*--------------------
	 * VersionsNr
	 */
	public static final String versionNr = "v1.4";
	
	// name of the log file
	private static String logFile = "logging.txt"; 	
	public static String logFileFullFileName() {
		if( logFile == "") return "";
		else {
			String projectDirectory = System.getProperty("user.dir");
			return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + logFile;
		}
	}
	
	// if :CanInfect created -> export to .csv
	private static String canInfectFile = "canInfect.csv"; 
	public static String canInfectFileFullFileName() {
		if( logFile == "") return "";
		else {
			String projectDirectory = System.getProperty("user.dir");
			return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + canInfectFile;
		}
	}
	
	
	
	/**--------------------
	 * calculate randomly the incubation and illness period
	 */
	public static int calculateRandomlyIncubationPeriod() {
		// min=1, max=14, avg=7, dev=2
		return Utils.randomGetGauss( 
			Config.getIncubationGaussValue( "Min"), Config.getIncubationGaussValue( "Max"),
			Config.getIncubationGaussValue( "Avg"), Config.getIncubationGaussValue( "Deviation")); 
	}
	public static int calculateRandomlyIllnessPeriod() {
		return Utils.randomGetGauss( 
			Config.getIllnessGaussValue( "Min"), Config.getIllnessGaussValue( "Max"),
			Config.getIllnessGaussValue( "Avg"), Config.getIllnessGaussValue( "Deviation")); 
	}
	
	
	
	/**--------------------
	 * how many CanInfect has a person ?
	 */
	public static int calculateRandomlyNumbCanInfect() {
		return Utils.randomGetGauss(
			Config.getCanInfectGaussValue( "Min"), Config.getCanInfectGaussValue( "Max"),
			Config.getCanInfectGaussValue( "Avg"), Config.getCanInfectGaussValue( "Deviation")); 
	}
	
	
	
	/**--------------------
	 * init: is there a connection due to distance? randomly calculated
	 */
	public static boolean canInfect( int distance) {
		return Utils.randomGetDouble() < 1.0 / Math.pow( distance/500.0, 2.0);
	}

}
