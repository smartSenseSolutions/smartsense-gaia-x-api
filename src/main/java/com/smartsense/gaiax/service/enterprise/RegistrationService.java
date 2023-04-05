/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.smartsense.gaiax.config.AWSSettings;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.request.RegisterRequest;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.utils.Validate;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nitin
 * @version 1.0
 */
@Service
public class RegistrationService {

    private final EnterpriseRepository enterpriseRepository;

    private final ScheduleService scheduleService;

    private final AWSSettings awsSettings;

    public RegistrationService(EnterpriseRepository enterpriseRepository, ScheduleService scheduleService, AWSSettings awsSettings) {
        this.enterpriseRepository = enterpriseRepository;
        this.scheduleService = scheduleService;
        this.awsSettings = awsSettings;
    }

    public long test() {
        return enterpriseRepository.count();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Enterprise registerEnterprise(RegisterRequest registerRequest) throws SchedulerException {
        //check legal name
        Validate.isTrue(enterpriseRepository.existsByLegalName(registerRequest.getLegalName())).launch("duplicate.legal.name");

        //check email
        Validate.isTrue(enterpriseRepository.existsByEmail(registerRequest.getEmail())).launch("duplicate.email");

        //check sub domain
        Validate.isTrue(enterpriseRepository.existsBySubDomainName(registerRequest.getEmail())).launch("duplicate.sub.domain");

        //check registration number
        Validate.isTrue(enterpriseRepository.existsByLegalRegistrationNumber(registerRequest.getEmail())).launch("duplicate.registration.number");

        //save enterprise details
        Enterprise enterprise = enterpriseRepository.save(Enterprise.builder()
                .email(registerRequest.getEmail())
                .headquarterAddress(registerRequest.getHeadquarterAddress())
                .legalAddress(registerRequest.getLegalAddress())
                .legalName(registerRequest.getLegalName())
                .legalRegistrationNumber(registerRequest.getLegalRegistrationNumber())
                .legalRegistrationType(registerRequest.getLegalRegistrationType())
                .status(RegistrationStatus.STARTED.getStatus())
                .subDomainName(registerRequest.getSubDomainName().toLowerCase() + "." + awsSettings.getBaseDomain())
                .password(registerRequest.getPassword()) //TODO need to create slat or use keycloak heer
                .build());

        //create job to create subdomain
        scheduleService.createJob(enterprise.getId(), StringPool.JOB_TYPE_CREATE_SUB_DOMAIN);
        return enterprise;
    }
}
