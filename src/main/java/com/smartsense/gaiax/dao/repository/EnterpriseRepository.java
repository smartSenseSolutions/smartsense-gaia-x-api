/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {
    boolean existsByLegalName(String legalName);

    boolean existsByEmail(String email);

    boolean existsBySubDomainName(String subDomain);

    boolean existsByLegalRegistrationNumber(String registrationNumber);

    Enterprise getBySubDomainName(String hostName);
}
