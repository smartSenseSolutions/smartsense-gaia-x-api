/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.SignerClient;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.repository.EnterpriseCredentialRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.CreateVPRequest;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.EntityNotFoundException;
import com.smartsense.gaiax.utils.S3Utils;
import com.smartsense.gaiax.utils.Validate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The type Credential service.
 */
@Service
public class CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final ObjectMapper objectMapper;

    private final SignerClient signerClient;
    private final S3Utils s3Utils;

    /**
     * Instantiates a new Credential service.
     *
     * @param enterpriseRepository           the enterprise repository
     * @param enterpriseCredentialRepository
     * @param objectMapper
     * @param signerClient
     * @param s3Utils
     */
    public CredentialService(EnterpriseRepository enterpriseRepository, EnterpriseCredentialRepository enterpriseCredentialRepository, ObjectMapper objectMapper, SignerClient signerClient, S3Utils s3Utils) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.objectMapper = objectMapper;
        this.signerClient = signerClient;
        this.s3Utils = s3Utils;
    }

    /**
     * Create did json.
     *
     * @param enterpriseId the enterprise id
     */
    public void createDidJson(long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElse(null);
        if (enterprise == null) {
            LOGGER.error("Invalid enterprise id ->{} , not creating did json", enterpriseId);
        }
    }

    public Map<String, Object> createVP(long enterpriseId, String name) throws JsonProcessingException {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(BadDataException::new);
        EnterpriseCredential enterpriseCredential = enterpriseCredentialRepository.getByEnterpriseIdAndLabel(enterpriseId, name);
        Validate.isNull(enterpriseCredential).launch(new EntityNotFoundException("Can not find participant credential for enterprise id->" + enterpriseId));
        JSONObject verifiableCredential = new JSONObject(enterpriseCredential.getCredentials()).getJSONObject("selfDescriptionCredential").getJSONArray("verifiableCredential").getJSONObject(0);

        CreateVPRequest createVPRequest = CreateVPRequest.builder()
                .holderDID("did:web:" + enterprise.getSubDomainName())
                .privateKeyUrl(s3Utils.getPreSignedUrl(enterpriseId + "/pkcs8_" + enterprise.getSubDomainName() + ".key"))
                .claims(List.of(verifiableCredential.toMap()))
                .build();

        ResponseEntity<Map<String, Object>> vp = signerClient.createVP(createVPRequest);

        String serviceOfferingString = objectMapper.writeValueAsString(((Map<String, Object>) vp.getBody().get("data")).get("verifiablePresentation"));
        return new JSONObject(serviceOfferingString).toMap();
    }
}
