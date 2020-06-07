package at.nsdb.nv;

import java.util.HashMap;

import org.neo4j.driver.Session;

import static org.neo4j.driver.Values.parameters;

import at.nsdb.nv.model.Persons;
import at.nsdb.nv.model.StatisticADay;
import at.nsdb.nv.utils.Cypher;
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
        
        Utils.logging("checking constraints & indexes");
		//neo4j.setConstraint();
        neo4j.setIndexForPerson( );

        Utils.logging("set all persons healthy");
        neo4j.setAllPersonsToHealthy();
        
        Utils.logging("remove all hasInfected-relations ");
        neo4j.removeAllHasInfectedRelations();

		Utils.logging("add biometric attributes to node");
        neo4j.setBiometricsForAllPersons();
        
		Utils.logging("create relations with distance");
        neo4j.setCanInfectRelationsForAllPersons();
        
		Utils.logging( "---- initialization finished");
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
				neo4j.deleteAllVarLabels( tx);
				
				// 1. person
				long id = persons.getPersonRandomly().getId();
				tx.run(Cypher.infectAPerson(), parameters("id", id, "day", day));
				Utils.logging( "id " + id + " infected on day 1");

				// 2. person
				id = persons.getPersonRandomly().getId();
				tx.run(Cypher.infectAPerson(), parameters("id", id, "day", day));
				Utils.logging( "id " + id + " infected on day 1");
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
				tx.run( Cypher.infectPersons(), parameters("day", day, "quote", quote));	
				return 1;
			});
		}
	}

	/** get a new daily statistic */
	private StatisticADay getNewStatisticADay(int day) {
		var statisticADay = new StatisticADay();
		try (Session session = neo4j.getDriver().session()) {
			session.writeTransaction(tx -> {		
				neo4j.deleteAllVarLabels( tx);
				neo4j.setAllVarLabels( day, tx);

				statisticADay.setNumbPersonsHealthy( neo4j.getNumbPersonsHealthy( day, tx));
				statisticADay.setNumbPersonsInIncubation( neo4j.getNumbPersonsInIncubation(tx));
				statisticADay.setNumbPersonsIll( neo4j.getNumbPersonsIll(tx));
				statisticADay.setNumbPersonsImmune( neo4j.getNumbPersonsImmune(tx));
				return 1;
			});
		}
		return statisticADay;
	} 
    
}