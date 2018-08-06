package com.razz.aws.reko;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.razz.common.aws.AwsConfig;
import com.razz.common.aws.AwsConfigKey;

public class AwsRekognition {

	private final AwsConfig awsConfig;
	
	private final AmazonRekognition aRekognition;
	
	public AwsRekognition(AwsConfig awsConfig) {
		this.awsConfig = awsConfig;
		
		final String accessKey = awsConfig.getString(AwsConfigKey.ACCESS_KEY);
		final String secretKey = awsConfig.getString(AwsConfigKey.SECRET_KEY);
		final BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey); 
		final AWSStaticCredentialsProvider awsProvider = new AWSStaticCredentialsProvider(awsCreds);
		
		final String endPoint = awsConfig.getString(AwsConfigKey.END_POINT);
		final String region = awsConfig.getString(AwsConfigKey.REGION);
		final EndpointConfiguration epConfig = new EndpointConfiguration(endPoint, region);
		aRekognition = AmazonRekognitionClientBuilder
				.standard()
				.withCredentials(awsProvider)
				.withEndpointConfiguration(epConfig)
				.build(); 
	}
	
	public AmazonRekognition get() {
		return aRekognition;
	}
	
	public AwsConfig getConfig() {
		return awsConfig;
	}
	
}
