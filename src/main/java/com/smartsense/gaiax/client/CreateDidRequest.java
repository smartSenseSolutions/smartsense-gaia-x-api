/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateDidRequest {
    
    private String domain;
}