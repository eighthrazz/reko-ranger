package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.razz.aws.reko.swing.service.RekoService;
import com.razz.common.mongo.model.VideoDO;

public class VideoPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -276654446537198078L;

	private final RekoService rekoService;
	private final List<PropertyChangeListener> pclList;
	
	private TableModel tableModel;
	private JTable table;
	private JButton discoverBTN, refreshBTN;
	
	public VideoPanel(RekoService rekoService) {
		this.rekoService = rekoService;
		pclList = new ArrayList<>();
		
		initComponents();
		
		table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				final VideoDO getSelectedVideo = getSelectedVideo();
				if(getSelectedVideo != null) {
					for(PropertyChangeListener pcl : pclList) {
						pcl.propertyChange( new PropertyChangeEvent(null, null, null, getSelectedVideo) );
					}
				}
			}
		});
	}
	
	private void initComponents() {
		tableModel = new TableModel();
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		refreshBTN = new JButton("Refresh");
		refreshBTN.addActionListener(this);
		
		discoverBTN = new JButton("Discover");
		discoverBTN.addActionListener(this);
		
		final JPanel btnPNL = new JPanel( new FlowLayout(FlowLayout.RIGHT) );
		btnPNL.add(discoverBTN);
		btnPNL.add(refreshBTN);
		
		final JPanel tblPNL = new JPanel( new BorderLayout() );
		tblPNL.add( new JScrollPane(table), BorderLayout.CENTER );
		
		setLayout( new BorderLayout() );
		add(tblPNL, BorderLayout.CENTER);
		add(btnPNL, BorderLayout.SOUTH);
		
		setBorder( BorderFactory.createTitledBorder("Local Videos") );
	}
	
	public void addTableSelectionListener(PropertyChangeListener pcl) {
		pclList.add(pcl);
	}
	
	public VideoDO getSelectedVideo() {
		final int selectedRow = table.getSelectedRow();
		if(selectedRow >= 0) {
			final VideoDO videoDO = tableModel.get(selectedRow);
			return videoDO;
		}
		return null;
	}
	
	public void refresh() {
		refreshBTN.setEnabled(false);
		final Runnable runnable = new Runnable() {
			public void run() {
				final List<VideoDO> videoList = new ArrayList<>();
				try {
					videoList.addAll( rekoService.getVideoList() );
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							tableModel.set(videoList);
							refreshBTN.setEnabled(true);
						}
					});
				}
			}
		};
		new Thread(runnable).start();
	}
	
	private void discover() {
		discoverBTN.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				try {
					rekoService.discoverNewVideos();
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							discoverBTN.setEnabled(true);
						}
					});
					refresh();
				}
			}
		}).start();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == refreshBTN ) {
			refresh();
		} else if( e.getSource() == discoverBTN ) {
			discover();
		}
	}
	
	class TableModel extends AbstractTableModel {

		private static final long serialVersionUID = 6885383098798741282L;
		
		private final String[] columns = {"Key"};
		private final List<VideoDO> vidList;
		
		TableModel() {
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
					return vidList.get(rowIndex).getKey();
				default :
					return "unknown column index";
			}
		}
	}

}
