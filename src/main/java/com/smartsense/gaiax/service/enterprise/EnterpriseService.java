/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.CreateVCRequest;
import com.smartsense.gaiax.client.SignerClient;
import com.smartsense.gaiax.dao.entity.*;
import com.smartsense.gaiax.dao.repository.*;
import com.smartsense.gaiax.dto.*;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.EntityNotFoundException;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.JWTUtil;
import com.smartsense.gaiax.utils.S3Utils;
import com.smartsense.gaiax.utils.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The type Enterprise service.
 */
@Service
public class EnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseService.class);
    /**
     * The constant INVALID_USERNAME_OR_PASSWORD.
     */
    public static final String INVALID_USERNAME_OR_PASSWORD = "invalid.username.or.password";

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final S3Utils s3Utils;

    private final ServiceOfferRepository serviceOfferRepository;

    private final SignerClient signerClient;

    private final ObjectMapper objectMapper;

    private final AdminRepository adminRepository;

    private final JWTUtil jwtUtil;

    private final ServiceOfferViewRepository serviceOfferViewRepository;

    private final ServiceAccessLogRepository serviceAccessLogRepository;


    /**
     * Instantiates a new Enterprise service.
     *
     * @param enterpriseRepository           the enterprise repository
     * @param enterpriseCredentialRepository the enterprise credential repository
     * @param s3Utils                        the s 3 utils
     * @param serviceOfferRepository         the service offer repository
     * @param signerClient                   the signer client
     * @param objectMapper                   the object mapper
     * @param adminRepository                the admin repository
     * @param jwtUtil                        the jwt util
     * @param serviceOfferViewRepository     the service offer view repository
     * @param serviceAccessLogRepository
     */
    public EnterpriseService(EnterpriseRepository enterpriseRepository, EnterpriseCredentialRepository enterpriseCredentialRepository, S3Utils s3Utils, ServiceOfferRepository serviceOfferRepository, SignerClient signerClient, ObjectMapper objectMapper, AdminRepository adminRepository, JWTUtil jwtUtil, ServiceOfferViewRepository serviceOfferViewRepository, ServiceAccessLogRepository serviceAccessLogRepository) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.s3Utils = s3Utils;
        this.serviceOfferRepository = serviceOfferRepository;
        this.signerClient = signerClient;
        this.objectMapper = objectMapper;
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
        this.serviceOfferViewRepository = serviceOfferViewRepository;
        this.serviceAccessLogRepository = serviceAccessLogRepository;
    }

    /**
     * Login login response.
     *
     * @param email    the email
     * @param password the password
     * @param type     the type
     * @return the login response
     */
    public LoginResponse login(String email, String password, int type) {
        if (type == 1) {
            //login as admin
            Admin admin = adminRepository.getByUserName(email);
            Validate.isNull(admin).launch(new BadDataException(INVALID_USERNAME_OR_PASSWORD));
            boolean valid = BCrypt.checkpw(password, admin.getPassword());
            Validate.isFalse(valid).launch(new BadDataException(INVALID_USERNAME_OR_PASSWORD));
            SessionDTO sessionDTO = SessionDTO.builder()
                    .role(StringPool.ADMIN_ROLE)
                    .email(admin.getUserName())
                    .enterpriseId(-1)
                    .build();
            return LoginResponse.builder()
                    .token("Bearer " + jwtUtil.generateToken(sessionDTO))
                    .session(sessionDTO)
                    .build();
        } else {
            //login aa enterprise
            Enterprise enterprise = enterpriseRepository.getByEmail(email);
            Validate.isNull(enterprise).launch(new BadDataException(INVALID_USERNAME_OR_PASSWORD));
            boolean valid = BCrypt.checkpw(password, enterprise.getPassword());
            Validate.isFalse(valid).launch(new BadDataException(INVALID_USERNAME_OR_PASSWORD));
            SessionDTO sessionDTO = SessionDTO.builder()
                    .role(StringPool.ENTERPRISE_ROLE)
                    .email(enterprise.getEmail())
                    .enterpriseId(enterprise.getId())
                    .build();
            return LoginResponse.builder()
                    .token("Bearer " + jwtUtil.generateToken(sessionDTO))
                    .session(sessionDTO)
                    .build();
        }
    }

    /**
     * Gets enterprise.
     *
     * @param id the id
     * @return the enterprise
     */
    public Enterprise getEnterprise(long id) {
        //TODO more details to added
        Enterprise enterprise = enterpriseRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        enterprise.setDidJson("https://" + enterprise.getSubDomainName() + "/.well-known/did.json");
        enterprise.setParticipantJson("https://" + enterprise.getSubDomainName() + "/.well-known/participant.json");
        enterprise.setCertificateChain("https://" + enterprise.getSubDomainName() + "/.well-known/x509CertificateChain.pem");
        enterprise.setDid("did:web:" + enterprise.getSubDomainName());
        return enterprise;
    }

    /**
     * List enterprise list.
     *
     * @return the list
     */
    public List<Enterprise> listEnterprise() {
        return enterpriseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Gets enterprise files.
     *
     * @param hostName the host name
     * @param fileName the file name
     * @return the enterprise files
     * @throws IOException the io exception
     */
    public String getEnterpriseFiles(String hostName, String fileName) throws IOException {
        File file = null;
        try {
            //Restrict key and csr file download
            //TODO can be improved by storing private key in more secure place
            if (fileName.endsWith("key") || fileName.endsWith("csr")) {
                throw new EntityNotFoundException("Can find file -> " + fileName);
            }
            Enterprise enterprise = enterpriseRepository.getBySubDomainName(hostName);
            if (enterprise == null) {
                throw new BadDataException("Can not find subdomain -> " + hostName);
            }

            String fileKey = enterprise.getId() + "/" + fileName;
            file = s3Utils.getObject(fileKey, fileName);
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } finally {
            CommonUtils.deleteFile(file);
        }
    }

    /**
     * Create service offering service offer.
     *
     * @param enterpriseId the enterprise id
     * @param request      the request
     * @return the service offer
     * @throws IOException the io exception
     */
    public ServiceOffer createServiceOffering(long enterpriseId, CreateServiceOfferingRequest request) throws IOException {
        LOGGER.debug("creating service offer for enterprise id -> {}", enterpriseId);
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);

        String name = request.getName().replaceAll("\\s", "").toLowerCase();
        File file = new File("/tmp/" + name + ".json");
        //check name:
        try {
            ServiceOffer serviceOffer = serviceOfferRepository.getByEnterpriseIdAndName(enterpriseId, name);
            if (serviceOffer != null) {
                throw new BadDataException("Duplicate service offering");
            }
            //create VC for service offering
            String domain = enterprise.getSubDomainName();
            String did = "did:web:" + enterprise.getSubDomainName();
            HashMap<String, String> data = new HashMap<>();
            data.put("name", name);
            data.put("fileName", file.getName());
            data.put("description", request.getDescription());
            data.put("policyUrl", request.getPolicy());
            data.put("termsAndConditionsUrl", request.getTerms());
            data.put("termsAndConditionsHash", "70c1d713215f95191a11d38fe2341faed27d19e083917bc8732ca4fea4976700");
            data.put("requestType", request.getRequestType());
            data.put("accessType", request.getAccessType());
            data.put("formatType", request.getFormatType());
            CreateVCRequest createVCRequest = CreateVCRequest.builder()
                    .data(data)
                    .templateId("ServiceOffering")
                    .domain(domain)
                    .privateKeyUrl(s3Utils.getPreSignedUrl(enterpriseId + "/pkcs8_" + domain + ".key"))
                    .build();
            ResponseEntity<Map<String, Object>> vc = signerClient.createVc(createVCRequest);
            String serviceOfferingString = objectMapper.writeValueAsString(((Map<String, Object>) vc.getBody().get("data")).get("verifiableCredential"));

            //save credentials
            EnterpriseCredential enterpriseCredential = EnterpriseCredential.builder()
                    .label(name)
                    .enterpriseId(enterpriseId)
                    .credentials(serviceOfferingString)
                    .build();
            enterpriseCredential = enterpriseCredentialRepository.save(enterpriseCredential);

            //Save file in S3
            FileUtils.writeStringToFile(file, serviceOfferingString, Charset.defaultCharset());
            s3Utils.uploadFile(enterpriseId + "/" + name + ".json", file);

            //Store service offer
            serviceOffer = ServiceOffer.builder()
                    .enterpriseId(enterpriseId)
                    .meta(request.getMeta())
                    .copyrightOwnedBy(did)
                    .name(name)
                    .label(request.getName())
                    .credentialId(enterpriseCredential.getId())
                    .description(request.getDescription())
                    .policy(request.getPolicy())
                    .producedBy(did)
                    .subjectDid(did)
                    .accessType(request.getAccessType())
                    .requestType(request.getRequestType())
                    .formatType(request.getFormatType())
                    .terms(request.getTerms())
                    .termsHash("70c1d713215f95191a11d38fe2341faed27d19e083917bc8732ca4fea4976700") //TODO this is hard coded
                    .build();
            return serviceOfferRepository.save(serviceOffer);
        } finally {
            CommonUtils.deleteFile(file);
        }
    }

    /**
     * Service offer list list.
     *
     * @return the list
     */
    public List<ServiceOfferView> allServiceOfferList(long enterpriseId, String query) {
        if (StringUtils.isBlank(query)) {
            return serviceOfferViewRepository.getAllServiceOffers(enterpriseId);
        } else {
            return serviceOfferViewRepository.getAllServiceOffers(enterpriseId, query);
        }

    }

    /**
     * Service offer list list.
     *
     * @param enterpriseId the enterprise id
     * @return the list
     */
    public List<ServiceOfferView> serviceOfferList(long enterpriseId) {
        return serviceOfferViewRepository.getByEnterpriseId(enterpriseId);
    }

    /**
     * Gets enterprise credentials.
     *
     * @param enterpriseId the enterprise id
     * @return the enterprise credentials
     */
    public List<EnterpriseCredential> getEnterpriseCredentials(long enterpriseId) {
        enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);
        return enterpriseCredentialRepository.getByEnterpriseId(enterpriseId);
    }

    /**
     * Service offer details map.
     *
     * @param offerId the offer id
     * @param vp      the vp
     * @return the map
     */
    public Map<String, Object> serviceOfferDetails(long enterpriseId, long offerId, Map<String, Object> vp) {
        ServiceOffer serviceOffer = serviceOfferRepository.findById(offerId).orElseThrow(EntityNotFoundException::new);
        //verify if VP is
        VerifyRequest verifyRequest = VerifyRequest.builder()
                .policies(Set.of("checkSignature", "gxCompliance"))
                .credential(vp)
                .build();
        ResponseEntity<Map<String, Object>> verify = signerClient.verify(verifyRequest);
        boolean valid = Boolean.parseBoolean(((Map<String, Object>) verify.getBody().get("data")).get("checkSignature").toString());
        boolean gxComplianceValid = Boolean.parseBoolean(((Map<String, Object>) verify.getBody().get("data")).get("gxCompliance").toString());
        Validate.isFalse(valid).launch(new BadDataException("Can not verify signature of VP"));
        Validate.isFalse(gxComplianceValid).launch(new BadDataException("Can not verify signature of Gaia-X"));

        //save access log
        ServiceAccessLog serviceAccessLog = ServiceAccessLog.builder()
                .provider(serviceOffer.getEnterpriseId())
                .consumer(enterpriseId)
                .serviceId(serviceOffer.getId())
                .build();
        serviceAccessLogRepository.save(serviceAccessLog);
        return serviceOffer.getMeta();
    }

    /**
     * Export keys map.
     *
     * @param enterpriseId the enterprise id
     * @return the map
     */
    public Map<String, String> exportKeys(long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);
        Map<String, String> keys = new HashMap<>();
        keys.put("key", s3Utils.getPreSignedUrl(s3Utils.getPreSignedUrl(enterpriseId + "/" + enterprise.getSubDomainName() + ".key")));
        keys.put("pkcs8Key", s3Utils.getPreSignedUrl(s3Utils.getPreSignedUrl(enterpriseId + "/pkcs8_" + enterprise.getSubDomainName() + ".key")));
        return keys;
    }

    public ServiceOfferView getServiceOfferDetailsById(long enterpriseId, long id) {
        ServiceOfferView serviceOfferView = serviceOfferViewRepository.getByEnterpriseIdAndId(enterpriseId, id);
        Validate.isNull(serviceOfferView).launch(new BadDataException("invalid.service.offer.id"));
        ServiceOffer serviceOffer = serviceOfferRepository.getByIdAndEnterpriseId(id, enterpriseId);
        Validate.isNull(serviceOfferView).launch(new BadDataException("invalid.service.offer.id"));
        Validate.isNull(serviceOffer).launch(new BadDataException());
        serviceOfferView.setMeta(serviceOffer.getMeta());
        return serviceOfferView;
    }

    public Enterprise changeStatus(long enterpriseId, int status) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);
        enterprise.setStatus(status);

        return enterpriseRepository.save(enterprise);
    }
}
