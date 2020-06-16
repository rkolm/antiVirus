package at.nsdb.nv;

import java.util.HashMap;

import org.neo4j.driver.Session;

import static org.neo4j.driver.Values.parameters;
import static at.nsdb.nv.utils.Constants.labelNameVar;

import at.nsdb.nv.db.Cypher;
import at.nsdb.nv.db.Neo4j;
import at.nsdb.nv.model.Persons;
import at.nsdb.nv.model.StatisticADay;
import at.nsdb.nv.utils.Config;
import at.nsdb.nv.utils.Utils;
import at.nsdb.nv.view.PanelPersons;
import at.nsdb.nv.view.PanelStatistics;

/**
 * AntiVirus-Simulation (Singleton)
 */
public final class VirusSimulation {

	private static VirusSimulation instance = null;
	
    private final Neo4j neo4j = Neo4j.getInstance();

    /** 
	 * private constructor 
	 */
    private VirusSimulation() {
        /** --------------------
		 * initializing database, optional, if neo4j structure is up to date
		 */
		Utils.logging( "**** initialization ...");
        
        Utils.logging("checking indexes");
        neo4j.setIndexForPerson( );
        
        // initalize labels, biometrics an relations if necessary
        neo4j.init();
    }

    /** 
	 * get the singleton-instance 
	 */
    static VirusSimulation getInstance() {
        if (instance == null) {
            instance = new VirusSimulation();
        }
        return instance;
    }

	/**
	 * run the simulation 
	 */
    public void run() {
        /**--------------------
		 * calculate the spreading of the virus day by day
		 */		
		int day = 0;
		var statistics = new HashMap<Integer, StatisticADay>();
		do {			
			day++;
			if( day == 1) Utils.logging( "**** Day 1 ...");
			simulateADay(day, statistics);			
			if( day == 1) Utils.logging( "---- Day 1 finished");
			else {
				PanelPersons.getInstance().paintADay( day, statistics);
				Utils.logging(statistics.get(day).getStatusString(day));
			} 
			
			while(!Config.running) Utils.sleepInSec(1);
						
		} while( day <= Config.getStopAfterDay() && statistics.get( day).getNumbPersonsInIncubation() > 0);
		
				
		/**--------------------
		 * last statistics
		 */
		new PanelStatistics( statistics).repaint();

		/** print the longest infection path */
		neo4j.printLongestInfectionPath();
		
		/** export :HasInfected-Relations */
		neo4j.exportHasInfected();
    }

    
    /**
	 * simulate the spreading of the virus for the given day and set statistics
	 */
	public void simulateADay( int day, HashMap<Integer, StatisticADay> statistics) {
				
		if( day == 1) {			
			infectRandomPersons(day);			
		} else {
			infectPersons(day, statistics.get(day-1).getQ());
		}		
		statistics.put( day, getNewStatisticADay(day));	
	}

	/**
	 * infect persons, which are selected randomly
	 * @param day relevant day
	 */
	private void infectRandomPersons(int day) {
		var persons = new Persons( neo4j.readAllPersons());
		Utils.logging( "infect randomly 1 or 2 persons");
		try (Session session = neo4j.getDriver().session()) {
			session.writeTransaction(tx -> {
				neo4j.removeAllVariableLabelsFromAllPersons( tx);
				
				// 1. person
				long id = persons.getPersonRandomly().getId();
				tx.run(Cypher.infectAPerson(), parameters("id", id, "day", day));
				Utils.logging( "id " + id + " infected on day 1");

//				// 2. person
//				id = persons.getPersonRandomly().getId();
//				tx.run(Cypher.infectAPerson(), parameters("id", id, "day", day));
//				Utils.logging( "id " + id + " infected on day 1");
				return 1;
			});
		}
	}

	/**
	 * infect persons, which are selected by quote-calculation
	 * @param day relevant day
	 * @param oldQuote infection quote of previous day
	 */
	private void infectPersons(int day, Double oldQuote) {
		try (Session session = neo4j.getDriver().session()) {
			session.writeTransaction(tx -> {
				double quote = Math.max( Math.min( 1.0, 1-oldQuote), 0.5);
				quote = Math.exp( (quote-1)*8);
				double accepts = Math.max( 0.01, 0.5 - Config.getAcceptCode()/100.0*0.5);
				tx.run( Cypher.infectPersons(), parameters( 
					"day", day, "quote", quote,	"accept", accepts));
				return 1;
			});
		}
	}

	/** get a new daily statistic */
	private StatisticADay getNewStatisticADay(int day) {
		var statisticADay = new StatisticADay();
		try (Session session = neo4j.getDriver().session()) {
			session.writeTransaction(tx -> {		
				neo4j.removeAllVariableLabelsFromAllPersons( tx);
				neo4j.setAllVarLabels( day, tx);

				statisticADay.setNumbPersonsHealthy( neo4j.getNumbPersons(labelNameVar.Healthy, tx));
				statisticADay.setNumbPersonsInIncubation( neo4j.getNumbPersons(labelNameVar.InIncubation, tx));
				statisticADay.setNumbPersonsIll( neo4j.getNumbPersons(labelNameVar.Ill, tx));
				statisticADay.setNumbPersonsImmune( neo4j.getNumbPersons(labelNameVar.Immune, tx));
				return 1;
			});
		}
		return statisticADay;
	} 
    
}