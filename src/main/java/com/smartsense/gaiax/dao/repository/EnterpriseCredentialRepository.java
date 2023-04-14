/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The interface Enterprise credential repository.
 */
public interface EnterpriseCredentialRepository extends JpaRepository<EnterpriseCredential, Long> {
    /**
     * Gets by enterprise id.
     *
     * @param enterpriseId the enterprise id
     * @return the by enterprise id
     */
    List<EnterpriseCredential> getByEnterpriseId(long enterpriseId);
}
