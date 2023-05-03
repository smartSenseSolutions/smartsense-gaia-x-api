/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


/**
 * The type Common utils.
 */
public class CommonUtils {

    private CommonUtils() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

    /**
     * Delete file.
     *
     * @param files the files
     */
    public static void deleteFile(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    LOGGER.error("Can not delete file >{}", file.getName(), e);
                }
            }
        }
    }

    public static String getEnterpriseDid(String subDomain) {
        return "did:web:" + subDomain;
    }

    public static String getParticipantJsonLink(String subDomain) {
        return "https://" + subDomain + "/.well-known/participant.json";
    }
}
