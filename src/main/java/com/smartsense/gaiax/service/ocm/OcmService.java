/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.ocm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.OfferCredentialRequest;
import com.smartsense.gaiax.client.OfferCredentialResponse;
import com.smartsense.gaiax.client.OcmClient;
import com.smartsense.gaiax.config.OcmServerSettings;
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
public class OcmService {

    public static final String NAME = "name";
    public static final String VALUE = "value";
    private final OcmClient ocmClient;

    private final OcmServerSettings ocmServerSettings;

    private final ObjectMapper objectMapper;

    private final String appName;

    private static final Logger LOGGER = LoggerFactory.getLogger(OcmService.class);


    public OcmService(OcmClient ocmClient, OcmServerSettings ocmServerSettings, ObjectMapper objectMapper, @Value("${spring.application.name}") String appName) {
        this.ocmClient = ocmClient;
        this.ocmServerSettings = ocmServerSettings;
        this.objectMapper = objectMapper;
        this.appName = appName;
    }


    public String offerLegalPersonCredentials(String connectionId, String did, String url, String legalName) throws JsonProcessingException {
        LOGGER.debug("Offering legal participant credentials in PCM for {}", legalName);
        //offer credentials
        List<Map<String, String>> attributes = new ArrayList<>();
        //did
        Map<String, String> didMap = new HashMap<>();
        didMap.put(NAME, "did");
        didMap.put(VALUE, did);
        attributes.add(didMap);

        //id
        Map<String, String> idMap = new HashMap<>();
        idMap.put(NAME, "id");
        idMap.put(VALUE, url);
        attributes.add(idMap);

        //type
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put(NAME, "type");
        typeMap.put(VALUE, "gx:LegalParticipant"); //static
        attributes.add(typeMap);

        //legalName
        Map<String, String> legalNameMap = new HashMap<>();
        legalNameMap.put(NAME, "gx:legalName");
        legalNameMap.put(VALUE, legalName);
        attributes.add(legalNameMap);

        return offerCredentials(connectionId, legalName, attributes, ocmServerSettings.getParticipantCredentialDefinitionId(), "gx:LegalParticipant issued on " + appName);

    }

    public String offerMembershipCredentials(RegisterRequest registerRequest) throws JsonProcessingException {
        LOGGER.debug("Offering membership credentials in PCM for {}", registerRequest.getLegalName());
        //offer credentials
        List<Map<String, String>> attributes = new ArrayList<>();
        //name
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put(NAME, NAME);
        nameMap.put(VALUE, registerRequest.getLegalName());
        attributes.add(nameMap);

        //email
        Map<String, String> emailMap = new HashMap<>();
        emailMap.put(NAME, "email");
        emailMap.put(VALUE, registerRequest.getEmail());
        attributes.add(emailMap);

        return offerCredentials(registerRequest.getConnectionId(), registerRequest.getLegalName(), attributes, ocmServerSettings.getCredentialDefinitionId(), "Login with " + appName);
    }

    private String offerCredentials(String connectionId, String legalName, List<Map<String, String>> attributes, String definitionId, String comment) throws JsonProcessingException {
        OfferCredentialRequest offerCredentialRequest = OfferCredentialRequest.builder()
                .connectionId(connectionId)
                .credentialDefinitionId(definitionId)
                .comment(comment)
                .autoAcceptCredential("never") ////static for POC
                .attributes(attributes)
                .build();
        LOGGER.debug("Request for offer credentials {}", objectMapper.writeValueAsString(offerCredentialRequest));
        ResponseEntity<OfferCredentialResponse> mapResponseEntity = ocmClient.offerCredential(offerCredentialRequest);
        String offerId = mapResponseEntity.getBody().getData().get("id").toString();
        LOGGER.debug("Offer created for enterprise -> {}, id ->{}", legalName, offerId);
        return offerId;
    }
}
