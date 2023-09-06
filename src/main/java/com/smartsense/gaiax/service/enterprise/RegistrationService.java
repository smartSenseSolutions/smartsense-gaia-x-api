/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartsense.gaiax.config.AWSSettings;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegisterRequest;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.service.vereign.VereignService;
import com.smartsense.gaiax.utils.Validate;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private final ScheduleService scheduleService;

    private final VereignService vereignService;


    /**
     * Instantiates a new Registration service.
     *
     * @param enterpriseRepository the enterprise repository
     * @param awsSettings          the aws settings
     * @param scheduleService
     * @param vereignService
     */
    public RegistrationService(EnterpriseRepository enterpriseRepository, AWSSettings awsSettings, ScheduleService scheduleService, VereignService vereignService) {
        this.enterpriseRepository = enterpriseRepository;
        this.awsSettings = awsSettings;
        this.scheduleService = scheduleService;
        this.vereignService = vereignService;
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
    public Enterprise registerEnterprise(RegisterRequest registerRequest) throws SchedulerException, JsonProcessingException {
        //check legal name
        Validate.isTrue(enterpriseRepository.existsByLegalName(registerRequest.getLegalName())).launch("duplicate.legal.name");

        //check email
        Validate.isTrue(enterpriseRepository.existsByEmail(registerRequest.getEmail())).launch("duplicate.email");

        //check registration number
//        Validate.isTrue(enterpriseRepository.existsByLegalRegistrationNumber(registerRequest.getLegalRegistrationNumber())).launch("duplicate.registration.number");

        String subdomain = (registerRequest.getSubDomainName().toLowerCase() + "." + awsSettings.getBaseDomain()).trim();

        //check sub domain
        Validate.isTrue(enterpriseRepository.existsBySubDomainName(subdomain)).launch("duplicate.sub.domain");

        String offerId = vereignService.offerMembershipCredentials(registerRequest);

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
