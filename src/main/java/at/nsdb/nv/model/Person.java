package at.nsdb.nv.model;

import at.nsdb.nv.InfectionCalculator;

public class Person {
	
	/**--------------------
	 * status of a Person. strictly order in time: healthy, inIncubation, ill, immune
	 * healthy means never infected. After illness person becomes immune (not status healthy)
	 */	
	public static enum status { healthy, inIncubation, ill, immune }
	/** 
	 * unique identifier */
	private long id;
	/** 
	 * geo-spatial data */
	private double longitude, latitude; 
	/** 
	 * name */
	private String name;
	/** 
	 * day when person gets infected (the difference from day 0) */
	private int dayOfInfection; 
	/** 
	 * number of days indicating how long the person is in incubation<p/>
	 * the value is calculated during program-initialization */
	private int incubationPeriod;
	/** 
	 * number of days indicates how long the person is ill<p/> 
	 * the value is calculated during program-initialization */
	private int illnessPeriod;
	
	
	
	
	/**--------------------
	 * set the biometric data of a person
	 * inkubationPeriod between 1 and 14 days, normal distribution of Gauss( 7, 2)
	 * illnessPeriod between 1 and 17 days, normal distribution of Gauss( 9, 2)
	 */	
	public void setBiometrics() {
		dayOfInfection = 0;
		incubationPeriod = InfectionCalculator.calculateRandomlyIncubationPeriod();
		illnessPeriod = InfectionCalculator.calculateRandomlyIllnessPeriod();
	}
	
		

	
	/**--------------------
	 * calculate distance in km between 2 persons, disregarding widening
	 * 1 degree = 60 nm, 1 nm = 1,852 km
	 */	
	public static int distance( Person p1, Person p2) {
		return (int) ( Math.sqrt( 
			Math.pow( p1.getLongitude() - p2.getLongitude(), 2)  +
			Math.pow( p1.getLatitude()  - p2.getLatitude(),  2)) * 60 * 1852);
	}
	
	

	/**--------------------
	 * calculate the status of the person
	 * the status is strictly order in time: healthy, inIncubation, ill, immune
	 */	
	public status getStatus( int day) {
		if( dayOfInfection == 0) 
			return status.healthy;
		
		else if( dayOfInfection <= day && day <= dayOfInfection + incubationPeriod) 
			return status.inIncubation;
		
		else if( dayOfInfection + incubationPeriod < day && 
					day <= dayOfInfection + incubationPeriod + illnessPeriod) 
			return status.ill;
		
		else return status.immune;
	}

	
	
	
	/**--------------------
	 * object as string
	 */	
	public String toString() {
		return String.format( "%7d name=%s dayOfInf=%d inkubationP=%d illnessP=%d",
			id, name, dayOfInfection, incubationPeriod, illnessPeriod);
	}
	/**
	 * object as string -  print the status of the person additionally
	 */
	public String toString( int day) {
		return String.format( "%s status=%s", this.toString(), this.getStatus( day));
	}
	
	
	
	
	/*--------------------
	 * Setters, Getters, no constructor!
	 */
	public long getId() {
		return id;
	}

	public int getDayOfInfection() {
		return dayOfInfection;
	}

	public void setDayOfInfection(int dayOfInfection) {
		this.dayOfInfection = dayOfInfection;
	}

	public int getIncubationPeriod() {
		return incubationPeriod;
	}

	public void setIncubationPeriod(int inkubationPeriod) {
		this.incubationPeriod = inkubationPeriod;
	}

	public int getIllnessPeriod() {
		return illnessPeriod;
	}

	public void setIllnessPeriod(int illnessPeriod) {
		this.illnessPeriod = illnessPeriod;
	}

	public void setId(long id) {
		this.id = id;
	}

	/** longitude - unit [degree] */
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/** latitude - unit [degree] */
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getFirstName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
