package at.nsdb.nv.db;

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
import at.nsdb.nv.utils.Config;
import at.nsdb.nv.utils.Constants;
import at.nsdb.nv.utils.InfectionCalculator;
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
	private boolean newCity;
	private boolean neo4jPreparedForActiveCity;
	private int x, y;

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
	
	/** get the singleton-instance */
	public void init() {
		if( this.neo4jPreparedForActiveCity()) {
			newCity = false;
			Utils.logging( "DB already prepared for " + Config.getPersonFilter());
		}
		else {
			newCity = true;
			Utils.logging( "preparing neo4j for " + Config.getPersonFilter() + " ...");
		}
		
		//new city?
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				this.removeAllVariableLabelsFromAllPersons( tx);
				if( newCity) {
					Utils.logging( "reorganize labels :Person");
					tx.run( Cypher.removeLabelPersonFromAllNodes());
					tx.run( Cypher.setLabelPersonToChoosenNodes());
					
				} else {
					Utils.logging( "set all Persons to :Healthy");
					tx.run( Cypher.setAllPersonsToHealthy());
				}
				return 1;
			});
		}
		
		Utils.logging("remove all :HasInfected-relations ");
		this.removeAllHasInfectedFromAllNodes();
		
		if( newCity) {
			Utils.logging( "initialize biometric attributes for :Person-nodes");
			this.removeBiometricsFromAllNodes();
			this.addBiometricsToPersons();
			this.setBiometricValues();

			this.setCanInfectRelationsForAllPersons();
			if (Config.getExportCanInfects().equals("whenNew")) exportCanInfects();	
		}
		
		if (Config.getExportCanInfects().equals("always")) exportCanInfects();
		
		Utils.logging( "---- initialization finished");
		
		Utils.logging( "**** printing db status/content ...");
		Neo4j.getInstance().printNeo4jContent();
	}
	
	
	/** 
	 * delete all variable node-labels
	 * @param tx Transaction
	 */
	public void removeAllVariableLabelsFromAllPersons( Transaction tx) {
		for( Constants.labelNameVar n : Constants.labelNameVar.values()) {
			tx.run(Cypher.removeAllVariableLabelsFromAllPersons( n));
		}
	}
	
	/** 
	 * create missing db-constraints - neo4j creates an index for an constraint  
	 */
//	public void setConstraint() {
//		try(Session session = getDriver().session()) {
//			session.run( Cypher.createConstraint());			
//		} catch(ClientException e) { // neo4j driver does not raise the exception until the session is closed! 
//			Utils.logging( "Constraint :Person(id) already exists");
//			return;
//		}
//		Utils.logging( "Constraint :Person(id) created");
//	}

	/** 
	 * create missing db-indices for the person node 
	 */
	public void setIndexForPerson( ) {		// index for attribute dayOfInfection and incubationPeriod
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
	 * remove all :HasInfected-relations
	 */
	void removeAllHasInfectedFromAllNodes() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				tx.run( Cypher.removeAllHasInfectedFromAllNodes( Constants.relTypeVar.HasInfected));
				return 1;
			});
		}	
	}
	void removeAllCanInfectFromAllNodes() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				tx.run( Cypher.removeAllCanInfectFromAllNodes( Constants.relType.CanInfect));
				return 1;
			});
		}	
	}

	/**
	 * remove biometric attributes from all :Person-nodes
	 */
	void removeBiometricsFromAllNodes() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {				
				tx.run(Cypher.removeBiometricAttributesFromAllNodes());				
				return 1;
			});			
		}
		Utils.logging("removing all biometric attributes finished");			
	}

	/**
	 * add biometric attributes from all :Person-nodes
	 */
	void addBiometricsToPersons() {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				tx.run(Cypher.addBiometricAttributesToPersons());								
				return 1;
			});
		}
		Utils.logging("adding biometric attributes e.g. incubation period to :Person-nodes finished");
	}

	/**
	 * set values for the biometric attributes for all selected :Person-nodes
	 */
	void setBiometricValues() {
		var persons = new Persons(readAllPersons());
		Utils.logging("setting biometric values for "+ persons.getAllPersons().size() + " persons " +
			" using Gauss-distribution ...");

		try (Session session = driver.session()) {
			session.writeTransaction(tx -> {
				// set biometrics and upload to neo4j
				// how to iterate a HashMap easily?
				for (Person p : persons.getAllPersons()) {
					p.setBiometrics();
					tx.run(Cypher.setBiometricsForOnePerson(),
							parameters("id", p.getId(), 
										"incubationPeriod", p.getIncubationPeriod(), 
										"illnessPeriod", p.getIllnessPeriod()));	
				}
				return 1;
			});
		} catch (Exception e) {
			Utils.logging("error setting biometrics: " + e.getCause());
		}
		Utils.logging("setting biometric attributes finished");		
	}
	
	
	/**
	 * set missing :CanInfect-relations for all :Person-nodes
	 */
	void setCanInfectRelationsForAllPersons() {        
        Utils.logging( "adding :CanInfect relations using Gauss- distribution ...");  
        this.removeAllCanInfectFromAllNodes();
                
        final var relations = new HashSet<String>();
        final var persons = new Persons( readAllPersons());
        
        // divided city in 100x100 sectors with the :Persons in a Vector
        final int fieldSize = 100;
        @SuppressWarnings("unchecked")
        Vector<Person>[][] field = (Vector<Person>[][]) new Vector[fieldSize][fieldSize];
        for( int xx=0; xx<fieldSize; xx++) {
        	for( int yy=0; yy<fieldSize; yy++) {
        		field[xx][yy] = new Vector<Person>();
        	}
        }
        for( Person p : persons.getAllPersons()) {
        	int xx = Math.max( 0, Math.min( fieldSize-1, 
        		(int)( 100. * (p.getLongitude()-persons.getMinLongitude()) / persons.getLongitudeRange())));
        	int yy = Math.max( 0, Math.min( fieldSize-1,
        		(int)( 100 * (p.getLatitude()-persons.getMinLatitude()) / persons.getLatitudeRange())));
        	field[xx][yy].add( p);
        }
		
        for( int xx=0; xx<fieldSize; xx++) {
        	for( int yy=0; yy<fieldSize; yy++) {
        		x = xx; y = yy; // why Java, do I need an global x and y ???
        		for( Person p : field[x][y]) {
		            try( Session session = driver.session()) {
		                session.writeTransaction(tx -> {
		                    // every node (person) creates a number of relations :CanInfect
		                    int numbCanInfect = InfectionCalculator.calculateRandomlyNumbCanInfect();
		                    int startLookings = lookings;
		                    do {
		                    	lookings++;
		                    	// select :CanInfect Person
		                    	int xNew, yNew;
		                    	if( Utils.randomGetDouble() < 0.5) {
		                    		xNew = x; yNew = y;
		                    	} else {
			                    	int xDiff = Utils.randomGetGauss( -20, 20, 0, 1);
			                    	xDiff += (lookings-startLookings)/100 * (xDiff < 0 ? -1 : 1);
			                    	xNew = Math.min( fieldSize-1, Math.max( 0, Math.min( fieldSize-1,	x + xDiff)));
			                    	int yDiff = Utils.randomGetGauss( -20, 20, 0, 1);
			                    	yDiff += (lookings-startLookings)/100 * (yDiff < 0 ? -1 : 1);
			                    	yNew = Math.min( fieldSize-1, Math.max( 0, Math.min( fieldSize-1,	y + yDiff)));
		                    	}
		                    	if( field[xNew][yNew].size() > 0) {
									Person q = field[xNew][yNew].get( Utils.randomGetInt(0, field[xNew][yNew].size()-1));		                        lookings++;
			                        if( ((p.getId() != q.getId()) && (! relations.contains( p.getId() + "-" + q.getId())))
			                        	|| ((lookings-startLookings)>10000)) {
			                            int distance = Math.max( 1, Person.distance( p, q));
		                                tx.run( Cypher.addCanInfect(),
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
        	}
		}
	}
	
//	/**
//	 * set missing :CanInfect-relations for all :Person-nodes
//	 */
//	void setCanInfectRelationsForAllPersons() {        
//        Utils.logging( "creating :CanInfect-relations");  
//        this.removeAllCanInfectFromAllNodes();
//        
//        final var relations = new HashSet<String>();
//        final var persons = new Persons( readAllPersons());
//		
//        for( Person p : persons.getAllPersons()) {
//            try( Session session = driver.session()) {
//                session.writeTransaction(tx -> {
//                    // every node (person) creates a number of relations :CanInfect
//                    int numbCanInfect = InfectionCalculator.calculateRandomlyNumbCanInfect();
//                    int startLookings = lookings;
//                    do {
//						Person q = persons.getPersonRandomly();
//                        lookings++;
//                        if( (p.getId() != q.getId()) && (! relations.contains( p.getId() + "-" + q.getId()))) {
//                            int distance = Math.max( 1, Person.distance( p, q));
//                            if( InfectionCalculator.canInfect( distance) || (lookings-startLookings)>50000) {
//                                tx.run( Cypher.addCanInfect(),
//                                    parameters("id1", p.getId(), "id2", q.getId(), "distance", distance));
//                                numbCanInfect--;
//                                relations.add( p.getId() + "-" + q.getId());
//                            }
//                        }
//                    } while( numbCanInfect > 0);
//                    nodesDone++;
//                    
//                    if( Math.floorMod( nodesDone, Math.min( 2500, (int)(persons.getNumberPersons() / 10))) == 0) {
//                        Utils.logging( String.format( 
//                            "%7d/%d, (%6.2f%%) persons, %7d CanInfect created, %7.1f Mio lookings", 
//                            nodesDone, persons.getNumberPersons(), 100.0 * nodesDone / persons.getNumberPersons(),
//                            relations.size(), lookings/1000000.0));
//                    }
//                    return 1;
//                });
//			}
//		}
//	}

	/**
	 * export :CanInfect-Relationsto csv
	 */
	private void exportCanInfects() {
		String canInfectFileName = Config.getCanInfectFileName();
		if (!canInfectFileName.isEmpty()) {
			Utils.logging( "exporting :CanInfect-relations to " + canInfectFileName);
			try {   
				// Open given file in append mode. 
				BufferedWriter out = new BufferedWriter( new FileWriter(canInfectFileName)); 
				out.write( CanInfect.toExportFileHeader()); out.newLine();
				for( CanInfect m : getAllCanInfects()) {
					out.write( m.toExportFile()); out.newLine();
				}
				out.close(); 
			} 
			catch (IOException e) { 
				System.out.println(" Error exporting :CanInfect-relations to " + canInfectFileName + " " + e); 
			} 
			Utils.logging( "exporting :CanInfect-relations finished");	
		}
	}

	/**
	 * set variable node labels to healthy, inIncubation, ill, immune<p/>
	 * depending on status of :Person<p/>
	 * for a given day
	 * 
	 * @param day Day
	 * @param tx Transaction
	 */
	public void setAllVarLabels( int day, Transaction tx) {
		var params = parameters("day", day);
		tx.run( Cypher.setPersonsToHealthy());	
		tx.run( Cypher.setPersonsToInIncubation(), params);
		tx.run( Cypher.setPersonsToIll(), params);
		tx.run( Cypher.setPersonsToImmune(), params);
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
	
	/** how many :Persons with a given labelNameVar */
	public int getNumbPersons( Constants.labelNameVar label, Transaction tx) {
		return getCount( Cypher.numbPersonsWithLabelNameVar( label), tx);
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
	private int getNumbPersonsWithCanInfects( Transaction tx) {		
		String cypherQ = Cypher.numbPersonsWithCanInfects();
		return getCount( cypherQ, tx);
	}
	int getNumbPersonsWithCanInfects() {	
		try(Session session = driver.session()) {
			return session.readTransaction(tx -> {
				return getNumbPersonsWithCanInfects( tx);
			});
		}
	}
	
	/** get count result */
	private int getCount( String cypherQ, Transaction tx) {
		Result result = tx.run( cypherQ);
//		return (int) (long) result.next().get( "count", Long.valueOf( 0));
		return result.single().get( 0 ).asInt();
		
	}
	
	

	
	/*-----------------------------------------------------------------------------
	/*
	/* get different sets of persons
	/* 
	/*-----------------------------------------------------------------------------
	 */
	/** read all persons from db */ 
	public Vector<Person> readAllPersons() {
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
			person.setName( node.get( Constants.fieldName.name.toString()).asString());
			person.setLongitude( node.get( Constants.fieldName.longitude.toString()).asDouble());
			person.setLatitude( node.get( Constants.fieldName.latitude.toString()).asDouble());
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
				if( status == Person.status.healthy) cypherQ = Cypher.idsFromNewPersonsHealthy();
				else if( status == Person.status.inIncubation) cypherQ =  Cypher.idsFromNewPersonsInIncubation();
				else if( status == Person.status.ill) cypherQ =  Cypher.idsFromNewPersonsIll();
				else if( status == Person.status.immune) cypherQ =  Cypher.idsFromNewPersonsImmune();				
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
	public Vector<CanInfect> getAllCanInfectsFromAllPersons( Transaction tx) {
		Vector<CanInfect> canInfects = new Vector<CanInfect>();
		String cypherQ = Cypher.getAllCanInfectsFromAllPersons();
		Result result = tx.run( cypherQ);
		canInfects = getCanInfectsFromResult( result);
		return canInfects;
	}
	/** read all :CanInfect-relations */
	Vector<CanInfect> getAllCanInfects() {
		try( Session session = driver.session()) {
			return session.readTransaction( tx -> {
				return getAllCanInfectsFromAllPersons( tx);
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

		// print number of persons (nodes in the database)
		if( Config.getPrintDBStatus() > 0) {
			Utils.logging(String.format( "%d (%d) persons found, for example:", 
			persons.getNumberPersons(), persons.getNumberPersons()));
		
			// print randomly 3 persons				
			Utils.logging( persons.getPersonRandomly());
			Utils.logging( persons.getPersonRandomly());
			Utils.logging( persons.getPersonRandomly());
		}

		if( Config.getPrintDBStatus() == 2) {
			try( Session session = driver.session()) {	
				session.readTransaction(tx -> {		
					// print number of CanInfect (relations in the database)
					var canInfects = this.getAllCanInfectsFromAllPersons( tx);
					Utils.logging( String.format("%d (%d) relations found, for example:",
						canInfects.size(), this.getNumbPersonsWithCanInfects( tx)));
					
					// print randomly 3 CanInfect
					for (int i = 1; i <= Math.min( 3, canInfects.size()); i++) {
						Utils.logging( canInfects.elementAt(Utils.randomGetInt( 0, canInfects.size() - 1)));
					}
					return 1;
				});
			}
		}
	}
	
	public boolean neo4jPreparedForActiveCity() {
		neo4jPreparedForActiveCity = true;
		try( Session session = driver.session()) {	
			session.readTransaction(tx -> {
				int numbPersons = getCount( Cypher.numbPersons(), tx);
				
				Utils.logging( "DBConsistency: " +  
					" numbPersons=" + numbPersons +
					" numbPersonsWithActiveCity=" + getCount( Cypher.numbPersonsWithActiveCity(), tx) +
					" numbNodesWithActiveCity=" +  getCount( Cypher.numbNodesWithActiveCity(), tx) +
					" numbPersonsWithCanInfects=" + getNumbPersonsWithCanInfects());
						
				if( numbPersons == 0) neo4jPreparedForActiveCity = false;
				else if( numbPersons != getCount( Cypher.numbPersonsWithActiveCity(), tx)) neo4jPreparedForActiveCity = false;
				else if( numbPersons != getCount( Cypher.numbNodesWithActiveCity(), tx)) neo4jPreparedForActiveCity = false;
				else if( numbPersons != getNumbPersonsWithCanInfects()) neo4jPreparedForActiveCity = false;
				return 1;
			});
		}
		return neo4jPreparedForActiveCity;
	}

	public Driver getDriver() {
		return driver;
	}
			
}
