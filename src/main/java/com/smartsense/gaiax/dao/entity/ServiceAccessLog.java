/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceAccessLog extends SuperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "serial")
    private Long id;

    private Long provider;

    private Long consumer;

    private Long serviceId;
}
