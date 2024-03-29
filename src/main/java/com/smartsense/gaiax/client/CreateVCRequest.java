/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * The type Create participant request.
 */
@Getter
@Setter
@Builder
public class CreateVCRequest {

    private String domain;

    private String templateId;

    private String privateKeyUrl;

    private Map<String, Object> data;

}
