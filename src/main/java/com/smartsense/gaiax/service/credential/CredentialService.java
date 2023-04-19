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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The type Credential service.
 */
@Service
public class CredentialService {

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final ObjectMapper objectMapper;

    private final SignerClient signerClient;
    private final S3Utils s3Utils;

    /**
     * Instantiates a new Credential service.
     *
     * @param enterpriseRepository           the enterprise repository
     * @param enterpriseCredentialRepository the enterprise credential repository
     * @param objectMapper                   the object mapper
     * @param signerClient                   the signer client
     * @param s3Utils                        the s 3 utils
     */
    public CredentialService(EnterpriseRepository enterpriseRepository, EnterpriseCredentialRepository enterpriseCredentialRepository, ObjectMapper objectMapper, SignerClient signerClient, S3Utils s3Utils) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.objectMapper = objectMapper;
        this.signerClient = signerClient;
        this.s3Utils = s3Utils;
    }

    /**
     * Create vp map.
     *
     * @param enterpriseId the enterprise id
     * @param name         the name
     * @return the map
     * @throws JsonProcessingException the json processing exception
     */
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
