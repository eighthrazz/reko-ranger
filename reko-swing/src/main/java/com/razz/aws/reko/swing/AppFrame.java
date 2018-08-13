package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.razz.common.mongo.model.VideoDO;

public class AppFrame extends JFrame implements ActionListener {
	
	private final App app;
	private VideoTableModel videoTableModel;
	private JTable videoTable;
	private JButton updateVideoBtn, refreshBTN, trimBTN;
	
	public AppFrame() {
		app = new App();
		initComponents();
	}
	
	private void initComponents() {
		videoTableModel = new VideoTableModel();
		videoTable = new JTable(videoTableModel);
		
		updateVideoBtn = new JButton("Update Video Table");
		updateVideoBtn.addActionListener(this);
		
		refreshBTN = new JButton("1. Refresh");
		refreshBTN.addActionListener(this);
		
		trimBTN = new JButton("2. Trim");
		trimBTN.addActionListener(this);
		
		final JPanel leftBtnPNL = new JPanel( new FlowLayout(FlowLayout.LEFT) );
		leftBtnPNL.add(updateVideoBtn);

		final JPanel rightBtnPNL = new JPanel( new FlowLayout(FlowLayout.RIGHT) );
		rightBtnPNL.add(refreshBTN);
		rightBtnPNL.add(trimBTN);
		
		final JPanel btnPNL = new JPanel( new BorderLayout() );
		btnPNL.add(leftBtnPNL, BorderLayout.WEST);
		btnPNL.add(rightBtnPNL, BorderLayout.EAST);
		
		final JPanel tblPNL = new JPanel( new BorderLayout() );
		tblPNL.setBorder( BorderFactory.createTitledBorder("Videos") );
		tblPNL.add( new JScrollPane(videoTable) );
		
		setLayout( new BorderLayout() );
		add(tblPNL, BorderLayout.CENTER);
		add(btnPNL, BorderLayout.SOUTH);
		
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
	
	private void refresh() {
		refreshBTN.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				final List<VideoDO> videoList = new ArrayList<>();
				try {
					videoList.addAll( app.getVideoList() );
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							videoTableModel.set(videoList);
							refreshBTN.setEnabled(true);
						}
					});
				}
			}
		}).start();
	}
	
	private void copyToLocal() {
		trimBTN.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				try {
					final int selectedRow = videoTable.getSelectedRow();
					if(selectedRow >= 0) {
						final VideoDO videoDO = videoTableModel.get(selectedRow);
						final File localFile = app.copyToLocal( videoDO.getPath() );
						System.out.format("localFile=%s%n", localFile);
						final File trimFile = app.trim(localFile);
						System.out.format("trimFile=%s%n", trimFile);
					}
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							trimBTN.setEnabled(true);
						}
					});
				}
			}
		}).start();
	}
	
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == updateVideoBtn ) {
			updateVideo();
		} else if( e.getSource() == refreshBTN ) {
			refresh();
		} else if( e.getSource() == trimBTN ) {
			copyToLocal();
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

	class VideoTableModel extends AbstractTableModel {

		private final String[] columns = {"Path"};
		private final List<VideoDO> vidList;
		
		VideoTableModel() {
			vidList = new ArrayList<>();
		}
		
		void set(List<VideoDO> newVidList) {
			vidList.clear();
			vidList.addAll(newVidList);
			fireTableDataChanged();
		}
		
		VideoDO get(int row) {
			return vidList.get(row);
		}
		
		@Override
		public int getRowCount() {
			return vidList.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch(columnIndex) {
				case 0:
					return vidList.get(rowIndex).getPath();
				default :
					return "unknown column index";
			}
		}
		
	}
	
}
