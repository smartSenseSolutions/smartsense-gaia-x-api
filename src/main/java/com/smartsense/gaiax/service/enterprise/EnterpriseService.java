/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.exception.EntityNotFoundException;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.S3Utils;
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

    private final S3Utils s3Utils;

    /**
     * Instantiates a new Enterprise service.
     *
     * @param enterpriseRepository the enterprise repository
     * @param s3Utils              the s 3 utils
     */
    public EnterpriseService(EnterpriseRepository enterpriseRepository, S3Utils s3Utils) {
        this.enterpriseRepository = enterpriseRepository;
        this.s3Utils = s3Utils;
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
        enterprise.setCertificateChain("https://" + enterprise.getSubDomainName() + "/.well-known/x509CertificateChain.json");
        return enterprise;
    }

    /**
     * List enterprise list.
     *
     * @return the list
     */
    public List<Enterprise> listEnterprise() {
        return enterpriseRepository.findAll(Sort.by(Sort.Direction.DESC, "created_at"));
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
}
