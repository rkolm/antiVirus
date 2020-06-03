package at.nsdb.nv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PanelPersons extends JFrame {
	private static final long serialVersionUID = 1L;
	private static PanelPersons instance = null;
	private Neo4j neo4j;
	
	// Vectors with id's of all persons with new status
	Vector<Integer> newInIncubation, newIll, newImmune; 

	// save a PaintType (xPos, yPos, status) Variable for each user
	private static HashMap<Integer, PaintType> paintTypeMap;
	int day;
	HashMap<Integer, StatisticADay> statistics;
	
	final static int panelWidth = 1200, panelHeight = 600;
	
	
	final JPanel jpanel = new JPanel() {
		private static final long serialVersionUID = 1L;
	
		@Override
		public void paintComponent( Graphics g) {
			paintPersons(g);
		}
	};


	private PanelPersons( Neo4j neo4j){
		this.neo4j = neo4j;
		
		// initialize xPos, yPos, status for each user 
		setLongLatToMap();
		
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);		
		this.setVisible( true);
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.setBackground( Color.white);
		 		
		this.repaint();				
		setSize(new Dimension( panelWidth, panelHeight));
	}

	public static PanelPersons getInstance( Neo4j neo4j) {
		if (instance == null) {
			instance = new PanelPersons( neo4j);
		}
		return instance;		
	}
	
	// initialize xPos, yPos, status for each user 
	private void setLongLatToMap() {
		Persons persons = neo4j.getAllPersons();
		paintTypeMap = new HashMap<Integer, PaintType>();
		for( Person p: persons.getAllPersons()) {
			paintTypeMap.put( p.getId(),  new PaintType(
				(int) ((double) (p.getLongitude() - persons.getMinLongitude()) / 
					( persons.getMaxLongitude()-persons.getMinLongitude()) * (panelWidth*0.7-2) + 1.0), 
				(int) ((double) (p.getLatitude() - persons.getMinLatitude()) / 
					( persons.getMaxLatitude()-persons.getMinLatitude()) * (panelHeight-2) + 1.0),
				Person.status.healthy));	
		}
	}

	private void paintPersons(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		if (paintTypeMap == null) return;
		
		// print all stati of all persons
		paintTypeMap.forEach( (id, paintType) -> {
			if( paintType.getStatus() == Person.status.inIncubation) g2.setColor( Color.red);
			else if( paintType.getStatus() == Person.status.ill) g2.setColor( Color.black);
			else if( paintType.getStatus() == Person.status.immune) g2.setColor( Color.green);
			else g2.setColor( Color.yellow);
			g2.drawRect( paintType.getXPos(), paintType.getYPos(), 1, 1);
		});
		
		
		statistics.forEach( (day, statisticADay) -> {
			if( (day >= 10) && (Math.floorMod( day, 2) == 0)) {
				g2.setColor( Color.black);
				g2.drawString( String.format( "%d: R=%5.2f Q=%5.1f%%", 
						day,  statisticADay.getR(), statisticADay.getQ()*100), 
					(int)(panelWidth * 0.71), (day-9) * 6 + 12);
				
				g2.setColor( Color.orange);
				g2.fillRect( 
					(int)(panelWidth * 0.82), (day-9) * 6, 
					(int)( 0.18 * (panelWidth-1) * statisticADay.getQ()), 10);
			}
		});
	}

	public void paintPanelPerson(int day, HashMap<Integer, StatisticADay> statistics) {
		this.day = day;
		this.statistics = statistics;
		// get all ids with the new Status inIncubation
		newInIncubation = neo4j.getIdsFromPersonsWithNewStatus( Person.status.inIncubation, day);
		newInIncubation.forEach( id -> paintTypeMap.get( id).setStatus( Person.status.inIncubation));
		
		// get all ids with the new Status ill
		newIll = neo4j.getIdsFromPersonsWithNewStatus( Person.status.ill, day);
		newIll.forEach( id -> paintTypeMap.get( id).setStatus( Person.status.ill));
		
		// get all ids with the new Status immune
		newImmune = neo4j.getIdsFromPersonsWithNewStatus( Person.status.immune, day);
		newImmune.forEach( id -> paintTypeMap.get( id).setStatus( Person.status.immune));
		
		StatisticADay statisticADay = statistics.get( day);
		statisticADay.setNewNumbPersonsInIncubation( newInIncubation.size());
		statisticADay.setNewNumbPersonsIll( newIll.size());
		statisticADay.setNewNumbPersonsImmune( newImmune.size());
		
		setTitle( Neo4j.getStatusPersons( day, statisticADay));
		
		this.getContentPane().validate();		
		this.repaint();
	}
}
	
	

