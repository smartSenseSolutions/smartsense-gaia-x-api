/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * The type Verify request.
 */
@Getter
@Setter
@Builder
public class VerifyRequest {

    private Set<String> policies;

    private Map<String, Object> credential;
}
