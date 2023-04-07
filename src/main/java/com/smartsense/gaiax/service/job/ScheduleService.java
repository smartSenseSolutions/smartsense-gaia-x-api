/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.job;


import com.smartsense.gaiax.dto.StringPool;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class ScheduleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleService.class);

    private final Scheduler scheduler;

    public ScheduleService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void deleteJob(JobKey jobKey) {
        try {
            scheduler.deleteJob(jobKey);
            LOGGER.debug("Job deleted for group-{}, name-{}", jobKey.getGroup(), jobKey.getName());
        } catch (SchedulerException e) {
            LOGGER.error("Can not delete job with group-{}, name-{}", jobKey.getGroup(), jobKey.getName());
        }
    }

    public void createJob(long enterpriseId, String type, int count) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(ScheduledJobBean.class)
                .withIdentity(UUID.randomUUID().toString(), type)
                .storeDurably()
                .requestRecovery()
                .usingJobData(StringPool.ENTERPRISE_ID, enterpriseId)
                .usingJobData(StringPool.JOB_TYPE, type)
                .build();

        SimpleTrigger activateEnterpriseUserTrigger = TriggerBuilder.newTrigger()
                .forJob(job)
                .withIdentity(UUID.randomUUID().toString(), type)
                .startAt(new Date(System.currentTimeMillis() + 10000)) //start after 10 sec
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(count).withIntervalInSeconds(30))
                .build();
        scheduler.scheduleJob(job, activateEnterpriseUserTrigger);
        LOGGER.debug("{}: job created for enterprise id->{}", type, enterpriseId);
    }
}
