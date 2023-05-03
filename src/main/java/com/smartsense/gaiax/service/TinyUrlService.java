/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service;

import com.smartsense.gaiax.client.TinyUrlClient;
import com.smartsense.gaiax.config.TinyUrlSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TinyUrlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinyUrlService.class);

    private final TinyUrlClient tinyUrlClient;

    private final TinyUrlSettings tinyUrlSettings;

    public TinyUrlService(TinyUrlClient tinyUrlClient, TinyUrlSettings tinyUrlSettings) {
        this.tinyUrlClient = tinyUrlClient;
        this.tinyUrlSettings = tinyUrlSettings;
    }

    public Map<String, String> createTinyUrl(String url) {
        Map<String, String> data = new HashMap<>();
        try {
            ResponseEntity<Map<String, Object>> mapResponseEntity = tinyUrlClient.create(Map.of("url", url), tinyUrlSettings.getKey());
            String string = ((Map<String, Object>) mapResponseEntity.getBody().get("data")).get("tiny_url").toString();
            data.put("url", string);
        } catch (Exception e) {
            LOGGER.error("Can not create tiny url, return original one", e);
            data.put("url", url);
        }
        return data;
    }

}
