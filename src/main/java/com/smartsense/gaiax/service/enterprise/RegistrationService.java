/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.smartsense.gaiax.client.OfferCredentialRequest;
import com.smartsense.gaiax.client.OfferCredentialResponse;
import com.smartsense.gaiax.client.VereignClient;
import com.smartsense.gaiax.config.AWSSettings;
import com.smartsense.gaiax.config.VereignSettings;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegisterRequest;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.utils.Validate;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Registration service.
 *
 * @author Nitin
 * @version 1.0
 */
@Service
public class RegistrationService {

    private final EnterpriseRepository enterpriseRepository;


    private final AWSSettings awsSettings;

    private final String appName;

    private final VereignClient vereignClient;

    private final VereignSettings vereignSettings;

    private final ScheduleService scheduleService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

    /**
     * Instantiates a new Registration service.
     *
     * @param enterpriseRepository the enterprise repository
     * @param awsSettings          the aws settings
     * @param vereignClient
     * @param vereignSettings
     * @param scheduleService
     */
    public RegistrationService(EnterpriseRepository enterpriseRepository, @Value("${spring.application.name}") String appName, AWSSettings awsSettings, VereignClient vereignClient, VereignSettings vereignSettings, ScheduleService scheduleService) {
        this.enterpriseRepository = enterpriseRepository;
        this.awsSettings = awsSettings;
        this.appName = appName;
        this.vereignClient = vereignClient;
        this.vereignSettings = vereignSettings;
        this.scheduleService = scheduleService;
    }

    /**
     * Test long.
     *
     * @return the long
     */
    public long test() {
        return enterpriseRepository.count();
    }

    /**
     * Register enterprise enterprise.
     *
     * @param registerRequest the register request
     * @return the enterprise
     * @throws SchedulerException the scheduler exception
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Enterprise registerEnterprise(RegisterRequest registerRequest) throws SchedulerException {
        //check legal name
        Validate.isTrue(enterpriseRepository.existsByLegalName(registerRequest.getLegalName())).launch("duplicate.legal.name");

        //check email
        Validate.isTrue(enterpriseRepository.existsByEmail(registerRequest.getEmail())).launch("duplicate.email");

        //check registration number
        Validate.isTrue(enterpriseRepository.existsByLegalRegistrationNumber(registerRequest.getLegalRegistrationNumber())).launch("duplicate.registration.number");

        String subdomain = (registerRequest.getSubDomainName().toLowerCase() + "." + awsSettings.getBaseDomain()).trim();

        //check sub domain
        Validate.isTrue(enterpriseRepository.existsBySubDomainName(subdomain)).launch("duplicate.sub.domain");

        //offer credentials
        List<Map<String, String>> attributes = new ArrayList<>();
        //name
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("name", "name");
        nameMap.put("value", registerRequest.getLegalName());
        attributes.add(nameMap);

        //email
        Map<String, String> emailMap = new HashMap<>();
        nameMap.put("name", "email");
        nameMap.put("value", registerRequest.getEmail());
        attributes.add(emailMap);

        OfferCredentialRequest offerCredentialRequest = OfferCredentialRequest.builder()
                .connectionId(registerRequest.getConnectionId())
                .credentialDefinitionId(vereignSettings.getCredentialDefinitionId())
                .comment("Login with " + appName)
                .autoAcceptCredential("never") ////static for POC
                .attributes(attributes)
                .build();
        ResponseEntity<OfferCredentialResponse> mapResponseEntity = vereignClient.offerCredential(offerCredentialRequest); //TODO do we need to save it?
        String offerId = mapResponseEntity.getBody().getData().get("id").toString();
        LOGGER.debug("Offer created for enterprise -> {}, id ->{}", registerRequest.getLegalName(), offerId);

        //save enterprise details
        Enterprise enterprise = enterpriseRepository.save(Enterprise.builder()
                .email(registerRequest.getEmail())
                .headquarterAddress(registerRequest.getHeadquarterAddress())
                .legalAddress(registerRequest.getLegalAddress())
                .legalName(registerRequest.getLegalName())
                .legalRegistrationNumber(registerRequest.getLegalRegistrationNumber())
                .legalRegistrationType(registerRequest.getLegalRegistrationType())
                .status(RegistrationStatus.STARTED.getStatus())
                .subDomainName(subdomain)
                .connectionId(registerRequest.getConnectionId())
                .offerId(offerId)
                .build());


        //create job to create subdomain
        scheduleService.createJob(enterprise.getId(), StringPool.JOB_TYPE_CREATE_SUB_DOMAIN, 0);
        return enterprise;
    }
}
