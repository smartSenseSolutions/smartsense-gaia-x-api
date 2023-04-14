/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.ServiceOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {
    List<ServiceOffer> getByEnterpriseId(long enterpriseId);

    ServiceOffer getByIdAndEnterpriseId(long offerId, long enterpriseId);
}
