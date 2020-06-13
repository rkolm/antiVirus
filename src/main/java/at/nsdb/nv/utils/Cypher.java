package at.nsdb.nv.utils;

import at.nsdb.nv.Config;
import at.nsdb.nv.utils.Constants.fieldName;
import at.nsdb.nv.utils.Constants.labelNameVar;

/**
 * creates all used Cypher-Queries as String
 */
public class Cypher {

	/** import node data */
	public static String importNodeData() {
		return ":auto USING PERIODIC COMMIT 10000" +
					"LOAD CSV WITH HEADERS FROM 'file:///VirtualData50k.csv' as p" +
					"FIELDTERMINATOR ';'" +
					"CREATE( n { " +
						"name:toString(p.name), " + 
						"companyName:toString(p.companyName), " +
						"address:toString(p.address), " +
						"city:toString(p.city), " +
						"county:toString(p.county), " + 
						"state:toString(p.state), " +
						"zip:toInteger(p.zip), " +
						"phone1:toString(p.phone1), " + 
						"phone2:toString(p.phone2), " +
						"email:toString(p.email), " +
						"web:toString(p.web), " +
						"age:toInteger(p.age), " +
						"longitude:toFloat(p.longitude), " +
						"latitude:toFloat(p.latitude) " +
						"})";
	}

	
	/** create constraint and index for Person.id */	
	public static String createConstraint() {
		return "CREATE CONSTRAINT ON (p:"+Constants.labelName.Person+") " +
			   "ASSERT p."+Constants.fieldName.id+" IS UNIQUE";
	}
	
	/*-----------------------------------------------------------------------------
	/*
	/* initialization
	/* 
	/*-----------------------------------------------------------------------------
	 */	

	public static String numbNodesWithActiveCity() {
		return	"MATCH (p) " +
				"WHERE " + Config.getPersonFilter() + " " +
				"RETURN count( p) as count";
	}
	public static String numbPersonsWithActiveCity() {
		return	"MATCH (p:" + Constants.labelName.Person + ") " +
				"WHERE " + Config.getPersonFilter() + " " +
				"RETURN count( p) as count";
	}
	// set label Person to all nodes of active city
	public static String setLabelPersonToChoosenNodes() {
		String cypherQ =
			"MATCH (p) " +
			"WHERE " + Config.getPersonFilter() + " " +
			"SET p:" + Constants.labelName.Person;
		return cypherQ;
	}
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* biometrics
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	/** remove biometric attributes from all nodes */
	public static String removeBiometricAttributesFromAllNodes() {
		return "MATCH( p) " +
			   "REMOVE p."+Constants.fieldName.illnessPeriod+", " +
					  "p."+Constants.fieldName.incubationPeriod+", " +
					  "p."+Constants.fieldName.dayOfInfection;
	}
	
	/** add biometric attributes to all persons */ 
	public static String addBiometricAttributesToPersons() {
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
			   "WHERE id(n) = $id " +
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
		return "MATCH (p:" +Constants.labelName.Person+") " +
			   " RETURN p";
	}
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* download ids
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	/** ids of persons, who are healthy */
	public static String idsFromNewPersonsHealthy() {
		return 	"MATCH (p:" + Constants.labelNameVar.Healthy + ") " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new in incubation period */
	public static String idsFromNewPersonsInIncubation() {
		return	"MATCH (p:" + Constants.labelNameVar.InIncubation + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection + " = $day " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new in ill period */
	public static String idsFromNewPersonsIll() {
			return
				"MATCH (p:" + Constants.labelNameVar.Ill.toString() + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection.toString() + 
					" + p." + Constants.fieldName.incubationPeriod.toString() + " + 1 = $day " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new of immune (after illness) persons */
	public static String idsFromNewPersonsImmune() {
		return "MATCH (p:" + Constants.labelNameVar.Immune.toString() + ") " +
				"WHERE p." + Constants.fieldName.dayOfInfection.toString() + 
					" + p." + Constants.fieldName.incubationPeriod.toString() +
					" + p." + Constants.fieldName.illnessPeriod.toString() + " + 1 = $day " +
				"RETURN id(p) as id";
	}
		
	
	/*-----------------------------------------------------------------------------
	/*
	/* download canInfect (relation)
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	/** download all relations (canInfect) */
	public static String getAllCanInfectsFromAllPersons() {
		return
			"MATCH (p:"+Constants.labelName.Person+")-[c:"+Constants.relType.CanInfect+"]->(q:"+Constants.labelName.Person+") " +
			"RETURN p, c, q";
	}
	
	/** get id2 and distance from all persons who can be infected */
	public static String infectPersons() {		
		String cypherQ =
		"MATCH (p)-[c:"+Constants.relType.CanInfect+"]->(q) " +	
		"WHERE p:"+Constants.labelName.Person + " AND q:"+Constants.labelName.Person+ " " +
		"  AND (q."+Constants.fieldName.dayOfInfection+" = 0) " +
		"  AND (p."+Constants.fieldName.dayOfInfection+" > 0) " +
		"  AND (p."+Constants.fieldName.dayOfInfection+" <= $day) " +
		"  AND ($day <= p."+Constants.fieldName.dayOfInfection+" + p."+Constants.fieldName.incubationPeriod+") " +
		"WITH p, q, c."+Constants.relAttribute.distance+" AS dist, rand() AS r, rand() AS r1, rand() AS r2 " +
		"WHERE (r2 < $quote) AND (r1 < $accept) " +
			"AND ((r < 0.002) OR (r < exp( -0.1 * dist / 1000))) " +
		"set q."+Constants.fieldName.dayOfInfection+" = $day " +
		"CREATE (p)-[:" + Constants.relTypeVar.HasInfected + 
			" {" + Constants.relAttribute.day + ":$day}]->(q)";
		

		return cypherQ;
	}
	
	
	/**
	 * CypherQuery to remove all variable relations between 2 nodes
	 */	
	public static String removeAllHasInfectedFromAllNodes( Constants.relTypeVar relType) {
		return "MATCH ()-[m:"+relType+"]->() DELETE m";
	}
	/**
	 * CypherQuery to remove canInfect relations between 2 persons
	 */	
	public static String removeAllCanInfectFromAllNodes( Constants.relType relType) {
		return "MATCH ()-[m:"+relType+"]->() DELETE m";
	}
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* asking neo4j for different numbers
	/* 
	/*-----------------------------------------------------------------------------
	 */	
	
	// number of persons over all in neo4J
	public static String numbPersons() {
		return	"MATCH (p:"+Constants.labelName.Person+") " +
				"RETURN count(p) as count";
	}
	public static String numbPersonsWithLabelNameVar(Constants.labelNameVar labelNameVar) {
//		return	"MATCH (p:"+Constants.labelName.Person+":"+ labelNameVar +") " +
		return	"MATCH (p) " +
				"WHERE (p:"+Constants.labelName.Person+") AND (p:"+labelNameVar+") " +
				"RETURN count(p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsWithAttribute( fieldName attribute) {
		return "MATCH (p:" + Constants.labelName.Person + " ) " +
				"WHERE EXISTS( p." + attribute + ") " +
				"RETURN count( p) as count";
	}
	
	// number of relations (canInfect)
	public static String numbNodesWithCanInfects() {
		return "MATCH (n)-[:"+Constants.relType.CanInfect+"]->() " +
			   "RETURN count (distinct n) as count";
	}
	public static String numbPersonsWithCanInfects() {
		return "MATCH (p:" + Constants.labelName.Person + ")-[:"+Constants.relType.CanInfect+"]->() " +
			   "RETURN count( distinct p) as count";
	}
	
	
	
	
	/*-----------------------------------------------------------------------------
	/*
	/* set labels depending on status
	/* 
	/*-----------------------------------------------------------------------------
	 */		
	/** set node labelVar to all :Persons who are healthy */
	public static String setPersonsToHealthy() {
		String cypherQ =
			"MATCH (p:" + Constants.labelName.Person + ") " +
			"WHERE p." + Constants.fieldName.dayOfInfection + " = 0 " +
			"SET p:" + Constants.labelNameVar.Healthy.toString();
		return cypherQ;
	}
	
	/** set node labelVar to all :Persons who are in incubation period */
	public static String setPersonsToInIncubation() {
		return
			"MATCH (p:" + Constants.labelName.Person + ") " + 
			"WHERE (p." + Constants.fieldName.dayOfInfection + " > 0) " +
				"AND (p." + Constants.fieldName.dayOfInfection + " <= $day) " +
				"AND ($day <= " +
					"p." + Constants.fieldName.dayOfInfection + " + p." + Constants.fieldName.incubationPeriod + ")" +
			"SET p: " + Constants.labelNameVar.InIncubation; 
	}
	
	/** set node labelVar to all :Persons who are ill */
	public static String setPersonsToIll() {
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
	
	/** set node labelVar to all :Persons who are immune */
	public static String setPersonsToImmune() {
		return
			"MATCH (p:" + Constants.labelName.Person + ") " +
			"WHERE p." + Constants.fieldName.dayOfInfection + " > 0 " + 
			" AND  $day > p." + Constants.fieldName.dayOfInfection + 
					  " + p." + Constants.fieldName.incubationPeriod + 
					  " + p." + Constants.fieldName.illnessPeriod + 
			" SET p: " + Constants.labelNameVar.Immune.toString();
	}

	/** add relation :CanInfect between two persons */
	public static String addCanInfect() {		
		String cypherQ = "MATCH (p), (q) " +
			"WHERE id(p) = $id1 AND id(q) = $id2 " +
			"CREATE (p)-[:" + Constants.relType.CanInfect + 
		     	" {" + Constants.relAttribute.distance + ":$distance}]->(q)";

		return cypherQ;

	}

	/** set biometrics for a :Person */
	public static String setBiometricsForOnePerson() {
		return "MATCH (n:" + Constants.labelName.Person + ") " +
				"WHERE id(n) = $id " +
				"SET n." + Constants.fieldName.dayOfInfection + "= 0," +
					" n." + Constants.fieldName.incubationPeriod + "= $incubationPeriod," +
					" n." + Constants.fieldName.illnessPeriod + "= $illnessPeriod";
	}

	/** remove a variable label from a :Person */
	public static String removeAllVariableLabelsFromAllPersons( labelNameVar labelName) {
		return "MATCH (p:" + Constants.labelName.Person + ") " +
				"REMOVE p:" + labelName;
	}
	
	/** remove label :Person from all nodes  */
	public static String removeLabelPersonFromAllNodes() {
		return "MATCH (p) " +
				"REMOVE p:" + Constants.labelName.Person;
	}
}
	
	

