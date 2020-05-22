package at.nsdb.nv;

public class Person {
	
	/*--------------------
	 * status of a Person. strictly order in time: healthy, inIncubation, ill, immune
	 * healthy means never infected. After illness person becomes immune (not status healthy)
	 */	
	public static enum status { healthy, inIncubation, ill, immune }
	
		
	
	
	/*--------------------
	 * attributes of the class
	 */	
	// from import file
	private int id, longitude, latitude, age;
	private String firstName;
	
	// from import file or optional created by this software
	private int dayOfInfection, incubationPeriod, illnessPeriod;
	
	
	
	
	/*--------------------
	 * set the biometric data of a person
	 * inkubationPeriod between 1 and 14 days, normal distribution of Gauss( 7, 2)
	 * illnessPeriod between 1 and 17 days, normal distribution of Gauss( 9, 2)
	 */	
	public void setBiometrics() {
		dayOfInfection = 0;
		incubationPeriod = Parameter.calculateRandomlyIncubationPeriod();
		illnessPeriod = Parameter.calculateRandomlyIllnessPeriod();
	}
	
		

	
	/*--------------------
	 * calculate distance in km between 2 persons, disregarding widening
	 * 1 degree = 60 nm, 1 nm = 1,852 km
	 */	
	public static int distance( Person p1, Person p2) {
		return (int) ( Math.sqrt( 
			Math.pow( p1.longitude - p2.longitude, 2)  +
			Math.pow( p1.latitude - p2.latitude, 2)) / 60 * 1852);
	}
	public int distance( Person p) {
		return distance( this, p);
	}
	
	
	

	/*--------------------
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

	
	
	
	/*--------------------
	 * if the day is given print the status of the person additionally
	 */	
	public String toString() {
		return String.format( "%7d name=%s age=%d dayOfInf=%d inkubationP=%d illnessP=%d",
			id, firstName, age, dayOfInfection, incubationPeriod, illnessPeriod);
	}
	public String toString( int day) {
		return String.format( "%s status=%s", this.toString(), this.getStatus( day));
	}
	
	
	
	
	/*--------------------
	 * Setters, Getters, no constructor!
	 */
	public int getId() {
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

	public void setId(int id) {
		this.id = id;
	}

	public int getLongitude() {
		return longitude;
	}

	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

}
