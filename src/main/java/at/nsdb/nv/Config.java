package at.nsdb.nv;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import at.nsdb.nv.utils.Utils;

/**
 * singleton to access configuration values for the application
 */
public final class Config {

    private static Config instance = null;
    private final Properties props = new Properties();
	public static boolean running = true;
	/*--------------------
	 * VersionsNr
	 */
	public static final String versionNr = "v1.5";

    /** constructor */
    private Config() {
        try {
            props.load(Config.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            Utils.logging("could not read config.properties");
        }
    }

    /**
     * @return content of file config.properties (from classpath)
     */
    private static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }


    /** 
     * @return uri for db-connection 
     * */
    public static String getDbUri() {
        return getInstance().props.getProperty("db.uri", "bolt://localhost:7687");
    }

    /** 
     * @return user-name for db-connection 
     * */
    public static String getDbUser() {
        return getInstance().props.getProperty("db.user", "neo4j");
    }

    /** 
     * @return password for db-connection 
     * */
    public static String getDbPassword() {
        return getInstance().props.getProperty("db.password", "nsdb");
    }

    /** 
     * @return dataset for simulation 
     * */
    public static String getPersonFilter() {

        String personFilter = getInstance().props.getProperty("run.personFilter", "");
		if (!personFilter.isEmpty()) {
            return personFilter;
        } else {
            return "true";
        }
    }
    
    
    /** 
     * @return dataset for simulation 
     * */
    public static int getAcceptCode() {
        String accepts = getInstance().props.getProperty("run.accepts", "");
        return !StringUtils.isNumeric( accepts) ? 5 : Math.max( 1, Math.min(10, Integer.valueOf( accepts)));
    }
    
    /** 
     * @return dataset for simulation 
     * */
    public static int getPrintDBSatus() {
        String printDBSatus = getInstance().props.getProperty("run.printDBSatus", "");
        return !StringUtils.isNumeric( printDBSatus) ? 0 : Math.max( 0, Math.min(2, Integer.valueOf( printDBSatus)));
    }

    /**
     * @return Programstop after day
     */
    public static int getStopAfterDay() {
        String stopAfterDay = getInstance().props.getProperty("run.stopAfterDay");
        return !StringUtils.isNumeric(stopAfterDay) ? 365 : Integer.valueOf(stopAfterDay);
    }

     /**
     * @return export relations
     */
    public static String getExportRelations() {
        return getInstance().props.getProperty("run.exportRelations", "whenNew");
    }

    /** 
     * @return Gauss-values for biometrics incubation period 
     * */
    public static int getIncubationGaussValue( String key) {
    	String defaultValue = "7";
    	switch( key) {
    	case "Min": case "Max":  case "Avg": case "Deviation":
    		return Integer.parseInt( getInstance().props.getProperty("incubationPeriod" + key, defaultValue));
    	default: 
    		Utils.logging( "Warning: gaussIncubationValue is defaultValue");
    		return Integer.parseInt( defaultValue);
    	}
    }
    
    /** 
     * @return Gauss- values for biometrics illness period 
     * */
    public static int getIllnessGaussValue( String key) {
    	String defaultValue = "9";
    	switch( key) {
    	case "Min": case "Max":  case "Avg": case "Deviation":
    		return Integer.parseInt( getInstance().props.getProperty("illnessPeriod" + key, defaultValue));
    	default: 
    		Utils.logging( "Warning: gaussIllnessValue is defaultValue");
    		return Integer.parseInt( defaultValue);
    	}
    }
    
    
    /** 
     * @return Gauss- values for the number of :getCanInfectGaussValue
     * */
    public static int getCanInfectGaussValue( String key) {
    	String defaultValue = "9";
    	switch( key) {
    	case "Min": case "Max":  case "Avg": case "Deviation": 
    		return Integer.parseInt( getInstance().props.getProperty("canInfectNumb" + key, defaultValue));
    	default: 
    		Utils.logging( "Warning: canInfectNumb is defaultValue");
    		return Integer.parseInt( defaultValue);
    	}
    }
    
    /**
     * print the content of the file config.properties
     */
    public static void printAll() {
        getInstance().props.forEach((key, value) -> Utils.logging("config.properties: " + key + "=" + value));
    }

    /**
     * 
     * @return file-name for logging
     */
	public static String getLogFileName() {        
        String logFile = getInstance().props.getProperty("run.logFile");
		return StringUtils.isEmpty(logFile) ? "" : logFile;
    }
    
    /**
     * 
     * @return file-name (with path) for logging
     */
	public static String getLogFileFullName() {
        
        String logFile = getLogFileName();
		if( StringUtils.isEmpty(logFile)) return "";
		else {
			String projectDirectory = System.getProperty("user.dir");
			return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + logFile;
		}
	}

	/** if :CanInfect created -> export to .csv */
	public static String canInfectFileFullFileName() {
		if( getLogFileName() == "") return "";
		else {
			String projectDirectory = System.getProperty("user.dir");
			return projectDirectory.substring( 0, projectDirectory.lastIndexOf( "\\") + 1) + "canInfect.csv";
		}
	}
    

    

    
}