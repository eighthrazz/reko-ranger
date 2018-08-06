package com.razz.aws.reko;

import com.amazonaws.services.rekognition.model.StartFaceDetectionRequest;
import com.amazonaws.services.rekognition.model.StartFaceDetectionResult;
import com.amazonaws.services.rekognition.model.StartFaceSearchRequest;
import com.amazonaws.services.rekognition.model.StartFaceSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.TimeLimitExceededException;

import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceAttributes;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceDetection;
import com.amazonaws.services.rekognition.model.FaceSearchSortBy;
import com.amazonaws.services.rekognition.model.GetFaceDetectionRequest;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import com.amazonaws.services.rekognition.model.GetFaceSearchRequest;
import com.amazonaws.services.rekognition.model.GetFaceSearchResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.PersonMatch;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.Video;
import com.amazonaws.services.rekognition.model.VideoJobStatus;
import com.amazonaws.services.rekognition.model.VideoMetadata;
import com.razz.common.aws.AwsConfig;
import com.razz.common.aws.AwsConfigKey;

public class AwsRekoFace {

	private final AwsRekognition awsRekognition;

	public AwsRekoFace(AwsRekognition awsRekognition) {
		this.awsRekognition = awsRekognition;
	}
	
	public List<FaceDetail> detectInImage(String bucketName, String keyName) {
		final S3Object s3Object = new S3Object()
				.withBucket(bucketName)
				.withName(keyName);
		final Image image = new Image()
				.withS3Object(s3Object);
		final DetectFacesRequest request = new DetectFacesRequest()
		         .withImage(image)
		         .withAttributes(Attribute.ALL);
		final DetectFacesResult result = awsRekognition.get().detectFaces(request);
		final List<FaceDetail> fdList = new ArrayList<FaceDetail>( result.getFaceDetails() );
		return fdList;
	}
	
	public StartFaceDetectionResult detectInVideo(String keyName) {
		final AwsConfig awsConfig = awsRekognition.getConfig();
		final String bucketName = awsConfig.getString(AwsConfigKey.BUCKET);
		final StartFaceDetectionResult result = detectInVideo(bucketName, keyName);
		return result;
	}
	
	public StartFaceDetectionResult detectInVideo(String bucketName, String keyName) {
		final S3Object s3Object = new S3Object()
				.withBucket(bucketName)
				.withName(keyName);
		final Video video = new Video()
				.withS3Object(s3Object);	
		final StartFaceDetectionRequest request = new StartFaceDetectionRequest()
				.withVideo(video)
				.withFaceAttributes(FaceAttributes.ALL);
		final StartFaceDetectionResult result = awsRekognition.get().startFaceDetection(request);
		return result;
	}
	
	public GetFaceDetectionResult waitFaceDetection(String jobId, int maxResults, long maxWaitMillis) throws TimeLimitExceededException {
		final long startMillis = System.currentTimeMillis();
		final GetFaceDetectionRequest request = getFaceDetectionRequest(jobId, maxResults);
		
		GetFaceDetectionResult result = null;
		while(true) {
			result = awsRekognition.get().getFaceDetection(request);
			if( result == null || 
				VideoJobStatus.valueOf(result.getJobStatus()) == VideoJobStatus.FAILED ||
				VideoJobStatus.valueOf(result.getJobStatus()) == VideoJobStatus.SUCCEEDED ) 
			{
				break;	
			}
			
			// wait for a bit before trying again
			try {
				Thread.sleep(1000);
			} catch(Exception IGNORE) {}
			
			// break if maxWaitMillis is met
			if(System.currentTimeMillis() - startMillis > maxWaitMillis) {
				final String message = String.format("maxWaitMillis of %sms has been met.", maxWaitMillis);
				throw new TimeLimitExceededException(message);
			}
		}
		return result;
	}
	
	public GetFaceDetectionRequest getFaceDetectionRequest(String jobId, int maxResults) {
		String paginationToken = null;
		final GetFaceDetectionRequest request = new GetFaceDetectionRequest()
				.withJobId(jobId)
				.withNextToken(paginationToken)
				.withMaxResults(maxResults);
		return request;
	}
	
	public void showResults(String jobId, int maxResults) {
		final GetFaceDetectionRequest request = getFaceDetectionRequest(jobId, maxResults);
		System.out.format("%s=%s%n", GetFaceDetectionRequest.class.getSimpleName(), request);
		
		GetFaceDetectionResult result = null;
		try {
			final long maxWaitMillis=30000;
			result = waitFaceDetection(jobId, maxResults, maxWaitMillis);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		final VideoMetadata videoMetaData = result.getVideoMetadata();
		if(videoMetaData != null) {
	        System.out.println("Format: " + videoMetaData.getFormat());
	        System.out.println("Codec: " + videoMetaData.getCodec());
	        System.out.println("Duration: " + videoMetaData.getDurationMillis());
	        System.out.println("FrameRate: " + videoMetaData.getFrameRate());
		}
        
        final List<FaceDetection> faces = result.getFaces();
        for (FaceDetection face : faces) { 
        	System.out.println(face);
        }
	}
	
	public StartFaceSearchResult startVideoFaceSearch(String keyName) {
		final AwsConfig awsConfig = awsRekognition.getConfig();
		final String bucketName = awsConfig.getString(AwsConfigKey.BUCKET);
		final String collectionId = awsConfig.getString(AwsConfigKey.COLLECTION);
		final StartFaceSearchResult result = startVideoFaceSearch(bucketName, keyName, collectionId);
		return result;
	}
	
	public StartFaceSearchResult startVideoFaceSearch(String bucketName, String keyName, String collectionId) {
		final S3Object s3Object = new S3Object()
				.withBucket(bucketName)
				.withName(keyName);
		final Video video = new Video()
				.withS3Object(s3Object);	
		final StartFaceSearchRequest request = new StartFaceSearchRequest()
                 .withCollectionId(collectionId)
                 .withVideo(video);
		final StartFaceSearchResult result = awsRekognition.get().startFaceSearch(request);
		return result;
	}
	
	public GetFaceSearchRequest getFaceSearchTest(String jobId, int maxResults) {
		final GetFaceSearchRequest request = new GetFaceSearchRequest()
				.withJobId(jobId)
				.withMaxResults(maxResults)
				.withNextToken(null)
				.withSortBy(FaceSearchSortBy.TIMESTAMP);
		return request;
	}
	
	public List<PersonMatch> getFaceSearch(String jobId, int maxResults) {
		final List<PersonMatch> pmList = new ArrayList<PersonMatch>();
		String paginationToken = null;
		while(true) {
			final GetFaceSearchRequest request = new GetFaceSearchRequest()
					.withJobId(jobId)
					.withMaxResults(maxResults)
					.withNextToken(paginationToken)
					.withSortBy(FaceSearchSortBy.TIMESTAMP);
			paginationToken = addPersonMatchToList(request, pmList);
			if(paginationToken == null)
				break;
		}
		return pmList;
	}
	
	String addPersonMatchToList(GetFaceSearchRequest requst, List<PersonMatch> pmList) {
		final GetFaceSearchResult result = awsRekognition.get().getFaceSearch(requst);
		final List<PersonMatch> list = result.getPersons();
		if(list != null)
			pmList.addAll(list);
		return result.getNextToken();
	}
	
	public static void showList(List<?> list) {
		for(Object a : list) {
			System.out.println(a);
		}
		System.out.println("");
	}
	
}
