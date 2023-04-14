/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.utils;

import com.smartsense.gaiax.config.JWTSetting;
import com.smartsense.gaiax.dto.SessionDTO;
import com.smartsense.gaiax.dto.StringPool;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Jwt util.
 */
@Component
public class JWTUtil {


    /**
     * Authorization header required in the request
     */
    private static final String BEARER = "Bearer ";

    private final JWTSetting jwtSetting;

    /**
     * Instantiates a new Jwt util.
     *
     * @param jwtSetting the jwt setting
     */
    @Autowired
    public JWTUtil(JWTSetting jwtSetting) {
        this.jwtSetting = jwtSetting;
    }

    /**
     * Get all the claims from the given token
     *
     * @param token access token
     * @return claims all claims from token
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(jwtSetting.getTokenSigningKey()).parseClaimsJws(token).getBody();
    }

    /**
     * Get username/email from the given token
     *
     * @param token access token
     * @return username /email
     */
    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Get expiration date from the token
     *
     * @param token access token
     * @return expiry date
     */
    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    /**
     * Checks if token is expired.
     *
     * @param token access token
     * @return true if token is expired
     */
    private boolean isTokenExpired(String token) {
        Date expiration;
        try {
            expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (ExpiredJwtException | MalformedJwtException ex) {
            return true;
        }
    }

    /**
     * Generate token string.
     *
     * @param sessionDTO the session dto
     * @return the string
     */
    public String generateToken(SessionDTO sessionDTO) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(StringPool.EMAIL, sessionDTO.getEmail());
        claims.put(StringPool.ENTERPRISE_ID, sessionDTO.getEnterpriseId());
        claims.put(StringPool.ROLE, sessionDTO.getRole());

        return doGenerateToken(claims, sessionDTO.getEmail());
    }

    /**
     * Generates token for the given claims, username and expiry date
     *
     * @param claims   claims
     * @param username username/email
     * @return access token
     */
    public String doGenerateToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setAudience("Smart-X")
                .setIssuer("Smart-X")
                .setNotBefore(new Date())
                .setSubject(username)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS512, jwtSetting.getTokenSigningKey())
                .compact();
    }

    /**
     * Validates token if it is not expired
     *
     * @param token access token
     * @return true if valid token
     */
    public boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    /**
     * Extracts token from the authorization header
     *
     * @param authorizationHeader authorization header
     * @return access token
     */
    public String extractToken(String authorizationHeader) {
        if (!authorizationHeader.startsWith(BEARER) || authorizationHeader.length() < BEARER.length()) {
            throw new MalformedJwtException("JWT cannot be empty");
        }
        return authorizationHeader.substring(BEARER.length());
    }

    /**
     * Retrieves given claims from the given access token
     *
     * @param token     access token
     * @param claimKeys claim keys for the claims retrieved from the access token
     * @return Map of claims
     */
    public Map<String, Object> getClaim(String token, String... claimKeys) {
        Claims claims = getAllClaimsFromToken(token);
        Map<String, Object> foundClaims = new HashMap<>();
        for (String key : claimKeys) {
            foundClaims.put(key, claims.get(key));
        }
        return foundClaims;
    }
}
