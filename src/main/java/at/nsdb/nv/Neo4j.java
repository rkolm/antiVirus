package at.nsdb.nv;

import static org.neo4j.driver.Values.parameters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import at.nsdb.nv.model.CanInfect;
import at.nsdb.nv.model.Person;
import at.nsdb.nv.model.Persons;
import at.nsdb.nv.model.StatisticADay;
import at.nsdb.nv.utils.Cypher;
import at.nsdb.nv.utils.Utils;

public class Neo4j {

	private int nodesDone = 0;
	private int lookings = 0;

	/*-----------------------------------------------------------------------------
	/*
	/* fieldnames of the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// node-labels
	public static enum labelName {
		Person
	}
	// variable node-labels
	public static enum labelName2nd {
		Healthy, InIncubation, Ill, Immune
	}

	// relation-types
	public static enum relType {
		CanInfect
	}

	// variable relation-types
	public static enum relTypeVar {
		HasInfected
	}
	
	// attributes of relations
	public static enum relAttribute {
		distance, day
	}

	// attributes of nodes
	public static enum fieldName {
		id, age, firstName, longitude, latitude, dayOfInfection, incubationPeriod, illnessPeriod
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
		Utils.logging("DB" + Config.getDbUri() + " is connected");
		Utils.logging("checking constraints & indexes");
		setConstraint();
		setIndexForPerson( );
	}

	/*-----------------------------------------------------------------------------
	/*
	/* constraints and indexed
	/* 
	/*-----------------------------------------------------------------------------
	 */
	/** create missing db-constraints - neo4j creates an index for an constraint  */
	private void setConstraint() {
		try(Session session = driver.session()) {
			session.run( Cypher.createConstraint());			
		} catch(ClientException e) { // neo4j driver does not raise the exception until the session is closed! 
			Utils.logging( "Constraint :Person(id) already exists");
			return;
		}
		Utils.logging( "Constraint :Person(id) created");
	}	
	/** create missing db-indices for the person node */
	private void setIndexForPerson( ) {		// index for attribute dayOfInfection and incubationPeriod
		for( String attributeName : new String[] {Neo4j.fieldName.dayOfInfection.toString(),
			Neo4j.fieldName.incubationPeriod.toString()}) {
				try (Session session = driver.session()) {
					session.run( Cypher.createIndex( attributeName));
				} catch(ClientException e) {
					Utils.logging( "Index Person." + attributeName + " already exists");
					continue;
				}
				Utils.logging( "Index Person." + attributeName + " created");
		}
	}	

	/**
	 * set biometric attributes for all person-nodes
	 */
	void setBiometricsForAllPersons() {

		int numbPersons = this.getNumbPersons();
		int withDayOfInfection = getNumbPersonsWithAttribute( Neo4j.fieldName.dayOfInfection.toString());
		int withIncubationPeriod = getNumbPersonsWithAttribute( Neo4j.fieldName.incubationPeriod.toString());
		int withIllnessPeriod = getNumbPersonsWithAttribute( Neo4j.fieldName.illnessPeriod.toString());
		if( (numbPersons == withDayOfInfection) && (withIncubationPeriod == withIllnessPeriod) &&
			(numbPersons == withIllnessPeriod)) {
			Utils.logging( "biometrics already exist");
		} else {
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					if( withDayOfInfection > 0 || withIncubationPeriod > 0 || withIllnessPeriod > 0) {
						tx.run(Cypher.removeBiometricAttributesFromAllPersons());
						Utils.logging("all biometric attributes deleted");
					}
	
					tx.run(Cypher.addBiometricAttributesToAllPersons());
					Utils.logging("biometric attributes e.g. incubation period to :person added");
					
					return 1;
				});
			}
	
			var persons = new Persons( readAllPersons());
			// set content to biometric attributes
			Utils.logging("set biometrics contents to all persons ...");
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
		}
	}

	/**
	 * set missing relations 'canInfect' for all person-nodes
	 */
	void setCanInfectRelationsForAllPersons() {
		if( this.getNumbCanInfect() != 0) {
			Utils.logging( "relations :CanInfect already exist");
		} else {
			Utils.logging( "relations :CanInfect creating ...");
			
			
			final var relations = new HashSet<String>();
			final var persons = new Persons( readAllPersons());

			for( Person p : persons.getAllPersons()) {
				int id1 = p.getId();
				try( Session session = driver.session()) {
					session.writeTransaction(tx -> {
						// every node (person) creates a number of relations :CanInfect
						int numbCanInfect = InfectionCalculator.calculateRandomlyNumbCanInfect();
						do {
							int id2 = persons.getPersonRandomly().getId();
							lookings++;
							if( (id1 != id2) && (! relations.contains( id1 + "-" + id2))) {
								int distance = 
									Math.max( 1, Person.distance( persons.getPersonById(id1), persons.getPersonById(id2)));
								if( InfectionCalculator.canInfect( distance)) {
									tx.run( 
										"MATCH (p:" + Neo4j.labelName.Person + "), (q:" + Neo4j.labelName.Person + ") " +
						 				"WHERE (p." + Neo4j.fieldName.id + " = $id1 AND q." + Neo4j.fieldName.id + "= $id2)" +
										"CREATE (p)-[:" + Neo4j.relType.CanInfect + " {" + Neo4j.relAttribute.distance + ":$distance}]->(q)",
								 		parameters("id1", id1, "id2", id2, "distance", distance));
									numbCanInfect--;
									relations.add( id1 + "-" + id2);
								}
							}
						} while( numbCanInfect > 0);
						nodesDone++;
						
						if( Math.floorMod( nodesDone, Math.min( 2500, (int)(persons.getNumberPersons() / 10))) == 0) {
							Utils.logging( String.format( 
								"%7d/%d, (%6.2f%%) persons, %7d CanInfect created, %7.1f Mio lookings", 
								nodesDone, persons.getNumberPersons(), 100.0 * nodesDone / persons.getNumberPersons(),
								relations.size(), lookings/1000000.0));
						}
						return 1;
					});
				}
			}
			
			//
			// canInfect- Relations export to csv
			//
			Utils.logging( "exporting :CanInfect ...");
			String fileName = Config.canInfectFileFullFileName();
		    if( fileName != "") {
		        try {   
		            // Open given file in append mode. 
		            BufferedWriter out = new BufferedWriter( new FileWriter(fileName)); 
		            out.write( CanInfect.toExportFileHeader()); out.newLine();
		            for( CanInfect m : this.getAllCanInfects()) {
		            	out.write( m.toExportFile()); out.newLine();
		            }
		            out.close(); 
		        } 
		        catch (IOException e) { 
		            System.out.println(" Error in writing export CanInfect.csv " + fileName + " " + e); 
		        } 
			}	
		    Utils.logging( "exporting :CanInfect finished");
		}
		
	}
	
	
	
	/**
	 * calculate the spreading of the virus for the given day and set statistics
	 */
	public void day( int day, HashMap<Integer, StatisticADay> statistics) {
		StatisticADay statisticADay = new StatisticADay();
		
		if( day == 1) {
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					tx.run( Cypher.setAllPersonsToHealthy());
					tx.run( Cypher.removeAllRelations( relTypeVar.HasInfected));
					return 1;
				});
			}	
			
			var persons = new Persons( readAllPersons());
			Utils.logging( "infect randomly 1 or 2 persons");
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					this.deleteAllLabels2nd( tx);
					
					// 1. person
					int id = persons.getPersonRandomly().getId();
					var params = new HashMap<String, Object>(Map.of("id", id, "day", day));
					tx.run(Cypher.infectAPerson(), params);
					Utils.logging( "id " + id + " infected on day 1");

					// 2. person
					id = persons.getPersonRandomly().getId();
					tx.run(Cypher.infectAPerson(), params);
					Utils.logging( "id " + id + " infected on day 1");
					return 1;
				});
			}
			
		}
		else {
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					double quote = Math.max( Math.min( 1.0, 1-statistics.get( day-1).getQ()), 0.5);
					var params = new HashMap<String,Object>();
					params.put( "day", day );
					params.put( "quote", quote );
					tx.run( Cypher.infectPersons(), params);	
					return 1;
				});
			}
		}
		
		
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {		
				this.deleteAllLabels2nd( tx);
				this.setAllLabels2nd( day, tx);

				statisticADay.setNumbPersonsHealthy( this.getNumbPersonsHealthy( day, tx));
				statisticADay.setNumbPersonsInIncubation( this.getNumbPersonsInIncubation(tx));
				statisticADay.setNumbPersonsIll( this.getNumbPersonsIll(tx));
				statisticADay.setNumbPersonsImmune( this.getNumbPersonsImmune(tx));
				return 1;
			});
		}
		
		statistics.put( day, statisticADay);	
	}


	
	
	 /** 
	  * delete all variable labels of the nodes
	  * @param tx Transaction
	  */
	private void deleteAllLabels2nd( Transaction tx) {
		for( labelName2nd n : labelName2nd.values()) {
			String cypher = 
				"MATCH (p:" + Neo4j.labelName.Person + ") " +
				"REMOVE p:" + n.toString();
			tx.run( cypher);
		}

	}
	/**
	 * set node-labels for a given day
	 * 
	 * @param day Day
	 * @param tx Transaction
	 */
	private void setAllLabels2nd( int day, Transaction tx) {
		// set labels to healthy, inIncubation, ill, immune: depending on status of :Person
		tx.run( Cypher.setLabelHealthy( day));
		tx.run( Cypher.setLabelInIncubation( day));
		tx.run( Cypher.setLabelIll( day));
		tx.run( Cypher.setLabelImmune( day));
	}
	
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* get the numbers of records of different questions
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// how many :Persons (nodes) in the database?
	private int getNumbPersons( Transaction tx) {
		return getCount( Cypher.numbPersons(), tx);
	}
	private int getNumbPersons() {	
		try( Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getNumbPersons( tx);
			});
		}
	}
	
	// how many :Persons healthy?
	private int getNumbPersonsHealthy( int day, Transaction tx) {
		return getCount( Cypher.numbPersonsHealthy( day), tx);
	}
	
	// how many :Persons inIncubation period?
	private int getNumbPersonsInIncubation(Transaction tx) {
		return getCount( Cypher.numbPersonsInIncubation(), tx);
	}
	
	// how many :Persons are ill
	private int getNumbPersonsIll(Transaction tx) {		
		return getCount( Cypher.numbPersonsIll(), tx);
	}
	
	// how many :Persons are immune
	private int getNumbPersonsImmune(Transaction tx) {		
		return getCount( Cypher.numbPersonsImmune(), tx);
	}
	
	// how many :Persons with attribute attributeName
	private int getNumbPersonsWithAttribute( String attributeName) {	
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				String cypherQ = Cypher.numbPersonsWithAttribute( attributeName);
				return getCount( cypherQ, tx);
			});
		}
	}
	
	// how may CanInfect in the db
	private int getNumbCanInfects( Transaction tx) {		
		String cypherQ = Cypher.numbCanInfects();
		return getCount( cypherQ, tx);
	}
	private int getNumbCanInfect() {	
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getNumbCanInfects( tx);
			});
		}
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
	// read all persons from db
	private Vector<Person> readAllPersons() {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAllPersons());
				return Neo4j.getPersonsFromResult( result);
			});
		}
	}
	
	public Persons getAllPersons() {
		return new Persons( readAllPersons());
	}

	
	// put nodes (persons) from Result- Record to Vector
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
	
	// return HashMap<id, status> of all persons, who have a new stati
	public Vector<Integer> getIdsFromPersonsWithNewStatus( Person.status status, int day) {
		Vector<Integer> newStatusVector = new Vector<Integer>();
		
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				
				String cypherQ = "";
				if( status == Person.status.healthy) cypherQ = Cypher.newPersonsHealthy();
				else if( status == Person.status.inIncubation) cypherQ =  Cypher.newPersonsInIncubation();
				else if( status == Person.status.ill) cypherQ =  Cypher.newPersonsIll();
				else if( status == Person.status.immune) cypherQ =  Cypher.newPersonsImmune();				
				Result result = tx.run( cypherQ, new HashMap<String, Object>(Map.of("day", day)));
				
				while( result.hasNext()) {
					Record record = result.next();
					int id = record.get( "id").asInt();
					newStatusVector.add( id);
				}
//				Utils.logging( cypherQ);
//				Utils.logging( newStatusVector.size());
				return newStatusVector;
			});
		}
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of canInfect
	/* 
	/*-----------------------------------------------------------------------------
	 */
	// download all CanInfect
	public Vector<CanInfect> getAllCanInfects( Transaction tx) {
		Vector<CanInfect> canInfects = new Vector<CanInfect>();
		String cypherQ = Cypher.getAllCanInfect();
		Result result = tx.run( cypherQ);
		canInfects = getCanInfectsFromResult( result);
		return canInfects;
	}
	private Vector<CanInfect> getAllCanInfects() {
		try( Session session = driver.session()) {
			return session.readTransaction( tx -> {
				return getAllCanInfects( tx);
			});
		}
	}
	
	// get CanInfect from Result to Vector
	public Vector<CanInfect> getCanInfectsFromResult( Result result) {
		Vector<CanInfect> canInfects = new Vector<CanInfect>();
		while( result.hasNext()) {
			Record record = result.next();
			int id1 = (record.get( "p").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			int id2 = (record.get( "q").asNode()).get( Neo4j.fieldName.id.toString()).asInt();
			Relationship c = record.get( "c").asRelationship();
			int distance = c.get( Neo4j.relAttribute.distance.toString()).asInt();
			canInfects.add(new CanInfect(id1, id2, distance));
		}
		return canInfects;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* print the content of the database
	/* 1. number of nodes with 3 examples, randomly
	/* 2. number of possible canInfect with 3 examples
	/* 
	/*-----------------------------------------------------------------------------
	 */
	public void printNeo4jContent() {
		var persons = new Persons( readAllPersons());
		try( Session session = driver.session()) {	
			session.readTransaction(tx -> {

				// print number of persons (nodes in the database)
				Utils.logging(String.format( "%d (%d) persons found, for example:", 
					persons.getNumberPersons(), persons.getNumberPersons()));
		
				// print randomly 3 persons
				for (int i = 1; i <= Math.min( 3, this.getNumbPersons( tx)); i++) {
					Utils.logging( persons.getPersonRandomly());
				}
		
				// print number of CanInfect (relations in the database)
				Vector<CanInfect> canInfects = new Vector<CanInfect>();
				canInfects = this.getAllCanInfects( tx);
				Utils.logging( String.format("%d (%d) relations found, for example:",
					canInfects.size(), this.getNumbCanInfects( tx)));
				
				// print randomly 3 CanInfect
				for (int i = 1; i <= Math.min( 3, canInfects.size()); i++) {
					Utils.logging( canInfects.elementAt(Utils.randomGetInt( 0, canInfects.size() - 1)));
				}
				return 1;
			});
		}
	}

	
	public void printStatusPersons( int day, StatisticADay statisticADay) {
		Utils.logging( getStatusPersons( day, statisticADay));
	}
	public static String getStatusPersons( int day, StatisticADay statisticADay) {
		return String.format( 
			"day=%d healthy=%d incubation=%d(%d) ill=%d(%d) immune=%d(%d) R=%5.2f Q=%5.1f%%",
				day, 
				statisticADay.getNumbPersonsHealthy(), 
				statisticADay.getNumbPersonsInIncubation(), statisticADay.getNewNumbPersonsInIncubation(),
				statisticADay.getNumbPersonsIll(), statisticADay.getNewNumbPersonsIll(),
				statisticADay.getNumbPersonsImmune(), statisticADay.getNewNumbPersonsImmune(),
				statisticADay.getR(), statisticADay.getQ()*100);
	}
		
	
}
