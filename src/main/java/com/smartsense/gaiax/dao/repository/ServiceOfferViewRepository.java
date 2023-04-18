/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.ServiceOfferView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Service offer view repository.
 */
@Repository
public interface ServiceOfferViewRepository extends JpaRepository<ServiceOfferView, Long> {
}
