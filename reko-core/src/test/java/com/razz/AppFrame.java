package com.razz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class AppFrame extends JFrame implements ActionListener {
	
	private final App app;
	private JButton updateVideoBtn;
	
	public AppFrame() {
		app = new App();
		initComponents();
	}
	
	private void initComponents() {
		updateVideoBtn = new JButton("Update Video Table");
		updateVideoBtn.addActionListener(this);
		
		final JPanel btnPanel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		btnPanel.add(updateVideoBtn);
		
		setLayout( new BorderLayout() );
		add(btnPanel, BorderLayout.CENTER);
		
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
	}
	
	private void updateVideo() {
		updateVideoBtn.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				try {
					app.updateVideo();
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							updateVideoBtn.setEnabled(true);
						}
					});
				}
			}
		}).start();
	}
	
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == updateVideoBtn ) {
			updateVideo();
		}
	}
	
	public static void main(String args[]) {
		final AppFrame appFrame = new AppFrame();
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				appFrame.setVisible(true);
			}
		});
	}

}
