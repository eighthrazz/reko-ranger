package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.razz.aws.reko.swing.service.RekoService;

public class AwsBucketPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -276654446537198078L;

	private final RekoService rekoService;
	
	private TableModel tableModel;
	private JTable table;
	private JButton refreshBTN, deleteBTN;
	
	public AwsBucketPanel(RekoService rekoService) {
		this.rekoService = rekoService;
		initComponents();
	}
	
	private void initComponents() {
		tableModel = new TableModel();
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		
		refreshBTN = new JButton("Refresh");
		refreshBTN.addActionListener(this);
		
		deleteBTN = new JButton("Delete");
		deleteBTN.addActionListener(this);
		
		final JPanel btnPNL = new JPanel( new FlowLayout(FlowLayout.RIGHT) );
		btnPNL.add(refreshBTN);
		btnPNL.add(deleteBTN);
		
		final JPanel tblPNL = new JPanel( new BorderLayout() );
		tblPNL.add( new JScrollPane(table), BorderLayout.CENTER );
		
		setLayout( new BorderLayout() );
		add(tblPNL, BorderLayout.CENTER);
		add(btnPNL, BorderLayout.SOUTH);
		
		setBorder( BorderFactory.createTitledBorder("AWS Bucket") );
	}
	
	public String getSelectedKeyName() {
		final int selectedRow = table.getSelectedRow();
		if(selectedRow >= 0) {
			final S3ObjectSummary row = tableModel.get(selectedRow);
			return row.getKey();
		}
		return null;
	}
	
	public void refresh() {
		refreshBTN.setEnabled(false);
		final Runnable runnable = new Runnable() {
			public void run() {
				final List<S3ObjectSummary> list = rekoService.listAwsBucket();
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						tableModel.set(list);
						refreshBTN.setEnabled(true);
					}
				});
			}
		};
		new Thread(runnable).start();
	}
	
	private void delete() {
		deleteBTN.setEnabled(false);
		final String keyName = getSelectedKeyName();
		final Runnable runnable = new Runnable() {
			public void run() {
				if(keyName != null) {
					rekoService.delete(keyName);
				}
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						deleteBTN.setEnabled(true);
						refresh();
					}
				});
			}
		};
		new Thread(runnable).start();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == refreshBTN ) {
			refresh();
		} else if( e.getSource() == deleteBTN ) {
			delete();
		} 
	}
	
	class TableModel extends AbstractTableModel {

		private static final long serialVersionUID = 6885383098798741282L;
		
		private final String[] columns = {"Key"};
		private final List<S3ObjectSummary> list;
		
		TableModel() {
			list = new ArrayList<>();
		}
		
		void set(List<S3ObjectSummary> newList) {
			list.clear();
			list.addAll(newList);
			fireTableDataChanged();
		}
		
		S3ObjectSummary get(int row) {
			return list.get(row);
		}
		
		@Override
		public int getRowCount() {
			return list.size();
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
					return list.get(rowIndex).getKey();
				default :
					return "unknown column index";
			}
		}
	}

}
