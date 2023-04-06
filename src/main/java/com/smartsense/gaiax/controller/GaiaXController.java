/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.controller;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.request.RegisterRequest;
import com.smartsense.gaiax.service.domain.DomainService;
import com.smartsense.gaiax.service.enterprise.RegistrationService;
import com.smartsense.gaiax.service.k8s.K8SService;
import com.smartsense.gaiax.service.ssl.CertificateService;
import io.kubernetes.client.openapi.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.quartz.SchedulerException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author Nitin
 * @version 1.0
 */
@RestController
public class GaiaXController {

    private final RegistrationService registrationService;

    private final DomainService domainService;

    private final CertificateService certificateService;

    private final K8SService k8SService;


    public GaiaXController(RegistrationService registrationService, DomainService domainService, CertificateService certificateService, K8SService k8SService) {
        this.registrationService = registrationService;
        this.domainService = domainService;
        this.certificateService = certificateService;
        this.k8SService = k8SService;
    }

    @Operation(summary = "For testing purpose only")
    @GetMapping(path = "test")
    public String test() {
        return "total enterprise ->" + registrationService.test();
    }

    @Operation(summary = "Register enterprise in the system. This will save enterprise data in database and create job to create subdomain")
    @PostMapping(path = "register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Enterprise registerBusiness(@RequestBody @Valid RegisterRequest registerRequest) throws SchedulerException {
        return registrationService.registerEnterprise(registerRequest);
    }


    @Operation(summary = "Will be removed. to test create subdomain in standalone mode")
    @GetMapping(path = "subdomain/{enterpriseId}")
    public String createSubDomain(@PathVariable(name = "enterpriseId") long enterpriseId) {
        domainService.createSubDomain(enterpriseId);
        return "Created";
    }

    @Operation(summary = "Will be removed. to test create subdomain in standalone mode")
    @GetMapping(path = "certificate/{enterpriseId}")
    public String createCertificate(@PathVariable(name = "enterpriseId") long enterpriseId) {
        certificateService.fetchCertificate(enterpriseId);
        return "Created";
    }


    @Operation(summary = "Will be removed. to test create subdomain in standalone mode")
    @GetMapping(path = "tls/{enterpriseId}")
    public String createTlsSecret(@PathVariable(name = "enterpriseId") long enterpriseId) throws IOException, ApiException {
        k8SService.createIngress(enterpriseId);
        return "Created";
    }
}
