package at.nsdb.nv;

import java.util.HashMap;
import java.util.Vector;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

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
	private Driver driver;


	
	/*-----------------------------------------------------------------------------
	/*
	/* constructor, connect database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public Neo4j(boolean createNewDB) {
		this.driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "nsdb"));
		this.driver.verifyConnectivity(); 
		create(createNewDB);
		Utils.logging( "DB is connected");
	}

	private void create(boolean createNewDB) {
		if (createNewDB) {
			setConstraint();
		}
	}
	
	private void setConstraint() {
		String cypherQ = Cypher.createConstraint();

		try(Session session = driver.session()) {
			session.run(cypherQ);
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
		
		setConstraint();
		
		// add biometrics attributes to the nodes and delete all relations		
		try(Session session = driver.session()) {		
			session.writeTransaction(tx -> {
				Utils.logging( "adding attributes e.g. incubation period to the neo4j-nodes (:Person)");		
				tx.run(Cypher.addBiometricsAttributes());
				Utils.logging( "delete all relations");		
				tx.run( Cypher.deleteAllRelations());		
				return 1;
			});
		}

		final var persons = getAllPersonsMap();
		final int numNodes = Math.min( maxNodes, persons.size());
		Utils.logging( String.format( "creating meetings into database for %d persons",	numNodes));		
					
		for( int i=1; i<=numNodes; i++) {
			
			final int id1 = i;
			try(Session session = driver.session()) {	
				session.readTransaction(tx -> {
				int createdMeetings = 0;
				int lookedMeetings = 0;
				// every node (person) creates a number of relations (MEETINGs)
				int numMeetings = Parameter.calculateRandomlyNumbMeetings();
				do {	
					int id2 = Utils.randomGetInt(1, numNodes);
					lookedMeetings++;
					if( id1 != id2) {
						int distance = Person.distance( persons.get( id1), persons.get( id2));
						if( Parameter.meetingPossible( distance)) {
							String cypherQ = Cypher.createMeeting( id1, id2, distance);
							tx.run( cypherQ);
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
				return 1;
				});
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

		final var persons = getAllPersonsMap();
		
		try( Session session = driver.session()) {
			session.writeTransaction(tx -> {
				// set biometrics and upload to neo4j
				// how to iterate a HashMap easily?
				for( int id: persons.keySet()) {
					Person p = persons.get( id);
					p.setBiometrics();
					tx.run( Cypher.setBiometrics( id, p.getIncubationPeriod(), p.getIllnessPeriod()));
				}
				return 1;
			});
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
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					tx.run(cypherQ);
					return 1;
				});
			}
		}
	
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				int id = Utils.randomGetInt(1, this.getNumbPersons( tx));
				String cypherQ = Cypher.infectAPerson( id, 1);
				tx.run( cypherQ);

				statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( 1, tx));
				statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation( 1, tx));
				statisticADay.setNumbPersonsIll( this.getNumbPersonsIll( 1, tx));
				statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune( 1, tx));
				return 1;
			});
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
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {

				// a person can infect only, if he is in the incubation period
				// select all persons, who can infect another person i.e. is in incubation period
				Result result = tx.run( Cypher.getAllPersonsInIncubation( day));
				
				while( result.hasNext()) {
					Value node = result.next().get("p");
					int id1 = node.get(Neo4j.fieldName.id.toString()).asInt();
					
					Vector<Meeting> meetings = new Vector<Meeting>();
					meetings = getMeetingsFromOneToHealthy( id1, tx);
					for( Meeting meeting : meetings) {
						if( Parameter.infected( meeting.getDistance())) {
							tx.run( Cypher.infectAPerson( meeting.getId2(), day));
						}
					}
				}
				
				this.printStatusPersons( day, tx);
				statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( day, tx));
				statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation( day, tx));
				statisticADay.setNumbPersonsIll( this.getNumbPersonsIll( day, tx));
				statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune( day, tx));

				
				return 1;				
			});			
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
		Result result = tx.run( cypherQ);
		final int numbPersons = (int)(long) result.next().get( "count", Long.valueOf(0));
		return (int) numbPersons;
	}
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of persons
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// download all persons 
	HashMap<Integer, Person> getAllPersonsMap() {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAllPersons());
				return getPersonsFromResult( result);
			});
		}
	}	
	// download 1 Person by id
	/* not needed?
	private Person getAPerson(int id) {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAPerson( id));
				return getPersonsFromResult( result).get( id);
			});
		}
	}
	*/	
	// put nodes (perons) from Result- Record to hashmap
	// the result- record must include column "p"
	public HashMap<Integer, Person> getPersonsFromResult( Result result) {
		HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
		while( result.hasNext()) {
			Record row = result.next();
			Node node = row.get("p").asNode();
			Person person = new Person();
			int id= node.get(Neo4j.fieldName.id.toString()).asInt();
			person.setId( id);
			person.setFirstName( node.get( Neo4j.fieldName.firstName.toString()).asString());
			person.setAge( node.get( Neo4j.fieldName.age.toString()).asInt());
			person.setLongitude( node.get( Neo4j.fieldName.longitude.toString()).asInt());
			person.setLatitude( node.get( Neo4j.fieldName.latitude.toString()).asInt());
			person.setIllnessPeriod( node.get( Neo4j.fieldName.illnessPeriod.toString()).asInt());
			person.setDayOfInfection( node.get( Neo4j.fieldName.dayOfInfection.toString()).asInt());
			person.setIncubationPeriod( node.get( Neo4j.fieldName.incubationPeriod.toString()).asInt());
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
		Result result = tx.run(Cypher.getAllMeetings());
		meetings = getMeetingsFromResult( result);
		return meetings;
	}
	
	// download all meetings from 1 person to all connected healthy persons
	public Vector<Meeting> getMeetingsFromOneToHealthy( int id, Transaction tx) {
		Result result = tx.run( Cypher.getAllMeetingsFromAPersonToHealthy( id));
		return getMeetingsFromResult( result);
	}
	
	// put meetings from Result to Vector
	public Vector<Meeting> getMeetingsFromResult( Result result) {
		Vector<Meeting> meetings = new Vector<Meeting>();
		while( result.hasNext()) {
			Record record = result.next();
			int id1 = ((Node) record.get( "p").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			int id2 = ((Node) record.get( "q").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			Relationship c = record.get("c").asRelationship();
			int distance = c.get(Neo4j.fieldName.distance.toString()).asInt();
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
		// print number of persons (nodes in the database)
		final var persons = this.getAllPersonsMap();
		
		try( Session session = driver.session()) {	
			session.readTransaction(tx -> {

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

				return 1;
			});
		}
	}

	
	private void printStatusPersons( int day, Transaction tx) {
		Utils.logging( String.format( 
			"day %3d, healthy=, %6d, incubation=, %6d, ill=, %6d, immune=, %6d",
				day, this.getNumbPersonsHealthy( day, tx), this.getNumbPersonsInIncubation( day, tx),
				this.getNumbPersonsIll( day, tx), this.getNumbPersonsImmune( day, tx)));
	}
	
}
