/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsense.gaiax.client.CreateVCRequest;
import com.smartsense.gaiax.client.SignerClient;
import com.smartsense.gaiax.dao.entity.Admin;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.entity.ServiceOffer;
import com.smartsense.gaiax.dao.repository.AdminRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseCredentialRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dao.repository.ServiceOfferRepository;
import com.smartsense.gaiax.dto.CreateServiceOfferingRequest;
import com.smartsense.gaiax.dto.LoginResponse;
import com.smartsense.gaiax.dto.SessionDTO;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.EntityNotFoundException;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.JWTUtil;
import com.smartsense.gaiax.utils.S3Utils;
import com.smartsense.gaiax.utils.Validate;
import org.apache.commons.io.FileUtils;
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

/**
 * The type Enterprise service.
 */
@Service
public class EnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseService.class);

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final S3Utils s3Utils;

    private final ServiceOfferRepository serviceOfferRepository;

    private final SignerClient signerClient;

    private final ObjectMapper objectMapper;

    private final AdminRepository adminRepository;

    private final JWTUtil jwtUtil;


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
     */
    public EnterpriseService(EnterpriseRepository enterpriseRepository, EnterpriseCredentialRepository enterpriseCredentialRepository, S3Utils s3Utils, ServiceOfferRepository serviceOfferRepository, SignerClient signerClient, ObjectMapper objectMapper, AdminRepository adminRepository, JWTUtil jwtUtil) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.s3Utils = s3Utils;
        this.serviceOfferRepository = serviceOfferRepository;
        this.signerClient = signerClient;
        this.objectMapper = objectMapper;
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
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
            Validate.isNull(admin).launch(new BadDataException("invalid.username.or.password"));
            boolean valid = BCrypt.checkpw(password, admin.getPassword());
            Validate.isFalse(valid).launch(new BadDataException("invalid.username.or.password"));
            SessionDTO sessionDTO = SessionDTO.builder()
                    .role(StringPool.ADMIN_ROLE)
                    .email(admin.getUserName())
                    .enterpriseId(-1)
                    .build();
            return LoginResponse.builder()
                    .token(jwtUtil.generateToken(sessionDTO))
                    .session(sessionDTO)
                    .build();
        } else {
            //login aa enterprise
            Enterprise enterprise = enterpriseRepository.getByEmail(email);
            Validate.isNull(enterprise).launch(new BadDataException("invalid.username.or.password"));
            boolean valid = BCrypt.checkpw(password, enterprise.getPassword());
            Validate.isFalse(valid).launch(new BadDataException("invalid.username.or.password"));
            SessionDTO sessionDTO = SessionDTO.builder()
                    .role(StringPool.ADMIN_ROLE)
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
            data.put("fileName", name); //TODO need to discuss
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
            FileUtils.writeStringToFile(file, serviceOfferingString);
            s3Utils.uploadFile(enterpriseId + "/" + name + ".json", file);

            //Store service offer
            serviceOffer = ServiceOffer.builder()
                    .enterpriseId(enterpriseId)
                    .meta(request.getMeta())
                    .license(request.getLicense())
                    .copyrightOwnedBy(did)
                    .name(name)
                    .credentialId(enterpriseCredential.getId())
                    .description(request.getDescription())
                    .policy(request.getPolicies())
                    .expirationDate(request.getExpirationDate())
                    .producedBy(did)
                    .subjectDid(did)
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
    public List<ServiceOffer> serviceOfferList() {
        return serviceOfferRepository.findAll();
    }

    /**
     * Service offer list list.
     *
     * @param enterpriseId the enterprise id
     * @return the list
     */
    public List<ServiceOffer> serviceOfferList(long enterpriseId) {
        return serviceOfferRepository.getByEnterpriseId(enterpriseId);
    }


    /**
     * Gets service offering details.
     *
     * @param enterpriseId the enterprise id
     * @param offerId      the offer id
     * @return the service offering details
     */
    public ServiceOffer getServiceOfferingDetails(long enterpriseId, long offerId) {
        ServiceOffer serviceOffer = serviceOfferRepository.getByIdAndEnterpriseId(offerId, enterpriseId);
        Validate.isNull(serviceOffer).launch(new EntityNotFoundException());
        //TODO need VC?

        return serviceOffer;
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
}
