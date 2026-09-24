package com.pedidos360.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class CurrentUserService {

    public String resolveUserId(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String oid = jwt.getClaimAsString("oid");
            if (StringUtils.hasText(oid)) {
                return oid;
            }
            String sub = jwt.getSubject();
            if (StringUtils.hasText(sub)) {
                return sub;
            }
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (StringUtils.hasText(preferredUsername)) {
                return preferredUsername;
            }
        }

        if (request != null) {
            String headerUser = request.getHeader("X-User-Id");
            if (StringUtils.hasText(headerUser)) {
                return headerUser;
            }
            String demoUser = request.getHeader("X-Demo-User");
            if (StringUtils.hasText(demoUser)) {
                return demoUser;
            }
        }

        return "default-user";
    }
}
