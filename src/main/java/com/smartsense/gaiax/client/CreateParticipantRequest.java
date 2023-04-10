/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class CreateParticipantRequest {

    private String domain;

    private String templateId;

    private String privateKeyUrl;

    private Map<String, String> data;

}
