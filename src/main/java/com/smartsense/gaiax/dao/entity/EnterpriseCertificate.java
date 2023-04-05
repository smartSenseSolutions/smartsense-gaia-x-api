/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnterpriseCertificate extends SuperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "serial")
    private Long id;

    @Column(nullable = false, unique = true)
    private Long enterpriseId;

    @Column(nullable = false, unique = true)
    private String privateKey;

    @Column(nullable = false, unique = true)
    private String certificateChain;

    @Column(nullable = false, unique = true)
    private String csr;

}
