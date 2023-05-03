/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class OfferCredentialRequest {

    private String connectionId;

    private String credentialDefinitionId;

    private String comment;

    private String autoAcceptCredential;

    private List<Map<String, String>> attributes;
}
