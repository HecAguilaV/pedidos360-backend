package com.pedidos360.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final String securityMode;

    public AuthController(@Value("${spring.profiles.active:dev}") String securityMode) {
        this.securityMode = securityMode;
    }

    public record UserInfoResponse(
        boolean authenticated,
        String userId,
        String name,
        List<String> scopes,
        List<String> roles
    ) {}

    @GetMapping("/me")
    public UserInfoResponse me(Authentication authentication, HttpServletRequest request) {
        if ("dev".equalsIgnoreCase(securityMode) && (authentication == null || !(authentication.getPrincipal() instanceof Jwt))) {
            String userId = Optional.ofNullable(request.getHeader("X-Demo-User"))
                .filter(StringUtils::hasText)
                .orElse("demo-user-123");
            String name = Optional.ofNullable(request.getHeader("X-Demo-Name"))
                .filter(StringUtils::hasText)
                .orElse("Héctor Águila (Dev Local)");
            return new UserInfoResponse(true, userId, name, List.of("pedidos360.read", "pedidos360.write"), List.of("ROLE_ADMIN"));
        }

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = Optional.ofNullable(jwt.getClaimAsString("oid"))
                .filter(StringUtils::hasText)
                .orElse(jwt.getSubject());
            String name = Optional.ofNullable(jwt.getClaimAsString("name"))
                .filter(StringUtils::hasText)
                .orElse(Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(userId));

            List<String> scopes = parseScopes(jwt.getClaimAsString("scp"));
            List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                .orElse(Collections.emptyList());

            return new UserInfoResponse(true, userId, name, scopes, roles);
        }

        return new UserInfoResponse(false, "anonymous", "Invitado", Collections.emptyList(), Collections.emptyList());
    }

    private List<String> parseScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) return Collections.emptyList();
        return Arrays.stream(scopes.split("\\s+"))
            .filter(StringUtils::hasText)
            .toList();
    }
}
