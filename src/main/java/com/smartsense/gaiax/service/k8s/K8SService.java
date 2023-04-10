/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.k8s;

import com.smartsense.gaiax.config.K8SSettings;
import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCertificate;
import com.smartsense.gaiax.dao.repository.EnterpriseCertificateRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.dto.RegistrationStatus;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.exception.BadDataException;
import com.smartsense.gaiax.service.job.ScheduleService;
import com.smartsense.gaiax.utils.CommonUtils;
import com.smartsense.gaiax.utils.S3Utils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type K 8 s service.
 */
@Service
public class K8SService {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8SService.class);
    /**
     * The constant DEFAULT.
     */
    public static final String DEFAULT = "default";

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCertificateRepository enterpriseCertificateRepository;

    private final S3Utils s3Util;

    private final K8SSettings k8SSettings;

    private final ScheduleService scheduleService;

    /**
     * Instantiates a new K 8 s service.
     *
     * @param enterpriseRepository            the enterprise repository
     * @param enterpriseCertificateRepository the enterprise certificate repository
     * @param s3Util                          the s 3 util
     * @param k8SSettings                     the k 8 s settings
     * @param scheduleService                 the schedule service
     */
    public K8SService(EnterpriseRepository enterpriseRepository, EnterpriseCertificateRepository enterpriseCertificateRepository, S3Utils s3Util, K8SSettings k8SSettings, ScheduleService scheduleService) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCertificateRepository = enterpriseCertificateRepository;
        this.s3Util = s3Util;
        this.k8SSettings = k8SSettings;
        this.scheduleService = scheduleService;
    }

    /**
     * Create ingress.
     *
     * @param enterpriseId the enterprise id
     */
    public void createIngress(long enterpriseId) {
        File crt = null;
        File key = null;
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(BadDataException::new);
        try {

            EnterpriseCertificate enterpriseCertificate = enterpriseCertificateRepository.getByEnterpriseId(enterpriseId);
            crt = s3Util.getObject(enterpriseCertificate.getCertificateChain(), "chain.crt");

            key = s3Util.getObject(enterpriseCertificate.getPrivateKey(), "private.key");
            //Step 1: create secret using SSL certificate
            ApiClient client = Config.fromToken(k8SSettings.getBasePath(), k8SSettings.getToken(), false);
            Configuration.setDefaultApiClient(client);


            CoreV1Api api = new CoreV1Api();

            V1Secret secret = new V1Secret();
            secret.setMetadata(new V1ObjectMeta().name(enterprise.getSubDomainName()));
            secret.setType("kubernetes.io/tls");


            String certString = Files.readString(crt.toPath());
            String keyString = Files.readString(key.toPath());
            LOGGER.debug("certString  -> {}", certString);
            LOGGER.debug("keyString  -> {}", keyString);

            secret.putDataItem("tls.crt", certString.getBytes());
            secret.putDataItem("tls.key", keyString.getBytes());


            api.createNamespacedSecret(DEFAULT, secret, null, null, null, null);
            LOGGER.debug("tls secret created for enterprise -{} domain ->{}", enterpriseId, enterprise.getSubDomainName());

            ///annotations
            Map<String, String> annotations = new HashMap<>();
            annotations.put("nginx.ingress.kubernetes.io/proxy-body-size", "35m");
            annotations.put("nginx.ingress.kubernetes.io/client-body-buffer-size", "35m");
            annotations.put("nginx.ingress.kubernetes.io/proxy-connect-timeout", "600");
            annotations.put("nginx.ingress.kubernetes.io/proxy-send-timeout", "600");
            annotations.put("nginx.ingress.kubernetes.io/proxy-read-timeout", "600");


            //Step 2: Create ingress
            NetworkingV1Api networkingV1Api = new NetworkingV1Api();
            V1ObjectMeta metadata = new V1ObjectMeta();
            metadata.setName(enterprise.getSubDomainName());
            metadata.setNamespace(DEFAULT);
            metadata.setAnnotations(annotations);

            //tls item
            V1IngressTLS ingressTLS = new V1IngressTLS();
            ingressTLS.setSecretName(enterprise.getSubDomainName());
            ingressTLS.setHosts(List.of(enterprise.getSubDomainName()));

            //service backend
            V1IngressServiceBackend backend = new V1IngressServiceBackend();
            backend.setName(k8SSettings.getServiceName());
            V1ServiceBackendPort port = new V1ServiceBackendPort();
            port.setNumber(8080);
            backend.setPort(port);

            V1IngressBackend v1IngressBackend = new V1IngressBackend();
            v1IngressBackend.setService(backend);

            //path
            V1HTTPIngressPath path = new V1HTTPIngressPath();
            path.backend(v1IngressBackend);
            path.pathType("Prefix");
            path.path("/");

            //http rule
            V1HTTPIngressRuleValue httpIngressRuleValue = new V1HTTPIngressRuleValue();
            httpIngressRuleValue.addPathsItem(path);

            //v1 rule
            V1IngressRule rule = new V1IngressRule();
            rule.host(enterprise.getSubDomainName());
            rule.http(httpIngressRuleValue);

            V1IngressSpec spec = new V1IngressSpec();
            spec.addTlsItem(ingressTLS);
            spec.addRulesItem(rule);

            //main ingress object
            V1Ingress v1Ingress = new V1Ingress();
            v1Ingress.metadata(metadata);
            v1Ingress.setSpec(spec);

            networkingV1Api.createNamespacedIngress(DEFAULT, v1Ingress, null, null, null, null);

            enterprise.setStatus(RegistrationStatus.INGRESS_CREATED.getStatus());

            LOGGER.debug("Ingress created for enterprise -> {} and domain ->{}", enterpriseId, enterprise.getSubDomainName());
            createDidcreationJob(enterpriseId, enterprise);
        } catch (Exception e) {
            LOGGER.error("Can not create ingress for enterprise -> {}", enterpriseId, e);
            enterprise.setStatus(RegistrationStatus.INGRESS_CREATION_FAILED.getStatus());
        } finally {
            enterpriseRepository.save(enterprise);
            CommonUtils.deleteFile(crt, key);
        }
    }

    private void createDidcreationJob(long enterpriseId, Enterprise enterprise) {
        try {
            scheduleService.createJob(enterpriseId, StringPool.JOB_TYPE_CREATE_DID, 0);
        } catch (SchedulerException e) {
            LOGGER.error("Can not create did creation job for enterprise->{}", enterprise, e);
            enterprise.setStatus(RegistrationStatus.DID_JSON_CREATION_FAILED.getStatus());
        }
    }
}
