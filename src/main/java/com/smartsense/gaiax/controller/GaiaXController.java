package com.smartsense.gaiax.controller;

import com.smartsense.gaiax.service.domain.RegistrationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nitin
 * @version 1.0
 */
@RestController
public class GaiaXController {


    private final RegistrationService registrationService;

    public GaiaXController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping(path = "register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void registerBusiness(){
    }
}
