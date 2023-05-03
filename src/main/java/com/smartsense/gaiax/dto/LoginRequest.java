/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Login request.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "${provide.email}")
    @Email(message = "${invalid.email}")
    private String email;

    @NotBlank(message = "${provide.password}")
    private String password;

}
