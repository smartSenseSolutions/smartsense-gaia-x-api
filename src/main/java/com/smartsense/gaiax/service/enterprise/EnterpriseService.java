/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.enterprise;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.utils.S3Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@Service
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;

    private final S3Utils s3Utils;

    private final Logger LOGGER = LoggerFactory.getLogger(EnterpriseService.class);

    public EnterpriseService(EnterpriseRepository enterpriseRepository, S3Utils s3Utils) {
        this.enterpriseRepository = enterpriseRepository;
        this.s3Utils = s3Utils;
    }

    public String getEnterpriseFiles(String hostName, String fileName) throws IOException {
        File file = null;
        try {
            Enterprise enterprise = enterpriseRepository.getBySubDomainName(hostName);
            if (enterprise == null) {
                throw new BadDataException("Can not find subdomain -> " + hostName);
            }

            String fileKey = enterprise.getId() + "/" + fileName;
            file = s3Utils.getObject(fileKey, fileName);
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } finally {
            if (file != null && file.exists()) {
                //  file.delete();
            }
        }
    }
}
