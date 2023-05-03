/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.signer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.CreateDidRequest;
import com.smartsense.gaiax.client.CreateVCRequest;
import com.smartsense.gaiax.client.SignerClient;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.repository.EnterpriseCredentialRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.service.vereign.VereignService;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.S3Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Signer service.
 */
@Service
public class SignerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignerService.class);

    private final EnterpriseRepository enterpriseRepository;

    private final SignerClient signerClient;

    private final S3Utils s3Utils;

    private final ObjectMapper objectMapper;

    private final ScheduleService scheduleService;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final VereignService vereignService;

    /**
     * Instantiates a new Signer service.
     *
     * @param enterpriseRepository           the enterprise repository
     * @param signerClient                   the signer client
     * @param s3Utils                        the s 3 utils
     * @param objectMapper                   the object mapper
     * @param scheduleService                the schedule service
     * @param enterpriseCredentialRepository the enterprise credential repository
     * @param vereignService
     */
    public SignerService(EnterpriseRepository enterpriseRepository, SignerClient signerClient, S3Utils s3Utils, ObjectMapper objectMapper, ScheduleService scheduleService, EnterpriseCredentialRepository enterpriseCredentialRepository, VereignService vereignService) {
        this.enterpriseRepository = enterpriseRepository;
        this.signerClient = signerClient;
        this.s3Utils = s3Utils;
        this.objectMapper = objectMapper;
        this.scheduleService = scheduleService;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.vereignService = vereignService;
    }

    /**
     * Create participant json.
     *
     * @param enterpriseId the enterprise id
     */
    public void createParticipantJson(long enterpriseId) {
        File file = new File("/tmp/participant.json");
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(BadDataException::new);
        try {
            String domain = enterprise.getSubDomainName();
            Map<String, String> data = new HashMap<>();
            data.put("legalName", enterprise.getLegalName());
            data.put("legalRegistrationType", enterprise.getLegalRegistrationType());
            data.put("legalRegistrationNumber", enterprise.getLegalRegistrationNumber());
            data.put("headquarterAddress", enterprise.getHeadquarterAddress());
            data.put("legalAddress", enterprise.getLegalAddress());
            CreateVCRequest request = CreateVCRequest.builder()
                    .data(data)
                    .templateId("LegalParticipant")
                    .domain(domain)
                    .privateKeyUrl(s3Utils.getPreSignedUrl(enterpriseId + "/pkcs8_" + domain + ".key"))
                    .build();
            ResponseEntity<Map<String, Object>> responseEntity = signerClient.createVc(request);
            String participantString = objectMapper.writeValueAsString(((Map<String, Object>) responseEntity.getBody().get("data")).get("verifiableCredential"));
            FileUtils.writeStringToFile(file, participantString, Charset.defaultCharset());
            s3Utils.uploadFile(enterpriseId + "/participant.json", file);

            //offer legal person credential in PCM
            String participantJsonLink = "https://" + enterprise.getSubDomainName() + "/.well-known/participant.json";
            String offerId = vereignService.offerLegalPersonCredentials(enterprise.getConnectionId(), enterprise.getDid(), participantJsonLink, enterprise.getLegalName());

            EnterpriseCredential participant = enterpriseCredentialRepository.getByEnterpriseIdAndLabel(enterpriseId, "participant");
            if (participant == null) {
                participant = EnterpriseCredential.builder()
                        .credentials(participantString)
                        .enterpriseId(enterpriseId)
                        .label("participant")
                        .offerId(offerId)
                        .build();
            } else {
                participant.setCredentials(participantString);
                participant.setOfferId(offerId);
            }

            enterpriseCredentialRepository.save(participant);
            enterprise.setStatus(RegistrationStatus.PARTICIPANT_JSON_CREATED.getStatus());


            LOGGER.debug("participant json created for enterprise->{} , json ->{}", enterpriseId, participantString);
        } catch (Exception e) {
            LOGGER.error("Error while creating participant json for enterprise -{}", enterpriseId, e);
            enterprise.setStatus(RegistrationStatus.PARTICIPANT_JSON_CREATION_FAILED.getStatus());
        } finally {
            enterpriseRepository.save(enterprise);
            CommonUtils.deleteFile(file);
        }
    }

    /**
     * Create did.
     *
     * @param enterpriseId the enterprise id
     */
    public void createDid(long enterpriseId) {
        File file = new File("/tmp/did.json");
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(BadDataException::new);
        try {
            String domain = enterprise.getSubDomainName();
            CreateDidRequest createDidRequest = CreateDidRequest.builder()
                    .domain(domain)
                    .build();
            ResponseEntity<Map<String, Object>> responseEntity = signerClient.createDid(createDidRequest);
            String didString = objectMapper.writeValueAsString(((Map<String, Object>) responseEntity.getBody().get("data")).get("did"));
            FileUtils.writeStringToFile(file, didString, Charset.defaultCharset());
            s3Utils.uploadFile(enterpriseId + "/did.json", file);
            enterprise.setStatus(RegistrationStatus.DID_JSON_CREATED.getStatus());
            LOGGER.debug("Did created for enterprise->{} , did ->{}", enterpriseId, didString);
            createParticipantCreationJob(enterpriseId, enterprise);
        } catch (Exception e) {
            LOGGER.error("Error while creating did json for enterprise -{}", enterpriseId, e);
            enterprise.setStatus(RegistrationStatus.DID_JSON_CREATION_FAILED.getStatus());
        } finally {
            enterpriseRepository.save(enterprise);
            CommonUtils.deleteFile(file);
        }
    }

    private void createParticipantCreationJob(long enterpriseId, Enterprise enterprise) {
        try {
            scheduleService.createJob(enterpriseId, StringPool.JOB_TYPE_CREATE_PARTICIPANT, 0);
            enterprise.setStatus(RegistrationStatus.PARTICIPANT_JSON_CREATION_FAILED.getStatus());
        } catch (Exception e) {
            LOGGER.error("Can not create participant job for enterprise -{}", enterpriseId, e);
        }
    }
}
