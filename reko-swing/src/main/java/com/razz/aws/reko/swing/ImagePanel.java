package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ImagePanel extends JPanel {

	private static final long serialVersionUID = -2520391881823208211L;

	private JPanel contentPNL;
	
	public ImagePanel() {
		initComponents();
	}
	
	private void initComponents() {
		contentPNL = new JPanel( new FlowLayout(FlowLayout.LEFT) );
		
		setLayout( new BorderLayout() );
		add( new JScrollPane(contentPNL), BorderLayout.CENTER );
		
		setBorder( BorderFactory.createTitledBorder("Faces") );
	}
	
	public void addImage(BufferedImage img) {
		final ImageIcon imageIcon = new ImageIcon(img);
		contentPNL.add( new JLabel(imageIcon) );
	}
	
	public void clearImages() {
		contentPNL.removeAll();
		contentPNL.repaint();
	}
	
}
