package at.nsdb.nv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PanelPersons extends JFrame {
	private static final long serialVersionUID = 1L;
	private static PanelPersons instance = null;
	// max/min locations (longitude, latitude) of persons
	int minLong=Integer.MAX_VALUE, maxLong=0, minLat=Integer.MAX_VALUE, maxLat=0;
	private int day;
	private Persons persons;
	final int panelWidth = 362, panelHeight = 362;
	
	final JPanel jpanel = new JPanel() {
		private static final long serialVersionUID = 1L;
	
		@Override
		public void paintComponent( Graphics g) {
			paintPersons(g);
		}
	};


	private PanelPersons( ) {		
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);		
		this.setVisible( true);
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.setBackground( Color.white);
		this.repaint();				
		setSize(new Dimension( panelWidth, panelHeight));
	}

	public static PanelPersons getInstance() {
		if (instance == null) {
			instance = new PanelPersons();
		}
		return instance;		
	}

	private void paintPersons(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		if (persons == null) {
			return;
		}
				
		for( Person p : persons.getAllPersons()) {					
			g2.setColor(Color.white);
			if( p.getStatus( day) == Person.status.healthy) g2.setColor( Color.yellow);
			if( p.getStatus( day) == Person.status.inIncubation) g2.setColor( Color.red);
			if( p.getStatus( day) == Person.status.ill) g2.setColor( Color.black);
			if( p.getStatus( day) == Person.status.immune) g2.setColor( Color.green);
			
			int w = (int) ((double)( p.getLongitude()-persons.getMinLongitude()) / 
				( persons.getMaxLongitude()-persons.getMinLongitude()) * (panelWidth-2) + 1.0);
			int h = (int) ((double) (p.getLatitude()-persons.getMinLatitude()) / 
				( persons.getMaxLatitude()-persons.getMinLatitude()) * (panelHeight-2) + 1.0);
			g2.drawRect( w, h, 1, 1);
			//Utils.logging( String.format( "l=%d h=%d", w, h));
		};
	}

	public void paintPanelPerson(int day, HashMap<Integer, StatisticADay> statistics, Persons persons) {

		this.day = day;
		this.persons = persons;
		
		StatisticADay statisticADay = new StatisticADay();
		statisticADay = statistics.get( day);
		
		// 3 panels in one row, 2 rows
		//this.setLocation( (day % 3) * (panelWidth + 60) + 10 + Math.floorDiv( day, 30) * 20, 
		//		(Math.floorDiv( day, 3) % 10) * 25+10);

		setTitle( String.format( "d=%d    %d / %d / %d / %d", 
			day, statisticADay.getNumbPersonsHealthy(), statisticADay.getNumbPersonsInIncubation(),
			statisticADay.getNumbPersonsIll(), statisticADay.getNumbPersonsImmune()));
		this.getContentPane().validate();		
		this.repaint();
	}

}
	
	

