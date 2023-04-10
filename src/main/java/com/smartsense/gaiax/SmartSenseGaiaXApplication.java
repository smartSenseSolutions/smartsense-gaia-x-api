/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableFeignClients
public class SmartSenseGaiaXApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartSenseGaiaXApplication.class, args);
    }
}