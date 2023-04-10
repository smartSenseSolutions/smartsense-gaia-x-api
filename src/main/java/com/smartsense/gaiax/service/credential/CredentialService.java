/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.credential;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The type Credential service.
 */
@Service
public class CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private final EnterpriseRepository enterpriseRepository;

    /**
     * Instantiates a new Credential service.
     *
     * @param enterpriseRepository the enterprise repository
     */
    public CredentialService(EnterpriseRepository enterpriseRepository) {
        this.enterpriseRepository = enterpriseRepository;
    }

    /**
     * Create did json.
     *
     * @param enterpriseId the enterprise id
     */
    public void createDidJson(long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElse(null);
        if (enterprise == null) {
            LOGGER.error("Invalid enterprise id ->{} , not creating did json", enterpriseId);
        }

    }
}
