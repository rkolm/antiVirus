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
	/* manipulates biometrics attributes of the nodes (persons)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// remove biometrics attributes from All nodes
	public static String removeBiometricsAttributesFromAllPersons() {
		String cypherQ = String.format( 
			"Match( p:%s) " +
			"REMOVE p.%s, p.%s, p.%s",
			Neo4j.labelName.Person,
			Neo4j.fieldName.illnessPeriod, Neo4j.fieldName.incubationPeriod,
				Neo4j.fieldName.dayOfInfection);
		return cypherQ;
	}
	
	// add biometrics attributes to Persons
	public static String addBiometricsAttributesToPersons() {
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
	/* download canInfect (relation)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// download all relations (canInfect) 
	public static String getAllCanInfect() {
		String cypherQ = String.format(
			"MATCH (p:%s)-[c:%s]->(q:%s) " +
			"RETURN p, c, q", 
			Neo4j.labelName.Person, Neo4j.relType.CanInfect, Neo4j.labelName.Person);
		return cypherQ;
	}
	
	
	// get id2 and distance from all persons who can be infected
	public static String infectPersons( int day) {
		String cypherQ = String.format(
				"MATCH (p:%s)-[c:%s]->(q:%s) " +
				"WHERE (q.%s = 0) AND (p.%s > 0) AND (p.%s <= %d) AND (%d <= p.%s + p.%s) " +
				"WITH p, q, c.%s AS dist, rand() AS r " +
				"WHERE ((r < 0.01) OR (r < 1000.0 * 100.0 / dist / dist)) " +
				"set q.%s = %d " +
				"CREATE (p)-[:" + Neo4j.relType2nd.HasInfected + 
					" {" + Neo4j.relAttribute.day + ":" + day + "}]->(q)",
				Neo4j.labelName.Person, Neo4j.relType.CanInfect, Neo4j.labelName.Person, 
				Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.dayOfInfection, 
					Neo4j.fieldName.dayOfInfection, day, day, Neo4j.fieldName.dayOfInfection,
					Neo4j.fieldName.incubationPeriod,
				Neo4j.relAttribute.distance.toString(),
				Neo4j.fieldName.dayOfInfection,	day);
		return cypherQ;
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* manipulate relations i.e. canInfect between 2 persons
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// remove all relations in neo4j
	public static String removeAllRelations( Neo4j.relType2nd relType) {
		String cypherQ = String.format( "Match ()-[m:%s]->() delete m",
			relType);
		return cypherQ;
	}
	
	// create a canInfect- relation between 2 persons
	public static String createCanInfect( int id1, int id2, int distance) {
		String cypherQ = String.format(
				"MATCH (p:%s {id: %d}), (q:%s {id: %d}) " +
				"CREATE (p)-[:%s {distance:%d}]->(q)", 
				Neo4j.labelName.Person, id1, Neo4j.labelName.Person, id2,
				Neo4j.relType.CanInfect, distance);
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
	
	// number of persons who are healthy
	public static String numbPersonsHealthy( int day) {
//		String cypherQ = String.format(	
//			"MATCH (p:%s) " + 
//			"WHERE (p.%s = 0) " +
//			"RETURN count( p) as count",
//			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection);
		String cypherQ = 
			"MATCH (p:" + Neo4j.labelName2nd.Healthy.toString() + ") " +
			"RETURN count( p) as count";
		return cypherQ;
	}
	
	// number of persons in incubation period
	public static String numbPersonsInIncubation( int day) {
//		String cypherQ = String.format(	
//			"MATCH (p:%s) " + 
//			"WHERE (p.%s > 0) AND (p.%s <= %d) AND (%d <= p.%s + p.%s) " +
//			"RETURN count( p) as count",
//			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
//			Neo4j.fieldName.dayOfInfection, day,
//			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod);
			String cypherQ = 
				"MATCH (p:" + Neo4j.labelName2nd.InIncubation.toString() + ") " +
				"RETURN count( p) as count";
		return cypherQ;
	}
	
	// number of persons who are ill
	public static String numbPersonsIll( int day) {
//		String cypherQ = String.format(	
//			"MATCH (p:%s) " + 
//			"WHERE (p.%s > 0) AND (p.%s + p.%s < %d) AND (%d <= p.%s + p.%s + p.%s) " +
//			"RETURN count( p) as count",
//			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
//			Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, day,
//			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, Neo4j.fieldName.illnessPeriod);
			String cypherQ = 
				"MATCH (p:" + Neo4j.labelName2nd.Ill.toString() + ") " +
				"RETURN count( p) as count";
		return cypherQ;
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsImmune( int day) {
//		String cypherQ = String.format(	
//			"MATCH (p:%s) " + 
//			"WHERE (p.%s > 0) AND (%d > p.%s + p.%s + p.%s) " +
//			"RETURN count( p) as count", 
//			Neo4j.labelName.Person, Neo4j.fieldName.dayOfInfection, 
//			day, Neo4j.fieldName.dayOfInfection, Neo4j.fieldName.incubationPeriod, Neo4j.fieldName.illnessPeriod);
			String cypherQ = 
				"MATCH (p:" + Neo4j.labelName2nd.Immune.toString() + ") " +
				"RETURN count( p) as count";
		return cypherQ;
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsWithAttribute( String attributeName) {
		String cypherQ =
			"MATCH (p:" + Neo4j.labelName.Person + " ) " +
			"WHERE EXISTS( p." + attributeName + ") " +
			"RETURN count( p) as count";
		return cypherQ;
	}
	
	// number of relations (canInfect)
	public static String numbCanInfects() {
		String cypherQ = String.format( 
			"MATCH ()-[r:%s]->() " +
			"RETURN count(r) as count",
			Neo4j.relType.CanInfect);
		return cypherQ;
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
	
	

