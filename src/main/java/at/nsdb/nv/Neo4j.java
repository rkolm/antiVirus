package at.nsdb.nv;

import java.util.Vector;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

public class Neo4j {

	private int createdMeetings = 0;
	private int lookedMeetings = 0;
	private int nodesDone = 0;
	private Persons persons;

	/*-----------------------------------------------------------------------------
	/*
	/* fieldnames of the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public static enum labelName {
		Person
	}

	public static enum relType {
		Meeting
	}

	public static enum fieldName {
		id, age, firstName, longitude, latitude, distance, dayOfInfection, incubationPeriod, illnessPeriod
	}

	/*-----------------------------------------------------------------------------
	/*
	/* variables to manage the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	private Driver driver;

	/*-----------------------------------------------------------------------------
	/*
	/* constructor, connect to database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public Neo4j() {
		this.driver = GraphDatabase.driver(Config.getDbUri(),
				AuthTokens.basic(Config.getDbUser(), Config.getDbPassword()));
		this.driver.verifyConnectivity();
		setConstraintsIndexes();
		Utils.logging("DB" + Config.getDbUri() + "is connected");
	}

	/*-----------------------------------------------------------------------------
	/*
	/* initializing database, optional (see class Parameter), if neo4j structure is up to date
	/* 1. add biometric attributes to node 2. create relations with distance
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void initialize() {
		// define constraints and indexes
		Utils.logging("checking constraints & indexes");
		setConstraintsIndexes();

		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {

				tx.run(Cypher.removeAllRelationsMeeting());
				Utils.logging("all relations :Meeting deleted");

				tx.run(Cypher.removeBiometricsAttributesFromAllPersons());
				Utils.logging("all biomeric attributes deleted");

				tx.run(Cypher.addBiometricsAttributesToPersons());
				Utils.logging("biometric attributes e.g. incubation period to :person added");
				return 1;
			});
		}

		persons = new Persons(downloadAllPersons());
		// set content to biometric attributes
		Utils.logging("set biometrics attributes for all persons ...");
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				// set biometrics and upload to neo4j
				// how to iterate a HashMap easily?
				for (Person p : persons.getAllPersons()) {
					p.setBiometrics();
					//String cypherQ = Cypher.setBiometrics(p.getId(), p.getIncubationPeriod(), p.getIllnessPeriod());					
					tx.run("MATCH (n:" + Neo4j.labelName.Person + ") " +
						   "WHERE (n." + Neo4j.fieldName.id + "= $id) " +
							 "SET n." + Neo4j.fieldName.dayOfInfection + "= 0," +
							 	" n." + Neo4j.fieldName.incubationPeriod + "= $incubationPeriod," +
								" n." + Neo4j.fieldName.illnessPeriod + "= $illnessPeriod",
							parameters("id", p.getId(), 
									   "incubationPeriod", p.getIncubationPeriod(), 
									   "illnessPeriod", p.getIllnessPeriod()));							   
				}
				return 1;
			});
		}
		Utils.logging("biometrics attributes for all persons set");
		persons = new Persons(downloadAllPersons());
		
		
		// create relations via cypher, O(n)
//		Utils.logging(String.format("creating meetings into database for %d persons ...", persons.getNumberPersons()));
//		// create new relations between persons
//		try (Session session = driver.session()) {
//			session.writeTransaction(tx -> {
//				tx.run(
//					"MATCH (p:Person) " + 
//					"CALL { " + 
//					"	MATCH (q:Person) " + 
//					"	WHERE rand() < 0.2 " + 
//					"	RETURN q as q " + 
//					"} " + 
//					"WITH p, q, rand() as r, " +
//						"round( 1 + distance( " +
//							"point( {x:p.longitude, y:p.latitude, crs:'cartesian'}), " +
//							"point( {x:q.longitude, y:q.latitude, crs:'cartesian'}))) AS dist " + 
//					"WHERE r < 1000.0/dist/dist " + 
//					"CREATE (p)-[m:Meeting {distance:dist}]->(q)");
//				
//				Utils.logging( String.format( "%d relations created!",
//					this.getNumbMeetings(tx)));
//				return 1;
//			});	
//		}
		
		
		
		for (Person p : persons.getAllPersons()) {
			int id1 = p.getId();
			try (Session session = driver.session()) {
				session.readTransaction(tx -> {
					// every node (person) creates a number of relations (MEETINGs)
					int numbMeetings = Parameter.calculateRandomlyNumbMeetings();
					do {
						int id2 = persons.getPersonRandomly().getId();
						lookedMeetings++;
						if (id1 != id2) {
							int distance = 
								Math.max( 1, Person.distance(persons.getPersonById(id1), persons.getPersonById(id2)));
							if( Parameter.meetingPossible( distance)) {
								//String cypherQ = Cypher.createMeeting( id1, id2, distance);
								tx.run( "MATCH (p:" + Neo4j.labelName.Person + "), (q:" + Neo4j.labelName.Person + ") " +
						 				"WHERE (p." + Neo4j.fieldName.id + " = $id1 AND q." + Neo4j.fieldName.id + "= $id2)" +
										"CREATE (p)-[:" + Neo4j.relType.Meeting + " {" + Neo4j.fieldName.distance + ":$distance}]->(q)",
								 		parameters("id1", id1, "id2", id2, "distance", distance));
								createdMeetings++;
								numbMeetings--;
							}
						}
					} while( numbMeetings > 0);
					nodesDone++;
					
					if( Math.floorMod( nodesDone, Math.min( 2500, (int)(persons.getNumberPersons() / 10))) == 0) {
						Utils.logging( String.format( 
							"%7d/%d (%6.2f%%) persons, %7d meetings created, %7d lookings", 
							nodesDone, persons.getNumberPersons(), 100.0 * nodesDone / persons.getNumberPersons(),
								createdMeetings, lookedMeetings));
					}
					return 1;
				});
			}
		}
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* Day 1, infect randomly 1. person
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public StatisticADay day1( ) {
		StatisticADay statisticADay = new StatisticADay();
		
		if( ! Parameter.initialize) {
			Utils.logging( "checking constraints & indexes");
			setConstraintsIndexes();
			
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					tx.run( Cypher.setAllPersonsToHealthy());
					return 1;
				});
			}	
		}
		
		// delete all labels (except :Person) from node:Person
		this.deleteAllLabels();
		
		persons = new Persons( downloadAllPersons());
		Utils.logging( "infect randomly 1 or 2 persons");
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				// 1. person
				int id = persons.getPersonRandomly().getId();
				tx.run( Cypher.infectAPerson( id, 1));
				// 2. person
				id = persons.getPersonRandomly().getId();
				tx.run( Cypher.infectAPerson( id, 1));
				
				this.printStatusPersons( 1, tx);
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
				
				tx.run( Cypher.infectPersons( day));
				
				this.printStatusPersons( day, tx);
				statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( day, tx));
				statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation( day, tx));
				statisticADay.setNumbPersonsIll( this.getNumbPersonsIll( day, tx));
				statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune( day, tx));
				
				return 1;
			});
		}
		
		this.deleteAllLabels();
		this.setAllLabels( day);
		
		return statisticADay;
	}


	
	
	/*-----------------------------------------------------------------------------
	/*
	/* set/delete the labels of the nodes
	/* 
	/*-----------------------------------------------------------------------------
	 */
	private void deleteAllLabels() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				for( Person.status status : Person.status.values()) {
					String cypher = 
						"MATCH (p:" + Neo4j.labelName.Person + ") " +
						"REMOVE p:" + status.toString();
					tx.run( cypher);
					Utils.logging("all biometrics labels removed");
				}
				return 1;
			});
		}
	}
	private void setAllLabels( int day) {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				// set labels to healthy, inIncubation, ill, immune: depending on status of :Person
				tx.run( Cypher.setLabelHealthy( day));
				tx.run( Cypher.setLabelInIncubation( day));
				tx.run( Cypher.setLabelIll( day));
				tx.run( Cypher.setLabelImmune( day));
				return 1;
			});
		}
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
		return (int) (long) result.next().get( "count", Long.valueOf( 0));
	}
	
	

	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of persons
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// download all persons 
	private Vector<Person> downloadAllPersons() {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAllPersons());
				return Neo4j.getPersonsFromResult( result);
			});
		}
	}
	
	public Persons getAllPersons() {
		persons = new Persons( downloadAllPersons());
		return persons;
	}

	
	// put nodes (perons) from Result- Record to Vector
	// the result- record must include column "p"
	private static Vector<Person> getPersonsFromResult( Result result) {
		final Vector<Person> persons = new Vector<Person>();
		while( result.hasNext()) {
			Record row = result.next();
			Node node = row.get("p").asNode();
			Person person = new Person();
			int id= node.get( Neo4j.fieldName.id.toString()).asInt();
			person.setId( id);
			person.setFirstName( node.get( Neo4j.fieldName.firstName.toString()).asString());
			person.setAge( node.get( Neo4j.fieldName.age.toString()).asInt());
			person.setLongitude( node.get( Neo4j.fieldName.longitude.toString()).asInt());
			person.setLatitude( node.get( Neo4j.fieldName.latitude.toString()).asInt());
			person.setIllnessPeriod( node.get( Neo4j.fieldName.illnessPeriod.toString()).asInt());
			person.setDayOfInfection( node.get( Neo4j.fieldName.dayOfInfection.toString()).asInt());
			person.setIncubationPeriod( node.get( Neo4j.fieldName.incubationPeriod.toString()).asInt());
			persons.add( person);
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
		Result result = tx.run( cypherQ);
		meetings = getMeetingsFromResult( result);
		return meetings;
	}
	
	// put meetings from Result to Vector
	public Vector<Meeting> getMeetingsFromResult( Result result) {
		Vector<Meeting> meetings = new Vector<Meeting>();
		while( result.hasNext()) {
			Record record = result.next();
			int id1 = (record.get( "p").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			int id2 = (record.get( "q").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			Relationship c = record.get( "c").asRelationship();
			int distance = c.get( Neo4j.fieldName.distance.toString()).asInt();
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
		try( Session session = driver.session()) {	
			session.readTransaction(tx -> {

				// print number of persons (nodes in the database)
				Utils.logging(String.format( "%d (%d) persons found, for example:", 
					persons.getNumberPersons(), persons.getNumberPersons()));
		
				// print randomly 3 persons
				for (int i = 1; i <= Math.min( 3, this.getNumbPersons( tx)); i++) {
					Utils.logging( persons.getPersonRandomly());
				}
		
				// print number of meetings (relations in the database)
				Vector<Meeting> meetings = new Vector<Meeting>();
				meetings = this.getAllMeetings( tx);
				Utils.logging( String.format("%d (%d) relations found, for example:",
					meetings.size(), this.getNumbMeetings( tx)));
				
				// print randomly 3 meetings
				for (int i = 1; i <= Math.min( 3, meetings.size()); i++) {
					Utils.logging( meetings.elementAt(Utils.randomGetInt( 0, meetings.size() - 1)));
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
		
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* constraints and indexed
	/* 
	/*-----------------------------------------------------------------------------
	 */
	private void setConstraintsIndexes() {
		setConstraint();
		setIndexForPerson( );
	}
	private void setConstraint() {
		String cypherQ = Cypher.createConstraint();
		
		try(Session session = driver.session()) {
			session.run( cypherQ);			
			Utils.logging( "Constraint :Person( id) created");
		} catch( Exception e) {
			Utils.logging( "Constraint :Person( id) already exists");
		}
	}	
	private void setIndexForPerson( ) {		// index for attribute dayOfInfection and incubationPeriod
		for( String attributeName : new String[] {Neo4j.fieldName.dayOfInfection.toString(),
			Neo4j.fieldName.incubationPeriod.toString()}) {
				try (Session session = driver.session()) {
					session.run(Cypher.createIndex(attributeName));
					Utils.logging( "Index Person." + attributeName + " created");
				} catch(Exception e) {
					Utils.logging( "Index Person." + attributeName + " already exists");
				}
		}
	}	
}
