/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.security;

import com.smartsense.gaiax.dto.SessionDTO;
import com.smartsense.gaiax.dto.StringPool;
import com.smartsense.gaiax.exception.SecurityException;
import com.smartsense.gaiax.utils.JWTUtil;
import com.smartsense.gaiax.utils.Validate;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * The type Security filter.
 */
@Component
public class SecurityFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    private final JWTUtil jwtUtil;
    private final Set<String> publicUrls;
    private final AntPathMatcher antPathMatcher;


    /**
     * Instantiates a new Security filter.
     *
     * @param jwtUtil the jwt util
     */
    public SecurityFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        antPathMatcher = new AntPathMatcher();

        //No authentication will be done, can be handle in better way
        publicUrls = new TreeSet<>();
        publicUrls.add("/login");
        publicUrls.add("/.well-known/**");
        publicUrls.add("/actuator/health");
        publicUrls.add("/webjars/**");
        publicUrls.add("/swagger-ui.html");
        publicUrls.add("/swagger-ui/**");
        publicUrls.add("/v3/api-docs/**");
        publicUrls.add("/favicon.ico");
        publicUrls.add("/swagger-resources/**");
        publicUrls.add("/v2/api-docs");
        publicUrls.add("/verify/presentation");
        publicUrls.add("/tinyurl");
        Collections.unmodifiableCollection(publicUrls);
    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        try {
            String requestURI = httpServletRequest.getRequestURI();
            LOGGER.debug("RequestLogger, uri={}, Method={}, remoteIp={}, userAgent={}", httpServletRequest.getRequestURI(), httpServletRequest.getMethod(), httpServletRequest.getRemoteAddr(), httpServletRequest.getHeader(HttpHeaders.USER_AGENT));
            if (isPublicPath(requestURI)) {
                chain.doFilter(request, response);
                return;
            }
            String accessToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            Validate.isNull(accessToken).launch(new SecurityException("Can not find token"));

            //get user info
            Claims claims = jwtUtil.getAllClaimsFromToken(jwtUtil.extractToken(accessToken));
            SessionDTO sessionDTO = SessionDTO.builder()
                    .role(claims.get(StringPool.ROLE, Integer.class))
                    .enterpriseId(claims.get(StringPool.ENTERPRISE_ID, Long.class))
                    .email(claims.get(StringPool.EMAIL, String.class))
                    .build();

            httpServletRequest.setAttribute(StringPool.SESSION_DTO, sessionDTO);

            chain.doFilter(request, response);
        } catch (SecurityException e) {
            LOGGER.debug("security exception ", e);
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


    /**
     * Is public path boolean.
     *
     * @param requestURI the request uri
     * @return the boolean
     */
    public boolean isPublicPath(String requestURI) {
        for (String path : publicUrls) {
            if (antPathMatcher.match(path, requestURI)) {
                return true;
            }
        }
        return false;
    }
}
