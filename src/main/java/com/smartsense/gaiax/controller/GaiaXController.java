/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.entity.ServiceOffer;
import com.smartsense.gaiax.dao.entity.ServiceOfferView;
import com.smartsense.gaiax.dto.*;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.SecurityException;
import com.smartsense.gaiax.service.TinyUrlService;
import com.smartsense.gaiax.service.credential.CredentialService;
import com.smartsense.gaiax.service.domain.DomainService;
import com.smartsense.gaiax.service.enterprise.EnterpriseService;
import com.smartsense.gaiax.service.enterprise.RegistrationService;
import com.smartsense.gaiax.service.k8s.K8SService;
import com.smartsense.gaiax.service.signer.SignerService;
import com.smartsense.gaiax.service.ssl.CertificateService;
import com.smartsense.gaiax.utils.Validate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.quartz.SchedulerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The type Gaia x controller.
 *
 * @author Nitin
 * @version 1.0
 */
@RestController
public class GaiaXController {

    /**
     * The constant CREATED.
     */
    private final RegistrationService registrationService;

    private final DomainService domainService;

    private final CertificateService certificateService;

    private final K8SService k8SService;

    private final EnterpriseService enterpriseService;

    private final SignerService signerService;

    private final CredentialService credentialService;

    private final TinyUrlService tinyUrlService;


    /**
     * Instantiates a new Gaia x controller.
     *
     * @param registrationService the registration service
     * @param domainService       the domain service
     * @param certificateService  the certificate service
     * @param k8SService          the k 8 s service
     * @param enterpriseService   the enterprise service
     * @param signerService       the signer service
     * @param credentialService   the credential service
     * @param tinyUrlService
     */
    public GaiaXController(RegistrationService registrationService, DomainService domainService, CertificateService certificateService, K8SService k8SService, EnterpriseService enterpriseService, SignerService signerService, CredentialService credentialService, TinyUrlService tinyUrlService) {
        this.registrationService = registrationService;
        this.domainService = domainService;
        this.certificateService = certificateService;
        this.k8SService = k8SService;
        this.enterpriseService = enterpriseService;
        this.signerService = signerService;
        this.credentialService = credentialService;
        this.tinyUrlService = tinyUrlService;
    }

    private void validateAccess(Set<Integer> requiredRoles, int userRole) {
        boolean contains = requiredRoles.contains(userRole);
        Validate.isFalse(contains).launch(new SecurityException("can not access API"));
    }

    /**
     * Register business enterprise.
     *
     * @param registerRequest the register request
     * @return the enterprise
     * @throws SchedulerException the scheduler exception
     */
    @Tag(name = "Login")
    @Operation(summary = "Login as Admin")
    @PostMapping(path = "login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<LoginResponse> login(@RequestBody @Valid LoginRequest registerRequest) {
        return CommonResponse.of(enterpriseService.login(registerRequest.getEmail(), registerRequest.getPassword()));
    }


    /**
     * Verify presentation common response.
     *
     * @param presentationId the presentation id
     * @return the common response
     * @throws JsonProcessingException the json processing exception
     */
    @Tag(name = "Login")
    @Operation(summary = "Verify membership VP")
    @GetMapping(path = "verify/presentation", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<LoginResponse> verifyPresentation(@RequestParam(name = "presentationId") String presentationId) throws JsonProcessingException {
        return CommonResponse.of(enterpriseService.verifyPresentation(presentationId));
    }

    /**
     * Gets enterprise files.
     *
     * @param fileName the file name
     * @param host     the host
     * @return the enterprise files
     * @throws IOException the io exception
     */
    @Operation(summary = "Get .well-known files, this is public API")
    @GetMapping(path = ".well-known/{fileName}")
    @Tag(name = "Well-known")
    public String getEnterpriseFiles(@PathVariable(name = "fileName") String fileName, @RequestHeader(name = HttpHeaders.HOST) String host) throws IOException {
        return enterpriseService.getEnterpriseFiles(host, fileName);
    }

    /**
     * Register business enterprise.
     *
     * @param registerRequest the register request
     * @param sessionDTO      the session dto
     * @return the enterprise
     * @throws SchedulerException      the scheduler exception
     * @throws JsonProcessingException the json processing exception
     */
    @Operation(summary = "Register enterprise in the system. This will save enterprise data in database and create job to create subdomain, role: admin")
    @PostMapping(path = "register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = "Onboarding")
    public CommonResponse<Enterprise> registerBusiness(@RequestBody @Valid RegisterRequest registerRequest,
                                                       @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO
    ) throws SchedulerException, JsonProcessingException {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        return CommonResponse.of(registrationService.registerEnterprise(registerRequest), "Enterprise registration process started and membership credentials issued in PCM");
    }


    /**
     * List enterprise common response.
     *
     * @param sessionDTO the session dto
     * @return the common response
     */
    @Tag(name = "Enterprise")
    @Operation(summary = "get all enterprises, pagination, search and sort will be added, role: Admin")
    @GetMapping(path = "enterprises/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<List<Enterprise>> listEnterprise(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.listEnterprise());
    }

    /**
     * Gets enterprise details.
     *
     * @param sessionDTO the session dto
     * @return the enterprise details
     */
    @Tag(name = "Enterprise")
    @Operation(summary = "get logged in user's enterprises, role: Enterprise")
    @GetMapping(path = "enterprises", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Enterprise> getEnterpriseDetails(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.getEnterprise(sessionDTO.getEnterpriseId()));
    }


    /**
     * Gets enterprise.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the enterprise
     * @throws SchedulerException the scheduler exception
     */
    @Tag(name = "Enterprise")
    @Operation(summary = "Get enterprise by id, role admin")
    @GetMapping(path = "enterprises/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Enterprise> getEnterprise(@PathVariable(name = "id") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.getEnterprise(enterpriseId));
    }


    /**
     * Create sub domain string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from sub domain creation, role Admin, (only used for manual step in case of failure)")
    @GetMapping(path = "subdomain/{enterpriseId}")
    public CommonResponse<Map<String, String>> createSubDomain(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        domainService.createSubDomain(enterpriseId);
        Map<String, String> map = new HashMap<>();
        map.put("message", "Subdomain creation started");
        return CommonResponse.of(map);
    }

    /**
     * Create certificate string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from SLL certificate creation, role = admin, (only used for manual step in case of failure)")
    @GetMapping(path = "certificate/{enterpriseId}")
    public CommonResponse<Enterprise> createCertificate(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        Enterprise enterprise1 = enterpriseService.getEnterprise(enterpriseId);
        if (enterprise1.getStatus() != RegistrationStatus.CERTIFICATE_CREATION_FAILED.getStatus()) {
            throw new BadDataException("Status is not certification creation failed");
        }
        Enterprise enterprise = enterpriseService.changeStatus(enterpriseId, RegistrationStatus.CERTIFICATE_CREATION_IN_PROCESS.getStatus());
        certificateService.createSSLCertificate(enterpriseId);
        return CommonResponse.of(enterprise);
    }


    /**
     * Create ingress string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from ingress creation, role = admin, (only used for manual step in case of failure)")
    @GetMapping(path = "ingress/{enterpriseId}")
    public CommonResponse<Map<String, String>> createIngress(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        k8SService.createIngress(enterpriseId);
        Map<String, String> map = new HashMap<>();
        map.put("message", "Ingress creation started");
        return CommonResponse.of(map);
    }

    /**
     * Create did string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from did creation, role-=admin, (only used for manual step in case of failure)")
    @GetMapping(path = "did/{enterpriseId}")
    public CommonResponse<Map<String, String>> createDid(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        signerService.createDid(enterpriseId);
        Map<String, String> map = new HashMap<>();
        map.put("message", "did creation started");
        return CommonResponse.of(map);
    }

    /**
     * Create participant json string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from participant credential creation, role Admin, (only used for manual step in case of failure)")
    @GetMapping(path = "participant/{enterpriseId}")
    public CommonResponse<Map<String, String>> createParticipantJson(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        signerService.createParticipantJson(enterpriseId);
        Map<String, String> map = new HashMap<>();
        map.put("message", "participant json creation started");
        return CommonResponse.of(map);
    }

    /**
     * Gets enterprise credentials.
     *
     * @param sessionDTO the session dto
     * @return the enterprise credentials
     */
    @Tag(name = "Credentials")
    @Operation(summary = "Get all issued VC of enterprise, role = Enterprise")
    @GetMapping(path = "enterprises/vcs")
    public CommonResponse<List<EnterpriseCredential>> getEnterpriseCredentials(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.getEnterpriseCredentials(sessionDTO.getEnterpriseId()));
    }

    /**
     * Create service offering common response.
     *
     * @param sessionDTO the session dto
     * @param request    the request
     * @return the common response
     * @throws IOException the io exception
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "Create Service offering for enterprise, role = enterprise")
    @PostMapping(path = "enterprises/service-offers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<ServiceOffer> createServiceOffering(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO,
                                                              @Valid @RequestBody CreateServiceOfferingRequest request
    ) throws IOException {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.createServiceOffering(sessionDTO.getEnterpriseId(), request));
    }

    /**
     * Create service offering common response.
     *
     * @param sessionDTO the session dto
     * @return the common response
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "Get service offering of enterprise, role enterprise")
    @GetMapping(path = "enterprises/service-offers", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<List<ServiceOfferView>> serviceOfferList(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.serviceOfferList(sessionDTO.getEnterpriseId()));
    }

    /**
     * Create service offering common response.
     *
     * @param sessionDTO the session dto
     * @param id         the id
     * @return the common response
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "Get service offering by id of enterprise, details with meta information, role enterprise")
    @GetMapping(path = "enterprises/service-offers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<ServiceOfferView> getServiceOfferDetailsById(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO,
                                                                       @PathVariable(name = "id") long id) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.getServiceOfferDetailsById(sessionDTO.getEnterpriseId(), id));
    }

    /**
     * Gets all service offers.
     *
     * @param sessionDTO the session dto
     * @param query      the query
     * @return the all service offers
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "List all service offering: Pagination, search and sort wil be added, role = enterprise")
    @GetMapping(path = "catalogue", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<List<ServiceOfferView>> getAllServiceOffers(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO
            , @RequestParam(name = "query", required = false) String query) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.allServiceOfferList(sessionDTO.getEnterpriseId(), query));
    }


    /**
     * Create VP
     *
     * @param sessionDTO the session dto
     * @param name       the name
     * @return the all service offers
     * @throws JsonProcessingException the json processing exception
     */
    @Tag(name = "Credentials")
    @Operation(summary = "Create/Get VP of Gaia-x participant of any credential, role = enterprise")
    @GetMapping(path = "enterprises/vc/{name}/vp", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Map<String, Object>> createVP(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO, @PathVariable(name = "name") String name) throws JsonProcessingException {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(credentialService.createVP(sessionDTO.getEnterpriseId(), name));
    }

    /**
     * Service offer details common response.
     *
     * @param sessionDTO     the session dto
     * @param offerId        the offer id
     * @param presentationId the presentation id
     * @return the common response
     * @throws JsonProcessingException the json processing exception
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "Get Service offer details. This API will consume credential presentation id of PCM and check with vereign API if credentials is shared , role = enterprise")
    @GetMapping(path = "enterprises/service-offers/{offerId}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<ServiceOfferDetailsResponse> serviceOfferDetails(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO,
                                                                           @PathVariable(name = "offerId") long offerId,
                                                                           @RequestParam(name = "presentationId") String presentationId) throws JsonProcessingException {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.serviceOfferDetails(sessionDTO.getEnterpriseId(), offerId, presentationId));
    }

    /**
     * Export keys common response.
     *
     * @param sessionDTO the session dto
     * @return the common response
     * @throws JsonProcessingException the json processing exception
     */
    @Tag(name = "Enterprise")
    @Operation(summary = "export private keys. it will return s3 pre-signed URLs")
    @GetMapping(path = "enterprises/keys/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Map<String, String>> exportKeys(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) throws JsonProcessingException {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.exportKeys(sessionDTO.getEnterpriseId()));
    }

    @Tag(name = "Utils")
    @Operation(summary = "Create tiny URL")
    @PostMapping(path = "tinyurl", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Map<String, String>> createTinyUrl(@RequestBody Map<String, String> map) {
        return CommonResponse.of(tinyUrlService.createTinyUrl(map.get("url")));
    }
}