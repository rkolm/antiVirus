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

	public PanelPersons( int day, HashMap<Integer, StatisticADay> statistics, HashMap<Integer, Person> persons) {
		StatisticADay statisticADay = new StatisticADay();
		statisticADay = statistics.get( day);
		
		// panel size 362x362 means 3 panels every row, 2 rows
		final int panelWidth = 362, panelHeight = 362;
		setSize(new Dimension( panelWidth, panelHeight));
		
		// 3 panels in one row, 2 rows
		this.setLocation( (day % 3) * (panelWidth + 60) + 10 + Math.floorDiv( day, 30) * 20, 
				(Math.floorDiv( day, 3) % 10) * 25+10);
		
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		this.setBackground( Color.white);
		this.setVisible( true);
		this.repaint();
		
		// max/min lokations (longitude, latitude) of persons
		int minLong=Integer.MAX_VALUE, maxLong=0, minLat=Integer.MAX_VALUE, maxLat=0;
		for( int id: persons.keySet()) {
			Person p = persons.get( id);
			minLong = Math.min( minLong, p.getLongitude());
			maxLong = Math.max( maxLong, p.getLongitude());
			minLat = Math.min( minLat, p.getLatitude());
			maxLat = Math.max( maxLat, p.getLatitude());			
		};
		
		
		JPanel jpanel = new JPanel() {
			private static final long serialVersionUID = 1L;
		
			@Override
			public void paintComponent( Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				this.setBackground( Color.white);
				
				for( int id: persons.keySet()) {
					Person p = persons.get( id);
					
					g2.setColor(Color.white);
					if( p.getStatus( day) == Person.status.healthy) g2.setColor(Color.yellow);
					if( p.getStatus( day) == Person.status.inIncubation) g2.setColor(Color.red);
					if( p.getStatus( day) == Person.status.ill) g2.setColor(Color.black);
					if( p.getStatus( day) == Person.status.immune) g2.setColor(Color.green);
					
					g2.drawRect( (p.getLongitude()-46800+1) / 10, 
						(p.getLatitude()-169200+1) / 10, 1, 1);
				};
			}
		};
		
		setTitle( String.format( "d=%d    %d / %d / %d / %d", 
			day, statisticADay.getNumbPersonsHealthy(), statisticADay.getNumbPersonsInIncubation(),
			statisticADay.getNumbPersonsIll(), statisticADay.getNumbPersonsImmune()));
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.repaint();
	}
}
	
	

