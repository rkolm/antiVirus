package at.nsdb.nv;

import java.util.HashMap;
import java.util.Vector;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;


public class Neo4j {
	
	private int createdMeetings = 0;
	private int lookedMeetings = 0;
	private int nodesDone = 0;
	private HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* fieldnames of the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public static enum labelName { Person }
	public static enum relType { Meeting }
	public static enum fieldName { id,
		age, firstName,
		longitude, latitude, distance,
		dayOfInfection, incubationPeriod, illnessPeriod }
	

	
	/*.2.*/
	/*-----------------------------------------------------------------------------
	/*
	/* variables to manage the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	private Driver driver;
	/*.2.*/
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* constructor, connect to database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public Neo4j() {
		this.driver = GraphDatabase.driver( Config.getDbUri(), 
						AuthTokens.basic( Config.getDbUser(), Config.getDbPassword()));
		this.driver.verifyConnectivity();
		Utils.logging( "DB is connected");
		setConstraintsIndexes();		
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
		Utils.logging( "checking constraints & indexes");
		setConstraintsIndexes();
		
		
		// choose i.e. 5.000 persons from all nodes randomly
		// add the pesons with biometrics attributes and delete all rel. between persons
		int numbNodes = this.getNumbNodes( );
		try(Session session = driver.session()) {		
			session.writeTransaction(tx -> {
				Utils.logging( String.format("select %d nodes (:Persons) from %d nodes in neo4j", 
					Parameter.numPersonsSelected, numbNodes));
				
				// remove all labels :Person
				tx.run( Cypher.removeAllLabelsPerson());
				
				// choose i.e 5.000 nodes randomly and set Label :Person
				tx.run( Cypher.selectPersons( numbNodes));
				
				Utils.logging( "adding attributes e.g. incubation period to the neo4j-nodes (:Person)");		
				tx.run( Cypher.addBiometricsAttributes());
				
				Utils.logging( "delete all relations");		
				tx.run( Cypher.deleteAllRelationsBetweenPersons());		
				return 1;
			});
		}
		
		
		if( persons.size() == 0) persons = getAllPersons();
		Utils.logging( String.format( "creating meetings into database for %d persons",	persons.size()));
		
		
		// create new relations between persons
		Integer[] ids = persons.keySet().toArray( new Integer[persons.size()]);
		for( int id1 : persons.keySet()) {
			try( Session session = driver.session()) {
				session.readTransaction( tx -> {
					// every node (person) creates a number of relations (MEETINGs)
					int numbMeetings = Parameter.calculateRandomlyNumbMeetings();
					do {	
						int id2 = persons.get( ids[Utils.randomGetInt(1, persons.size())-1]).getId();
						lookedMeetings++;
						if( id1 != id2) {
							int distance = Person.distance( persons.get( id1), persons.get( id2));
							if( Parameter.meetingPossible( distance)) {
								String cypherQ = Cypher.createMeeting( id1, id2, distance);
								tx.run( cypherQ);
								createdMeetings++;
								numbMeetings--;
							}
						}
					} while( numbMeetings > 0);
					nodesDone++;
					
					if( Math.floorMod( nodesDone, Math.min( 2500, (int)(persons.size() / 10))) == 0) {
						Utils.logging( String.format( 
							"%7d/%d (%6.2f%%) persons, %7d meetings created, %7d lookings", 
							nodesDone, persons.size(), 100.0 * nodesDone / persons.size(), createdMeetings, lookedMeetings));
					}
					return 1;
				});
			}
		}
		
		// set content to biometric attributes
		Utils.logging( "set biometrics attributes into neo4j");
		try(Session session = driver.session()) {		
			session.writeTransaction( tx -> {		
				// set biometrics and upload to neo4j
				// how to iterate a HashMap easily?
				for( int id: persons.keySet()) {
					Person p = persons.get( id);
					p.setBiometrics();
					String cypherQ = Cypher.setBiometrics( id, p.getIncubationPeriod(), p.getIllnessPeriod());
					tx.run( cypherQ);
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
		
		if( persons.size() == 0) persons = getAllPersons();
		Utils.logging( "infect randomly 1 person");
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				Integer[] ids = persons.keySet().toArray( new Integer[persons.size()]);
						
				int id = persons.get( ids[Utils.randomGetInt(1, persons.size())-1]).getId();
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
			
				// a person can infect only, if he is in the incubation period
				// select all persons with friends, who can infect another person i.e. is in incubation period
				Result result = tx.run( Cypher.getAllPersonsWhoCanBeInfected( day));
				
				while( result.hasNext()) {
					Record row = result.next();
					
					if( Parameter.infected( row.get( "distance").asInt())) {
						tx.run( Cypher.infectAPerson( row.get( "id2").asInt(), day));
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
	// how many nodes in the database?
	private int getNumbNodes( ) {
		try (Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getCount( Cypher.numbNodes(), tx);
			});
		}
	}
	
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
	HashMap<Integer, Person> getAllPersons() {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAllPersons());
				return getPersonsFromResult( result);
			});
		}
	}

	
	// put nodes (perons) from Result- Record to hashmap
	// the result- record must include column "p"
	public HashMap<Integer, Person> getPersonsFromResult( Result result) {
		final HashMap<Integer, Person> persons = new HashMap<Integer, Person>();
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
		int numbNodes = this.getNumbNodes( );
		if( persons.size() == 0) persons = getAllPersons();
		
		try( Session session = driver.session()) {	
			session.readTransaction(tx -> {

				// print number of persons (nodes in the database)
				Utils.logging(String.format("%d nodes, %d (%d) persons found, for example:", 
					numbNodes, persons.size(), persons.size()));
		
				// print randomly 3 persons
				Integer[] ids = persons.keySet().toArray( new Integer[persons.size()]);
				for (int i = 1; i <= Math.min( 3, this.getNumbPersons( tx)); i++) {
					Utils.logging( persons.get( ids[Utils.randomGetInt(1, persons.size())-1]));
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
	private void setIndexForPerson( ) {				
		// index for attribute dayOfInfection and incubationPeriod
		for( String attributeName : new String[] {Neo4j.fieldName.dayOfInfection.toString(),
			Neo4j.fieldName.incubationPeriod.toString()}) {
				try (Session session = driver.session()) {
					session.run(Cypher.createIndex(attributeName));
					Utils.logging( "Index Person." + attributeName + " created");
				} catch(Exception e) {
					Utils.logging( "Index Person." + attributeName + " already exists");
				}
		}

		// index for label Person
		/*
        try ( Transaction tx = graphDb.beginTx() ) {
            Schema schema = tx.schema();
            schema.indexFor( Label.label( Neo4j.labelName.Person.toString()) )
                .withName( "idx" + Neo4j.labelName.Person.toString())
                .create();
            tx.commit();    
    		Utils.logging( "Index Person." + Neo4j.labelName.Person.toString() + " created");
    		
		} catch( Exception e) {
			Utils.logging( "Index Person." + Neo4j.labelName.Person.toString() + " already exists");
		}
		

        // index for attribute dayOfInfection and incubationPeriod
        //String attributeName = Neo4j.fieldName.dayOfInfection.toString();
        for( String attributeName : new String[] {Neo4j.fieldName.dayOfInfection.toString(),
        		Neo4j.fieldName.incubationPeriod.toString()}) {
            try ( Transaction tx = graphDb.beginTx() ) {
                Schema schema = tx.schema();
                schema.indexFor( Label.label( Neo4j.labelName.Person.toString()) )
                    .on( attributeName)
                    .withName( "idx" + attributeName)
                    .create();
                tx.commit();    
        		Utils.logging( "Index Person." + attributeName + " created");
        		
    		} catch( Exception e) {
    			Utils.logging( "Index Person." + attributeName + " already exists");
    		}
		}
		*/
	}
	


}
