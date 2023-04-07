/*
 * Copyright (c) 2023 | smartSense
 */
package com.smartsense.gaiax.service.ssl;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCertificate;
import com.smartsense.gaiax.dao.repository.EnterpriseCertificateRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.service.domain.DomainService;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.utils.S3Utils;
import org.quartz.JobKey;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.security.KeyPair;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
@Service
public class CertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateService.class);
    private static final File USER_KEY_FILE = new File("user.key");

    //Challenge type to be used
    private static final ChallengeType CHALLENGE_TYPE = ChallengeType.DNS;

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateService.class);

    public CertificateService(DomainService domainService, EnterpriseRepository enterpriseRepository, S3Utils s3Utils, EnterpriseCertificateRepository enterpriseCertificateRepository, ScheduleService scheduleService) {
        this.domainService = domainService;
        this.enterpriseRepository = enterpriseRepository;
        this.s3Utils = s3Utils;
        this.enterpriseCertificateRepository = enterpriseCertificateRepository;
        this.scheduleService = scheduleService;
    }

    private enum ChallengeType {HTTP, DNS}

    private final DomainService domainService;

    private final EnterpriseRepository enterpriseRepository;

    private final S3Utils s3Utils;

    private final EnterpriseCertificateRepository enterpriseCertificateRepository;

    private final ScheduleService scheduleService;

    public void createSSLCertificate(long enterpriseId, JobKey jobKey) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElse(null);
        if (enterprise == null) {
            LOGGER.error("Invalid enterprise id");
            return;
        }
        String domain = enterprise.getSubDomainName();
        File domainChainFile = new File("/tmp/" + domain + "_chain.crt");
        File csrFile = new File("/tmp/" + domain + ".csr");
        File keyfile = new File("/tmp/" + domain + ".key");

        try {

            // Load the user key file. If there is no key file, create a new one.
            KeyPair userKeyPair = loadOrCreateUserKeyPair();

            // Create a session for Let's Encrypt.
            // Use "acme://letsencrypt.org" for production server
            Session session = new Session("acme://letsencrypt.org");

            // Get the Account.
            // If there is no account yet, create a new one.
            Account acct = findOrRegisterAccount(session, userKeyPair);

            // Load or create a key pair for the domains. This should not be the userKeyPair!
            KeyPair domainKeyPair = loadOrCreateDomainKeyPair(keyfile);

            // Order the certificate
            Order order = acct.newOrder().domain(domain).create();

            // Perform all required authorizations
            for (Authorization auth : order.getAuthorizations()) {
                authorize(auth);
            }

            // Generate a CSR for all of the domains, and sign it with the domain key pair.
            CSRBuilder csrb = new CSRBuilder();
            csrb.addDomain(domain);
            csrb.sign(domainKeyPair);


            // Write the CSR to a file, for later use.
            try (Writer out = new FileWriter(csrFile)) {
                csrb.write(out);
            }

            // Order the certificate
            order.execute(csrb.getEncoded());

            // Wait for the order to complete
            try {
                int attempts = 10;
                while (order.getStatus() != Status.VALID && attempts-- > 0) {
                    LOGGER.debug("Waiting for order confirmation attempts->{}", attempts);
                    // Did the order fail?
                    if (order.getStatus() == Status.INVALID) {
                        LOG.error("Order has failed, reason: {}", order.getError());
                        throw new AcmeException("Order failed... Giving up.");
                    }

                    // Wait for a few seconds
                    Thread.sleep(6000L);

                    // Then update the status
                    order.update();
                }
            } catch (InterruptedException ex) {
                LOG.error("interrupted", ex);
                Thread.currentThread().interrupt();
            }

            // Get the certificate
            Certificate certificate = order.getCertificate();

            LOG.info("Success! The certificate for domains {} has been generated!", domain);
            LOG.info("Certificate URL: {}", certificate.getLocation());


            // Write a combined file containing the certificate and chain.
            try (FileWriter fw = new FileWriter(domainChainFile)) {
                certificate.writeCertificate(fw);
            }

            String certificateChain = enterpriseId + "/x509CertificateChain.pem";
            String csr = enterpriseId + "/" + csrFile.getName();
            String keyFile = enterpriseId + "/" + keyfile.getName();
            //save files in s3
            s3Utils.uploadFile(certificateChain, domainChainFile);
            s3Utils.uploadFile(csr, csrFile);
            s3Utils.uploadFile(keyFile, keyfile);


            enterprise.setStatus(RegistrationStatus.CERTIFICATE_CREATED.getStatus());

            //save certificate location
            EnterpriseCertificate enterpriseCertificate = EnterpriseCertificate.builder()
                    .certificateChain(certificateChain)
                    .enterpriseId(enterpriseId)
                    .csr(csr)
                    .privateKey(keyFile)
                    .build();
            enterpriseCertificateRepository.save(enterpriseCertificate);

            //create Job tp create ingress and tls secret
            scheduleService.createJob(enterpriseId, StringPool.JOB_TYPE_CREATE_INGRESS, 0);
            if (jobKey != null) {
                //delete job
                scheduleService.deleteJob(jobKey);
            }

        } catch (Exception e) {
            LOGGER.error("Can not create certificate for enterprise ->{}, domain ->{}", enterpriseId, enterprise.getSubDomainName(), e);
            enterprise.setStatus(RegistrationStatus.CERTIFICATE_CREATION_FAILED.getStatus());
        } finally {
            enterpriseRepository.save(enterprise);
            //delete files
            if (domainChainFile.exists()) {
                domainChainFile.delete();
            }
            if (csrFile.exists()) {
                csrFile.delete();
            }
            if (keyfile.exists()) {
                keyfile.delete();
            }

        }
    }

    /**
     * Loads a user key pair from {@link #USER_KEY_FILE}. If the file does not exist, a
     * new key pair is generated and saved.
     * <p>
     * Keep this key pair in a safe place! In a production environment, you will not be
     * able to access your account again if you should lose the key pair.
     *
     * @return User's {@link KeyPair}.
     */
    private KeyPair loadOrCreateUserKeyPair() throws IOException {
        if (USER_KEY_FILE.exists()) {
            // If there is a key file, read it
            try (FileReader fr = new FileReader(USER_KEY_FILE)) {
                return KeyPairUtils.readKeyPair(fr);
            }

        } else {
            // If there is none, create a new key pair and save it
            KeyPair userKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(USER_KEY_FILE)) {
                KeyPairUtils.writeKeyPair(userKeyPair, fw);
            }
            return userKeyPair;
        }
    }

    private KeyPair loadOrCreateDomainKeyPair(File domainChainFile) throws IOException {
        if (domainChainFile.exists()) {
            try (FileReader fr = new FileReader(domainChainFile)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
            try (FileWriter fw = new FileWriter(domainChainFile)) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }
            return domainKeyPair;
        }
    }

    /**
     * Finds your {@link Account} at the ACME server. It will be found by your user's
     * public key. If your key is not known to the server yet, a new account will be
     * created.
     * <p>
     * This is a simple way of finding your {@link Account}. A better way is to get the
     * URL of your new account with {@link Account#getLocation()} and store it somewhere.
     * If you need to get access to your account later, reconnect to it via {@link
     * Session#login(URL, KeyPair)} by using the stored location.
     *
     * @param session {@link Session} to bind with
     * @return {@link Account}
     */
    private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {

        Account account = new AccountBuilder()
                .agreeToTermsOfService()
                .useKeyPair(accountKey)
                .create(session);
        LOG.info("Registered a new user, URL: {}", account.getLocation());

        return account;
    }

    /**
     * Authorize a domain. It will be associated with your account, so you will be able to
     * retrieve a signed certificate for the domain later.
     *
     * @param auth {@link Authorization} to perform
     */
    private void authorize(Authorization auth) throws AcmeException {
        LOG.info("Authorization for domain {}", auth.getIdentifier().getDomain());
        Dns01Challenge dnsChallenge = auth.findChallenge(Dns01Challenge.TYPE);
        String valuesToBeAdded = dnsChallenge.getDigest();
        String domain = Dns01Challenge.toRRName(auth.getIdentifier());

        try {
            // The authorization is already valid. No need to process a challenge.
            if (auth.getStatus() == Status.VALID) {
                return;
            }

            // Find the desired challenge and prepare it.
            Challenge challenge = null;
            if (CHALLENGE_TYPE == ChallengeType.DNS) {
                challenge = dnsChallenge(auth);
            }

            if (challenge == null) {
                throw new AcmeException("No challenge found");
            }

            // If the challenge is already verified, there's no need to execute it again.
            if (challenge.getStatus() == Status.VALID) {
                return;
            }

            // Now trigger the challenge.
            challenge.trigger();

            // Poll for the challenge to complete.
            try {
                int attempts = 6;
                while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
                    LOGGER.debug("Waiting for 30 sec before check of DNS record attempts -> {}", attempts);
                    // Wait for a few seconds
                    Thread.sleep(30000L);

                    // Then update the status
                    challenge.update();
                }
                // Did the authorization fail?
                if (challenge.getStatus() == Status.INVALID) {
                    LOG.error("Challenge has failed, reason: {}", challenge.getError());
                    throw new AcmeException("Challenge failed... Giving up.");
                }
            } catch (InterruptedException ex) {
                LOG.error("interrupted", ex);
                Thread.currentThread().interrupt();
            }

            // All reattempts are used up and there is still no valid authorization?
            if (challenge.getStatus() != Status.VALID) {
                throw new AcmeException("Failed to pass the challenge for domain "
                        + auth.getIdentifier().getDomain() + ", ... Giving up.");
            }

            LOG.info("Challenge has been completed. Remember to remove the validation resource.");

        } finally {
            domainService.deleteTxtRecordForSSLCertificate(domain, valuesToBeAdded);
        }
    }


    /**
     * Prepares a DNS challenge.
     * <p>
     * The verification of this challenge expects a TXT record with a certain content.
     * <p>
     * This example outputs instructions that need to be executed manually. In a
     * production environment, you would rather configure your DNS automatically.
     *
     * @param auth {@link Authorization} to find the challenge in
     * @return {@link Challenge} to verify
     */
    private Challenge dnsChallenge(Authorization auth) throws AcmeException {
        // Find a single dns-01 challenge
        Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
        if (challenge == null) {
            throw new AcmeException("Found no " + Dns01Challenge.TYPE + " challenge, don't know what to do...");
        }

        String valuesToBeAdded = challenge.getDigest();
        String domain = Dns01Challenge.toRRName(auth.getIdentifier());

        //Create TXT records
        domainService.createTxtRecordForSSLCertificate(domain, valuesToBeAdded);

        return challenge;
    }

    /**
     * Presents the instructions for removing the challenge validation, and waits for
     * dismissal.
     *
     * @param message Instructions to be shown in the dialog
     */
    public void completeChallenge(String message) throws AcmeException {
        JOptionPane.showMessageDialog(null,
                message,
                "Complete Challenge",
                JOptionPane.INFORMATION_MESSAGE);
    }
}