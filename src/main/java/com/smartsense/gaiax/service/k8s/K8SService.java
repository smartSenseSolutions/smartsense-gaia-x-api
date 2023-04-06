/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.service.k8s;

import com.smartsense.gaiax.dao.entity.Enterprise;
import com.smartsense.gaiax.dao.entity.EnterpriseCertificate;
import com.smartsense.gaiax.dao.repository.EnterpriseCertificateRepository;
import com.smartsense.gaiax.dao.repository.EnterpriseRepository;
import com.smartsense.gaiax.utils.S3Utils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class K8SService {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8SService.class);

    private final EnterpriseRepository enterpriseRepository;

    private final EnterpriseCertificateRepository enterpriseCertificateRepository;

    private final S3Utils s3Util;

    public K8SService(EnterpriseRepository enterpriseRepository, EnterpriseCertificateRepository enterpriseCertificateRepository, S3Utils s3Util) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseCertificateRepository = enterpriseCertificateRepository;
        this.s3Util = s3Util;
    }

    public void createIngress(long enterpriseId) throws IOException, ApiException {

        Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElse(null);

        EnterpriseCertificate enterpriseCertificate = enterpriseCertificateRepository.getByEnterpriseId(enterpriseId);
        File crt = s3Util.getObject(enterpriseCertificate.getCertificateChain(), "chain.crt");

        File key = s3Util.getObject(enterpriseCertificate.getPrivateKey(), "private.key");


       /* ApiClient client = new ClientBuilder()
                .setVerifyingSsl(false)
                .setBasePath("https://203.129.213.107:8443") // Replace with your Kubernetes API hostname
                .setAuthentication(new AccessTokenAuthentication("Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Im1ockgwMGFnbkRQMjhBYmdGZ3VmUkswa0VTOERPWFZXR1o1QkJURExFajAifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImFwaS1zZXJ2aWNlLWFjY291bnQtdG9rZW4tdHc3cDQiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiYXBpLXNlcnZpY2UtYWNjb3VudCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjgzNTc5MzIzLWQ0N2EtNGIzMS05ZjA0LTFlMGNiZDNhOTc0NyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmFwaS1zZXJ2aWNlLWFjY291bnQifQ.PxnNzPP_n1E7reeH0hkeps-03cq3kPvYXXz5Y2W3b01gB2Xcz1wmRvlMlcW6I-tZQBfw9puVfCGofStGGs45P6Iqf0smM3-3vLbto0WvumtFYQCYhnWimzewkFqabL4txJ2pthVmE5FLm8u4VVqgYrBSlPiKRd9Ge0lpUHTqUGZ5syxZNuv2Ywm-AqvDfnme-MjGkb_1tcTwA27XLjMel_8f0VfQ_zeRXrcDLv-KUlaXPh88mhEXutBMcq5yY6fQzG5oGccjQGoYfWBJaCTK4lKmi-V9dPQbvaQbpNz7Soc4g2APke634nhJNM_6cOMnOT0N4rBPIyHVYFCuRkfUqQ")) // Replace with your access token
                .build();*/

        //Step 1: create secret using SSL certificate
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);


        CoreV1Api api = new CoreV1Api();

        V1Secret secret = new V1Secret();
        secret.setMetadata(new V1ObjectMeta().name(enterprise.getSubDomainName()));
        secret.setType("kubernetes.io/tls");
        secret.putDataItem("tls.crt", Base64.getEncoder().encodeToString(
                Files.readAllBytes(crt.toPath())).getBytes());
        secret.putDataItem("tls.key", Base64.getEncoder().encodeToString(
                Files.readAllBytes(key.toPath())).getBytes());

        V1Secret aDefault = api.createNamespacedSecret("default", secret, null, null, null, null);
        LOGGER.debug("tls secret created for domain ->{}", enterprise.getSubDomainName());

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
        metadata.setNamespace("default");
        metadata.setAnnotations(annotations);

        //tls item
        V1IngressTLS ingressTLS = new V1IngressTLS();
        ingressTLS.setSecretName(enterprise.getSubDomainName());
        ingressTLS.setHosts(List.of(enterprise.getSubDomainName()));

        //service backend
        V1IngressServiceBackend backend = new V1IngressServiceBackend();
        backend.setName("smartsense-gaia-x-api");
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

        String dump = Yaml.dump(v1Ingress);
        System.out.println(dump);
        networkingV1Api.createNamespacedIngress("default", v1Ingress, null, null, null, null);
        LOGGER.debug("Ingress created for enterprise -> {} and domain ->{}", enterpriseId, enterprise.getSubDomainName());
        //Stpe 3: Call node JS API to create did json
        //Step 4: Store did.json in s3

    }
}
