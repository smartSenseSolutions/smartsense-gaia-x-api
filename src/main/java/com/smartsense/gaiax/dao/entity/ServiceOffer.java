/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ServiceOffer extends SuperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "serial")
    private Long id;

    private Long enterpriseId;

    private Long credentialId;

    private String subjectDid;

    private String name;

    private String producedBy;

    private String copyrightOwnedBy;

    private String description;

    private String license;

    private String policy;

    private String expirationDate;

    @Convert(converter = StringToMapConvertor.class)
    private Map<String, Object> meta;
}
