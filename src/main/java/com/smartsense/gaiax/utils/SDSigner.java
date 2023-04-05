/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;


/**
 * @author Nitin
 * @version 1.0
 */
public class SDSigner {

    private final Vertx vertx;
    private final WebClient webClient;

    private final String complianceBaseURL;

    private final String verificationMethod;

    private final String x5uURL;

    private final String privateKey;

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public SDSigner(Vertx vertx, String verificationMethod, String x5uURL, String apiVersion, String baseURL, String privateKey) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.verificationMethod = verificationMethod;
        this.x5uURL = x5uURL;
        this.privateKey = privateKey;
        this.complianceBaseURL = baseURL + "/" + apiVersion + "/api";
        LOGGER.info("Compliance API set to " + complianceBaseURL);

    }

    /**
     * Create a canonical representation of the JSON-LD self-description
     *
     * @param selfDescription
     * @return
     */
    public Future<String> canonizeSD(JsonObject selfDescription) {
        // Normalize the RDF with: https://w3c-ccg.github.io/rdf-dataset-canonicalization/
        // https://connect2id.com/products/nimbus-jose-jwt/examples/jws-with-unencoded-payload
        Promise<String> promise = Promise.promise();
        webClient.postAbs(complianceBaseURL + "/normalize").expect(ResponsePredicate.SC_CREATED).sendJsonObject(selfDescription).onSuccess(ar -> {
            promise.complete(ar.bodyAsString());
        }).onFailure(ar -> {
            LOGGER.error(ar.getMessage());
            promise.fail(ar.getMessage());
        });
        return promise.future();
    }


    /**
     * Creates a HEX encoded SHA256 Hash of the input
     *
     * @param input
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String createHash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash));
    }

    /**
     * Creates a JSON Web Signature with unencoded Payload
     * https://connect2id.com/products/nimbus-jose-jwt/examples/jws-with-unencoded-payload
     * https://www.w3.org/community/reports/credentials/CG-FINAL-lds-jws2020-20220721/#json-web-signature-2020
     *
     * @param payload
     * @return
     */
    public String createJWSSignature(String payload, RSAKey rsaKey) throws JOSEException {
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS256)
                .base64URLEncodePayload(false)
                .criticalParams(Collections.singleton("b64"))
                .build();

        JWSObject jwsObject = new JWSObject(header, new Payload(payload));
        jwsObject.sign(signer);

        boolean isDetached = true;
        return jwsObject.serialize(isDetached);
    }

    /**
     * Verifies a JSON Web Signature with unencoded Payload
     *
     * @param payload
     * @param jws
     * @param rsaPublicKey
     * @return
     * @throws ParseException
     * @throws JOSEException
     */
    public Boolean verifyJWSSignature(String payload, String jws, RSAKey rsaPublicKey) throws ParseException, JOSEException {
        JWSObject parsedJWSObject = JWSObject.parse(jws, new Payload(payload));
        JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
        return parsedJWSObject.verify(verifier);
    }

    /**
     * Creates and adds the proof section to a self-description
     *
     * @param sd
     * @param jws
     * @return
     */
    public JsonObject addProofToSD(JsonObject sd, String jws) {
        JsonObject proof = new JsonObject();
        proof.put("type", "JsonWebSignature2020");
        proof.put("created", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        proof.put("proofPurpose", "assertionMethod");
        proof.put("verificationMethod", verificationMethod);
        proof.put("jws", jws);
        sd.put("proof", proof);
        return sd;
    }

    /**
     * Signs a self-description with the Gaia-X compliance service
     *
     * @param selfDescription
     * @param signEndpoint
     * @return
     */
    public Future<JsonObject> signSD(JsonObject selfDescription, String signEndpoint) {
        Promise<JsonObject> promise = Promise.promise();
        webClient.postAbs(signEndpoint).expect(ResponsePredicate.SC_CREATED).sendJsonObject(selfDescription).onSuccess(ar -> {
            JsonObject finalSD = new JsonObject();
            finalSD.put("selfDescriptionCredential", selfDescription);
            finalSD.put("complianceCredential", ar.bodyAsJsonObject().getJsonObject("complianceCredential"));
            promise.complete(finalSD);
        }).onFailure(ar -> {
            LOGGER.error(ar.getMessage());
            promise.fail(ar.getMessage());
        });
        return promise.future();
    }


    /**
     * Verifies the self-description with the Gaia-X compliance service
     *
     * @param selfDescription
     * @param verifyEndpoint
     * @return
     */
    public Future<Void> verifySD(JsonObject selfDescription, String verifyEndpoint) {
        Promise<Void> promise = Promise.promise();
        webClient.postAbs(verifyEndpoint).expect(ResponsePredicate.SC_OK).sendJsonObject(selfDescription).onSuccess(ar -> {
            promise.complete();
        }).onFailure(ar -> {
            LOGGER.error(ar.getMessage());
            promise.fail(ar.getMessage());
        });
        return promise.future();
    }

    /**
     * Creates a DID with a JWK
     *
     * @param jwk
     * @return
     */
    public JsonObject createDID(JWK jwk) {
        JsonObject did = new JsonObject();

        JsonObject verificationMethodObject = new JsonObject();
        JsonObject jwkJSON = new JsonObject(jwk.toPublicJWK().toJSONString());

        jwkJSON.put("alg", "PS256");
        jwkJSON.put("x5u", x5uURL);

        verificationMethodObject.put("@context", "https://w3c-ccg.github.io/lds-jws2020/contexts/v1/");
        verificationMethodObject.put("id", verificationMethod + "#JWK2020-RSA");
        verificationMethodObject.put("type", "JsonWebKey2020");
        verificationMethodObject.put("controller", verificationMethod);
        verificationMethodObject.put("publicKeyJwk", jwkJSON);

        did.put("@context", new JsonArray().add("https://www.w3.org/ns/did/v1"));
        did.put("id", verificationMethod);
        did.put("verificationMethod", new JsonArray().add(verificationMethodObject));
        did.put("assertionMethod", new JsonArray().add(verificationMethod + "#JWK2020-RSA"));
        return did;
    }

    public void writeFile(String name, JsonObject content) {
        if (!vertx.fileSystem().existsBlocking("output")) {
            vertx.fileSystem().mkdirBlocking("output");
        }
        vertx.fileSystem().writeFileBlocking("output/" + Instant.now().getEpochSecond() + "_" + name + ".json", Buffer.buffer(content.encodePrettily()));
    }

    public Future<Void> start(JsonObject sd) {
        Promise<Void> promise = Promise.promise();

        canonizeSD(sd).onSuccess(canonizedSD -> {
            try {
                // Create the Hash of the normalized input
                String inputHash = createHash(canonizedSD);
                LOGGER.info("✅ Created canonical representation: " + inputHash);

                // Get the private and public key from the private key in PEM format
                JWK jwk = JWK.parseFromPEMEncodedObjects(privateKey);
                RSAKey rsaKey = jwk.toRSAKey();
                RSAKey rsaPublicKey = rsaKey.toPublicJWK();

                // Create the signature
                String jws = createJWSSignature(inputHash, rsaKey);
                LOGGER.info("✅ Signature: " + jws);

                // Verify the signature
                if (verifyJWSSignature(inputHash, jws, rsaPublicKey)) {
                    LOGGER.info("✅ Verification successful (local)");
                } else {
                    LOGGER.debug("✖ Verification failed");
                }
                // Add the proof to the self-description
                JsonObject sdWithProof = addProofToSD(sd, jws);
                writeFile("self-signed", sdWithProof);

                // Create the DID
                JsonObject did = createDID(jwk);
                writeFile("did", did);

                // Execute the signing and verification process with the Gaia-X compliance service
                signSD(sdWithProof, complianceBaseURL + "/sign").onSuccess(signResult -> {
                    LOGGER.info("✅ Self-description signed with Gaia-X Compliance Service");
                    verifySD(signResult, complianceBaseURL + "/participant/verify/raw").onSuccess(verificationResult -> {
                        LOGGER.info("✅ Self-description verified with Gaia-X Compliance Service");
                        writeFile("complete", signResult);
                        promise.complete();
                    }).onFailure(verificationResult -> {
                        LOGGER.error("✖ Self-description verfication with Gaia-X Compliance Service failed");
                        promise.fail(verificationResult.getMessage());
                    });
                }).onFailure(signResult -> {
                    LOGGER.error("✖ Self-description signing with Gaia-X Compliance Service failed");
                    promise.fail(signResult.getMessage());
                });
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                promise.fail(e.getMessage());
            }

        }).onFailure(result -> {
            LOGGER.error("✖ Creation of canonical representation failed");
            promise.fail(result.getMessage());
        });

        return promise.future();
    }
}