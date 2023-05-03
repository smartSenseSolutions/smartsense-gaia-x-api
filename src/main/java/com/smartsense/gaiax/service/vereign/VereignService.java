/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.vereign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.OfferCredentialRequest;
import com.smartsense.gaiax.client.OfferCredentialResponse;
import com.smartsense.gaiax.client.VereignClient;
import com.smartsense.gaiax.config.VereignSettings;
import com.smartsense.gaiax.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VereignService {

    private final VereignClient vereignClient;

    private final VereignSettings vereignSettings;

    private final ObjectMapper objectMapper;

    private final String appName;

    private static final Logger LOGGER = LoggerFactory.getLogger(VereignService.class);


    public VereignService(VereignClient vereignClient, VereignSettings vereignSettings, ObjectMapper objectMapper, @Value("${spring.application.name}") String appName) {
        this.vereignClient = vereignClient;
        this.vereignSettings = vereignSettings;
        this.objectMapper = objectMapper;
        this.appName = appName;
    }


    public String offerLegalPersonCredentials(String connectionId, String did, String url, String legalName) throws JsonProcessingException {
        LOGGER.debug("Offering legal participant credentials in PCM for {}", legalName);
        //offer credentials
        List<Map<String, String>> attributes = new ArrayList<>();
        //did
        Map<String, String> didMap = new HashMap<>();
        didMap.put("name", "did");
        didMap.put("value", did);
        attributes.add(didMap);

        //id
        Map<String, String> idMap = new HashMap<>();
        idMap.put("name", "id");
        idMap.put("value", url);
        attributes.add(idMap);

        //type
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("name", "type");
        typeMap.put("value", "gx:LegalParticipant"); //static
        attributes.add(typeMap);

        //legalName
        Map<String, String> legalNameMap = new HashMap<>();
        legalNameMap.put("name", "gx:legalName");
        legalNameMap.put("value", legalName);
        attributes.add(legalNameMap);

        return offerCredentials(connectionId, legalName, attributes);

    }

    public String offerMembershipCredentials(RegisterRequest registerRequest) throws JsonProcessingException {
        LOGGER.debug("Offering membership credentials in PCM for {}", registerRequest.getLegalName());
        //offer credentials
        List<Map<String, String>> attributes = new ArrayList<>();
        //name
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("name", "name");
        nameMap.put("value", registerRequest.getLegalName());
        attributes.add(nameMap);

        //email
        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("name", "email");
        emailMap.put("value", registerRequest.getEmail());
        attributes.add(emailMap);

        return offerCredentials(registerRequest.getConnectionId(), registerRequest.getLegalName(), attributes);
    }

    private String offerCredentials(String connectionId, String legalName, List<Map<String, String>> attributes) throws JsonProcessingException {
        OfferCredentialRequest offerCredentialRequest = OfferCredentialRequest.builder()
                .connectionId(connectionId)
                .credentialDefinitionId(vereignSettings.getCredentialDefinitionId())
                .comment("Login with " + appName)
                .autoAcceptCredential("never") ////static for POC
                .attributes(attributes)
                .build();
        LOGGER.debug("Request for offer credentials {}", objectMapper.writeValueAsString(offerCredentialRequest));
        ResponseEntity<OfferCredentialResponse> mapResponseEntity = vereignClient.offerCredential(offerCredentialRequest);
        String offerId = mapResponseEntity.getBody().getData().get("id").toString();
        LOGGER.debug("Offer created for enterprise -> {}, id ->{}", legalName, offerId);
        return offerId;
    }
}
