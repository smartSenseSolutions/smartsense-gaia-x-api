/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPresentationResponse {
    private PresentationData data;

    private String message;

}

