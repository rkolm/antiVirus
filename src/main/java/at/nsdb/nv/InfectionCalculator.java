package at.nsdb.nv;

import at.nsdb.nv.utils.Utils;

public abstract class InfectionCalculator {
	
	
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
		if( distance < 35) return true; // ca 1 km
		else return Utils.randomGetDouble() < Math.max( 0.0001, Math.exp( -0.01 * distance));
	}

}
