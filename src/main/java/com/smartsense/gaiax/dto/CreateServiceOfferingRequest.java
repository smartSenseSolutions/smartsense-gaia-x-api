/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class CreateServiceOfferingRequest {

    private String name;

    private String description;

    private String license;

    private Set<String> policies;

    private String expirationDate;

    private Map<String, String> meta;
}
