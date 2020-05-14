package at.nsdb.nv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


/*--------------------
 * some utilities
 */
public abstract class Utils {
	

	static Random random = new Random();
	
	/*--------------------
	 * randomGetInt( 10, 12) means randomly 10, 11 or 12
	 */
	public static int randomGetInt( int min, int max) {
		return random.nextInt( max - min + 1) + min;
	}
	
	
	
	/*--------------------
	 * random double number between 0 (inklusive) and 1 exklusive
	 */
	public static double randomGetDouble() {
		return random.nextDouble();
	}
	
	
	
	/*--------------------
	 * Random variable according to Gauss distribution with 
	/* left/right- border and according to average and deviation 
	 */
	public static int randomGetGauss( int min, int max, int mean, int dev) {
		double g =  mean + random.nextGaussian() * dev;
		return Math.min( max, Math.max( min, (int) Math.round( g)));
	}
	
	
	
	/*--------------------
	 * logging in console and log- file
	 */
	public static void logging( Object o) {
	    DateFormat formatter = new SimpleDateFormat( "dd.MM. HH:mm:ss");
	    String s = String.format( "%s:, %s", formatter.format(new Date()), o.toString());
	    if( o.toString().startsWith( "****")) s = "\n" + s;
	    System.out.println( s);
	    
		String fileName = Parameter.logFileFullFileName();
	    if( fileName != "") {
	        try {   
	            // Open given file in append mode. 
	            BufferedWriter out = new BufferedWriter( new FileWriter(fileName, true)); 
	            out.write(s); 
	            out.newLine();
	            out.close(); 
	        } 
	        catch (IOException e) { 
	            System.out.println(" Error in writing logfile " + fileName + " " + e); 
	        } 
		}
	}
	
}
