/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.dao.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Subselect(value = "select so.id , so.credential_id , so.subject_did , so .\"name\" , so.produced_by , so.copyright_owned_by , so.description , so.license , so.expiration_date , e.legal_name as enterprise_name, e.sub_domain_name  from service_offer so inner join enterprise e  on e.id = so.enterprise_id")
@Immutable
@Getter
@Setter
public class ServiceOfferView {

    @Id
    private Long id;

    private String enterpriseName;

    private String subDomainName;

    private Long credentialId;

    private String subjectDid;

    private String name;

    private String producedBy;

    private String copyrightOwnedBy;

    private String description;

    private String license;

    private String expirationDate;

    @Transient
    private String offerLink;

    public String getOfferLink() {
        return "https://" + this.subDomainName + "/.well-known/" + name + ".json";
    }
}
