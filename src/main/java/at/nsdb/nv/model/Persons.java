package at.nsdb.nv.model;

import java.util.Vector;

import at.nsdb.nv.utils.Utils;

/**
 * collection of persons with statistical information
 */
public class Persons {
	private Vector<Person> persons;
	private double minLongitude = 1000.0;
	private double maxLongitude = -1000.0;
	private double minLatitude = 1000.0;
	private double maxLatitude = -1000.0;
	
		
	public Persons( Vector<Person> persons) {
		this.persons= persons;
		for( Person p : persons) {
			minLongitude = Math.min( minLongitude, p.getLongitude());
			maxLongitude = Math.max( maxLongitude, p.getLongitude());
			minLatitude = Math.min( minLatitude, p.getLatitude());
			maxLatitude = Math.max( maxLatitude, p.getLatitude());
		}
	}

	public int getNumberPersons() {
		return this.persons.size();
	}
	
	public Person getByIndex( int index) {
		return persons.get(index);
	}
	public Person getPersonRandomly() {
		int id = Utils.randomGetInt( 0, persons.size()-1);
		return persons.get( id);
	}
	
	
	public Vector<Person> getAllPersons() {
		return persons;
	}

	public double getMinLongitude() {
		return minLongitude;
	}
	public double getMaxLongitude() {
		return maxLongitude;
	}
	public double getLongitudeRange() {
		return maxLongitude - minLongitude;
	}
	
	public double getMinLatitude() {
		return minLatitude;
	}
	public double getMaxLatitude() {
		return maxLatitude;
	}
	public double getLatitudeRange() {
		return maxLatitude - minLatitude;
	}





	
	

	

}
