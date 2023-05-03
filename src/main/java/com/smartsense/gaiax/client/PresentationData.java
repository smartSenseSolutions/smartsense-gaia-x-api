/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresentationData {
    private String state;

    private List<Presentation> presentations;

}
