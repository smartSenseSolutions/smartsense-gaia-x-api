/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Presentation {
    private Map<String, String> credentialSubject;
}
