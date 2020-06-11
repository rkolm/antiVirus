package at.nsdb.nv;

import at.nsdb.nv.utils.Utils;

/** main-class */
public class Main {

	/** main entry */
	public static void main( final String[] args ) 
    {			
		//IntStream.range( 1, 10).forEach( i -> Utils.logging( " "));		
		Utils.logging( "logFile = " + Config.getLogFileName());

		Utils.logging( "**** start Virus-Simulation");
		VirusSimulation simulation = VirusSimulation.getInstance();
		simulation.run();
		
    }
}
