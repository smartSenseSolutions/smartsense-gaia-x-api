package com.smartsense.gaiax.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.*;
import com.smartsense.gaiax.config.AWSSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Nitin
 * @version 1.0
 */
@Service
public class DomainService {


    private static final Logger LOGGER = LoggerFactory.getLogger(DomainService.class);

    private final AWSSettings awsSettings;

    private final AmazonRoute53 amazonRoute53;

    public DomainService(AWSSettings awsSettings) {
        this.awsSettings = awsSettings;
        this.amazonRoute53 = getAmazonRoute53();
    }


    public void createTxtRecordForSSLCertificate(String domainName, String value){
        ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue("\""+value+"\"");

        ResourceRecordSet recordsSet = new ResourceRecordSet();
        recordsSet.setResourceRecords(List.of(resourceRecord));
        recordsSet.setType(RRType.TXT);
        recordsSet.setTTL(900L);
        recordsSet.setName(domainName);

        Change change = new Change(ChangeAction.CREATE, recordsSet);

        ChangeBatch batch = new ChangeBatch(List.of(change));

        ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
        request.setChangeBatch(batch);



        request.setHostedZoneId(awsSettings.getHostedZoneId());
        ChangeResourceRecordSetsResult result = amazonRoute53.changeResourceRecordSets(request);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("TXT record created -> {}, result-> {}", domainName, result);
    }
    public void createSubDomain(String domainName) {

        ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue(awsSettings.getServerIp());

        ResourceRecordSet recordsSet = new ResourceRecordSet();
        recordsSet.setResourceRecords(List.of(resourceRecord));
        recordsSet.setType(RRType.A);
        recordsSet.setTTL(900L);
        recordsSet.setName(domainName);

        Change change = new Change(ChangeAction.CREATE, recordsSet);

        ChangeBatch batch = new ChangeBatch(List.of(change));

        ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
        request.setChangeBatch(batch);

        request.setHostedZoneId(awsSettings.getHostedZoneId());
        ChangeResourceRecordSetsResult result = amazonRoute53.changeResourceRecordSets(request);
        LOGGER.info("subdomain created -> {}, result-> {}", domainName, result);
    }

    private AmazonRoute53 getAmazonRoute53() {
        return AmazonRoute53ClientBuilder.standard().withCredentials(new AWSCredentialsProvider() {
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
}