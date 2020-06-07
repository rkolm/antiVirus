package at.nsdb.nv;

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
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.exceptions.ClientException;

import static org.neo4j.driver.Values.parameters;

import at.nsdb.nv.model.CanInfect;
import at.nsdb.nv.model.Person;
import at.nsdb.nv.model.Persons;
import at.nsdb.nv.utils.Constants;
import at.nsdb.nv.utils.Cypher;
import at.nsdb.nv.utils.Utils;
import at.nsdb.nv.utils.Constants.fieldName;

/**
 * Access to the Neo4j-Database (Singleton)
 */
public class Neo4j {

	/** instance of singleton-class */
	private static Neo4j instance = null;
	/** Neo4j database-driver  */
	private Driver driver;

	private int nodesDone = 0;
	private int lookings = 0;

	/** private constructor */
	private Neo4j() {
		this.driver = GraphDatabase.driver(Config.getDbUri(),
		AuthTokens.basic(Config.getDbUser(), Config.getDbPassword()));
		this.driver.verifyConnectivity();
		Utils.logging("Neo4j-DB connected at " + Config.getDbUri());
	}

	/** get the singleton-instance */
	public static Neo4j getInstance() {
		if (instance == null) {
			instance = new Neo4j();
		}
		return instance;
	}
	
	/** 
	 * delete all variable node-labels
	 * @param tx Transaction
	 */
	void deleteAllVarLabels( Transaction tx) {
		for( Constants.labelNameVar n : Constants.labelNameVar.values()) {
			String cypher = 
				"MATCH (p:" + Constants.labelName.Person + ") " +
				"REMOVE p:" + n.toString();
			tx.run( cypher);
		}
	}
	
	/** 
	 * create missing db-constraints - neo4j creates an index for an constraint  
	 *
	void setConstraint() {
		try(Session session = getDriver().session()) {
			session.run( Cypher.createConstraint());			
		} catch(ClientException e) { // neo4j driver does not raise the exception until the session is closed! 
			Utils.logging( "Constraint :Person(id) already exists");
			return;
		}
		Utils.logging( "Constraint :Person(id) created");
	}*/

	/** 
	 * create missing db-indices for the person node 
	 */
	void setIndexForPerson( ) {		// index for attribute dayOfInfection and incubationPeriod
		for( String attributeName : new String[] {Constants.fieldName.dayOfInfection.toString(),
			Constants.fieldName.incubationPeriod.toString()}) {
				try (Session session = getDriver().session()) {
					session.run( Cypher.createIndex( attributeName));
				} catch(ClientException e) {
					Utils.logging( "Index Person." + attributeName + " already exists");
					continue;
				}
				Utils.logging( "Index Person." + attributeName + " created");
		}
	}   
	
	/**
	 * set all persons to healthy
	 */
	void setAllPersonsToHealthy() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				tx.run( Cypher.setAllPersonsToHealthy());
				return 1;
			});
		}	
	}

	/**
	 * remove all :HasInfected-relations
	 */
	void removeAllHasInfectedRelations() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				tx.run( Cypher.removeAllRelations( Constants.relTypeVar.HasInfected));
				return 1;
			});
		}	
	}

	/**
	 * set biometric attributes for all :Person-nodes
	 */
	void setBiometricsForAllPersons() {
		int numbPersons = getNumbPersons();
		int withDayOfInfection = getNumbPersonsWithAttribute( Constants.fieldName.dayOfInfection);
		int withIncubationPeriod = getNumbPersonsWithAttribute( Constants.fieldName.incubationPeriod);
		int withIllnessPeriod = getNumbPersonsWithAttribute( Constants.fieldName.illnessPeriod);
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
	
			var persons = new Persons(readAllPersons());
			// set content to biometric attributes
			Utils.logging("set biometrics contents to all persons ...");
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> {
					// set biometrics and upload to neo4j
					// how to iterate a HashMap easily?
					for (Person p : persons.getAllPersons()) {
						p.setBiometrics();
						//String cypherQ = Cypher.setBiometrics(p.getId(), p.getIncubationPeriod(), p.getIllnessPeriod());					
						tx.run("MATCH (n:" + Constants.labelName.Person + ") " +
							   "WHERE (id(n) = $id) " +
								 "SET n." + Constants.fieldName.dayOfInfection + "= 0," +
								 	" n." + Constants.fieldName.incubationPeriod + "= $incubationPeriod," +
									" n." + Constants.fieldName.illnessPeriod + "= $illnessPeriod",
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
	 * set missing :CanInfect-relations for all :Person-nodes
	 */
	void setCanInfectRelationsForAllPersons() {
		if( getNumbCanInfect() != 0) {
            Utils.logging( "relations :CanInfect already exist");
            return;
        } 
        
        Utils.logging( "relations :CanInfect creating ...");        
        
        final var relations = new HashSet<String>();
        final var persons = new Persons( readAllPersons());
		
        for( Person p : persons.getAllPersons()) {
            try( Session session = driver.session()) {
                session.writeTransaction(tx -> {
                    // every node (person) creates a number of relations :CanInfect
                    int numbCanInfect = InfectionCalculator.calculateRandomlyNumbCanInfect();
                    do {
						Person q = persons.getPersonRandomly();
                        lookings++;
                        if( (p.getId() != q.getId()) && (! relations.contains( p.getId() + "-" + q.getId()))) {
                            int distance = Math.max( 1, Person.distance( p, q));
                            if( InfectionCalculator.canInfect( distance)) {
                                tx.run( 
                                    "MATCH (p:" + Constants.labelName.Person + "), (q:" + Constants.labelName.Person + ") " +
                                    "WHERE (id(p) = $id1 AND id(q) = $id2)" +
                                    "CREATE (p)-[:" + Constants.relType.CanInfect + " {" + Constants.relAttribute.distance + ":$distance}]->(q)",
                                    parameters("id1", p.getId(), "id2", q.getId(), "distance", distance));
                                numbCanInfect--;
                                relations.add( p.getId() + "-" + q.getId());
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
                for( CanInfect m : getAllCanInfects()) {
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

	/**
	 * set variable node labels to healthy, inIncubation, ill, immune<p/>
	 * depending on status of :Person<p/>
	 * for a given day
	 * 
	 * @param day Day
	 * @param tx Transaction
	 */
	void setAllVarLabels( int day, Transaction tx) {
		var params = parameters("day", day);
		tx.run( Cypher.setLabelHealthy());		
		tx.run( Cypher.setLabelInIncubation(), params);
		tx.run( Cypher.setLabelIll(), params);
		tx.run( Cypher.setLabelImmune(), params);
	}	
		
	
	/*-----------------------------------------------------------------------------
	/*
	/* get the numbers of records of different questions
	/* 
	/*-----------------------------------------------------------------------------
	 */
	/** how many :Persons-nodes are in the database? */
	private int getNumbPersons( Transaction tx) {
		return getCount( Cypher.numbPersons(), tx);
	}
	/** get the number of :Person-nodes */
	int getNumbPersons() {	
		try( Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getNumbPersons( tx);
			});
		}
	}
	
	/** how many :Persons healthy? */
	int getNumbPersonsHealthy( int day, Transaction tx) {
		return getCount( Cypher.numbPersonsHealthy(), tx);
	}
	
	/** how many :Persons inIncubation period? */
	int getNumbPersonsInIncubation(Transaction tx) {
		return getCount( Cypher.numbPersonsInIncubation(), tx);
	}
	
	/** how many :Persons are ill */
	int getNumbPersonsIll(Transaction tx) {		
		return getCount( Cypher.numbPersonsIll(), tx);
	}
	
	/** how many :Persons are immune */
	int getNumbPersonsImmune(Transaction tx) {		
		return getCount( Cypher.numbPersonsImmune(), tx);
	}
	
	/** how many :Persons with attribute attributeName */
	int getNumbPersonsWithAttribute( fieldName attribute) {	
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				String cypherQ = Cypher.numbPersonsWithAttribute( attribute);
				return getCount( cypherQ, tx);
			});
		}
	}
	
	/** how many :CanInfect-realations are in the db */ 
	private int getNumbCanInfects( Transaction tx) {		
		String cypherQ = Cypher.numbCanInfects();
		return getCount( cypherQ, tx);
	}
	int getNumbCanInfect() {	
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getNumbCanInfects( tx);
			});
		}
	}
	
	/** get count result */
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
	/** read all persons from db */ 
	Vector<Person> readAllPersons() {
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				// download all nodes
				Result result = tx.run( Cypher.getAllPersons());
				return getPersonsFromResult( result);
			});
		}
	}
	/** get all persons */
	public Persons getAllPersons() {
		return new Persons(readAllPersons());
	}
	
	/**
	 * put nodes (persons) from Result- Record to Vector<p/>
	 * the result-record must include column "p"
	 */
	private static Vector<Person> getPersonsFromResult( Result result) {
		final Vector<Person> persons = new Vector<Person>();
		while( result.hasNext()) {
			Record row = result.next();
			Node node = row.get("p").asNode();
			Person person = new Person();
			person.setId( node.id());
			person.setName( node.get( Constants.fieldName.firstName.toString()).asString());
			//person.setAge( node.get( Constants.fieldName.age.toString()).asInt());
			person.setLongitude( node.get( Constants.fieldName.longitude.toString()).asInt());
			person.setLatitude( node.get( Constants.fieldName.latitude.toString()).asInt());
			person.setIllnessPeriod( node.get( Constants.fieldName.illnessPeriod.toString()).asInt());
			person.setDayOfInfection( node.get( Constants.fieldName.dayOfInfection.toString()).asInt());
			person.setIncubationPeriod( node.get( Constants.fieldName.incubationPeriod.toString()).asInt());
			persons.add(person);
		}
		return persons;
	}
	
	/**
	 * @param status relevant status of person
	 * @param day relevant day
	 * @return id of all persons, who have new stati
	 */
	public Vector<Long> getIdsFromPersonsWithNewStatus( Person.status status, int day) {
		var newStatusVector = new Vector<Long>();
		
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
					long id = record.get("id").asLong();
					newStatusVector.add( id);
				}
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
	/** get all :CanInfect-relations */
	public Vector<CanInfect> getAllCanInfects( Transaction tx) {
		Vector<CanInfect> canInfects = new Vector<CanInfect>();
		String cypherQ = Cypher.getAllCanInfect();
		Result result = tx.run( cypherQ);
		canInfects = getCanInfectsFromResult( result);
		return canInfects;
	}
	/** read all :CanInfect-relations */
	Vector<CanInfect> getAllCanInfects() {
		try( Session session = driver.session()) {
			return session.readTransaction( tx -> {
				return getAllCanInfects( tx);
			});
		}
	}
	
	/** get CanInfect from Result to Vector */
	public Vector<CanInfect> getCanInfectsFromResult( Result result) {
		Vector<CanInfect> canInfects = new Vector<CanInfect>();
		while( result.hasNext()) {
			Record record = result.next();
			long id1 = record.get("p").asNode().id();
			long id2 = record.get("q").asNode().id();
			Relationship c = record.get("c").asRelationship();
			int distance = c.get( Constants.relAttribute.distance.toString()).asInt();
			canInfects.add(new CanInfect(id1, id2, distance));
		}
		return canInfects;
	}
	
	
	
	
	/**-----------------------------------------------------------------------------
	 * print the content of the database
	 * 1. number of nodes with 3 examples, randomly
	 * 2. number of possible canInfect with 3 examples
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

	public Driver getDriver() {
		return driver;
	}
			
}
