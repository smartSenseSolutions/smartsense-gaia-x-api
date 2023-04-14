/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The type Jwt setting.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JWTSetting {

    private String tokenSigningKey;

}
