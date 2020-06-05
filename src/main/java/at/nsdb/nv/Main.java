/*----------------------------------------------------
 * simulate the spreading of a virus
 * 
 */

package at.nsdb.nv;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

import javax.swing.JFrame;

import at.nsdb.nv.model.StatisticADay;
import at.nsdb.nv.utils.Utils;
import at.nsdb.nv.view.PanelPersons;
import at.nsdb.nv.view.PanelStatistics;

public class Main  extends JFrame {
	private static final long serialVersionUID = 1L;
		
	public static void main( final String[] args ) throws IOException
    {	
		// for statistics
		var statistics = new HashMap<Integer, StatisticADay>();
		
		IntStream.range( 1, 10).forEach( i -> Utils.logging( " "));
		Utils.logging( "**** start Simulation-----------------------------------------------------");
		Utils.logging( "logFile = " + Config.getLogFileFullName());
		
		
		/*--------------------
		 * connect to DB
		 */
		Neo4j neo4j = new Neo4j();

	
		
		/*--------------------
		 * initializing database, optional, if neo4j structure is up to date
		 * 1. add biometric attributes to node 2. create relations with distance
		 */
		Utils.logging( "**** initialization ...");
		neo4j.setBiometricsForAllPersons();
		neo4j.setCanInfectRelationsForAllPersons();
		Utils.logging( "---- initialization finished");

		
		
		/*--------------------
		 * calculate the spreading of the virus day by day
		 */		
		int day = 0;
		do {			
			day++;
			if( day == 1) Utils.logging( "**** Day 1 ...");
			neo4j.day( day, statistics);
			PanelPersons.getInstance( neo4j).paintPanelPerson( day, statistics);
			if( day == 1) Utils.logging( "---- Day 1 finished");
			else neo4j.printStatusPersons( day, statistics.get( day));
			
			while( ! InfectionCalculator.running) Utils.sleepInSec( 1);
						
		//} while( day <= Parameter.stopAfterDay && statistics.get( day).getNumbPersonsInIncubation() > 0);
		} while( day <= Config.getStopAfterDay() && statistics.get( day).getNumbPersonsInIncubation() > 0);
		
		
		
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
