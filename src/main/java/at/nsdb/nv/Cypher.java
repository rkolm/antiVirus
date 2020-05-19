/***** -----------------------------------------------------------------------------
/*
/* creates all used Cypher- Queries as String
/* 
/***** -----------------------------------------------------------------------------
 */

package at.nsdb.nv;

public class Cypher {
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* create constraint and index for Person.id
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	public static String createConstraint() {
		String cypherQ = String.format( 
			"CREATE CONSTRAINT ON (p:%s) " +
			"ASSERT p.%s IS UNIQUE",
			Neo4j.labelName.Person, Neo4j.fieldName.id);
		return cypherQ;
	}
	
	/*-----------------------------------------------------------------------------
	/*
	/* index for Person-attribute
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	public static String createIndex(String attribute) {
		String cypherQ = String.format( 
			"CREATE INDEX idx_%s" +
			" FOR (p:%s)" +
			"  ON (p.%s) ",
			attribute, Neo4j.labelName.Person, attribute);
		return cypherQ;
	}
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* manipulates biometrics attributes of the nodes (persons)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// create biometrics attributes
	public static String addBiometricsAttributes() {
		String cypherQ = String.format( 
			"Match( p:%s) " +
			"Set p.%s = 0, p.%s = 0, p.%s = 0",
			Neo4j.labelName.Person, Neo4j.fieldName.illnessPeriod, Neo4j.fieldName.incubationPeriod,
			Neo4j.fieldName.dayOfInfection);
		return cypherQ;
	}
	
	// initialize biometrics attributes
	public static String setBiometrics( int id, int incubationPeriod, int illnessPeriod) {
		String cypherQ = String.format( 
			"Match( n:%s { id:%d}) " +
			"Set n.%s=%d, n.%s=%d, n.%s=%d",
			Neo4j.labelName.Person, id,
			Neo4j.fieldName.dayOfInfection, 0,
			Neo4j.fieldName.incubationPeriod, incubationPeriod,
			Neo4j.fieldName.illnessPeriod, illnessPeriod);
		return cypherQ;
	}
	
	// set all persons to healthy (set dayOfInfection = 0)
	public static String setAllPersonsToHealthy() {
		String cypherQ = String.format( 
			"Match( n:%s ) " +
			"Set n.%s=0",
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection);
		return cypherQ;
	}
	
	// infect a person
	public static String infectAPerson( int id, int day) {
		String cypherQ = String.format( 
			"Match( n:%s { id:%d}) " +
			"Set n.%s=%d",
			Neo4j.labelName.Person, id,	Neo4j.fieldName.dayOfInfection, day);
		return cypherQ;
	}
	
	
	
		
	/*-----------------------------------------------------------------------------
	/*
	/* download persons (nodes)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// download all persons 
	public static String getAllPersons() {
		String cypherQ = String.format(
			"MATCH (p:%s) " +
			"RETURN p",
			Neo4j.labelName.Person);
		return cypherQ;
	}
	
	// download all persons in incubation period. only persons in incubation period can infect
	public static String getAllPersonsInIncubation( int day) {
		String cypherQ = String.format(	
			"MATCH (p:%s) " + 
			"WHERE (p.%s > 0) AND (p.%s <= %d) AND (%d <= p.%s + p.%s)" +
			"RETURN p", 
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
			Neo4j.fieldName.dayOfInfection, day,
			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod);
		return cypherQ;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* labels :Person
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// w�hle 5.000 Knoten zuf�llig aus und setze label :Person
	public static String selectPersons( int numbNodes) {
		String cypherQ = String.format(
				"MATCH (n) " +
				"WHERE rand() < %s " +
				"SET n:%s", 
				String.valueOf( (double) Parameter.numPersonsSelected / numbNodes), Neo4j.labelName.Person);
		return cypherQ;
	}
	
	// l�sche alle labels :Person
	public static String removeAllLabelsPerson() {
		String cypherQ = String.format(
				"MATCH (p:%s) " +
				"REMOVE p:%s", 
				Neo4j.labelName.Person, Neo4j.labelName.Person);
		return cypherQ;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* download MEETING (relation)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// download all relations (meetings) 
	public static String getAllMeetings() {
		String cypherQ = String.format(
			"MATCH (p:%s)-[c:%s]->(q) " +
			"RETURN p, c, q", 
			Neo4j.labelName.Person, Neo4j.relType.Meeting);
		return cypherQ;
	}
	
	// get id2 and distance from all persons who can be infected
	public static String getAllPersonsWhoCanBeInfected( int day) {
		String cypherQ = String.format(
				"MATCH (p:%s)-[c:%s]->(q:%s) " +
				"WHERE (q.%s = 0) AND (p.%s > 0) AND (p.%s <= %d) AND (%d <= p.%s + p.%s) " +
				"RETURN q.%s as id2, c.%s as distance",
				Neo4j.labelName.Person, Neo4j.relType.Meeting, Neo4j.labelName.Person, 
				Neo4j.fieldName.dayOfInfection, 
				Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.dayOfInfection, day,
				day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod,
				Neo4j.fieldName.id, Neo4j.fieldName.distance);
		return cypherQ;
	}
		
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* manipulate relations i.e. MEETINGs between 2 persons
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// delete all relations in neo4j
	public static String deleteAllRelationsBetweenPersons() {
		String cypherQ = String.format( "Match (p:%s)-[m]->(q:%s) delete m",
			Neo4j.labelName.Person, Neo4j.labelName.Person);
		return cypherQ;
	}
	
	// create a Meeting- relation between 2 persons
	public static String createMeeting( int id1, int id2, int distance) {
		String cypherQ = String.format(
				"MATCH (p:%s {id: %d}), (q:%s {id: %d}) " +
				"CREATE (p)-[:%s {distance:%d}]->(q)", 
				Neo4j.labelName.Person, id1, Neo4j.labelName.Person, id2,
				Neo4j.relType.Meeting, distance);
		return cypherQ;
	}
		
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* asking neo4j for different numbers
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// number of persons over all in neo4J
	public static String numbPersons() {
		String cypherQ = String.format(
			"MATCH (n:%s) " +
			"RETURN count(n) as count", 
			Neo4j.labelName.Person);
		return cypherQ;
	}
	
	// number of nodes over all in neo4J
	public static String numbNodes() {
		String cypherQ = 
			"MATCH (n) " +
			"RETURN count(n) as count";
		return cypherQ;
	}
	
	// number of persons who are healthy
	public static String numbPersonsHealthy( int day) {
		String cypherQ = String.format(	
			"MATCH (p:%s) " + 
			"WHERE (p.%s = 0) " +
			"RETURN count( p) as count",
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection);
		return cypherQ;
	}
	
	// number of persons in incubation period
	public static String numbPersonsInIncubation( int day) {
		String cypherQ = String.format(	
			"MATCH (p:%s) " + 
			"WHERE (p.%s > 0) AND (p.%s <= %d) AND (%d <= p.%s + p.%s) " +
			"RETURN count( p) as count",
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
			Neo4j.fieldName.dayOfInfection, day,
			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod);
		return cypherQ;
	}
	
	// number of persons who are ill
	public static String numbPersonsIll( int day) {
		String cypherQ = String.format(	
			"MATCH (p:%s) " + 
			"WHERE (p.%s > 0) AND (p.%s + p.%s < %d) AND (%d <= p.%s + p.%s + p.%s) " +
			"RETURN count( p) as count",
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
			Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, day,
			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, Neo4j.fieldName.illnessPeriod);
		return cypherQ;
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsImmune( int day) {
		String cypherQ = String.format(	
			"MATCH (p:%s) " + 
			"WHERE (p.%s > 0) AND (%d > p.%s + p.%s + p.%s) " +
			"RETURN count( p) as count", 
			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, Neo4j.fieldName.illnessPeriod);
		return cypherQ;
	}
	
	// number of relations (meetings)
	public static String numbMeetings() {
		String cypherQ = String.format( 
			"MATCH (:%s)-[r:%s]->(:%s) " +
			"RETURN count(r) as count",
			Neo4j.labelName.Person, Neo4j.relType.Meeting, Neo4j.labelName.Person);
		return cypherQ;
	}
}
	
	

