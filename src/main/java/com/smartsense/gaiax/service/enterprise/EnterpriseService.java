/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCredential;
import com.smartsense.gaiax.dao.entity.ServiceOffer;
import com.smartsense.gaiax.dao.repository.EnterpriseCredentialRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dao.repository.ServiceOfferRepository;
import com.smartsense.gaiax.dto.CreateServiceOfferingRequest;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.EntityNotFoundException;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.S3Utils;
import com.smartsense.gaiax.utils.Validate;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The type Enterprise service.
 */
@Service
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCredentialRepository enterpriseCredentialRepository;

    private final S3Utils s3Utils;

    private final ServiceOfferRepository serviceOfferRepository;

    /**
     * Instantiates a new Enterprise service.
     *
     * @param enterpriseRepository           the enterprise repository
     * @param enterpriseCredentialRepository
     * @param s3Utils                        the s 3 utils
     * @param serviceOfferRepository
     */
    public EnterpriseService(EnterpriseRepository enterpriseRepository, EnterpriseCredentialRepository enterpriseCredentialRepository, S3Utils s3Utils, ServiceOfferRepository serviceOfferRepository) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCredentialRepository = enterpriseCredentialRepository;
        this.s3Utils = s3Utils;
        this.serviceOfferRepository = serviceOfferRepository;
    }

    /**
     * Gets enterprise.
     *
     * @param id the id
     * @return the enterprise
     */
    public Enterprise getEnterprise(long id) {
        //TODO more details to added
        Enterprise enterprise = enterpriseRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        enterprise.setDidJson("https://" + enterprise.getSubDomainName() + "/.well-known/did.json");
        enterprise.setParticipantJson("https://" + enterprise.getSubDomainName() + "/.well-known/participant.json");
        enterprise.setCertificateChain("https://" + enterprise.getSubDomainName() + "/.well-known/x509CertificateChain.pem");
        return enterprise;
    }

    /**
     * List enterprise list.
     *
     * @return the list
     */
    public List<Enterprise> listEnterprise() {
        return enterpriseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Gets enterprise files.
     *
     * @param hostName the host name
     * @param fileName the file name
     * @return the enterprise files
     * @throws IOException the io exception
     */
    public String getEnterpriseFiles(String hostName, String fileName) throws IOException {
        File file = null;
        try {
            //Restrict key and csr file download
            //TODO can be improved by storing private key in more secure place
            if (fileName.endsWith("key") || fileName.endsWith("csr")) {
                throw new EntityNotFoundException("Can find file -> " + fileName);
            }
            Enterprise enterprise = enterpriseRepository.getBySubDomainName(hostName);
            if (enterprise == null) {
                throw new BadDataException("Can not find subdomain -> " + hostName);
            }

            String fileKey = enterprise.getId() + "/" + fileName;
            file = s3Utils.getObject(fileKey, fileName);
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } finally {
            CommonUtils.deleteFile(file);
        }
    }

    public ServiceOffer createServiceOffering(long enterpriseId, CreateServiceOfferingRequest request) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);

        String did = "did:web:" + enterprise.getSubDomainName();

        //TODO integrate signer tool API ro create service offering VC

        //TODO store VC

        //TODO store service offering
        return null;
    }

    public List<ServiceOffer> serviceOfferList() {
        return serviceOfferRepository.findAll();
    }

    public List<ServiceOffer> serviceOfferList(long enterpriseId) {
        return serviceOfferRepository.getByEnterpriseId(enterpriseId);
    }


    public ServiceOffer getServiceOfferingDetails(long enterpriseId, long offerId) {
        ServiceOffer serviceOffer = serviceOfferRepository.getByIdAndEnterpriseId(offerId, enterpriseId);
        Validate.isNull(serviceOffer).launch(new EntityNotFoundException());
        //TODO need VC?

        return serviceOffer;
    }

    public List<EnterpriseCredential> getEnterpriseCredentials(long enterpriseId) {
        enterpriseRepository.findById(enterpriseId).orElseThrow(EntityNotFoundException::new);
        return enterpriseCredentialRepository.getByEnterpriseId(enterpriseId);
    }
}
