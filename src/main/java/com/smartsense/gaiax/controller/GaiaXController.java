/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.controller;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.entity.ServiceOffer;
import com.smartsense.gaiax.dto.*;
import com.smartsense.gaiax.exception.SecurityException;
import com.smartsense.gaiax.request.RegisterRequest;
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
import java.util.List;
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
    public static final String CREATED = "Created";
    private final RegistrationService registrationService;

    private final DomainService domainService;

    private final CertificateService certificateService;

    private final K8SService k8SService;

    private final EnterpriseService enterpriseService;

    private final SignerService signerService;


    /**
     * Instantiates a new Gaia x controller.
     *
     * @param registrationService the registration service
     * @param domainService       the domain service
     * @param certificateService  the certificate service
     * @param k8SService          the k 8 s service
     * @param enterpriseService   the enterprise service
     * @param signerService       the signer service
     */
    public GaiaXController(RegistrationService registrationService, DomainService domainService, CertificateService certificateService, K8SService k8SService, EnterpriseService enterpriseService, SignerService signerService) {
        this.registrationService = registrationService;
        this.domainService = domainService;
        this.certificateService = certificateService;
        this.k8SService = k8SService;
        this.enterpriseService = enterpriseService;
        this.signerService = signerService;
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
    @Operation(summary = "Login(type=1 for login as admin, type =2 for login as enterprise)")
    @PostMapping(path = "login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<LoginResponse> login(@RequestBody @Valid LoginRequest registerRequest) {
        return CommonResponse.of(enterpriseService.login(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getType()));
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
     * @throws SchedulerException the scheduler exception
     */
    @Operation(summary = "Register enterprise in the system. This will save enterprise data in database and create job to create subdomain, role: admin")
    @PostMapping(path = "register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = "Onboarding")
    public Enterprise registerBusiness(@RequestBody @Valid RegisterRequest registerRequest,
                                       @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO
    ) throws SchedulerException {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        return registrationService.registerEnterprise(registerRequest);
    }


    /**
     * List enterprise common response.
     *
     * @param sessionDTO the session dto
     * @return the common response
     * @throws SchedulerException the scheduler exception
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
    @Operation(summary = "Resume onboarding process from sub domain creation, role Admin")
    @GetMapping(path = "subdomain/{enterpriseId}")
    public String createSubDomain(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        domainService.createSubDomain(enterpriseId);
        return CREATED;
    }

    /**
     * Create certificate string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from SLL certificate creation, role = admin")
    @GetMapping(path = "certificate/{enterpriseId}")
    public String createCertificate(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        certificateService.createSSLCertificate(enterpriseId, null);
        return CREATED;
    }


    /**
     * Create ingress string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from ingress creation, role = admin")
    @GetMapping(path = "ingress/{enterpriseId}")
    public String createIngress(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        k8SService.createIngress(enterpriseId);
        return CREATED;
    }

    /**
     * Create did string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from did creation, role-=admin")
    @GetMapping(path = "did/{enterpriseId}")
    public String createDid(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        signerService.createDid(enterpriseId);
        return CREATED;
    }

    /**
     * Create participant json string.
     *
     * @param enterpriseId the enterprise id
     * @param sessionDTO   the session dto
     * @return the string
     */
    @Tag(name = "Onboarding")
    @Operation(summary = "Resume onboarding process from participant credential creation, role Admin")
    @GetMapping(path = "participant/{enterpriseId}")
    public String createParticipantJson(@PathVariable(name = "enterpriseId") long enterpriseId, @Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ADMIN_ROLE), sessionDTO.getRole());
        signerService.createParticipantJson(enterpriseId);
        return CREATED;
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
    public CommonResponse<List<ServiceOffer>> createServiceOffering(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.serviceOfferList(sessionDTO.getEnterpriseId()));
    }

    /**
     * Gets all service offers.
     *
     * @param sessionDTO the session dto
     * @return the all service offers
     */
    @Tag(name = "Catalogue")
    @Operation(summary = "List all service offering: Pagination, search and sort wil be added, role = enterprise")
    @GetMapping(path = "catalogue", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<List<ServiceOffer>> getAllServiceOffers(@Parameter(hidden = true) @RequestAttribute(value = StringPool.SESSION_DTO) SessionDTO sessionDTO) {
        validateAccess(Set.of(StringPool.ENTERPRISE_ROLE), sessionDTO.getRole());
        return CommonResponse.of(enterpriseService.serviceOfferList());
    }
}
