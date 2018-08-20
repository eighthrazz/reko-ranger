package com.razz.aws.reko.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceDetection;
import com.razz.aws.reko.swing.service.RekoService;
import com.razz.common.helper.FileHelper;
import com.razz.common.mongo.model.VideoDO;
import com.razz.common.util.media.VideoUtils;

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
		
		vidPNL.addPropertyChangeListener( new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				final VideoDO videoDO = (VideoDO)evt.getNewValue();
				//TODO
			}
		});
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
		
		final JPanel centerPNL = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		centerPNL.add(vidPNL);
		centerPNL.add(awsBucketPNL);
		
		final JPanel southPNL = new JPanel( new BorderLayout() );
		southPNL.add(imagePNL, BorderLayout.CENTER);
		southPNL.add(btnPNL, BorderLayout.SOUTH);
		
		setLayout( new BorderLayout() );
		add(centerPNL, BorderLayout.CENTER);
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
						final String ftpPath = videoDO.getSrcPath();
						final File localFile = copyToLocal(ftpPath);
						final File trimFile = trim(localFile);
						storeTrimVideo(videoDO, trimFile);
						
						final String keyName = addToBucket(trimFile);
						final List<FaceDetection> fdList = rekoService.detectFaces(keyName);
						for(int i=0;i<fdList.size();i++) {
							final FaceDetection faceDetection = fdList.get(i);
							final File mp4File = trimFile;
							final long timestamp = faceDetection.getTimestamp();
							final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
							final BufferedImage buffImg = VideoUtils.mp4ToImage(mp4File, timestamp, timeUnit);
							VideoUtils.writeJpg(buffImg, FileHelper.getTmpFile("jpg"));
							
							final BoundingBox boundingBox = faceDetection.getFace().getBoundingBox();
							final float leftRatio = boundingBox.getLeft();
							final float topRatio = boundingBox.getTop();
							final float widthRatio = boundingBox.getWidth();
							final float heightRatio = boundingBox.getHeight();
							final BufferedImage faceImage = VideoUtils.subImage(buffImg, leftRatio, topRatio, widthRatio, heightRatio);
							VideoUtils.writeJpg(faceImage, FileHelper.getTmpFile("jpg"));
						}
						//updateFaceList(videoDO, faceDetailList);
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
	
	private void storeTrimVideo(VideoDO videoDO, File localFile) throws Exception {
		System.out.format("storeVideo...%n");
		final File remoteFile = rekoService.storeVideo(videoDO, localFile);
		videoDO.setPreviewPath( remoteFile.getPath() );
		rekoService.update(videoDO);
		System.out.format("...remoteFile=%s%n", remoteFile);
	}
	
	private String addToBucket(File file) {
		final String keyName = rekoService.saveToAwsBucket(file);
		System.out.format("saveToAwsBucket: keyName=%s%n", keyName);
		return keyName;
	}
	
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == trimBTN ) {
			localTrimBucket();
		} 
	}
	
}
