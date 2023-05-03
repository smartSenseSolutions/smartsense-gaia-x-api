/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Login response.
 */
@Getter
@Setter
@Builder
public class LoginResponse {

    private int statusCode;

    private String status;

    private String token;

    private SessionDTO session;
}
