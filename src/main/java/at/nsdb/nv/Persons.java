package at.nsdb.nv;

import java.util.Vector;

public class Persons {
	private Vector<Person> persons;
	private int minLongitude = Integer.MAX_VALUE;
	private int maxLongitude = Integer.MIN_VALUE;
	private int minLatitude = Integer.MAX_VALUE;
	private int maxLatitude = Integer.MIN_VALUE;
	private int minId = Integer.MAX_VALUE;
		
	public Persons( Vector<Person> persons) {
		this.persons= persons;
		for( Person p : persons) {
			if( p.getLongitude() < minLongitude) minLongitude = p.getLongitude();
			if( p.getLongitude() > maxLongitude) maxLongitude = p.getLongitude();
			if( p.getLatitude() < minLatitude) minLatitude = p.getLatitude();
			if( p.getLatitude() > maxLatitude) maxLatitude = p.getLatitude();
			if( p.getId() < minId) minId = p.getId();
		}
	}

	public int getNumberPersons() {
		return this.persons.size();
	}
	
	public Person getPersonById( int id) {
		return persons.get( id - minId);
	}
	public Person getPersonRandomly() {
		return persons.get( Utils.randomGetInt( 0, persons.size() - minId));
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

	public int getMinId() {
		return minId;
	}





	
	

	

}
