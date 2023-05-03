/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(value = "tinyurlapi", url = "${tinyurl.host}")
public interface TinyUrlClient {

    @PostMapping(path = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> data, @RequestParam(name = "api_token") String apiToken);
}
