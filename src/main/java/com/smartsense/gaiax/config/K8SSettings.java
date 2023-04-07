/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "k8s")
@Configuration
@Getter
@Setter
public class K8SSettings {

    private String basePath;

    private String token;

    private String serviceName;
}
