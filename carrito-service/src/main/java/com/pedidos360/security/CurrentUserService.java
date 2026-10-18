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
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (StringUtils.hasText(preferredUsername)) {
                return preferredUsername.trim();
            }
            String email = jwt.getClaimAsString("email");
            if (StringUtils.hasText(email)) {
                return email.trim();
            }
            String oid = jwt.getClaimAsString("oid");
            if (StringUtils.hasText(oid)) {
                return oid;
            }
            String sub = jwt.getSubject();
            if (StringUtils.hasText(sub)) {
                return sub;
            }
        }

        if (request != null) {
            String headerUser = request.getHeader("X-User-Id");
            if (StringUtils.hasText(headerUser)) {
                return headerUser.trim();
            }
            String demoUser = request.getHeader("X-Demo-User");
            if (StringUtils.hasText(demoUser)) {
                return demoUser.trim();
            }
        }

        return "default-user";
    }
}

