/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.controller;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dto.CommonResponse;
import com.smartsense.gaiax.request.RegisterRequest;
import com.smartsense.gaiax.service.domain.DomainService;
import com.smartsense.gaiax.service.enterprise.EnterpriseService;
import com.smartsense.gaiax.service.enterprise.RegistrationService;
import com.smartsense.gaiax.service.k8s.K8SService;
import com.smartsense.gaiax.service.signer.SignerService;
import com.smartsense.gaiax.service.ssl.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.quartz.SchedulerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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

    /**
     * Test string.
     *
     * @return the string
     */
    @Operation(summary = "For testing purpose only")
    @GetMapping(path = "test")
    public String test() {
        return "total enterprise ->" + registrationService.test();
    }

    /**
     * Gets enterprise files.
     *
     * @param fileName the file name
     * @param host     the host
     * @return the enterprise files
     * @throws IOException the io exception
     */
    @Operation(summary = "Get .well-known files")
    @GetMapping(path = ".well-known/{fileName}")
    public String getEnterpriseFiles(@PathVariable(name = "fileName") String fileName, @RequestHeader(name = HttpHeaders.HOST) String host) throws IOException {
        return enterpriseService.getEnterpriseFiles(host, fileName);
    }

    /**
     * Register business enterprise.
     *
     * @param registerRequest the register request
     * @return the enterprise
     * @throws SchedulerException the scheduler exception
     */
    @Operation(summary = "Register enterprise in the system. This will save enterprise data in database and create job to create subdomain")
    @PostMapping(path = "register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Enterprise registerBusiness(@RequestBody @Valid RegisterRequest registerRequest) throws SchedulerException {
        return registrationService.registerEnterprise(registerRequest);
    }


    /**
     * List enterprise common response.
     *
     * @return the common response
     * @throws SchedulerException the scheduler exception
     */
    @Operation(summary = "get all enterprises, pagination, search and sort will be added")
    @GetMapping(path = "enterprises", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<List<Enterprise>> listEnterprise() throws SchedulerException {
        return CommonResponse.of(enterpriseService.listEnterprise());
    }

    /**
     * Gets enterprise.
     *
     * @param enterpriseId the enterprise id
     * @return the enterprise
     * @throws SchedulerException the scheduler exception
     */
    @Operation(summary = "Get enterprise by id")
    @GetMapping(path = "enterprises/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CommonResponse<Enterprise> getEnterprise(@PathVariable(name = "id") long enterpriseId) throws SchedulerException {
        return CommonResponse.of(enterpriseService.getEnterprise(enterpriseId));
    }


    /**
     * Create sub domain string.
     *
     * @param enterpriseId the enterprise id
     * @return the string
     */
    @Operation(summary = "Will be removed. to test create subdomain in standalone mode")
    @GetMapping(path = "subdomain/{enterpriseId}")
    public String createSubDomain(@PathVariable(name = "enterpriseId") long enterpriseId) {
        domainService.createSubDomain(enterpriseId);
        return CREATED;
    }

    /**
     * Create certificate string.
     *
     * @param enterpriseId the enterprise id
     * @return the string
     */
    @Operation(summary = "Will be removed. to test create subdomain in standalone mode")
    @GetMapping(path = "certificate/{enterpriseId}")
    public String createCertificate(@PathVariable(name = "enterpriseId") long enterpriseId) {
        certificateService.createSSLCertificate(enterpriseId, null);
        return CREATED;
    }


    /**
     * Create ingress string.
     *
     * @param enterpriseId the enterprise id
     * @return the string
     */
    @Operation(summary = "Will be removed. to test create ingress")
    @GetMapping(path = "tls/{enterpriseId}")
    public String createIngress(@PathVariable(name = "enterpriseId") long enterpriseId) {
        k8SService.createIngress(enterpriseId);
        return CREATED;
    }

    /**
     * Create did string.
     *
     * @param enterpriseId the enterprise id
     * @return the string
     */
    @Operation(summary = "Will be removed. to test create did")
    @GetMapping(path = "did/{enterpriseId}")
    public String createDid(@PathVariable(name = "enterpriseId") long enterpriseId) {
        signerService.createDid(enterpriseId);
        return CREATED;
    }

    /**
     * Create participant json string.
     *
     * @param enterpriseId the enterprise id
     * @return the string
     */
    @Operation(summary = "Will be removed. to test create participant json")
    @GetMapping(path = "participant/{enterpriseId}")
    public String createParticipantJson(@PathVariable(name = "enterpriseId") long enterpriseId) {
        signerService.createParticipantJson(enterpriseId);
        return CREATED;
    }
}
