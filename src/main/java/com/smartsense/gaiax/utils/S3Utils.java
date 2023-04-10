/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.utils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.smartsense.gaiax.config.AWSSettings;
import com.smartsense.gaiax.dto.StringPool;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Service
public class S3Utils {
    private final AmazonS3 s3Client;

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

                    }
                }).build();
    }


    /**
     * Upload file.
     *
     * @param objectName the object name
     * @param file       the file
     */
    public void uploadFile(String objectName, File file) {
        s3Client.putObject(StringPool.S3_BUCKET_NAME, objectName, file);
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
        expTimeMillis += 20000; // 10 seconds
        expiration.setTime(expTimeMillis);
        return s3Client.generatePresignedUrl(StringPool.S3_BUCKET_NAME, objectName, expiration).toString();
    }

    public File getObject(String key, String fileName) {
        File localFile = new File("/home/nitin/" + fileName);
        if (localFile.exists()) {
            localFile.delete();
        }
        s3Client.getObject(new GetObjectRequest(StringPool.S3_BUCKET_NAME, key), localFile);
        return localFile;
    }

    /**
     * Object exist boolean.
     *
     * @param objectName the object name
     * @return the boolean
     */
    public boolean doesObjectExist(String objectName) {
        return s3Client.doesObjectExist(StringPool.S3_BUCKET_NAME, objectName);
    }

    private S3Object getS3Object(String objectName) {
        AmazonS3 myS3 = AmazonS3ClientBuilder.standard().
                withRegion(Regions.AP_SOUTH_1).withCredentials(new SystemPropertiesCredentialsProvider()).
                build();
        S3Object s3Object = myS3.getObject(StringPool.S3_BUCKET_NAME, objectName);
        if (s3Object == null) {
            System.out.println("Can not find s3 object");
            return null;
        }
        return s3Object;
    }
}
