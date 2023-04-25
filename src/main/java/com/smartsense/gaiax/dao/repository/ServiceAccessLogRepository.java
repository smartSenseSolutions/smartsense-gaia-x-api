/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.repository;

import com.smartsense.gaiax.dao.entity.ServiceAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAccessLogRepository extends JpaRepository<ServiceAccessLog, Long> {
}
