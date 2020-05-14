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

	public PanelPersons( int day, HashMap<Integer, Person> persons) {

		setSize(new Dimension( 362, 362));
		this.setLocation( (day % 3) * 420 + 10 + Math.floorDiv( day, 30) * 20, 
				(Math.floorDiv( day, 3) % 10) * 25+10);
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		this.setBackground( Color.white);
		this.setVisible( true);
		this.repaint();
		
		int hl=0, ic=0, il=0, im=0;

		JPanel jpanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			public int hl=0, ic=0, il=0, im=0;
		
			@Override
			public void paintComponent( Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				this.setBackground( Color.white);
				
				hl = ic = il= im=0;
				for( int id: persons.keySet()) {
					Person p = persons.get( id);
					
					g2.setColor(Color.white);
					if( p.getStatus( day) == Person.status.healthy) { g2.setColor(Color.yellow); hl++;}
					if( p.getStatus( day) == Person.status.inIncubation) { g2.setColor(Color.red); ic++;}
					if( p.getStatus( day) == Person.status.ill) { g2.setColor(Color.black); il++;}
					if( p.getStatus( day) == Person.status.immune) { g2.setColor(Color.green); im++;}
					
					g2.drawRect( 
						(p.getLongitude()-46800+1) / 10, 
						(p.getLatitude()-169200+1) / 10, 1, 1);
				};
				Utils.str = String.format( "d=%d    %d / %d / %d / %d", day, hl, ic, il, im);
				
			}
		};
		
		setTitle( Utils.str);
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.repaint();
	}
}
	
	

