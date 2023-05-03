/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "Signerapi", url = "${vereign.host}")
public interface VereignClient {
    @PostMapping(path = "ocm/attestation/v1/create-offer-credential", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OfferCredentialResponse> offerCredential(@RequestBody OfferCredentialRequest offerCredentialRequest);

    @GetMapping(path = "ocm/proof/v1/find-by-presentation-id", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<VerifyPresentationResponse> verifyPresentation(@RequestParam(name = "presentationId") String presentationId);
}