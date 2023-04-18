/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.Set;

/**
 * The type Service offer.
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    @Convert(converter = StringToSetConvertor.class)
    private Set<String> policy;

    private String expirationDate;

    @JsonIgnore
    @Convert(converter = StringToMapConvertor.class)
    private Map<String, Object> meta;
}
