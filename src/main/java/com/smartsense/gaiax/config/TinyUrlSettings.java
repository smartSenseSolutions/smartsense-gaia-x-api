/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "tinyurl")
@Configuration
@Getter
@Setter
public class TinyUrlSettings {

    private String key;
}
