/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The interface Enterprise credential repository.
 */
public interface EnterpriseCredentialRepository extends JpaRepository<EnterpriseCredential, Long> {
}
