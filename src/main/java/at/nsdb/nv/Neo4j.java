package at.nsdb.nv;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;

public class Neo4j {
	
	/*-----------------------------------------------------------------------------
	/*
	/* fieldnames of the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public static enum labelName { Person }
	public static enum relType { meeting }
	public static enum fieldName { id,
		age, firstName,
		longitude, latitude, distance,
		dayOfInfection, incubationPeriod, illnessPeriod }
	

	
	
	/*-----------------------------------------------------------------------------
	/*
	/* variables to manage the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	private File databaseDirectory;
	private DatabaseManagementService managementService;
	private GraphDatabaseService graphDb;



	
	/*-----------------------------------------------------------------------------
	/*
	/* constructor, create database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public Neo4j( File databaseDirectory, boolean createNewDB) {
		super();
		this.databaseDirectory = databaseDirectory;
		try {
			create( createNewDB);
		} catch ( IOException e) {
			e.printStackTrace();
		}
	}

	private void create( boolean createNewDB) throws IOException {
		if( createNewDB) {
			FileUtils.deleteRecursively( databaseDirectory);
		}

		managementService = new DatabaseManagementServiceBuilder(databaseDirectory).build();
		graphDb = managementService.database(DEFAULT_DATABASE_NAME);
		registerShutdownHook(managementService);
		
		if( createNewDB) {
			setConstraint();
		}
	}
	
	private void setConstraint() {
		String cypherQ = Cypher.createConstraint();
		
		try( Transaction tx = graphDb.beginTx()) {
			tx.execute( cypherQ);			
			tx.commit();
			Utils.logging( "Constraint :Person( id) created");
		} catch( Exception e) {
			Utils.logging( "Constraint :Person( id) already exists");
		}
		
		// another possibility for constraint
//		creating constraint and index
//		try (Transaction tx = graphDb.beginTx()) {
//			tx.schema()
//				.constraintFor( Label.label( "Person"))
//				.assertPropertyIsUnique( "lastName")
//				.create();
//			tx.commit();
//		}
	}


	
	
	/*-----------------------------------------------------------------------------
	/*
	/* initializing database, optional (see class Parameter), if neo4j structure is up to date
	/* 1. add biometric attributes to node 2. create relations with distance
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void initMeetings() {		
		final int maxNodes = 1000*1000*1000;
		int createdMeetings = 0;
		int lookedMeetings = 0;
		int numNodes;
		HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
		
		setConstraint();
		
		try( Transaction tx = graphDb.beginTx()) {	
			// add biometrics attributes to the nodes
			String cypherQ = Cypher.addBiometricsAttributes();
			Utils.logging( "adding attributes e.g. incubation period to the neo4j-nodes (:Person)");
			tx.execute( cypherQ);
			tx.commit();
		}
		
		try( Transaction tx = graphDb.beginTx()) {	
			// download all nodes
			persons = this.getAllPersons( tx);
			
			// delete all relations
			String cypherQ = Cypher.deleteAllRelations();
			tx.execute( cypherQ);		
			
			numNodes = Math.min( maxNodes, this.getNumbPersons( tx));
			Utils.logging( String.format( "creating meetings into database for %d persons",	numNodes));
			
			tx.commit();
		}
		
			
		for( int id1=1; id1<=numNodes; id1++) {
			
			try( Transaction tx = graphDb.beginTx()) {	
				// every node (person) creates a number of relations (MEETINGs)
				int numMeetings = Parameter.calculateRandomlyNumbMeetings();
				do {	
					int id2 = Utils.randomGetInt(1, numNodes);
					lookedMeetings++;
					if( id1 != id2) {
						int distance = Person.distance( persons.get( id1), persons.get( id2));
						if( Parameter.meetingPossible( distance)) {
							String cypherQ = Cypher.createMeeting( id1, id2, distance);
							tx.execute( cypherQ);
							createdMeetings++;
							numMeetings--;
						}
					}
				} while( numMeetings > 0);
				
				if( Math.floorMod( id1, Math.min( 2500, (int)(numNodes / 10))) == 0) {
					Utils.logging( String.format( 
						"%7d/%d (%6.2f%%) persons, %7d meetings created, %7d lookings", 
						id1, numNodes, 100.0 * id1 / numNodes, createdMeetings, lookedMeetings));
				}
				tx.commit();
			}
		}
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* do day0, optional (see class Parameter) if biometrics attributes already set
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void day0() {
		Utils.logging( "set biometrics into neo4j");
		
		try( Transaction tx = graphDb.beginTx()) {
			
			// download all nodes
			HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
			persons = getAllPersons( tx);
			
			
			// set biometrics and upload to neo4j
			// how to iterate a HashMap easily?
			for( int id: persons.keySet()) {
				Person p = persons.get( id);
				p.setBiometrics();
				String cypherQ = Cypher.setBiometrics( id, p.getIncubationPeriod(), p.getIllnessPeriod());
				tx.execute( cypherQ);
			}
			tx.commit();
		}
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* Day 1, infect randomly 1. person
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public StatisticADay day1( ) {
		// if day0 not executed set all dayOfInfection to 0
		StatisticADay statisticADay = new StatisticADay();
		
		if( ! Parameter.day0) {
			String cypherQ = Cypher.setAllPersonsToHealthy();
			try (Transaction tx = graphDb.beginTx()) {
				tx.execute( cypherQ);
				tx.commit();
			}
		}
		
		Utils.logging( "infect randomly 1 person");
		try (Transaction tx = graphDb.beginTx()) {
			int id = Utils.randomGetInt(1, this.getNumbPersons( tx));
			String cypherQ = Cypher.infectAPerson( id, 1);
			tx.execute( cypherQ);
			this.printStatusPersons( 1, tx);
			
			statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( 1, tx));
			statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation( 1, tx));
			statisticADay.setNumbPersonsIll( this.getNumbPersonsIll( 1, tx));
			statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune( 1, tx));
			
			tx.commit();
		}
		return statisticADay;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* calculate the spreading of the virus on day day
	 * and returns the number of persons, who are in incubation period
	 */
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public StatisticADay day( int day) {
		StatisticADay statisticADay = new StatisticADay();
		try (Transaction tx = graphDb.beginTx()) {
			
			// a person can infect only, if he is in the incubation period
			// select all persons, who can infect another person i.e. is in incubation period
			String cypherQ = Cypher.getAllPersonsInIncubation( day);
			Result result = tx.execute( cypherQ);
			
			while( result.hasNext()) {
				Map<String, Object> row = result.next();
				Node node = (Node) row.get("p");
				int id1 = ((int)(long) node.getProperty( Neo4j.fieldName.id.toString()));
				
				Vector<Meeting> meetings = new Vector<Meeting>();
				meetings = getMeetingsFromOneToHealthy( id1, tx);
				for( Meeting meeting : meetings) {
					if( Parameter.infected( meeting.getDistance())) {
						cypherQ = Cypher.infectAPerson( meeting.getId2(), day);
						tx.execute( cypherQ);
					}
				}
			}
			
			this.printStatusPersons( day, tx);
			statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( day, tx));
			statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation( day, tx));
			statisticADay.setNumbPersonsIll( this.getNumbPersonsIll( day, tx));
			statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune( day, tx));
			
			tx.commit();
		}
		return statisticADay;
	}


	
	
	/*-----------------------------------------------------------------------------
	/*
	/* get the numbers of records of different questions
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// how many persons (nodes) in the database?
	private int getNumbPersons( Transaction tx) {
		String cypherQ = Cypher.numbPersons();
		return getCount( cypherQ, tx);
	}
	
	// how may persons inIncubation period?
	private int getNumbPersonsHealthy( int day, Transaction tx) {
		String cypherQ = Cypher.numbPersonsHealthy( day);
		return getCount( cypherQ, tx);
	}
	
	// how may persons inIncubation period?
	private int getNumbPersonsInIncubation( int day, Transaction tx) {
		String cypherQ = Cypher.numbPersonsInIncubation( day);
		return getCount( cypherQ, tx);
	}
	
	// how may persons are ill
	private int getNumbPersonsIll( int day, Transaction tx) {		
		String cypherQ = Cypher.numbPersonsIll( day);
		return getCount( cypherQ, tx);
	}
	
	// how may persons are immune
	private int getNumbPersonsImmune( int day, Transaction tx) {		
		String cypherQ = Cypher.numbPersonsImmune( day);
		return getCount( cypherQ, tx);
	}
	
	// how may meetings in the db
	private int getNumbMeetings( Transaction tx) {		
		String cypherQ = Cypher.numbMeetings();
		return getCount( cypherQ, tx);
	}
	
	private int getCount( String cypherQ, Transaction tx) {
		int numbPersons = 0;
		Result result = tx.execute( cypherQ);
		numbPersons = (int) (long) result.columnAs( "count").next();
		return (int) numbPersons;
	}
	
	

	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of persons
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// download all persons 
	public HashMap<Integer, Person> getAllPersons() {
		HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
		
		try (Transaction tx = graphDb.beginTx()) {
			persons = this.getAllPersons( tx);
		}
		return persons;
	}
	public HashMap<Integer, Person> getAllPersons( Transaction tx) {
		HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
		String cypherQ = Cypher.getAllPersons();
		Result result = tx.execute( cypherQ);
		persons = getPersonsFromResult( result);
		return persons;
	}
	
	// download 1 Person by id
	public Person getAPerson( int id, Transaction tx) {
		String cypherQ = Cypher.getAPerson( id);
		Result result = tx.execute( cypherQ);
		return getPersonsFromResult( result).get( id);
	}
	
	// put nodes (perons) from Result- Record to hashmap
	// the result- record must include column "p"
	public HashMap<Integer, Person> getPersonsFromResult( Result result) {
		HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
		while( result.hasNext()) {
			Map<String, Object> row = result.next();
			Node node = (Node) row.get("p");
			Person person = new Person();
			int id= ((int)(long) node.getProperty( Neo4j.fieldName.id.toString()));
			person.setId( id);
			person.setFirstName( ((String) node.getProperty( Neo4j.fieldName.firstName.toString())));
			person.setAge( ((int)(long) node.getProperty( Neo4j.fieldName.age.toString())));
			person.setLongitude( ((int)(long) node.getProperty( Neo4j.fieldName.longitude.toString())));
			person.setLatitude( ((int)(long) node.getProperty( Neo4j.fieldName.latitude.toString())));
			person.setIllnessPeriod( ((int)(long) node.getProperty( Neo4j.fieldName.illnessPeriod.toString())));
			person.setDayOfInfection( ((int)(long) node.getProperty( Neo4j.fieldName.dayOfInfection.toString())));
			person.setIncubationPeriod( ((int)(long) node.getProperty( Neo4j.fieldName.incubationPeriod.toString())));
			persons.put( id, person);
		}
		return persons;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of meetings
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// download all meetings
	public Vector<Meeting> getAllMeetings( Transaction tx) {
		Vector<Meeting> meetings = new Vector<Meeting>();
		String cypherQ = Cypher.getAllMeetings();
		Result result = tx.execute( cypherQ);
		meetings = getMeetingsFromResult( result);
		return meetings;
	}
	
	// download all meetings from 1 person to all connected healthy persons
	public Vector<Meeting> getMeetingsFromOneToHealthy( int id, Transaction tx) {
		String cypherQ = Cypher.getAllMeetingsFromAPersonToHealthy( id);
		Result result = tx.execute( cypherQ);
		return getMeetingsFromResult( result);
	}
	
	// put meetings from Result to Vector
	public Vector<Meeting> getMeetingsFromResult( Result result) {
		Vector<Meeting> meetings = new Vector<Meeting>();
		while( result.hasNext()) {
			Map<String, Object> row = result.next();
			int id1 = (int)(long) ((Node) row.get( "p")).getProperty( Neo4j.fieldName.id.toString());
			int id2 = (int)(long) ((Node) row.get( "q")).getProperty( Neo4j.fieldName.id.toString());
			int distance = (int)(long) (((Entity) row.get( "c")).getProperty( Neo4j.fieldName.distance.toString()));
			meetings.add(new Meeting(id1, id2, distance));
		}
		return meetings;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* print the content of the database
	/* 1. number of nodes with 3 examples, randomly
	/* 2. number of possible meetings with 3 examples
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void printNeo4jContent() {
		try( Transaction tx = graphDb.beginTx()) {	
			// print number of persons (nodes in the database)
			HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
			persons = this.getAllPersons( tx);
			Utils.logging(String.format("%d (%d) nodes found, for example:", 
				persons.size(), this.getNumbPersons( tx)));
	
			// print randomly 3 persons
			for (int i = 1; i <= Math.min( 3, this.getNumbPersons( tx)); i++) {
				Utils.logging(persons.get( Utils.randomGetInt(1, persons.size() - 1)));
			}
	
			// print number of meetings (relations in the database)
			Vector<Meeting> meetings = new Vector<Meeting>();
			meetings = this.getAllMeetings( tx);
			Utils.logging( String.format("%d (%d) relations found, for example:",
				meetings.size(), this.getNumbMeetings( tx)));
			
		
			// print randomly 3 meetings
			for (int i = 1; i <= Math.min( 3, meetings.size()); i++) {
				Utils.logging( meetings.elementAt(Utils.randomGetInt(1, meetings.size() - 1)));
			}
			tx.commit();
		}
	}

	
	private void printStatusPersons( int day, Transaction tx) {
		Utils.logging( String.format( 
			"day %3d, healthy=, %6d, incubation=, %6d, ill=, %6d, immune=, %6d",
				day, this.getNumbPersonsHealthy( day, tx), this.getNumbPersonsInIncubation( day, tx),
				this.getNumbPersonsIll( day, tx), this.getNumbPersonsImmune( day, tx)));
	}
	
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* shutdown database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void shutDown() {
		// tag::shutdownServer[]
		managementService.shutdown();
		// end::shutdownServer[]
	}

	private static void registerShutdownHook(final DatabaseManagementService managementService) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				managementService.shutdown();
			}
		});
	}
}
