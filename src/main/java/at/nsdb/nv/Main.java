/*----------------------------------------------------
 * simulate the spreading of a virus
 * 
 */

package at.nsdb.nv;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

import javax.swing.JFrame;

public class Main  extends JFrame {
	private static final long serialVersionUID = 1L;
		
	public static void main( final String[] args ) throws IOException
    {	
		// for statistics
		HashMap<Integer, StatisticADay> statistics = new HashMap<Integer, StatisticADay>();
		
		IntStream.range( 1, 10).forEach( i -> Utils.logging( " "));
		Utils.logging( "**** start Simulation-----------------------------------------------------");
		Utils.logging( "logFile = " + Parameter.logFileFullFileName());
		
		
		/*--------------------
		 * connect to DB
		 */
		Neo4j neo4j = new Neo4j();

	
		
		/*--------------------
		 * initializing database, optional, if neo4j structure is up to date
		 * 1. add biometric attributes to node 2. create relations with distance
		 */
		Utils.logging( "**** initialization ...");
		neo4j.initialize();
		Utils.logging( "---- initialization finished");


		
		
		/*--------------------
		 * Day 1, infect randomly 1. person
		 */		
		Utils.logging( "**** Day 1 ...");
		statistics.put( 1, neo4j.day1());
		Utils.logging( "---- Day 1 finished");
		
		
		
		/*--------------------
		 * calculate the spreading of the virus day by day
		 */		
		int day = 1;
		do {
			day++;
			statistics.put( day, neo4j.day( day));
			PanelPersons.getInstance().paintPanelPerson(day, statistics, neo4j.getAllPersons());
			//MyTimer.delay( 2000);
		} while( day <= Parameter.stopAfterDay && statistics.get( day).getNumbPersonsInIncubation() > 0);
		
		
		
		/*--------------------
		 * last statistics
		 */
		new PanelStatistics( statistics).repaint();
		
		
		
		/*--------------------
		 * print content of the database
		 */
        Utils.logging( "**** printing db status/content ...");
		neo4j.printNeo4jContent();
		Utils.logging( "---- printing db status/content finished");
    }
}
