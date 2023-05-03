/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferCredentialResponse {

    private Map<String, Object> data;

    private String message;
}
