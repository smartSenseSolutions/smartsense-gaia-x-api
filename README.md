# Gaia-x on boarding and credential verification MVP by smartSense

This is MVP to showcase capability of smartSense in contect of Gaia-X economy.
This MVP cover below usercase:

1. On-boarding in Gaia-x
    1. Create sub domain for enterprise
    2. Create SSL jey value pair for enterprise
    3. Create web did
    4. Create participant credentials and sign using Gaia-x API
    5. Host public key, did.json and participant files under .well-known path
2. Create service offering and create service offering credential and host offer file under .well-known path
3. List Catalogue
4. Create verifiable presentation of Gaia-X participant credentials
5. validate VP and see masked(Secure) information bt verifying VP

## Tools and Technologies

1. Spring boot with JPA
2. K8S Java SDK
3. Certbot SDK acme4j
4. AWS Route53 SDK
5. AWS S3 SDK
6. NodeJS for signer tool (https://github.com/smartSenseSolutions/smartsense-gaia-x-signer)

## Onboarding flow

![onboarding.png](doc%2Fonboarding.png)

## Create service offer flow

![Create service offer.png](doc%2FCreate%20service%20offer.png)

## service offer flow

![Service offer flow.png](doc%2FService%20offer%20flow.png)

## Known issue or improvement

1. Authentication and Authorization flow can be improved
2. Data exchange based on Gaia-x trust framework(Ocean protocol??)