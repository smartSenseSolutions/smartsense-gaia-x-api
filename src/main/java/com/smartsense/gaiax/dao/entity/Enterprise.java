/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * The type Enterprise.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Enterprise extends SuperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "serial")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String legalName;

    @Column(nullable = false, unique = true)
    private String subDomainName;

    @Column(nullable = false, unique = true)
    private String legalRegistrationNumber;

    @Column(nullable = false)
    private String legalRegistrationType;

    @Column(nullable = false)
    private String headquarterAddress;

    @Column(nullable = false)
    private String legalAddress;

    @Column(nullable = false)
    private Integer status;

    @Transient
    private String didJson;

    @Transient
    private String participantJson;

    @Transient
    private String certificateChain;

    @Transient
    private String did;

}
