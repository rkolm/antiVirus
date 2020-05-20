package at.nsdb.nv;

import java.io.IOException;
import java.util.Properties;

/**
 * singleton to access configuration values for the application
 */
public final class Config {

    private static Config instance = null;
    private final Properties props = new Properties();

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
     * print the content of the file config.properties
     */
    public static void printAll() {
        getInstance().props.forEach((key, value) -> Utils.logging("config.properties: " + key + "=" + value));
    }
    
}