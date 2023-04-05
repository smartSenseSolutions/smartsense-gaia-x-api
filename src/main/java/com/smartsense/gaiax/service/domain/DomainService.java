/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.domain;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.*;
import com.smartsense.gaiax.config.AWSSettings;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.service.job.ScheduleService;
import org.quartz.SchedulerException;
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

    private final EnterpriseRepository enterpriseRepository;

    private final ScheduleService scheduleService;

    public DomainService(AWSSettings awsSettings, EnterpriseRepository enterpriseRepository, ScheduleService scheduleService) {
        this.awsSettings = awsSettings;
        this.enterpriseRepository = enterpriseRepository;
        this.scheduleService = scheduleService;
        this.amazonRoute53 = getAmazonRoute53();
    }


    public void updateTxtRecords(String domainName, String value, ChangeAction action) {
        ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue("\"" + value + "\"");

        ResourceRecordSet recordsSet = new ResourceRecordSet();
        recordsSet.setResourceRecords(List.of(resourceRecord));
        recordsSet.setType(RRType.TXT);
        recordsSet.setTTL(900L);
        recordsSet.setName(domainName);

        Change change = new Change(action, recordsSet);

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
        LOGGER.info("TXT record updated -> {}, result-> {}", domainName, result);
    }

    public void deleteTxtRecordForSSLCertificate(String domainName, String value) {
        updateTxtRecords(domainName, value, ChangeAction.DELETE);
        LOGGER.info("TXT record deleted -> {}", domainName);
    }


    public void createTxtRecordForSSLCertificate(String domainName, String value) {
        updateTxtRecords(domainName, value, ChangeAction.CREATE);
        LOGGER.info("TXT record created -> {} ", domainName);
    }

    public void createSubDomain(long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElse(null);
        if (enterprise == null) {
            LOGGER.error("Invalid enterprise id ->{}", enterpriseId);
            return;
        }
        try {
            String domainName = enterprise.getSubDomainName();
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
            LOGGER.info("subdomain created -> {} for enterprise id->{}, result-> {}", domainName, enterpriseId, result);
            enterprise.setStatus(RegistrationStatus.DOMAIN_CREATED.getStatus());

            //create job to create certificate
            try {
                scheduleService.createJob(enterpriseId, StringPool.JOB_TYPE_CREATE_CERTIFICATE);
            } catch (SchedulerException e) {
                LOGGER.error("Can not create certificate creation job for enterprise->{}", enterprise, e);
                enterprise.setStatus(RegistrationStatus.CERTIFICATE_CREATION_FAILED.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("Can not create sub domain for enterprise->{}", enterpriseId, e);
            enterprise.setStatus(RegistrationStatus.DOMAIN_CREATION_FAILED.getStatus());
        } finally {
            enterpriseRepository.save(enterprise);
        }
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
                })
                .withRegion("us-east-1")
                .build();
    }
}