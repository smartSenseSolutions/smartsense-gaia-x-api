/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Admin repository.
 */
@Repository

public interface AdminRepository extends JpaRepository<Admin, Long> {
    /**
     * Gets by user name.
     *
     * @param email the email
     * @return the by user name
     */
    Admin getByUserName(String email);

}
