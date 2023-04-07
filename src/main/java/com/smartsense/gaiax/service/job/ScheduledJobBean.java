/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.job;

import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.service.domain.DomainService;
import com.smartsense.gaiax.service.k8s.K8SService;
import com.smartsense.gaiax.service.ssl.CertificateService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class ScheduledJobBean extends QuartzJobBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobBean.class);

    private final DomainService domainService;

    private final CertificateService certificateService;

    private final K8SService k8SService;

    public ScheduledJobBean(DomainService domainService, CertificateService certificateService, K8SService k8SService) {
        this.domainService = domainService;
        this.certificateService = certificateService;
        this.k8SService = k8SService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        String jobType = jobDetail.getJobDataMap().getString(StringPool.JOB_TYPE);

        if (jobType.equals(StringPool.JOB_TYPE_CREATE_SUB_DOMAIN)) {
            domainService.createSubDomain(jobDetail.getJobDataMap().getLong(StringPool.ENTERPRISE_ID));
        } else if (jobType.equals(StringPool.JOB_TYPE_CREATE_CERTIFICATE)) {
            certificateService.createSSLCertificate(jobDetail.getJobDataMap().getLong(StringPool.ENTERPRISE_ID));
        } else if (jobType.equals(StringPool.JOB_TYPE_CREATE_INGRESS)) {
            k8SService.createIngress(jobDetail.getJobDataMap().getLong(StringPool.ENTERPRISE_ID));
        } else {
            LOGGER.error("Invalid job type -> {}", jobType);
        }
        LOGGER.info("job completed");
    }
}
