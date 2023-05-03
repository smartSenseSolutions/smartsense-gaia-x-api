/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.utils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.smartsense.gaiax.config.AWSSettings;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

/**
 * The type S 3 utils.
 */
@Service
public class S3Utils {
    private final AmazonS3 s3Client;

    private final AWSSettings awsSettings;

    /**
     * Instantiates a new S 3 utils.
     *
     * @param awsSettings the aws settings
     */
    public S3Utils(AWSSettings awsSettings) {
        s3Client = AmazonS3ClientBuilder.standard().
                withRegion(Regions.US_EAST_1).
                withCredentials(new AWSCredentialsProvider() {
                    @Override
                    public AWSCredentials getCredentials() {
                        return new AWSCredentials() {
                            @Override
                            public String getAWSAccessKeyId() {
                                return awsSettings.getAccessKey();
                            }

                            @Override
                            public String getAWSSecretKey() {
                                return awsSettings.getSecretKey();
                            }
                        };
                    }

                    @Override
                    public void refresh() {
                        //Do nothing
                    }
                }).build();
        this.awsSettings = awsSettings;
    }


    /**
     * Upload file.
     *
     * @param objectName the object name
     * @param file       the file
     */
    public void uploadFile(String objectName, File file) {
        s3Client.putObject(awsSettings.getBucketName(), objectName, file);
    }

    /**
     * Gets pre signed url.
     *
     * @param objectName the object name
     * @return the pre signed url
     */
    public String getPreSignedUrl(String objectName) {
        Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 10000; // 10 seconds
        expiration.setTime(expTimeMillis);
        return s3Client.generatePresignedUrl(awsSettings.getBucketName(), objectName, expiration).toString();
    }

    /**
     * Gets object.
     *
     * @param key      the key
     * @param fileName the file name
     * @return the object
     */
    public File getObject(String key, String fileName) {
        File localFile = new File("/tmp/" + fileName);
        CommonUtils.deleteFile(localFile);
        s3Client.getObject(new GetObjectRequest(awsSettings.getBucketName(), key), localFile);
        return localFile;
    }
}
