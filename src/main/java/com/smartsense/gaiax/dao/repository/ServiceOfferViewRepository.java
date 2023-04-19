/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.ServiceOfferView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Service offer view repository.
 */
@Repository
public interface ServiceOfferViewRepository extends JpaRepository<ServiceOfferView, Long> {
    List<ServiceOfferView> getByEnterpriseId(long enterpriseId);

    @Query("from ServiceOfferView where enterpriseId <> :enterpriseId")
    List<ServiceOfferView> getAllServiceOffers(@Param("enterpriseId") long enterpriseId);

}
