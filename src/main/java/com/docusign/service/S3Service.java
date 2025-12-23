package com.docusign.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class S3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presign-expires-minutes}")
    private long presignMinutes;

    public S3Service(
            @Value("${aws.region}") String region,
            @Value("${aws.accessKey}") String accessKey,
            @Value("${aws.secretKey}") String secretKey) {
        
        // Use StaticCredentialsProvider with credentials from application.properties
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);
        
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
        
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    public String generatePresignedPutUrl(String key, String contentType) {
        // Build PutObjectRequest
        // Note: For presigned URLs, we don't include Content-Type in the signature
        // to avoid header mismatch issues. The Content-Type can be set by the client
        // but won't be part of the signature validation.
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        
        // Don't set Content-Type in the request builder - this avoids signature mismatch
        // The client can send Content-Type header, but it won't be part of signature validation
        // This is more flexible and avoids 403 errors from header mismatches
        
        PutObjectRequest putObjectRequest = requestBuilder.build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        
        return presigned.url().toString();
    }

    public String generatePresignedGetUrl(String key) {
        // Generate a presigned URL for viewing/downloading files
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        
        return presigned.url().toString();
    }

    public void copyObject(String sourceKey, String destinationKey) {
        // Copy object from source to destination within the same bucket
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(destinationKey)
                .build();
        
        s3Client.copyObject(copyRequest);
    }
}
