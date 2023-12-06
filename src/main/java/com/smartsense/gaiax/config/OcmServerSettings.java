/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "ocm-server")
@Configuration
@Getter
@Setter
public class OcmServerSettings {

    private String credentialDefinitionId;

    private String participantCredentialDefinitionId;
}
