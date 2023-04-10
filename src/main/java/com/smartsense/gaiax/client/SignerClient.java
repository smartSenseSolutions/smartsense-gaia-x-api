/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(value = "Signerapi", url = "${signer.host}")
public interface SignerClient {

    @PostMapping(path = "createWebDID", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> createDid(@RequestBody CreateDidRequest createDidRequest);

    @PostMapping(path = "onBoardToGaiaX", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> onBoardToGaiaX(@RequestBody CreateParticipantRequest request);
}
