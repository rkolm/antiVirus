package at.nsdb.nv.utils;

import at.nsdb.nv.Neo4j;

/**
 * creates all used Cypher-Queries as String
 */
public class Cypher {
	
	/** create constraint and index for Person.id */	
	public static String createConstraint() {
		return "CREATE CONSTRAINT ON (p:"+Neo4j.labelName.Person+") " +
			   "ASSERT p."+Neo4j.fieldName.id+" IS UNIQUE";
	}
	
	
	/** remove biometric attributes from all nodes */
	public static String removeBiometricAttributesFromAllPersons() {
		return "MATCH( p:"+Neo4j.labelName.Person+") " +
			   "REMOVE p."+Neo4j.fieldName.illnessPeriod+", " +
					  "p."+Neo4j.fieldName.incubationPeriod+", " +
					  "p."+Neo4j.fieldName.dayOfInfection;
	}
	
	/** add biometric attributes to all persons */ 
	public static String addBiometricAttributesToAllPersons() {
		return "MATCH( p:"+Neo4j.labelName.Person+") " +
			   "SET p."+Neo4j.fieldName.illnessPeriod+" = 0," +
				  " p."+Neo4j.fieldName.incubationPeriod+" = 0," +
				  " p."+Neo4j.fieldName.dayOfInfection+" = 0";
	}
	
	
	// set all persons to healthy (set dayOfInfection = 0)
	public static String setAllPersonsToHealthy() {
		return "MATCH( n:"+Neo4j.labelName.Person+" ) " +
			   "SET n."+Neo4j.fieldName.dayOfInfection+"=0";
	}
	
	// infect a person
	public static String infectAPerson() {
		return "MATCH( n:"+Neo4j.labelName.Person+") " +
			   "WHERE n.id = $id " +
			   "SET n."+Neo4j.fieldName.dayOfInfection+"= $day";
	}
	
	/** index for Person-attribute */	
	public static String createIndex(String attribute) {
		return "CREATE INDEX idx_"+attribute+"" +
			   " FOR (p:"+Neo4j.labelName.Person+")" +
			   "  ON (p."+attribute+") ";
	}
	
		
	/** get all persons */
	public static String getAllPersons() {
		return "MATCH (p:"+Neo4j.labelName.Person+") " +
				"RETURN p";
	}
	
	/** download all persons in incubation period. only persons in incubation period can infect */
	public static String getAllPersonsInIncubation() {
		return
			"MATCH (p:"+Neo4j.labelName.Person+") " + 
			"WHERE (p."+Neo4j.fieldName.dayOfInfection+" > 0) " +
			 " AND (p."+Neo4j.fieldName.dayOfInfection+" <= $day) " +
			 " AND ($day <= p."+Neo4j.fieldName.dayOfInfection+" + p."+Neo4j.fieldName.incubationPeriod+")" +
			"RETURN p";
	}
	
	/*-----------------------------------------------------------------------------
	/*
	/* download ids
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	/** ids of persons, who are healthy */
	public static String newPersonsHealthy() {
		return 	"MATCH (p:" + Neo4j.labelName2nd.Healthy + ") " +
				"RETURN p." + Neo4j.fieldName.id + " as id";
	}
	/** ids of persons, who are new in incubation period */
	public static String newPersonsInIncubation() {
		return	"MATCH (p:" + Neo4j.labelName2nd.InIncubation + ") " +
				"WHERE p." + Neo4j.fieldName.dayOfInfection + " = $day " +
				"RETURN p." + Neo4j.fieldName.id + " as id";
	}
	/** ids of persons, who are new in ill period */
	public static String newPersonsIll() {
			return
				"MATCH (p:" + Neo4j.labelName2nd.Ill.toString() + ") " +
				"WHERE p." + Neo4j.fieldName.dayOfInfection.toString() + 
					" + p." + Neo4j.fieldName.incubationPeriod.toString() + " + 1 = $day " +
				"RETURN p." + Neo4j.fieldName.id + " as id";
	}
	/** ids of persons, who are new of immune (after illness) persons */
	public static String newPersonsImmune() {
		return "MATCH (p:" + Neo4j.labelName2nd.Immune.toString() + ") " +
				"WHERE p." + Neo4j.fieldName.dayOfInfection.toString() + 
					" + p." + Neo4j.fieldName.incubationPeriod.toString() +
					" + p." + Neo4j.fieldName.illnessPeriod.toString() + " + 1 = $day " +
				"RETURN p." + Neo4j.fieldName.id + " as id";
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* download canInfect (relation)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	/** download all relations (canInfect) */
	public static String getAllCanInfect() {
		return
			"MATCH (p:"+Neo4j.labelName.Person+")-[c:"+Neo4j.relType.CanInfect+"]->(q:"+Neo4j.labelName.Person+") " +
			"RETURN p, c, q";
	}
	
	
	/** get id2 and distance from all persons who can be infected */
	public static String infectPersons() {
		return "MATCH (p:"+Neo4j.labelName.Person+")-[c:"+Neo4j.relType.CanInfect+"]->(q:"+Neo4j.labelName.Person+") " +
				"WHERE (q."+Neo4j.fieldName.dayOfInfection+" = 0) " +
				"  AND (p."+Neo4j.fieldName.dayOfInfection+" > 0) " +
				"  AND (p."+Neo4j.fieldName.dayOfInfection+" <= $day) " +
				"  AND ($day <= p."+Neo4j.fieldName.dayOfInfection+" + p."+Neo4j.fieldName.incubationPeriod+") " +
				"WITH p, q, c."+Neo4j.relAttribute.distance+" AS dist, rand() AS r " +
				"WHERE ((r < 0.01) OR (r < $quote * 1000.0 * 100.0 / dist / dist)) " +
				"set q."+Neo4j.fieldName.dayOfInfection+" = $day " +
				"CREATE (p)-[:" + Neo4j.relTypeVar.HasInfected + 
					" {" + Neo4j.relAttribute.day + ":$day}]->(q)";
	}
	
	
	/**
	 * CypherQuery to remove all variable relations between 2 persons
	 */	
	public static String removeAllRelations( Neo4j.relTypeVar relType) {
		return "MATCH ()-[m:"+relType+"]->() DELETE m";
	}
	
	/**
	 * CypherQuery create a canInfect- relation between 2 persons
	 */
	public static String createCanInfect( int id1, int id2, int distance) {
		return	"MATCH (p:"+Neo4j.labelName.Person+" {id:"+id1+"}), (q:"+Neo4j.labelName.Person+" {id:"+id2+"}) " +
				"CREATE (p)-[:"+Neo4j.relType.CanInfect+" {distance:"+distance+"}]->(q)";
	}
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* asking neo4j for different numbers
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// number of persons over all in neo4J
	public static String numbPersons() {
		return	"MATCH (n:"+Neo4j.labelName.Person+") " +
				"RETURN count(n) as count";
	}
	
	// number of persons who are healthy
	public static String numbPersonsHealthy( int day) {
		return	"MATCH (p:" + Neo4j.labelName2nd.Healthy + ") " +
				"RETURN count( p) as count";
	}
	
	// number of persons in incubation period
	public static String numbPersonsInIncubation() {
		return	"MATCH (p:" + Neo4j.labelName2nd.InIncubation + ") " +
				"RETURN count( p) as count";
	}
	
	// number of persons who are ill
	public static String numbPersonsIll() {
		return	"MATCH (p:" + Neo4j.labelName2nd.Ill + ") " +
				"RETURN count( p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsImmune() {
		return	"MATCH (p:" + Neo4j.labelName2nd.Immune + ") " +
				"RETURN count( p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsWithAttribute( String attributeName) {
		return "MATCH (p:" + Neo4j.labelName.Person + " ) " +
				"WHERE EXISTS( p." + attributeName + ") " +
				"RETURN count( p) as count";
	}
	
	// number of relations (canInfect)
	public static String numbCanInfects() {
		return "MATCH ()-[r:"+Neo4j.relType.CanInfect+"]->() " +
				"RETURN count(r) as count";
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* set labels depending on status
	/* 
	/*-----------------------------------------------------------------------------
	 */		
	// set node label to all :Persons who are healthy
	public static String setLabelHealthy( int day) {
		String cypherQ =
			"MATCH (p:" + Neo4j.labelName.Person + ") " +
			"WHERE p." + Neo4j.fieldName.dayOfInfection + " = 0 " +
			"SET p:" + Neo4j.labelName2nd.Healthy.toString();
		return cypherQ;
	}
	
	// set node label to all :Persons who are in incubation period
	public static String setLabelInIncubation( int day) {
		String cypherQ = 
			"MATCH (p:" + Neo4j.labelName.Person + ") " + 
			"WHERE (p." + Neo4j.fieldName.dayOfInfection + " > 0) " +
				"AND (p." + Neo4j.fieldName.dayOfInfection + " <= " + day + ") " +
				"AND (" + day + " <= " +
					"p." + Neo4j.fieldName.dayOfInfection + " + p." + Neo4j.fieldName.incubationPeriod + ")" +
			"SET p: " + Neo4j.labelName2nd.InIncubation.toString(); 
		return cypherQ;
	}
	
	// set node label to all :Persons who are ill
	public static String setLabelIll( int day) {
		String cypherQ = 	
			"MATCH (p:" + Neo4j.labelName.Person + ") " +
			"WHERE (p." + Neo4j.fieldName.dayOfInfection + " > 0) " + 
				"AND (p." + Neo4j.fieldName.dayOfInfection + " + p." + Neo4j.fieldName.incubationPeriod + 
					" < " + day + ") " +
				"AND (" + day + " <= " + 
					"p." + Neo4j.fieldName.dayOfInfection + " + p." + Neo4j.fieldName.incubationPeriod + 
						" + p." + Neo4j.fieldName.illnessPeriod + ") " +
			"SET p: " + Neo4j.labelName2nd.Ill.toString();
		return cypherQ;
	}
	
	// set node label to all :Persons who are immune
	public static String setLabelImmune( int day) {
		String cypherQ =
			"MATCH (p:" + Neo4j.labelName.Person + ") " +
			"WHERE (p." + Neo4j.fieldName.dayOfInfection + " > 0) " + 
				"AND (" + day + " > " + 
					"p." + Neo4j.fieldName.dayOfInfection + " + p." + Neo4j.fieldName.incubationPeriod + 
						" + p." + Neo4j.fieldName.illnessPeriod + ") " +
			"SET p: " + Neo4j.labelName2nd.Immune.toString();
		return cypherQ;
	}
}
	
	

