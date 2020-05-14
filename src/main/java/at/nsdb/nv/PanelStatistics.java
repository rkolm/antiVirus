package at.nsdb.nv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PanelStatistics extends JFrame {
	private static final long serialVersionUID = 1L;

	public PanelStatistics( HashMap<Integer, StatisticADay> statistics) {
		
		final int panelWidth = 800, panelHeight = 500;
		setSize(new Dimension( panelWidth + 10, panelHeight + 10));
		
		this.setLocation( 10, 10);
		
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		this.setBackground( Color.white);
		this.setVisible( true);
		this.repaint();
		
		JPanel jpanel = new JPanel() {
			private static final long serialVersionUID = 1L;
		
			@Override
			public void paintComponent( Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				this.setBackground( Color.white);
			
				int w = (int) Math.floor( (double) panelWidth / statistics.size());
				
				for( int day : statistics.keySet()) {
					int incubation = statistics.get( day).getNumbPersonsInIncubation();
					int ill = statistics.get( day).getNumbPersonsIll();
					int immune = statistics.get( day).getNumbPersonsImmune();
					
					g2.setColor(Color.red);
					g2.fillRect( w*day + 10, panelHeight - (incubation + ill + immune) /10, (int) (w*0.8), incubation);
					g2.setColor(Color.black);
					g2.fillRect( w*day + 10, panelHeight - (ill + immune) /10, (int) (w*0.8), ill);
					g2.setColor(Color.green);
					g2.fillRect( w*day + 10, panelHeight - immune /10, (int) (w*0.8), immune);
				}
			}
		};
		
		setTitle( "in sequence");
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.repaint();
	}
}
	
	

