/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "Signerapi", url = "${vereign.host}")
public interface VereignClient {
    @PostMapping(path = "ocm/attestation/v1/create-offer-credential", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OfferCredentialResponse> offerCredential(@RequestBody OfferCredentialRequest offerCredentialRequest);
}