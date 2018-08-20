package com.razz.aws.reko.swing;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

	public static void main(String args[]) {
		final RekoFrame appFrame = new RekoFrame();
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				appFrame.setVisible(true);
			}
		});
	}
	
}
