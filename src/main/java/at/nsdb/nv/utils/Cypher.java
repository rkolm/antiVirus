package at.nsdb.nv.utils;

import at.nsdb.nv.utils.Constants.fieldName;

/**
 * creates all used Cypher-Queries as String
 */
public class Cypher {
	
	/** create constraint and index for Person.id */	
	public static String createConstraint() {
		return "CREATE CONSTRAINT ON (p:"+Constants.labelName.Person+") " +
			   "ASSERT p."+Constants.fieldName.id+" IS UNIQUE";
	}
	
	
	/** remove biometric attributes from all nodes */
	public static String removeBiometricAttributesFromAllPersons() {
		return "MATCH( p:"+Constants.labelName.Person+") " +
			   "REMOVE p."+Constants.fieldName.illnessPeriod+", " +
					  "p."+Constants.fieldName.incubationPeriod+", " +
					  "p."+Constants.fieldName.dayOfInfection;
	}
	
	/** add biometric attributes to all persons */ 
	public static String addBiometricAttributesToAllPersons() {
		return "MATCH( p:"+Constants.labelName.Person+") " +
			   "SET p."+Constants.fieldName.illnessPeriod+" = 0," +
				  " p."+Constants.fieldName.incubationPeriod+" = 0," +
				  " p."+Constants.fieldName.dayOfInfection+" = 0";
	}
	
	
	// set all persons to healthy (set dayOfInfection = 0)
	public static String setAllPersonsToHealthy() {
		return "MATCH( n:"+Constants.labelName.Person+" ) " +
			   "SET n."+Constants.fieldName.dayOfInfection+"=0";
	}
	
	// infect a person
	public static String infectAPerson() {
		return "MATCH( n:"+Constants.labelName.Person+") " +
			   "WHERE n.id = $id " +
			   "SET n."+Constants.fieldName.dayOfInfection+"= $day";
	}
	
	/** index for Person-attribute */	
	public static String createIndex(String attribute) {
		return "CREATE INDEX idx_"+attribute+"" +
			   " FOR (p:"+Constants.labelName.Person+")" +
			   "  ON (p."+attribute+") ";
	}
	
		
	/** get all persons */
	public static String getAllPersons() {
		return "MATCH (p:"+Constants.labelName.Person+") " +
				"RETURN p";
	}
	
	/** download all persons in incubation period. only persons in incubation period can infect */
	public static String getAllPersonsInIncubation() {
		return
			"MATCH (p:"+Constants.labelName.Person+") " + 
			"WHERE (p."+Constants.fieldName.dayOfInfection+" > 0) " +
			 " AND (p."+Constants.fieldName.dayOfInfection+" <= $day) " +
			 " AND ($day <= p."+Constants.fieldName.dayOfInfection+" + p."+Constants.fieldName.incubationPeriod+")" +
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
		return 	"MATCH (p:" + Constants.labelNameVar.Healthy + ") " +
				"RETURN p." + Constants.fieldName.id + " as id";
	}
	/** ids of persons, who are new in incubation period */
	public static String newPersonsInIncubation() {
		return	"MATCH (p:" + Constants.labelNameVar.InIncubation + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection + " = $day " +
				"RETURN p." + Constants.fieldName.id + " as id";
	}
	/** ids of persons, who are new in ill period */
	public static String newPersonsIll() {
			return
				"MATCH (p:" + Constants.labelNameVar.Ill.toString() + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection.toString() + 
					" + p." + Constants.fieldName.incubationPeriod.toString() + " + 1 = $day " +
				"RETURN p." + Constants.fieldName.id + " as id";
	}
	/** ids of persons, who are new of immune (after illness) persons */
	public static String newPersonsImmune() {
		return "MATCH (p:" + Constants.labelNameVar.Immune.toString() + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection.toString() + 
					" + p." + Constants.fieldName.incubationPeriod.toString() +
					" + p." + Constants.fieldName.illnessPeriod.toString() + " + 1 = $day " +
				"RETURN p." + Constants.fieldName.id + " as id";
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
			"MATCH (p:"+Constants.labelName.Person+")-[c:"+Constants.relType.CanInfect+"]->(q:"+Constants.labelName.Person+") " +
			"RETURN p, c, q";
	}
	
	
	/** get id2 and distance from all persons who can be infected */
	public static String infectPersons() {
		return "MATCH (p:"+Constants.labelName.Person+")-[c:"+Constants.relType.CanInfect+"]->(q:"+Constants.labelName.Person+") " +
				"WHERE (q."+Constants.fieldName.dayOfInfection+" = 0) " +
				"  AND (p."+Constants.fieldName.dayOfInfection+" > 0) " +
				"  AND (p."+Constants.fieldName.dayOfInfection+" <= $day) " +
				"  AND ($day <= p."+Constants.fieldName.dayOfInfection+" + p."+Constants.fieldName.incubationPeriod+") " +
				"WITH p, q, c."+Constants.relAttribute.distance+" AS dist, rand() AS r " +
				"WHERE ((r < 0.01) OR (r < $quote * 1000.0 * 100.0 / dist / dist)) " +
				"set q."+Constants.fieldName.dayOfInfection+" = $day " +
				"CREATE (p)-[:" + Constants.relTypeVar.HasInfected + 
					" {" + Constants.relAttribute.day + ":$day}]->(q)";
	}
	
	
	/**
	 * CypherQuery to remove all variable relations between 2 persons
	 */	
	public static String removeAllRelations( Constants.relTypeVar relType) {
		return "MATCH ()-[m:"+relType+"]->() DELETE m";
	}
	
	/**
	 * CypherQuery create a canInfect- relation between 2 persons
	 */
	public static String createCanInfect( int id1, int id2, int distance) {
		return	"MATCH (p:"+Constants.labelName.Person+" {id:"+id1+"}), (q:"+Constants.labelName.Person+" {id:"+id2+"}) " +
				"CREATE (p)-[:"+Constants.relType.CanInfect+" {distance:"+distance+"}]->(q)";
	}
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* asking neo4j for different numbers
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	// number of persons over all in neo4J
	public static String numbPersons() {
		return	"MATCH (n:"+Constants.labelName.Person+") " +
				"RETURN count(n) as count";
	}
	
	// number of persons who are healthy
	public static String numbPersonsHealthy() {
		return	"MATCH (p:" + Constants.labelNameVar.Healthy + ") " +
				"RETURN count( p) as count";
	}
	
	// number of persons in incubation period
	public static String numbPersonsInIncubation() {
		return	"MATCH (p:" + Constants.labelNameVar.InIncubation + ") " +
				"RETURN count( p) as count";
	}
	
	// number of persons who are ill
	public static String numbPersonsIll() {
		return	"MATCH (p:" + Constants.labelNameVar.Ill + ") " +
				"RETURN count( p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsImmune() {
		return	"MATCH (p:" + Constants.labelNameVar.Immune + ") " +
				"RETURN count( p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsWithAttribute( fieldName attribute) {
		return "MATCH (p:" + Constants.labelName.Person + " ) " +
				"WHERE EXISTS( p." + attribute + ") " +
				"RETURN count( p) as count";
	}
	
	// number of relations (canInfect)
	public static String numbCanInfects() {
		return "MATCH ()-[r:"+Constants.relType.CanInfect+"]->() " +
				"RETURN count(r) as count";
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* set labels depending on status
	/* 
	/*-----------------------------------------------------------------------------
	 */		
	/** set node label to all :Persons who are healthy */
	public static String setLabelHealthy() {
		String cypherQ =
			"MATCH (p:" + Constants.labelName.Person + ") " +
			"WHERE p." + Constants.fieldName.dayOfInfection + " = 0 " +
			"SET p:" + Constants.labelNameVar.Healthy.toString();
		return cypherQ;
	}
	
	/** set node label to all :Persons who are in incubation period */
	public static String setLabelInIncubation() {
		return
			"MATCH (p:" + Constants.labelName.Person + ") " + 
			"WHERE (p." + Constants.fieldName.dayOfInfection + " > 0) " +
				"AND (p." + Constants.fieldName.dayOfInfection + " <= $day) " +
				"AND ($day <= " +
					"p." + Constants.fieldName.dayOfInfection + " + p." + Constants.fieldName.incubationPeriod + ")" +
			"SET p: " + Constants.labelNameVar.InIncubation; 
	}
	
	/** set node label to all :Persons who are ill */
	public static String setLabelIll() {
		return	
			"MATCH (p:" + Constants.labelName.Person + ") " +
			"WHERE (p." + Constants.fieldName.dayOfInfection + " > 0) " + 
				"AND (p." + Constants.fieldName.dayOfInfection + " + p." + Constants.fieldName.incubationPeriod + 
					" < $day) " +
				"AND ($day <= " + 
					"p." + Constants.fieldName.dayOfInfection + " + p." + Constants.fieldName.incubationPeriod + 
						" + p." + Constants.fieldName.illnessPeriod + ") " +
			"SET p: " + Constants.labelNameVar.Ill.toString();
	}
	
	/** set node label to all :Persons who are immune */
	public static String setLabelImmune() {
		return
			"MATCH (p:" + Constants.labelName.Person + ") " +
			"WHERE (p." + Constants.fieldName.dayOfInfection + " > 0) " + 
			" AND ($day > " + 
					"p." + Constants.fieldName.dayOfInfection + " + p." + Constants.fieldName.incubationPeriod + 
						" + p." + Constants.fieldName.illnessPeriod + ") " +
			"SET p: " + Constants.labelNameVar.Immune.toString();
	}
}
	
	

