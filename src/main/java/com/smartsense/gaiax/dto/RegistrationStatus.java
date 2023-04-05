/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dto;

public enum RegistrationStatus {

    STARTED(1),

    DOMAIN_CREATED(2),

    DOMAIN_CREATION_FAILED(3),

    CERTIFICATE_CREATED(4),

    CERTIFICATE_CREATION_FAILED(5);

    private final int status;

    RegistrationStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
