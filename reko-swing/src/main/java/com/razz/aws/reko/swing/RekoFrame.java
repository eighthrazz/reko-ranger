package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.amazonaws.services.rekognition.model.FaceDetection;
import com.razz.aws.reko.swing.service.RekoService;
import com.razz.common.mongo.model.VideoDO;

public class RekoFrame extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 2158003710840511604L;
	
	private final RekoService rekoService;
	private VideoPanel vidPNL;
	private AwsBucketPanel awsBucketPNL;
	private ImagePanel imagePNL;
	private JButton trimBTN;
	
	public RekoFrame() {
		rekoService = new RekoService();
		initComponents();
	}
	
	private void initComponents() {
		vidPNL = new VideoPanel(rekoService);
		awsBucketPNL = new AwsBucketPanel(rekoService);
		imagePNL = new ImagePanel();
		
		trimBTN = new JButton("Local/Trim/Bucket/Detect");
		trimBTN.addActionListener(this);
		
		final JPanel rightBtnPNL = new JPanel( new FlowLayout(FlowLayout.RIGHT) );
		rightBtnPNL.add(trimBTN);
		
		final JPanel btnPNL = new JPanel( new BorderLayout() );
		btnPNL.add(rightBtnPNL, BorderLayout.EAST);
		
		final JPanel appPNL = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		appPNL.add(vidPNL);
		appPNL.add(awsBucketPNL);
		appPNL.add(imagePNL);
		
		setLayout( new BorderLayout() );
		add(appPNL, BorderLayout.CENTER);
		add(btnPNL, BorderLayout.SOUTH);
		
		pack();
		setResizable(true);
		setLocationRelativeTo(null);
	}
	
	private void localTrimBucket() {
		trimBTN.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				try {
					final VideoDO videoDO = vidPNL.getSelectedVideo();
					if(videoDO != null) {
						final String ftpPath = videoDO.getPath();
						final File localFile = copyToLocal(ftpPath);
						final File trimFile = trim(localFile);
						final String keyName = addToBucket(trimFile);
						final List<FaceDetection> faceList = detectFaces(keyName);
						updateFaceList(videoDO, faceList);
					}
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							trimBTN.setEnabled(true);
							vidPNL.refresh();
							awsBucketPNL.refresh();
						}
					});
				}
			}
		}).start();
	}
	
	private File copyToLocal(String ftpPath) throws Exception {
		final File localFile = rekoService.copyToLocal(ftpPath);
		System.out.format("copyToLocal: %s%n", localFile);
		return localFile;
	}
	
	private File trim(File file) throws Exception {
		final File trimFile = rekoService.trim(file);
		System.out.format("trim: %s%n", trimFile);
		return trimFile;
	}
	
	private String addToBucket(File file) {
		final String keyName = rekoService.saveToAwsBucket(file);
		System.out.format("saveToAwsBucket: keyName=%s%n", keyName);
		return keyName;
	}
	
	private List<FaceDetection> detectFaces(String keyName) {
		final List<FaceDetection> faceList = rekoService.detectFaces(keyName);
		System.out.format("detectFaces: faceList.size=%s%n", faceList.size());
		return faceList;
	}
	
	private void updateFaceList(VideoDO videoDO, List<FaceDetection> faceList) throws Exception {
		videoDO.setFaceList(faceList);
		rekoService.update(videoDO);
		System.out.format("updateFaceList: videoDO=%s%n", videoDO);
	}
	
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == trimBTN ) {
			localTrimBucket();
		} 
	}
	
}
