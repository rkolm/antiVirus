package at.nsdb.nv.db;

import static at.nsdb.nv.utils.Constants.labelName.Person;
import static at.nsdb.nv.utils.Constants.labelNameVar.Healthy;
import static at.nsdb.nv.utils.Constants.labelNameVar.Ill;
import static at.nsdb.nv.utils.Constants.labelNameVar.InIncubation;
import static at.nsdb.nv.utils.Constants.labelNameVar.Immune;
import static at.nsdb.nv.utils.Constants.fieldName.dayOfInfection;
import static at.nsdb.nv.utils.Constants.fieldName.illnessPeriod;
import static at.nsdb.nv.utils.Constants.fieldName.incubationPeriod;
import static at.nsdb.nv.utils.Constants.fieldName.id;
import static at.nsdb.nv.utils.Constants.relType.CanInfect;
import static at.nsdb.nv.utils.Constants.relTypeVar.HasInfected;
import static at.nsdb.nv.utils.Constants.relAttribute.distance;
import static at.nsdb.nv.utils.Constants.relAttribute.day;

import at.nsdb.nv.utils.Config;
import at.nsdb.nv.utils.Constants.fieldName;
import at.nsdb.nv.utils.Constants.labelNameVar;
import at.nsdb.nv.utils.Constants.relType;
import at.nsdb.nv.utils.Constants.relTypeVar;

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
		return "CREATE CONSTRAINT ON (p:"+Person+") " +
			   "ASSERT p."+id+" IS UNIQUE";
	}
	
	/*-----------------------------------------------------------------------------
	/*
	/* initialization
	/* 
	/*-----------------------------------------------------------------------------
	 */	

	public static String numbNodesWithFilter() {
		return	"MATCH (p) " +
				"WHERE " + Config.getPersonFilter() + " " +
				"RETURN count( p) as count";
	}
	public static String numbPersonsWithFilter() {
		return	"MATCH (p:" + Person + ") " +
				"WHERE " + Config.getPersonFilter() + " " +
				"RETURN count( p) as count";
	}
	// set label Person to all nodes of active city
	public static String setLabelPersonToChoosenNodes() {
		String cypherQ =
			"MATCH (p) " +
			"WHERE " + Config.getPersonFilter() + " " +
			"SET p:" + Person;
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
			   "REMOVE p."+illnessPeriod+", " +
					  "p."+incubationPeriod+", " +
					  "p."+dayOfInfection;
	}
	
	/** add biometric attributes to all persons */ 
	public static String addBiometricAttributesToPersons() {
		return "MATCH( p:"+Person+") " +
			   "SET p."+illnessPeriod+" = 0," +
				  " p."+incubationPeriod+" = 0," +
				  " p."+dayOfInfection+" = 0";
	}
	
	
	// set all persons to healthy (set dayOfInfection = 0)
	public static String setAllPersonsToHealthy() {
		return "MATCH( n:"+Person+" ) " +
			   "SET n."+dayOfInfection+"=0";
	}
	
	// infect a person
	public static String infectAPerson() {
		return "MATCH( n:"+Person+") " +
			   "WHERE id(n) = $id " +
			   "SET n."+dayOfInfection+"= $day";
	}
	
	/** index for Person-attribute */	
	public static String createIndex(String attribute) {
		return "CREATE INDEX idx_"+attribute+"" +
			   " FOR (p:"+Person+")" +
			   "  ON (p."+attribute+") ";
	}
	
		
	/** get all persons */
	public static String getAllPersons() {
		return "MATCH (p:" +Person+") " +
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
		return 	"MATCH (p:" + Healthy + ") " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new in incubation period */
	public static String idsFromNewPersonsInIncubation() {
		return	"MATCH (p:" + InIncubation + ") " +
				"WHERE p." + dayOfInfection + " = $day " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new in ill period */
	public static String idsFromNewPersonsIll() {
			return
				"MATCH (p:" + Ill + ") " +
				"WHERE p." + dayOfInfection + 
					" + p." + incubationPeriod + " + 1 = $day " +
				"RETURN id(p) as id";
	}
	/** ids of persons, who are new of immune (after illness) persons */
	public static String idsFromNewPersonsImmune() {
		return "MATCH (p:" + Immune + ") " +
				"WHERE p." + dayOfInfection + 
					" + p." + incubationPeriod +
					" + p." + illnessPeriod + " + 1 = $day " +
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
			"MATCH (p:"+Person+")-[c:"+CanInfect+"]->(q:"+Person+") " +
			"RETURN p, c, q";
	}

	/** get all :HasInfected-relations */
	public static String getAllHasInfectedFromAllPersons() {
		return
			"MATCH (p:"+Person+")-[c:"+HasInfected+"]->(q:"+Person+") " +
			"RETURN p, c, q";
	}
	
	/** get id2 and distance from all persons who can be infected */
	public static String infectPersons() {		
		String cypherQ =
		"MATCH (p)-[c:"+CanInfect+"]->(q) " +	
		"WHERE p:"+Person + " AND q:"+Person+ " " +
		"  AND (q."+dayOfInfection+" = 0) " +
		"  AND (p."+dayOfInfection+" > 0) " +
		"  AND (p."+dayOfInfection+" <= $day) " +
		"  AND ($day <= p."+dayOfInfection+" + p."+incubationPeriod+") " +
		"WITH p, q, c."+distance+" AS dist, rand() AS r, rand() AS r1, rand() AS r2 " +
		"WHERE (r2 < $quote) AND (r1 < $accept) " +
			"AND ((r < 0.002) OR (r < exp( -0.1 * dist / 1000))) " +
		"set q."+dayOfInfection+" = $day " +
		"CREATE (p)-[:"+HasInfected+ " {" +day+":$day}]->(q)";
		
		return cypherQ;
	}
	
	
	/**
	 * CypherQuery to remove all variable relations between 2 nodes
	 */	
	public static String removeAllHasInfectedFromAllNodes( relTypeVar relType) {
		return "MATCH ()-[m:"+relType+"]->() DELETE m";
	}
	/**
	 * CypherQuery to remove canInfect relations between 2 persons
	 */	
	public static String removeAllCanInfectFromAllNodes( relType relType) {
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
		return	"MATCH (p:"+Person+") " +
				"RETURN count(p) as count";
	}
	public static String numbPersonsWithLabelNameVar(labelNameVar labelNameVar) {
//		return	"MATCH (p:"+Person+":"+ labelNameVar +") " +
		return	"MATCH (p) " +
				"WHERE (p:"+Person+") AND (p:"+labelNameVar+") " +
				"RETURN count(p) as count";
	}
	
	// number of immune (after illness) persons
	public static String numbPersonsWithAttribute( fieldName attribute) {
		return "MATCH (p:" + Person + " ) " +
				"WHERE EXISTS( p." + attribute + ") " +
				"RETURN count( p) as count";
	}
	
	// number of relations (canInfect)
	public static String numbNodesWithCanInfects() {
		return "MATCH (n)-[:"+CanInfect+"]->() " +
			   "RETURN count (distinct n) as count";
	}
	public static String numbPersonsWithCanInfects() {
		return "MATCH (p:" + Person + ")-[:"+CanInfect+"]->() " +
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
			"MATCH (p:" + Person + ") " +
			"WHERE p." + dayOfInfection + " = 0 " +
			"SET p:" + Healthy;
		return cypherQ;
	}
	
	/** set node labelVar to all :Persons who are in incubation period */
	public static String setPersonsToInIncubation() {
		return
			"MATCH (p:" + Person + ") " + 
			"WHERE (p." + dayOfInfection + " > 0) " +
				"AND (p." + dayOfInfection + " <= $day) " +
				"AND ($day <= " +
					"p." + dayOfInfection + " + p." + incubationPeriod + ")" +
			"SET p: " + InIncubation; 
	}
	
	/** set node labelVar to all :Persons who are ill */
	public static String setPersonsToIll() {
		return	
			"MATCH (p:"+Person+ ") " +
			"WHERE (p."+dayOfInfection+ " > 0) " + 
			"AND (p."+dayOfInfection+ " + p."+incubationPeriod+" < $day) " +
			"AND ($day <= " + "p."+dayOfInfection+" + p."+incubationPeriod+" + p." +illnessPeriod+") " +
			"SET p:"+Ill;
	}
	
	/** set node labelVar to all :Persons who are immune */
	public static String setPersonsToImmune() {
		return
			"MATCH (p:" + Person + ") " +
			"WHERE p." + dayOfInfection + " > 0 " + 
			" AND  $day > p." + dayOfInfection + 
					  " + p." + incubationPeriod + 
					  " + p." + illnessPeriod + 
			" SET p: " + Immune;
	}

	/** add relation :CanInfect between two persons */
	public static String addCanInfect() {		
		String cypherQ = "MATCH (p), (q) " +
			"WHERE id(p) = $id1 AND id(q) = $id2 " +
			"CREATE (p)-[:"+CanInfect+ 	" {" +distance+":$distance}]->(q)";

		return cypherQ;

	}

	/** set biometrics for a :Person */
	public static String setBiometricsForOnePerson() {
		return "MATCH (n:" + Person + ") " +
				"WHERE id(n) = $id " +
				"SET n." + dayOfInfection + "= 0," +
					" n." + incubationPeriod + "= $incubationPeriod," +
					" n." + illnessPeriod + "= $illnessPeriod";
	}

	/** remove a variable label from a :Person */
	public static String removeAllVariableLabelsFromAllPersons( labelNameVar labelName) {
		return "MATCH (p:" + Person + ") " +
				"REMOVE p:" + labelName;
	}
	
	/** remove label :Person from all nodes  */
	public static String removeLabelPersonFromAllNodes() {
		return "MATCH (p) " +
				"REMOVE p:" + Person;
	}

	/** get longest infection path */
	public static String getLongestInfectionPath() {
		return "MATCH (p:" + Person + ") " +
		        "WHERE p."+dayOfInfection+ " > 0 " + 
				"WITH min(p."+dayOfInfection+") as firstDay, max(p."+dayOfInfection+") as lastDay " +
				"MATCH path = (startNode:"+Person+")-[:"+ HasInfected+"*]->(endNode:"+Person+") " +
				"WHERE startNode."+dayOfInfection+" = firstDay " +
				  "AND endNode."+dayOfInfection+" = lastDay " +				  
				"RETURN path " +
				"ORDER BY LENGTH(path) " +
				"LIMIT 1";
	}
}
	
	

