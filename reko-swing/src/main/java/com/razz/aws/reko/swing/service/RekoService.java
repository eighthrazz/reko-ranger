package com.razz.aws.reko.swing.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.services.rekognition.model.FaceDetection;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import com.amazonaws.services.rekognition.model.StartFaceDetectionResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.razz.aws.reko.AwsRekoFace;
import com.razz.aws.reko.AwsRekognition;
import com.razz.common.aws.AwsBucket;
import com.razz.common.aws.AwsConfig;
import com.razz.common.helper.FileHelper;
import com.razz.common.mongo.Mongo;
import com.razz.common.mongo.MongoConfig;
import com.razz.common.mongo.dao.VideoDAO;
import com.razz.common.mongo.model.VideoDO;
import com.razz.common.util.config.ConfigManager;
import com.razz.common.util.ftp.Ftp;
import com.razz.common.util.ftp.FtpConfig;
import com.razz.common.util.media.VideoUtils;

public class RekoService {
	
	final Properties props;
	final FtpConfig ftpConfig;
	final MongoConfig mongoConfig;
	final AwsConfig awsConfig;
	final AtomicReference<AwsBucket> awsBucketRef;
	
	public RekoService() {
		props = ConfigManager.get();
		ftpConfig = new FtpConfig(props);
		mongoConfig = new MongoConfig(props);
		awsConfig = new AwsConfig(props);
		awsBucketRef = new AtomicReference<AwsBucket>( new AwsBucket(awsConfig) );
	}
	
	public void discoverNewVideos() throws Exception {
		try( final Ftp ftp = new Ftp(ftpConfig);
			 final Mongo mongo = new Mongo(mongoConfig)	) 
		{
			final Path ftpPath = Paths.get( props.getProperty("ftp.path") );
			final List<File> fileList = ftp.getFileList(ftpPath);
			for(File file : fileList) {
				final String path = file.getPath();
				final VideoDO video = new VideoDO(path);
				final VideoDAO videoDAO = new VideoDAO( mongo.getDatastore() );
				videoDAO.save(video);
			}
		}
	}
	
	public List<VideoDO> getVideoList() throws Exception {
		try( final Mongo mongo = new Mongo(mongoConfig) ) {
			final VideoDAO videoDAO = new VideoDAO( mongo.getDatastore() );
			return videoDAO.get();
		}
	}
	
	public void update(VideoDO videoDO) throws Exception {
		try( final Mongo mongo = new Mongo(mongoConfig) ) {
			final VideoDAO videoDAO = new VideoDAO( mongo.getDatastore() );
			videoDAO.update(videoDO);
		}
	}
	
	public File copyToLocal(String remoteFile) throws Exception {
		try( final Ftp ftp = new Ftp(ftpConfig) ) {
			final File localFile = ftp.copy(remoteFile);
			return localFile;
		}
	}
	
	public File trim(File localFile) throws Exception {
		final File mp4SrcFile = localFile;
		final File mp4DstFile = FileHelper.getTmpFile("mp4");
		final long begin = 0;
		final long end = 15;
		final TimeUnit timeUnit = TimeUnit.SECONDS;
		VideoUtils.trim(mp4SrcFile, mp4DstFile, begin, end, timeUnit);
		return mp4DstFile;
	}
	
	public List<S3ObjectSummary> listAwsBucket() {
		final AwsBucket awsBucket = awsBucketRef.get();
		final List<S3ObjectSummary> list = awsBucket.list();
		return list;
	}
	
	public void delete(String keyName) {
		final AwsBucket awsBucket = awsBucketRef.get();
		awsBucket.delete(keyName);
	}
	
	public String saveToAwsBucket(File file) {
		final AwsBucket awsBucket = awsBucketRef.get();
		final String keyName = AwsBucket.getKeyName(file);
		awsBucket.save(keyName, file);
		return keyName;
	}
	
	public File downloadFromAwsBucket(String keyName) throws Exception {
		final AwsBucket awsBucket = awsBucketRef.get();
		final File tmpFile = FileHelper.getTmpFile("mp4");
		awsBucket.download(keyName, tmpFile);
		return tmpFile;
	}
	
	public List<FaceDetection> detectFaces(String keyName) {
		final AwsRekognition awsRekognition = new AwsRekognition(awsConfig);
		final AwsRekoFace awsRekoFace = new AwsRekoFace(awsRekognition);
		
		final StartFaceDetectionResult result = awsRekoFace.detectInVideo(keyName);
		System.out.format("result=%s%n", result);
		
		final String jobId = result.getJobId();
		final int maxResults = 10;
		GetFaceDetectionResult faceDetectionResult = null;
		try {
			final long maxWaitMillis = 30000;
			faceDetectionResult = awsRekoFace.waitFaceDetection(jobId, maxResults, maxWaitMillis);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
		final List<FaceDetection> faces = faceDetectionResult.getFaces();
		return faces;
	}
	
	//TODO remove me
    public static void main( String[] args ) {
    	final Properties props = ConfigManager.get();
		
		final FtpConfig ftpConfig = new FtpConfig(props);
		final Ftp ftp = new Ftp(ftpConfig);
		
		File remoteFile = null;
		File localFile = null;
		try {
			ftp.connect();
			
			final Path ftpPath = Paths.get(props.getProperty("ftp.path"));
			final List<File> fileList = ftp.getFileList(ftpPath);
			remoteFile = fileList.get(0);
			System.out.format("remoteFile=%s%n", remoteFile);
			
			final boolean deleteOnExit = false;
			localFile = ftp.copy(remoteFile, deleteOnExit);
			System.out.format("localFile=%s%n", localFile);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		} finally {
			ftp.close();
		}
		
		final MongoConfig mongoConfig = new MongoConfig(props);
		final Mongo noSql = new Mongo(mongoConfig);
		try { 
			noSql.connect();
			
			final VideoDO video = new VideoDO(remoteFile.toString());
			final VideoDAO videoDAO = new VideoDAO(noSql.getDatastore());
			videoDAO.save(video);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		} finally {
			noSql.close();
		}

		File mp4DstFile = null;
		try {
			final File mp4SrcFile = localFile;
			mp4DstFile = FileHelper.getTmpFile("mp4");
			final long begin = 0;
			final long end = 8000;
			final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
			VideoUtils.trim(mp4SrcFile, mp4DstFile, begin, end, timeUnit);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		// save to bucket
		final AwsConfig awsConfig = new AwsConfig(props);
		final AwsBucket awsBucket = new AwsBucket(awsConfig);
		final String keyName = AwsBucket.getKeyName(mp4DstFile);
		System.out.format("keyName=%s%n", keyName);
		awsBucket.save(keyName, mp4DstFile);

		final AwsRekognition awsRekognition = new AwsRekognition(awsConfig);
		final AwsRekoFace awsRekoFace = new AwsRekoFace(awsRekognition);
		
		final StartFaceDetectionResult result = awsRekoFace.detectInVideo(keyName);
		System.out.format("result=%s%n", result);
		
		final String jobId = result.getJobId();
		final int maxResults = 10;
		GetFaceDetectionResult faceDetectionResult = null;
		try {
			final long maxWaitMillis = 30000;
			faceDetectionResult = awsRekoFace.waitFaceDetection(jobId, maxResults, maxWaitMillis);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		final List<FaceDetection> faces = faceDetectionResult.getFaces();
        for (FaceDetection face : faces) { 
        	System.out.println(face);
        	final long timestamp = face.getTimestamp();
        	final String fileName = Long.toString(timestamp).concat(".jpg");
        	final File destJpg = Paths.get("C:/Users/Robby Razzbag/TEST", fileName).toFile();
        	System.out.println(destJpg);
        	try {
        		final BufferedImage buffImg = VideoUtils.mp4ToImage(mp4DstFile, timestamp, TimeUnit.MILLISECONDS);
        		//VideoUtils.subImage(buffImg, leftRatio, topRatio, widthRatio, heightRatio);
        		VideoUtils.writeJpg(buffImg, destJpg);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }
		
		// delete from bucket
		awsBucket.delete(mp4DstFile);
    }
}
