package at.nsdb.nv.model;

import java.util.Vector;

import at.nsdb.nv.utils.Utils;

/**
 * collection of persons with statistical information
 */
public class Persons {
	private Vector<Person> persons;
	private int minLongitude = Integer.MAX_VALUE;
	private int maxLongitude = Integer.MIN_VALUE;
	private int minLatitude = Integer.MAX_VALUE;
	private int maxLatitude = Integer.MIN_VALUE;
	
	
		
	public Persons( Vector<Person> persons) {
		this.persons= persons;
		for( Person p : persons) {
			if( p.getLongitude() < minLongitude) minLongitude = p.getLongitude();
			if( p.getLongitude() > maxLongitude) maxLongitude = p.getLongitude();
			if( p.getLatitude() < minLatitude) minLatitude = p.getLatitude();
			if( p.getLatitude() > maxLatitude) maxLatitude = p.getLatitude();
		}
	}

	public int getNumberPersons() {
		return this.persons.size();
	}
	
	public Person getByIndex( int index) {
		return persons.get(index);
	}
	public Person getPersonRandomly() {
		int i = Utils.randomGetInt( 0, persons.size()-1);
		return persons.get(i);
	}
	
	
	public Vector<Person> getAllPersons() {
		return persons;
	}

	public int getMinLongitude() {
		return minLongitude;
	}
	public int getMaxLongitude() {
		return maxLongitude;
	}
	public int getMinLatitude() {
		return minLatitude;
	}
	public int getMaxLatitude() {
		return maxLatitude;
	}





	
	

	

}
