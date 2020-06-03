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
		
		final int panelWidth = 1200, panelHeight = 600;
		setSize(new Dimension( panelWidth + 10, panelHeight + 10));
		
		this.setLocation( 50, 50);
		
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
			
				int w = (int) Math.floor( (double) panelWidth / statistics.size() / 1.0);
				
				for( int day : statistics.keySet()) {
					int incubation = statistics.get( day).getNumbPersonsInIncubation();
					int ill = statistics.get( day).getNumbPersonsIll();
					int immune = statistics.get( day).getNumbPersonsImmune();
					int all = statistics.get( day).getNumbPersonsHealthy() + incubation + ill + immune;
					
					int immuneHeight = (int) (((double) immune / all) * (panelHeight-20));
					int illHeight = (int) (((double) ill / all) * (panelHeight-20));
					int incubationHeight = (int) (((double) incubation / all) * (panelHeight-20));
					//Utils.logging( incubationHeight + " " + illHeight + " " + immuneHeight + " " + panelHeight);
					
					g2.setColor(Color.red);
					g2.fillRect( w*day + 10, panelHeight - illHeight - immuneHeight - incubationHeight - 20, 
						(int) (w*0.8), incubationHeight);
					
					g2.setColor(Color.black);
					g2.fillRect( w*day + 10, panelHeight -immuneHeight - illHeight - 20,
						(int) (w*0.8), illHeight);
					
					g2.setColor(Color.green);
					g2.fillRect( w*day + 10, panelHeight - immuneHeight - 20,
						(int) (w*0.8), immuneHeight);
				}
			}
		};
		
		setTitle( "in sequence");
		this.getContentPane().add( jpanel);
		this.getContentPane().setBackground( Color.white);
		this.repaint();
	}
}
	
	

